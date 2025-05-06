package com.wirecat.core_capture;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainView {

    private final ObservableList<Packet> packetList = FXCollections.observableArrayList();
    private final CaptureService captureService;
    private final XYChart.Series<String, Number> protocolSeries = new XYChart.Series<>();
    private TableView<Packet> table;

    public MainView(CaptureService captureService) {
        this.captureService = captureService;
        protocolSeries.setName("Protocols");
    }

    public void show(Stage stage) {
        // --- Top bar: title, spacer, filter ---
        Label title = new Label("WIRECAT");
        title.getStyleClass().add("title-label");

        ComboBox<String> protocolFilter = new ComboBox<>();
        protocolFilter.getItems().addAll("All", "TCP", "UDP", "ICMP");
        protocolFilter.setValue("All");
        protocolFilter.getStyleClass().add("protocol-combo");
        protocolFilter.setOnAction(e -> applyFilter(protocolFilter.getValue()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, title, spacer, protocolFilter);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));

        // --- Left pane: navigation & stop ---
        Button backBtn = new Button("Settings");
        backBtn.getStyleClass().add("secondary-button");
        backBtn.setOnAction(e -> {
            captureService.stopCapture();
            new SettingsView().show(stage);
        });

        Button stopBtn = new Button("Stop");
        stopBtn.getStyleClass().add("stop-button");
        stopBtn.setOnAction(e -> captureService.stopCapture());

        VBox leftPane = new VBox(12, backBtn, stopBtn);
        leftPane.setAlignment(Pos.TOP_CENTER);
        leftPane.setPadding(new Insets(15));

        // --- Center: table + detail ---
        table = buildPacketTable();
        TextArea hexAscii = new TextArea();
        hexAscii.setEditable(false);
        hexAscii.setWrapText(true);
        hexAscii.getStyleClass().add("details-area");
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                hexAscii.setText(sel.getHexDump() + "\n\n" + sel.getAsciiDump());
            }
        });

        SplitPane centerPane = new SplitPane(table, hexAscii);
        centerPane.setOrientation(Orientation.VERTICAL);
        centerPane.setDividerPositions(0.6);

        // --- Right pane: stats & save ---
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Protocol");
        NumberAxis yAxis = new NumberAxis();    yAxis.setLabel("Count");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.getData().add(protocolSeries);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.getStyleClass().add("stats-chart");

        Label total = new Label("Total Packets: 0");
        total.getStyleClass().add("total-label");

        Button saveBtn = new Button("Save As...");
        saveBtn.getStyleClass().add("save-button");
        saveBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();            // ← Usage now compiles
            fc.setTitle("Save Packet Log");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PCAP Files","*.pcap"));
            File f = fc.showSaveDialog(stage);
            if (f != null) captureService.saveCapture(f);
        });

        VBox rightPane = new VBox(20, new Label("Statistics"), chart, total, saveBtn);
        rightPane.getStyleClass().add("right-pane");
        rightPane.setPadding(new Insets(15));
        rightPane.setPrefWidth(240);

        // --- Layout root ---
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(leftPane);
        root.setCenter(centerPane);
        root.setRight(rightPane);

        Scene scene = new Scene(root, 1000, 600);
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("WireCat");
        stage.show();

        // --- Packet callback ---
        captureService.setOnPacketCaptured(model -> Platform.runLater(() -> {
            Packet p = model.toPacket();
            packetList.add(p);
            updateStats(p);
            total.setText("Total Packets: " + packetList.size());
        }));
    }

    private TableView<Packet> buildPacketTable() {
        TableView<Packet> tv = new TableView<>(packetList);
        String[] hdrs = {"No","Time","Source","Destination","Protocol","Length"};
        String[] props = {"no","time","src","dst","proto","len"};
        for (int i = 0; i < hdrs.length; i++) {
            TableColumn<Packet, ?> c = new TableColumn<>(hdrs[i]);
            c.setCellValueFactory(new PropertyValueFactory<>(props[i]));
            c.setPrefWidth(140);
            tv.getColumns().add(c);
        }
        return tv;
    }

    private void updateStats(Packet p) {
        boolean found = false;
        for (XYChart.Data<String, Number> d : protocolSeries.getData()) {
            if (d.getXValue().equals(p.getProto())) {
                d.setYValue(d.getYValue().intValue() + 1);
                found = true;
                break;
            }
        }
        if (!found) protocolSeries.getData().add(new XYChart.Data<>(p.getProto(), 1));
    }

    private void applyFilter(String proto) {
        if ("All".equals(proto)) table.setItems(packetList);
        else table.setItems(packetList.filtered(p -> p.getProto().equals(proto)));
    }
}
