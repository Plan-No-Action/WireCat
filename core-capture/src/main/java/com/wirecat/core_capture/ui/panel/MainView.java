package com.wirecat.core_capture.ui.panel;
import com.wirecat.core_capture.model.Conversation;
import com.wirecat.core_capture.model.ConversationKey;
import java.util.concurrent.ConcurrentHashMap;
import com.wirecat.core_capture.model.CapturedPacket;
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
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.beans.binding.Bindings;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import com.sandec.mdfx.MarkdownView;

public class MainView {
    private final ObservableList<CapturedPacket> packets = FXCollections.observableArrayList();
    private final FilteredList<CapturedPacket> filteredPackets = new FilteredList<>(packets, p -> true);
    private final CaptureService svc;
    private final String interfaceName;
    private final String bpfFilter;
    private final int packetLimit;

    private final Map<ConversationKey, Conversation> conversationMap = new ConcurrentHashMap<>();
    private final ObservableList<Conversation> conversationList = FXCollections.observableArrayList();

    private final XYChart.Series<String, Number> protoSeries = newSeries("Protocols");
    private final XYChart.Series<Number, Number> timeSeries = newSeries("Packets/sec");

    private ScheduledExecutorService scheduler;
    private TablePanel tablePanel;

    public MainView(CaptureService svc, String interfaceName, String bpfFilter, int packetLimit) {
        this.svc = svc;
        this.interfaceName = interfaceName;
        this.bpfFilter = bpfFilter;
        this.packetLimit = packetLimit;
    }


    public void show(Stage stage) {
        List<String> protoList = List.of("TCP", "UDP", "ICMP", "ARP", "HTTP", "HTTPS");
        LineChart<Number, Number> spark = createSparkline();

        InspectorPanel inspectorPanel = new InspectorPanel();
        this.tablePanel = new TablePanel(filteredPackets, inspectorPanel::showPacket);

        TopBarPanel topBar = new TopBarPanel(
                protoList,
                searchText -> tablePanel.filterBySearch(searchText),
                selectedProtocols -> tablePanel.filterByProtocols(selectedProtocols),
                autoScroll -> tablePanel.setAutoScroll(autoScroll),
                this::showAIAnalysisDialog
        );

        RightPanel rightPanel = new RightPanel(
                conversationList,
                this::onConversationSelected,
                packets,
                protoSeries,
                stage
        );


        SidebarPanel sidebar = new SidebarPanel(
                stage,
                svc,
                () -> new SettingsView().show(stage),
                this::startCapture,
                this::stopCapture
        );
        sidebar.getExportPcapBtn().setOnAction(e -> savePcap(stage));
        sidebar.getExportCsvBtn().setOnAction(e -> saveCsv(stage));
        sidebar.getClearBtn().setOnAction(e -> clearAllPackets());

        HBox statusBar = createStatusBar();

        SplitPane center = new SplitPane(tablePanel, inspectorPanel);
        center.setOrientation(Orientation.VERTICAL);
        center.setDividerPositions(0.6);

        BorderPane root = new BorderPane(center, topBar, rightPanel , statusBar, sidebar);
        Scene scene = new Scene(root, 1400, 900);

        addStylesheets(scene, List.of(
                "/css/dark-theme.css",
                "/css/components/sidebar.css",
                "/css/components/topbar.css",
                "/css/components/inspector.css",
                "/css/components/statusbar.css",
                "/css/components/rightpanel.css"
        ));

        stage.setScene(scene);
        stage.setTitle("WireCat");
        stage.setOnCloseRequest(e -> {
            if (scheduler != null) scheduler.shutdownNow();
            svc.stopCapture();
        });
        stage.show();

        svc.onStatus(msg -> Platform.runLater(() -> setStatus(statusBar, msg)));
        startCapture();
    }

    private void onConversationSelected(Conversation conv) {
        if (conv == null) {
            filteredPackets.setPredicate(p -> true); // Show all
            return;
        }
        filteredPackets.setPredicate(p ->
                p.getSourceIP().equals(conv.getSrcIP()) &&
                        p.getSourcePort() == conv.getSrcPort() &&
                        p.getDestinationIP().equals(conv.getDstIP()) &&
                        p.getDestinationPort() == conv.getDstPort() &&
                        p.getProtocol().equals(conv.getProto())
        );
    }


    private void startCapture() {
        svc.startCapture(interfaceName, bpfFilter, packetLimit);
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
                    long dt = cp.getTimestampMs() - packets.get(packets.size() - 1).getTimestampMs();
                    cp.setDeltaTime(dt);
                }
                packets.add(cp);
                updateStats(cp);
                // --- Conversation/session tracking ---
                ConversationKey key = new ConversationKey(cp.getSourceIP(), cp.getSourcePort(),
                        cp.getDestinationIP(), cp.getDestinationPort(), cp.getProtocol());
                Conversation conv = conversationMap.get(key);
                if (conv == null) {
                    conv = new Conversation(
                            cp.getSourceIP(), cp.getSourcePort(),
                            cp.getDestinationIP(), cp.getDestinationPort(), cp.getProtocol(),
                            cp.getTimestampMs(), cp.getLength(),
                            cp.getSourceMAC(), cp.getDestinationMAC()   // <-- add these two!
                    );
                    conversationMap.put(key, conv);
                    conversationList.add(conv);
                } else {
                    conv.addPacket(cp.getLength(), cp.getTimestampMs());
                    // To trigger update in TableView, remove and add again
                    conversationList.remove(conv);
                    conversationList.add(conv);
                }

            }
            tablePanel.scrollToBottom();
        });
    }

    // ---- UI PANELS ----
    private LineChart<Number, Number> createSparkline() {
        LineChart<Number, Number> spark = new LineChart<>(new NumberAxis(), new NumberAxis());
        spark.getData().add(timeSeries);
        spark.setLegendVisible(false);
        spark.setAnimated(false);
        spark.setPrefHeight(80);
        spark.setId("sparkline");
        return spark;
    }

    private VBox createRightPane(Stage stage) {
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.getData().add(protoSeries);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.getStyleClass().add("stats-chart");

        Label totalLabel = new Label();
        totalLabel.getStyleClass().add("total-label");
        totalLabel.textProperty().bind(Bindings.size(packets).asString("Total: %d"));

        VBox right = new VBox(20, new Label("Statistics"), chart, totalLabel);
        right.setPadding(new Insets(15));
        right.setPrefWidth(260);
        right.getStyleClass().add("right-pane");
        return right;
    }

    private HBox createStatusBar() {
        Label status = new Label("Idle");
        status.setId("status-label");
        HBox bottom = new HBox(status);
        bottom.setPadding(new Insets(6, 10, 6, 10));
        bottom.getStyleClass().add("statusbar-panel");
        return bottom;
    }

    private void setStatus(HBox statusBar, String msg) {
        if (statusBar.getChildren().size() > 0 && statusBar.getChildren().get(0) instanceof Label label) {
            label.setText(msg);
        }
    }

    // ---- Save Features ----
    private void savePcap(Stage s) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PCAP", "*.pcap"));
        File f = fc.showSaveDialog(s);
        if (f != null) svc.save(f);
    }
    private void saveCsv(Stage s) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showSaveDialog(s);
        if (f == null) return;
        try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
            w.write("No,Time,ΔTime,SrcMAC,DstMAC,SrcIP,DstIP,Proto,SrcPort,DstPort,Len,Risk\n");
            for (CapturedPacket p : packets) {
                w.write(String.format("%d,%s,%d,%s,%s,%s,%s,%s,%d,%d,%d,%.2f\n",
                        p.getNumber(), p.getTimestamp(), p.getDeltaTime(),
                        p.getSourceMAC(), p.getDestinationMAC(),
                        p.getSourceIP(), p.getDestinationIP(),
                        p.getProtocol(), p.getSourcePort(), p.getDestinationPort(),
                        p.getLength(), p.getRiskScore()));
            }
        } catch (Exception ex) {
            Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR, "CSV export failed: " + ex.getMessage()).show()
            );
        }
    }

    // ---- Stats ----
    private void updateStats(CapturedPacket p) {
        for (XYChart.Data<String, Number> d : protoSeries.getData()) {
            if (d.getXValue().equals(p.getProtocol())) {
                d.setYValue(d.getYValue().intValue() + 1);
                return;
            }
        }
        protoSeries.getData().add(new XYChart.Data<>(p.getProtocol(), 1));
    }
    private <X, Y> XYChart.Series<X, Y> newSeries(String name) {
        XYChart.Series<X, Y> s = new XYChart.Series<>();
        s.setName(name);
        return s;
    }

    // ---- Ask AI Feature ----
    private void showAIAnalysisDialog() {
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
            try { return GeminiClient.analyzePacket(analysisPrompt); }
            catch (Exception ex) { return "❌ Analysis failed: " + ex.getMessage(); }
        }).thenAccept(summary -> Platform.runLater(() -> {
            MarkdownView mdView = new MarkdownView(summary);
            mdView.setPrefSize(600, 400);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Packet Analysis (AI)");
            alert.getDialogPane().setContent(mdView);
            alert.setResizable(true);
            alert.getDialogPane().setPrefSize(600, 400);
            alert.showAndWait();
        }));
    }

    private void addStylesheets(Scene scene, List<String> stylesheets) {
        for (String css : stylesheets) {
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(css)).toExternalForm());
        }
    }

    // Clears all UI and backend packet/conversation state
    private void clearAllPackets() {
        packets.clear();
        conversationList.clear();
        conversationMap.clear();
        protoSeries.getData().clear();
        timeSeries.getData().clear();
        svc.clearPackets();
    }

    private void stopCapture() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        svc.stopCapture();
    }
}
