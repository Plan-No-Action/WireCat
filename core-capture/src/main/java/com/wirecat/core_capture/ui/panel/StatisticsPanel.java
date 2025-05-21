package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.CapturedPacket;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

public class StatisticsPanel extends VBox {
    private final Label totalLabel, rateLabel, bytesLabel, avgSizeLabel, topTalkerLabel;
    private final BarChart<String, Number> chart;
    private final Map<String, Label> protoCounters = new HashMap<>();
    private final XYChart.Series<String, Number> protoSeries;
    private final List<String> protos = Arrays.asList("TCP","UDP","ICMP","ARP","HTTP","HTTPS");

    public StatisticsPanel(ObservableList<CapturedPacket> packets,
                           XYChart.Series<String, Number> protoSeries,
                           Stage stage,
                           Runnable onExportPcap,
                           Runnable onExportCsv) {
        this.protoSeries = protoSeries;
        getStyleClass().add("stats-panel");
        setSpacing(13);
        setPadding(new Insets(18, 20, 18, 16));
        setPrefWidth(295);

        // Title Row
        Label statsTitle = new Label("📊 Statistics");
        statsTitle.getStyleClass().add("stats-title");

        HBox protocolBadges = new HBox(6);
        protocolBadges.setPadding(new Insets(6, 0, 8, 0));
        for (String proto : protos) {
            Label lbl = new Label(proto + ": 0");
            lbl.getStyleClass().addAll("proto-badge", "badge-" + proto.toLowerCase());
            protoCounters.put(proto, lbl);
            Tooltip.install(lbl, new Tooltip("Packets using " + proto));
            protocolBadges.getChildren().add(lbl);
        }

        // Bar chart: small, protocol-colored
        chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.getData().add(protoSeries);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(120);
        chart.setId("stats-chart");
        chart.setHorizontalGridLinesVisible(false);
        chart.setVerticalGridLinesVisible(false);

        // Stats
        rateLabel = new Label("Rate: 0 pkts/sec");
        rateLabel.getStyleClass().add("stats-meta");
        Tooltip.install(rateLabel, new Tooltip("Packet rate over last 10 seconds"));

        bytesLabel = new Label("Bytes: 0");
        bytesLabel.getStyleClass().add("stats-meta");
        Tooltip.install(bytesLabel, new Tooltip("Total bytes captured"));

        avgSizeLabel = new Label("Avg Size: 0 bytes");
        avgSizeLabel.getStyleClass().add("stats-meta");
        Tooltip.install(avgSizeLabel, new Tooltip("Average packet size"));

        topTalkerLabel = new Label("Top IP: N/A");
        topTalkerLabel.getStyleClass().add("stats-meta");
        Tooltip.install(topTalkerLabel, new Tooltip("Source IP with most packets"));

        // Session summary (bottom)
        totalLabel = new Label();
        totalLabel.textProperty().bind(Bindings.size(packets).asString("Total Packets: %,d"));
        totalLabel.getStyleClass().add("total-label");

        Button copyStatsBtn = new Button("📋 Copy Stats");
        copyStatsBtn.getStyleClass().add("stats-btn");
        copyStatsBtn.setOnAction(e -> copyStatsToClipboard());

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

        VBox statsMeta = new VBox(3, rateLabel, bytesLabel, avgSizeLabel, topTalkerLabel);
        statsMeta.setPadding(new Insets(4, 0, 8, 0));

        VBox buttons = new VBox(8, copyStatsBtn, exportPcapBtn, exportCsvBtn);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        getChildren().addAll(
                statsTitle,
                protocolBadges,
                chart,
                statsMeta,
                totalLabel,
                buttons
        );

        // === Live statistics update ===
        packets.addListener((ListChangeListener<CapturedPacket>) change -> updateStats(packets));
        updateStats(packets);

        // Live chart bar color updates after every change (JavaFX hack)
        protoSeries.getData().forEach(data -> {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                setBarColor(data);
            });
        });
    }

    private void updateStats(ObservableList<CapturedPacket> packets) {
        Map<String, Long> protoCounts = packets.stream().collect(Collectors.groupingBy(CapturedPacket::getProtocol, Collectors.counting()));
        for (String proto : protoCounters.keySet()) {
            long count = protoCounts.getOrDefault(proto, 0L);
            protoCounters.get(proto).setText(proto + ": " + count);
        }

        // Traffic rate: pkts/sec over last 10 seconds
        if (packets.size() > 1) {
            long now = packets.get(packets.size()-1).getTimestampMs();
            long tenSecAgo = now - 10_000;
            long count10 = packets.stream().filter(p -> p.getTimestampMs() >= tenSecAgo).count();
            rateLabel.setText("Rate: " + (count10/10) + " pkts/sec");
        } else {
            rateLabel.setText("Rate: 0 pkts/sec");
        }

        // Total bytes and average
        long totalBytes = packets.stream().mapToLong(CapturedPacket::getLength).sum();
        bytesLabel.setText("Bytes: " + totalBytes);
        avgSizeLabel.setText("Avg Size: " + (packets.isEmpty() ? 0 : totalBytes / packets.size()) + " bytes");

        // Top talker (IP with most packets)
        Map<String, Long> ipCounts = packets.stream().collect(Collectors.groupingBy(CapturedPacket::getSourceIP, Collectors.counting()));
        String topIp = ipCounts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("N/A");
        long topCnt = ipCounts.getOrDefault(topIp, 0L);
        topTalkerLabel.setText("Top IP: " + topIp + " (" + topCnt + ")");

        // Update chart bar colors after data changes
        protoSeries.getData().forEach(this::setBarColor);
    }

    private void setBarColor(XYChart.Data<String, Number> data) {
        String proto = data.getXValue();
        Node node = data.getNode();
        if (node != null && proto != null) {
            node.getStyleClass().add("chart-bar-" + proto.toLowerCase());
        }
    }

    private void copyStatsToClipboard() {
        StringBuilder sb = new StringBuilder();
        protoCounters.values().forEach(lbl -> sb.append(lbl.getText()).append("\n"));
        sb.append(rateLabel.getText()).append("\n");
        sb.append(bytesLabel.getText()).append("\n");
        sb.append(avgSizeLabel.getText()).append("\n");
        sb.append(topTalkerLabel.getText()).append("\n");
        sb.append(totalLabel.getText()).append("\n");
        ClipboardContent cc = new ClipboardContent();
        cc.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(cc);
    }
}
