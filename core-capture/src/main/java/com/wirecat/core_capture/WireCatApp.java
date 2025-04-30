package com.wirecat.core_capture;
import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class WireCatApp extends Application {
    @Override
    public void start(Stage stage) {
        // === Header ===
        ImageView logo = new ImageView(new Image(getClass()
            .getResourceAsStream("/icons/cat.png"), 24, 24, true, true));
        Label title = new Label("WIRECAT", logo);
        title.getStyleClass().add("header-label");
        HBox header = new HBox(title);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        // === Controls ===
        Button startBtn = new Button("Start Capture");
        ComboBox<String> filter = new ComboBox<>();
        filter.getItems().addAll("All", "TCP", "UDP", "ICMP");
        filter.setValue("All");
        HBox controls = new HBox(startBtn, filter);
        controls.setSpacing(10);
        controls.setPadding(new Insets(10));

        // === Table ===
        TableView<Packet> table = new TableView<>();
        String[] cols = {"No", "Time", "Source", "Destination", "Protocol", "Length"};
        int i = 0;
        for (String c : cols) {
            TableColumn<Packet, ?> col = new TableColumn<>(c);
            switch (i) {
                case 0 -> col.setCellValueFactory(new PropertyValueFactory<>("no"));
                case 1 -> col.setCellValueFactory(new PropertyValueFactory<>("time"));
                case 2 -> col.setCellValueFactory(new PropertyValueFactory<>("src"));
                case 3 -> col.setCellValueFactory(new PropertyValueFactory<>("dst"));
                case 4 -> col.setCellValueFactory(new PropertyValueFactory<>("proto"));
                case 5 -> col.setCellValueFactory(new PropertyValueFactory<>("len"));
            }
            col.setPrefWidth(100);
            table.getColumns().add(col);
            i++;
        }
        table.setItems(getDummyData());

        // === Statistics Chart ===
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Protocol Distribution");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().addAll(
            new XYChart.Data<>("TCP", 55),
            new XYChart.Data<>("UDP", 30),
            new XYChart.Data<>("ICMP", 15)
        );
        chart.getData().add(series);
        chart.setLegendVisible(false);
        chart.setPrefHeight(150);

        Label totalPkts = new Label("Total Packets\n150");
        totalPkts.getStyleClass().add("section-title");
        Button saveBtn = new Button("Save As...");
        VBox stats = new VBox(new Label("Statistics"), chart, totalPkts, saveBtn);
        stats.setSpacing(8);
        stats.setPadding(new Insets(10));
        stats.getStyleClass().add("vbox");

        // === Packet Details Dump ===
        TextArea hexDump = new TextArea("00 1A 2B 3C 4D ...");
        TextArea asciiDump = new TextArea("...The...");
        hexDump.getStyleClass().add("hex-dump");
        asciiDump.getStyleClass().add("ascii-dump");
        hexDump.setPrefRowCount(4);
        asciiDump.setPrefRowCount(4);

        HBox dumpBox = new HBox(hexDump, asciiDump);
        dumpBox.setSpacing(10);
        dumpBox.setPadding(new Insets(10));

        // === Layout ===
        BorderPane root = new BorderPane();
        root.setTop(new VBox(header, controls));
        root.setCenter(table);
        root.setRight(stats);
        root.setBottom(dumpBox);

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass()
            .getResource("/css/wirecat.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("WireCat");
        stage.show();
    }

    private ObservableList<Packet> getDummyData() {
        return FXCollections.observableArrayList(
          new Packet(1, "12:30:15.879", "192.168.1.10", "192.168.1.5", "TCP", 50),
          new Packet(2, "12:30:16.120", "192.168.1.5", "192.168.1.10", "UDP", 30),
          new Packet(3, "12:30:16.123", "203.0.113.15", "192.168.1.5", "ICMP", 40),
          new Packet(4, "12:30:16.136", "203.0.113.10", "192.168.1.15", "TCP", 74)
          // …add as many rows as you like
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}

