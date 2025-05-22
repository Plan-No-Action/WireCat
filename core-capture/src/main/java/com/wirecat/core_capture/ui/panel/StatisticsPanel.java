package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.CapturedPacket;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.*;
import java.util.stream.Collectors;

public class StatisticsPanel extends VBox {
    private final Label totalLabel, rateLabel;
    private final BarChart<String, Number> chart;
    private final XYChart.Series<String, Number> protoSeries;

    public StatisticsPanel(
            ObservableList<CapturedPacket> packets,
            XYChart.Series<String, Number> protoSeries,
            Stage stage
    ) {
        getStyleClass().add("stats-panel");
        setSpacing(8);
        setPadding(new Insets(10, 10, 10, 10));
        setFillWidth(true);

        this.protoSeries = protoSeries;

        // --- Traffic Chart (expands with parent) ---
        chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.getData().add(protoSeries);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setId("stats-chart");
        chart.setHorizontalGridLinesVisible(false);
        chart.setVerticalGridLinesVisible(false);
        chart.setMinHeight(40); // avoid vanishing
        chart.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(chart, Priority.ALWAYS);

        // --- Minimal Info Row ---
        rateLabel = new Label();
        rateLabel.getStyleClass().add("stats-meta");

        // --- Packet Count Row ---
        totalLabel = new Label();
        totalLabel.textProperty().bind(Bindings.size(packets).asString("Total: %,d"));
        totalLabel.getStyleClass().add("total-label");

        getChildren().addAll(chart, rateLabel, totalLabel);
        VBox.setVgrow(chart, Priority.ALWAYS);

        // === Live updates ===
        packets.addListener((ListChangeListener<CapturedPacket>) change -> updateStats(packets));
        updateStats(packets);

        // Chart bar color update
        protoSeries.getData().forEach(data ->
                data.nodeProperty().addListener((obs, oldNode, newNode) -> setBarColor(data))
        );
    }

    private void updateStats(ObservableList<CapturedPacket> packets) {
        String rate = "0";
        if (packets.size() > 1) {
            long now = packets.get(packets.size()-1).getTimestampMs();
            long tenSecAgo = now - 10_000;
            long count10 = packets.stream().filter(p -> p.getTimestampMs() >= tenSecAgo).count();
            rate = String.valueOf(count10 / 10);
        }
        rateLabel.setText("Rate: " + rate + " pkts/s");
        protoSeries.getData().forEach(this::setBarColor);
    }

    private void setBarColor(XYChart.Data<String, Number> data) {
        String proto = data.getXValue();
        Node node = data.getNode();
        if (node != null && proto != null) {
            node.getStyleClass().add("chart-bar-" + proto.toLowerCase());
        }
    }
}
