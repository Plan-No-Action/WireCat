package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.CapturedPacket;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.function.Consumer;

public class StatisticsPanel extends VBox {
    private final Label totalLabel;
    private final BarChart<String, Number> chart;

    public StatisticsPanel(ObservableList<CapturedPacket> packets,
                           XYChart.Series<String, Number> protoSeries,
                           Stage stage,
                           Runnable onExportPcap,
                           Runnable onExportCsv) {
        getStyleClass().add("stats-panel");
        setSpacing(13);
        setPadding(new Insets(18, 20, 18, 16));
        setPrefWidth(270);

        Label statsTitle = new Label("📊 Statistics");
        statsTitle.getStyleClass().add("stats-title");

        chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.getData().add(protoSeries);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(180);
        chart.setId("stats-chart");

        totalLabel = new Label();
        totalLabel.textProperty().bind(Bindings.size(packets).asString("Total Packets: %,d"));
        totalLabel.getStyleClass().add("total-label");

        Button exportPcapBtn = new Button("💾 Export PCAP");
        exportPcapBtn.getStyleClass().add("stats-btn");
        exportPcapBtn.setMaxWidth(Double.MAX_VALUE);
        exportPcapBtn.setTooltip(new Tooltip("Export captured packets as a .pcap file"));
        exportPcapBtn.setOnAction(e -> { if (onExportPcap != null) onExportPcap.run(); });

        Button exportCsvBtn = new Button("📑 Export CSV");
        exportCsvBtn.getStyleClass().add("stats-btn");
        exportCsvBtn.setMaxWidth(Double.MAX_VALUE);
        exportCsvBtn.setTooltip(new Tooltip("Export table as .csv"));
        exportCsvBtn.setOnAction(e -> { if (onExportCsv != null) onExportCsv.run(); });

        VBox buttons = new VBox(8, exportPcapBtn, exportCsvBtn);
        buttons.setPadding(new Insets(8,0,0,0));

        getChildren().addAll(statsTitle, chart, totalLabel, buttons);
    }
}
