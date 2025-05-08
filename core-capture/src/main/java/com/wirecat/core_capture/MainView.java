// src/main/java/com/wirecat/core_capture/MainView.java
package com.wirecat.core_capture;

<<<<<<< Updated upstream
import javafx.animation.AnimationTimer;
=======
import com.wirecat.core_capture.inspector.PacketInspector;
>>>>>>> Stashed changes
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
<<<<<<< Updated upstream
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
=======
import javafx.geometry.*;
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
import java.io.FileWriter;
import java.net.URL;
import java.util.function.Predicate;
=======
>>>>>>> Stashed changes

public class MainView {
    /* ---------- state ---------- */
    private final ObservableList<CapturedPacket> packets = FXCollections.observableArrayList();
    private final FilteredList<CapturedPacket> view = new FilteredList<>(packets, p -> true);
    private final CaptureService svc;
    private final XYChart.Series<String, Number> protoSeries = new XYChart.Series<>();

    private TableView<CapturedPacket> table;
    private String selectedProto = "All";
    private String searchText = "";
    private boolean autoScroll = true;

    public MainView(CaptureService svc) {
<<<<<<< Updated upstream
        this.svc = svc;
        protoSeries.setName("Protocols");
=======
        this.captureService = svc;
        protocolSeries.setName("Protocols");
>>>>>>> Stashed changes
    }

    /* ---------- UI ---------- */
    public void show(Stage stage) {
<<<<<<< Updated upstream
        /* -------- top bar -------- */
        Label title = new Label("WIRECAT");
        title.getStyleClass().add("title-label");

        ComboBox<String> protoFilter = new ComboBox<>();
        protoFilter.getItems().addAll("All", "TCP", "UDP", "ICMP", "ARP");
        protoFilter.setValue("All");
        protoFilter.valueProperty().addListener((o, old, v) -> {
            selectedProto = v;
            refreshPredicate();
        });

        TextField search = new TextField();
        search.setPromptText("Search IP / MAC / Port …");
        search.textProperty().addListener((o, old, v) -> {
            searchText = v.trim().toLowerCase();
            refreshPredicate();
        });
        search.setPrefWidth(200);

        CheckBox auto = new CheckBox("Auto‑scroll");
        auto.setSelected(true);
        auto.selectedProperty().addListener((o, old, v) -> autoScroll = v);

        HBox top = new HBox(10, title, new Region(), search, protoFilter, auto);
        HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS);
        top.setPadding(new Insets(10));

        /* -------- left bar -------- */
        Button settings = new Button("Settings");
        settings.setOnAction(e -> {
            svc.stop();
            new SettingsView().show(stage);
        });
        Button stop = new Button("Stop");
        stop.setOnAction(e -> svc.stop());
        VBox left = new VBox(12, settings, stop);
        left.setPadding(new Insets(15));

        /* -------- table -------- */
        table = buildTable();
        table.setItems(view);
        table.setFixedCellSize(24);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ContextMenu ctx = new ContextMenu();
        MenuItem copy = new MenuItem("Copy row(s)");
        copy.setOnAction(e -> copyRows());
        ctx.getItems().add(copy);
        table.setContextMenu(ctx);

        /* -------- dump pane -------- */
        TextArea dump = new TextArea();
        dump.setEditable(false);
        dump.setWrapText(false);
        table.getSelectionModel().selectedItemProperty().addListener((o, old, sel) -> {
            if (sel != null) dump.setText("Hex Dump:\n" + sel.getHexDump() + "\n\nASCII:\n" + sel.getAsciiDump());
=======
        // Top bar
        Label title = new Label("WIRECAT");
        title.getStyleClass().add("title-label");
        ComboBox<String> filter = new ComboBox<>(FXCollections.observableArrayList("All","TCP","UDP","ICMP"));
        filter.setValue("All");
        filter.getStyleClass().add("protocol-combo");
        filter.setOnAction(e -> applyFilter(filter.getValue()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(10, title, spacer, filter);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));

        // Left pane
        Button back = new Button("Settings");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> { captureService.stopCapture(); new SettingsView().show(stage); });
        Button stop = new Button("Stop");
        stop.getStyleClass().add("stop-button");
        stop.setOnAction(e -> captureService.stopCapture());
        VBox left = new VBox(12, back, stop);
        left.setAlignment(Pos.TOP_CENTER);
        left.setPadding(new Insets(15));

        // Hex + inspector
        TextArea hexAscii = new TextArea();
        hexAscii.setEditable(false);
        hexAscii.setWrapText(true);
        hexAscii.getStyleClass().add("details-area");
        PacketInspector inspector = new PacketInspector();

        // Table
        table = new TableView<>(packetList);
        String[] hdrs = {"No","Time","Source","Destination","Protocol","Length"};
        String[] props= {"no","time","src","dst","proto","len"};
        for (int i=0;i<hdrs.length;i++) {
            TableColumn<Packet,?> col = new TableColumn<>(hdrs[i]);
            col.setCellValueFactory(new PropertyValueFactory<>(props[i]));
            col.setPrefWidth(140);
            table.getColumns().add(col);
        }

        // Split detail + inspector
        VBox detailBox = new VBox(hexAscii, inspector.getNode());
        detailBox.setSpacing(10);
        detailBox.setPadding(new Insets(8));
        SplitPane center = new SplitPane(table, detailBox);
        center.setOrientation(Orientation.VERTICAL);
        center.setDividerPositions(0.6);

        // Selection listener
        table.getSelectionModel().selectedItemProperty().addListener((obs,old,sel)->{
            if (sel!=null) {
                hexAscii.setText(sel.getHexDump()+"\n\n"+sel.getAsciiDump());
                PacketModel pm = captureService.getRawPacketByIndex(sel.getNo());
                if (pm!=null) {
                    PacketDetail pd = PacketModel.dissect(pm.getRawPacket());
                    inspector.display(pd);
                }
            }
>>>>>>> Stashed changes
        });
        TabPane tabs = new TabPane(new Tab("Hex / ASCII", dump));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

<<<<<<< Updated upstream
        SplitPane vertical = new SplitPane(table, tabs);
        vertical.setOrientation(Orientation.VERTICAL);
        vertical.setDividerPositions(.55);

        /* -------- stats pane -------- */
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.getData().add(protoSeries);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        Label total = new Label();
        total.textProperty().bind(Bindings.size(packets).asString("Total Packets: %d"));

        Button savePcap = new Button("Export PCAP");
        savePcap.setOnAction(e -> savePcap(stage));
        Button saveCsv = new Button("Export CSV");
        saveCsv.setOnAction(e -> saveCsv(stage));

        VBox right = new VBox(20, new Label("Statistics"), chart, total, savePcap, saveCsv);
        right.setPadding(new Insets(15));
        right.setPrefWidth(260);

        /* -------- status bar -------- */
        Label status = new Label("Idle");
        svc.onStatus(msg -> Platform.runLater(() -> status.setText(msg)));
        HBox bottom = new HBox(status);
        bottom.setPadding(new Insets(6, 10, 6, 10));
        bottom.getStyleClass().add("status-bar");

        /* -------- scene -------- */
        BorderPane root = new BorderPane(vertical, top, right, bottom, left);
        Scene sc = new Scene(root, 1400, 800);
        URL css = getClass().getResource("/dark-theme.css");
        if (css != null) sc.getStylesheets().add(css.toExternalForm());
        stage.setScene(sc);
        stage.setTitle("WireCat");
        stage.show();

        /* -------- consumer loop -------- */
        new AnimationTimer() {
            @Override public void handle(long now) {
                int burst = 0;
                while (burst++ < 500 && !svc.queue().isEmpty()) {
                    CapturedPacket cp = svc.queue().poll().toPacket();
                    packets.add(cp);
                    updateStats(cp);
                }
                if (autoScroll && !packets.isEmpty())
                    table.scrollTo(packets.size() - 1);
            }
        }.start();
    }

    /* ---------- helpers ---------- */
    private TableView<CapturedPacket> buildTable() {
        TableView<CapturedPacket> tv = new TableView<>();
        add(tv, "No", "number", 60);
        add(tv, "Time", "timestamp", 120);
        add(tv, "Src MAC", "sourceMAC", 140);
        add(tv, "Dst MAC", "destinationMAC", 140);
        add(tv, "Src IP", "sourceIP", 140);
        add(tv, "Dst IP", "destinationIP", 140);
        add(tv, "Proto", "protocol", 80);
        add(tv, "Src Port", "sourcePort", 80);
        add(tv, "Dst Port", "destinationPort", 80);
        add(tv, "Len", "length", 60);
        add(tv, "Risk", "riskScore", 60);
        return tv;
    }
    private <T> void add(TableView<CapturedPacket> t, String title, String prop, int w) {
        TableColumn<CapturedPacket, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        t.getColumns().add(c);
    }

    private void refreshPredicate() {
        Predicate<CapturedPacket> protoPred = "All".equals(selectedProto) ? p -> true
                : p -> p.getProtocol().equals(selectedProto);
        Predicate<CapturedPacket> searchPred;
        if (searchText.isEmpty()) searchPred = p -> true;
        else searchPred = p -> (p.getSourceIP() + p.getDestinationIP() + p.getSourceMAC() + p.getDestinationMAC()
                + p.getSourcePort() + p.getDestinationPort()).toLowerCase().contains(searchText);

        view.setPredicate(protoPred.and(searchPred));
    }

    private void updateStats(CapturedPacket p) {
        for (XYChart.Data<String, Number> d : protoSeries.getData()) {
            if (d.getXValue().equals(p.getProtocol())) {
                d.setYValue(d.getYValue().intValue() + 1);
                return;
            }
        }
        protoSeries.getData().add(new XYChart.Data<>(p.getProtocol(), 1));
    }

    private void savePcap(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PCAP", "*.pcap"));
        File f = fc.showSaveDialog(stage);
        if (f != null) svc.save(f);
    }

    private void saveCsv(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showSaveDialog(stage);
        if (f == null) return;
        try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
            w.write("No,Time,SrcMAC,DstMAC,SrcIP,DstIP,Proto,SrcPort,DstPort,Len,Risk\n");
            for (CapturedPacket p : packets) {
                w.write(String.format("%d,%s,%s,%s,%s,%s,%s,%d,%d,%d,%f\n",
                        p.getNumber(), p.getTimestamp(), p.getSourceMAC(), p.getDestinationMAC(),
                        p.getSourceIP(), p.getDestinationIP(), p.getProtocol(),
                        p.getSourcePort(), p.getDestinationPort(), p.getLength(), p.getRiskScore()));
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "CSV export failed: " + ex.getMessage()).show();
        }
    }

    private void copyRows() {
        ObservableList<CapturedPacket> sel = table.getSelectionModel().getSelectedItems();
        if (sel.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (CapturedPacket p : sel) {
            sb.append(p.getNumber()).append('\t').append(p.getTimestamp()).append('\t')
                    .append(p.getSourceIP()).append(" → ")
                    .append(p.getDestinationIP()).append('\t').append(p.getProtocol()).append('\n');
        }
        ClipboardContent cc = new ClipboardContent();
        cc.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(cc);
=======
        // Right pane: stats + save
        CategoryAxis x = new CategoryAxis(); x.setLabel("Protocol");
        NumberAxis y = new NumberAxis();    y.setLabel("Count");
        BarChart<String,Number> chart = new BarChart<>(x,y);
        chart.getData().add(protocolSeries);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.getStyleClass().add("stats-chart");
        Label total = new Label("Total Packets: 0");
        total.getStyleClass().add("total-label");
        Button save = new Button("Save As…");
        save.getStyleClass().add("save-button");
        save.setOnAction(e->{
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Packet Log");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PCAP Files","*.pcap"));
            File f = fc.showSaveDialog(stage);
            if (f!=null) captureService.saveCapture(f);
        });
        VBox right = new VBox(20, new Label("Statistics"), chart, total, save);
        right.getStyleClass().add("right-pane");
        right.setPadding(new Insets(15));
        right.setPrefWidth(240);

        // Layout
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(left);
        root.setCenter(center);
        root.setRight(right);

        Scene scene = new Scene(root,1000,600);
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("WireCat");
        stage.show();

        // Hook capture → UI
        captureService.setOnPacketCaptured(m -> Platform.runLater(() -> {
            Packet uiPkt = m.toPacket();
            packetList.add(uiPkt);
            updateStats(uiPkt);
            total.setText("Total Packets: " + packetList.size());
        }));
    }

    private void updateStats(Packet p) {
        for (XYChart.Data<String,Number> d : protocolSeries.getData()) {
            if (d.getXValue().equals(p.getProto())) {
                d.setYValue(d.getYValue().intValue()+1);
                return;
            }
        }
        protocolSeries.getData().add(new XYChart.Data<>(p.getProto(),1));
    }

    private void applyFilter(String proto) {
        if ("All".equals(proto)) table.setItems(packetList);
        else table.setItems(packetList.filtered(p->p.getProto().equals(proto)));
>>>>>>> Stashed changes
    }
}
