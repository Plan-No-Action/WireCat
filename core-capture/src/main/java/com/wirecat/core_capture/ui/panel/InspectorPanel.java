package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.CapturedPacket;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;

public class InspectorPanel extends VBox {
    private final Label headerLabel;
    private final HBox badgesBox;
    private final GridPane detailsGrid;
    private final TextArea hexArea;
    private final Button copyAllBtn;

    public InspectorPanel() {
        getStyleClass().add("inspector-panel");
        setSpacing(6);
        setPadding(new Insets(14, 14, 8, 16));
        setMinHeight(120);

        // Header + badges inline
        HBox headerRow = new HBox(8);
        headerLabel = new Label("No packet selected");
        headerLabel.getStyleClass().add("inspector-header");
        badgesBox = new HBox(6);
        badgesBox.setStyle("-fx-alignment:center-right;");
        headerRow.getChildren().addAll(headerLabel, new Region(), badgesBox);
        HBox.setHgrow(headerRow.getChildren().get(1), Priority.ALWAYS);

        detailsGrid = new GridPane();
        detailsGrid.setHgap(10);
        detailsGrid.setVgap(2);

        hexArea = new TextArea();
        hexArea.setEditable(false);
        hexArea.setWrapText(false);
        hexArea.setPrefRowCount(7);
        hexArea.setPrefHeight(110);
        hexArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11.4px; -fx-background-radius:7px;");
        hexArea.getStyleClass().add("hex-dump");

        copyAllBtn = new Button("Copy All");
        copyAllBtn.setStyle("-fx-font-size:11.6px;-fx-padding:2 11 2 11;-fx-background-radius:8px;");
        copyAllBtn.setOnAction(e -> copyAll());

        setSpacing(9);
        getChildren().addAll(headerRow, detailsGrid, new Label("Hex Dump:"), hexArea, copyAllBtn);
    }

    public void showPacket(CapturedPacket packet) {
        detailsGrid.getChildren().clear();
        badgesBox.getChildren().clear();

        if (packet == null) {
            headerLabel.setText("No packet selected");
            hexArea.setText("");
            return;
        }
        headerLabel.setText("Packet #" + packet.getNumber());
        // Protocol badge
        Label protoBadge = new Label(packet.getProtocol());
        protoBadge.setStyle("-fx-background-color:" + protoColor(packet.getProtocol()) + ";-fx-text-fill:#fff;-fx-font-size:11.5px;-fx-background-radius:7px;-fx-padding:1 12 1 12;-fx-font-weight:bold;");
        badgesBox.getChildren().add(protoBadge);

        // Risk badge
        double risk = packet.getRiskScore();
        String riskLabel = (risk < 3.0) ? "Low" : (risk < 7.0 ? "Medium" : "High");
        String riskColor = (risk < 3.0) ? "#34c759" : (risk < 7.0) ? "#ffd60a" : "#ff453a";
        Label riskBadge = new Label(riskLabel + " (" + String.format("%.1f", risk) + ")");
        riskBadge.setStyle("-fx-background-color:" + riskColor + ";-fx-text-fill:#181e19;-fx-font-size:11.5px;-fx-background-radius:7px;-fx-padding:1 12 1 12;-fx-font-weight:bold;");
        badgesBox.getChildren().add(riskBadge);

        // Fields: left label, right value
        addField("Source IP", packet.getSourceIP() + ":" + packet.getSourcePort());
        addField("Dest IP", packet.getDestinationIP() + ":" + packet.getDestinationPort());
        addField("Source MAC", packet.getSourceMAC());
        addField("Dest MAC", packet.getDestinationMAC());
        addField("Packet Size", String.valueOf(packet.getLength()));
        // Risk badge also as field? (optional)

        // Hex dump format
        hexArea.setText(formatHexDump(packet.getHexDump()));
    }

    private void addField(String name, String value) {
        int row = detailsGrid.getRowCount();
        Label lbl = new Label(name + ":");
        lbl.setStyle("-fx-font-size:11.7px;-fx-font-weight:bold;-fx-text-fill:#b4dbbb;");
        Label val = new Label(value);
        val.setStyle("-fx-font-family:'Consolas',monospace;-fx-font-size:11.5px;-fx-text-fill:#e1ffe5;-fx-background-radius:6px;-fx-padding:2 8 2 8;");
        detailsGrid.add(lbl, 0, row);
        detailsGrid.add(val, 1, row);
    }

    private String protoColor(String proto) {
        return switch (proto) {
            case "TCP" -> "#0094ff";
            case "UDP" -> "#32c8ff";
            case "ICMP" -> "#e4ff23";
            case "ARP" -> "#ffaa00";
            case "HTTP" -> "#4caf50";
            case "HTTPS" -> "#2027e5";
            default -> "#bbbbbb";
        };
    }

    private String formatHexDump(String hexDump) {
        // Split bytes into lines of 16, with offset and ASCII
        String[] bytes = hexDump.replaceAll("[^0-9A-Fa-f ]", "").split(" +");
        StringBuilder out = new StringBuilder();
        int offset = 0;
        for (int i = 0; i < bytes.length; i += 16) {
            out.append(String.format("%04X: ", offset));
            for (int j = 0; j < 16; j++) {
                if (i + j < bytes.length)
                    out.append(bytes[i + j].length() == 1 ? "0" : "").append(bytes[i + j]).append(" ");
                else
                    out.append("   ");
            }
            out.append(" |");
            for (int j = 0; j < 16; j++) {
                if (i + j < bytes.length) {
                    int val = Integer.parseInt(bytes[i + j], 16);
                    char c = (val >= 32 && val < 127) ? (char) val : '.';
                    out.append(c);
                } else {
                    out.append(" ");
                }
            }
            out.append("|\n");
            offset += 16;
        }
        return out.toString();
    }

    private void copyAll() {
        StringBuilder sb = new StringBuilder();
        for (Node n : detailsGrid.getChildren()) {
            if (n instanceof Label lbl && GridPane.getColumnIndex(lbl) == 0) {
                int row = GridPane.getRowIndex(lbl);
                Node valNode = detailsGrid.getChildren().stream()
                        .filter(x -> GridPane.getColumnIndex(x) == 1 && GridPane.getRowIndex(x) == row)
                        .findFirst().orElse(null);
                if (valNode != null && valNode instanceof Label v)
                    sb.append(lbl.getText()).append(" ").append(v.getText()).append("\n");
            }
        }
        sb.append("Hex Dump:\n").append(hexArea.getText());
        ClipboardContent cc = new ClipboardContent();
        cc.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(cc);
    }
}
