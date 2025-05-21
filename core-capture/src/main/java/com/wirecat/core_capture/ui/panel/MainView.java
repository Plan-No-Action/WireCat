package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.CapturedPacket;
import com.wirecat.core_capture.ui.panel.SidebarPanel;
import com.wirecat.core_capture.ui.panel.TopBarPanel;
import com.wirecat.core_capture.ui.panel.TablePanel;
import com.wirecat.core_capture.model.PacketModel;
import com.wirecat.core_capture.service.CaptureService;
import com.wirecat.core_capture.service.GeminiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;

public class MainView {
    private final ObservableList<CapturedPacket> packets = FXCollections.observableArrayList();
    private final FilteredList<CapturedPacket> view = new FilteredList<>(packets, p -> true);
    private final CaptureService svc;

    private CheckBox autoScrollToggle;
    private HBox filterChips;
    private TextField searchField;
    private TablePanel tablePanel;

    private final XYChart.Series<String, Number> protoSeries = newSeries("Protocols");
    private final XYChart.Series<Number, Number> timeSeries = newSeries("Packets/sec");
    private ScheduledExecutorService scheduler;

    public MainView(CaptureService svc) {
        this.svc = svc;
    }

    private void refreshPredicate() {
        Predicate<CapturedPacket> protoFilter = p -> 
            filterChips.getChildren().stream()
                .filter(node -> ((CheckBox) node).isSelected())
                .anyMatch(node -> ((CheckBox) node).getText().equalsIgnoreCase(p.getProtocol()));
        
        Predicate<CapturedPacket> searchFilter = p -> 
            p.getSourceIP().contains(searchField.getText()) ||
            p.getDestinationIP().contains(searchField.getText()) ||
            String.valueOf(p.getSourcePort()).contains(searchField.getText()) ||
            String.valueOf(p.getDestinationPort()).contains(searchField.getText());

        view.setPredicate(protoFilter.and(searchFilter));
    }

    public void show(Stage stage) {
        // Sparkline setup
        LineChart<Number, Number> spark = createSparkline();
        List<String> protoList = List.of("TCP", "UDP", "ICMP", "ARP", "HTTP", "HTTPS");

        TopBarPanel topBar = new TopBarPanel(
                spark,
                protoList,
                searchText -> { /* implement search filter logic */ },
                selectedProtocols -> { /* implement protocol filter logic */ },
                autoScroll -> { /* implement auto-scroll logic */ },
                () -> { /* implement AI action logic */ }
        );


        this.tablePanel = new TablePanel(view, selected -> {
            // ...
        });


        // Left controls
        // Left controls (Sidebar)
        SidebarPanel sidebar = new SidebarPanel(stage, svc, () -> new SettingsView().show(stage));

        // Inspector pane
        SplitPane center = createCenterPane();
        
        // Statistics pane
        VBox right = createRightPane(stage);
        
        // Status bar
        HBox bottom = createStatusBar();
        
        // Main scene setup
        setupMainScene(stage, topBar, sidebar, center, right, bottom);

        // Start capture
        startCapture();
    }

    private LineChart<Number, Number> createSparkline() {
        LineChart<Number, Number> spark = new LineChart<>(new NumberAxis(), new NumberAxis());
        spark.getData().add(timeSeries);
        spark.setLegendVisible(false);
        spark.setAnimated(false);
        spark.setPrefHeight(80);
        return spark;
    }

    private Button createAIButton() {
        Button aiBtn = new Button("🤖 Ask AI");
        aiBtn.setOnAction(e -> {
            CapturedPacket sel = tablePanel.getTableView().getSelectionModel().getSelectedItem();
            if (sel == null) return;
            
            String analysisPrompt = String.format(
                "Analyze this network packet and explain it to a network administrator:\n" +
                "Protocol: %s\nSource: %s:%d\nDestination: %s:%d\n" +
                "Size: %d bytes\nRisk Score: %.1f\nHex Start: %s",
                sel.getProtocol(),
                sel.getSourceIP(), sel.getSourcePort(),
                sel.getDestinationIP(), sel.getDestinationPort(),
                sel.getLength(), sel.getRiskScore(),
                sel.getHexDump().substring(0, Math.min(50, sel.getHexDump().length()))
            );

            CompletableFuture.supplyAsync(() -> {
                try {
                    return GeminiClient.analyzePacket(analysisPrompt);
                } catch (Exception ex) {
                    return "❌ Analysis failed: " + ex.getMessage();
                }
            }).thenAccept(summary -> Platform.runLater(() -> {
                TextArea content = new TextArea(summary);
                content.setWrapText(true);
                content.setEditable(false);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Packet Analysis");
                alert.getDialogPane().setContent(content);
                alert.setResizable(true);
                alert.getDialogPane().setPrefSize(600, 400);
                alert.showAndWait();
            }));
        });
        return aiBtn;
    }

    private SplitPane createCenterPane() {
        VBox inspectorPane = new VBox();
        inspectorPane.getChildren().add(new PacketInspector().getNode());

        SplitPane center = new SplitPane(tablePanel, inspectorPane);
        center.setOrientation(Orientation.VERTICAL);
        center.setDividerPositions(0.6);
        return center;
    }

    private VBox createRightPane(Stage stage) {
        BarChart<String, Number> chart = createBarChart();
        Label totalLabel = new Label();
        totalLabel.textProperty().bind(Bindings.size(packets).asString("Total: %d"));
        
        Button savePcap = new Button("Export PCAP"); 
        savePcap.setOnAction(e->savePcap(stage));
        
        Button saveCsv = new Button("Export CSV"); 
        saveCsv.setOnAction(e->saveCsv(stage));
        
        VBox right = new VBox(20, new Label("Statistics"), chart, totalLabel, savePcap, saveCsv);
        right.setPadding(new Insets(15)); 
        right.setPrefWidth(260);
        return right;
    }

    private HBox createStatusBar() {
        Label status = new Label("Idle");
        svc.onStatus(msg -> Platform.runLater(() -> status.setText(msg)));
        HBox bottom = new HBox(status);
        bottom.setPadding(new Insets(6,10,6,10));
        return bottom;
    }

    private void setupMainScene(Stage stage, TopBarPanel topBar, VBox sidebar, SplitPane center, VBox right, HBox bottom) {
        BorderPane root = new BorderPane(center, topBar, right, bottom, sidebar);
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/components/sidebar.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/css/components/topbar.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("WireCat");
        stage.setOnCloseRequest(e -> {
            scheduler.shutdownNow();
            svc.stopCapture();
        });
        stage.show();
    }

    private void startCapture() {
        svc.startCapture("eth0","",0);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::drainAndRender, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void drainAndRender() {
        List<PacketModel> batch = new ArrayList<>();
        svc.queue().drainTo(batch);
        if (batch.isEmpty()) return;
        
        Platform.runLater(() -> {
            for (PacketModel pm : batch) {
                CapturedPacket cp = pm.toPacket();
                if (!packets.isEmpty()) {
                    long dt = cp.getTimestampMs() - packets.get(packets.size()-1).getTimestampMs();
                    cp.setDeltaTime(dt);
                }
                packets.add(cp);
                updateStats(cp);
            }
            if (autoScrollToggle.isSelected()) tablePanel.getTableView().scrollTo(packets.size()-1);

        });
    }

    private <T> TableColumn<CapturedPacket, T> col(String title, String prop, int w) {
        TableColumn<CapturedPacket, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    private BarChart<String, Number> createBarChart() {
        BarChart<String, Number> c = new BarChart<>(new CategoryAxis(), new NumberAxis());
        c.getData().add(protoSeries);
        c.setLegendVisible(false);
        c.setAnimated(false);
        return c;
    }

    private void updateStats(CapturedPacket p) {
        for (XYChart.Data<String, Number> d : protoSeries.getData()) {
            if (d.getXValue().equals(p.getProtocol())) {
                d.setYValue(d.getYValue().intValue()+1);
                return;
            }
        }
        protoSeries.getData().add(new XYChart.Data<>(p.getProtocol(), 1));
    }

    private void savePcap(Stage s) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PCAP","*.pcap"));
        File f = fc.showSaveDialog(s);
        if (f != null) svc.save(f);
    }

    private void saveCsv(Stage s) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV","*.csv"));
        File f = fc.showSaveDialog(s);
        if (f == null) return;
        
        try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
            writeCsvHeader(w);
            writeCsvData(w);
        } catch (Exception ex) {
            Platform.runLater(() -> 
                new Alert(Alert.AlertType.ERROR, "CSV export failed: " + ex.getMessage()).show()
            );
        }
    }

    private void writeCsvHeader(BufferedWriter w) throws Exception {
        w.write("No,Time,ΔTime,SrcMAC,DstMAC,SrcIP,DstIP,Proto,SrcPort,DstPort,Len,Risk\n");
    }

    private void writeCsvData(BufferedWriter w) throws Exception {
        for (CapturedPacket p : packets) {
            w.write(String.format("%d,%s,%d,%s,%s,%s,%s,%s,%d,%d,%d,%.2f\n",
                p.getNumber(), p.getTimestamp(), p.getDeltaTime(),
                p.getSourceMAC(), p.getDestinationMAC(),
                p.getSourceIP(), p.getDestinationIP(),
                p.getProtocol(), p.getSourcePort(), p.getDestinationPort(),
                p.getLength(), p.getRiskScore()));
        }
    }

    private <X,Y> XYChart.Series<X,Y> newSeries(String name) {
        XYChart.Series<X,Y> s = new XYChart.Series<>(); 
        s.setName(name); 
        return s;
    }
}