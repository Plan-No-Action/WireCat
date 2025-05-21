package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.CapturedPacket;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class StatisticsPanel extends VBox {
    private final BarChart<String, Number> protoChart;
    private final Label totalLabel;
    private final Button savePcap;
    private final Button saveCsv;

    public StatisticsPanel(ObservableList<CapturedPacket> packets, XYChart.Series<String, Number> protoSeries, Stage stage, Runnable savePcapAction, Runnable saveCsvAction) {
        getStyleClass().add("stats-panel");
        setSpacing(20);
        setPadding(new Insets(16));
        setPrefWidth(260);

        protoChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        protoChart.getData().add(protoSeries);
        protoChart.setLegendVisible(false);
        protoChart.setAnimated(false);
        protoChart.setPrefHeight(160);
        protoChart.getStyleClass().add("stats-chart");

        totalLabel = new Label();
        totalLabel.textProperty().bind(Bindings.size(packets).asString("Total: %d"));
        totalLabel.getStyleClass().add("total-label");

        savePcap = new Button("Export PCAP");
        savePcap.getStyleClass().add("save-button");
        savePcap.setOnAction(e -> savePcapAction.run());

        saveCsv = new Button("Export CSV");
        saveCsv.getStyleClass().add("save-button");
        saveCsv.setOnAction(e -> saveCsvAction.run());

        getChildren().addAll(new Label("Statistics"), protoChart, totalLabel, savePcap, saveCsv);
    }
}
