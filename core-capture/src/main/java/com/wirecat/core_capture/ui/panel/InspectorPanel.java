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
    private final Label protoBadge;
    private final GridPane detailsGrid;
    private final TextArea hexArea;
    private final Button copyAllBtn;

    public InspectorPanel() {
        getStyleClass().add("inspector-panel");
        setSpacing(14);
        setPadding(new Insets(16));
        setMinHeight(220);

        headerLabel = new Label("No packet selected");
        headerLabel.getStyleClass().add("inspector-header");

        protoBadge = new Label("");
        protoBadge.getStyleClass().add("proto-badge");

        detailsGrid = new GridPane();
        detailsGrid.setHgap(8);
        detailsGrid.setVgap(4);

        hexArea = new TextArea();
        hexArea.setEditable(false);
        hexArea.setWrapText(false);
        hexArea.setPrefRowCount(9);
        hexArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
        hexArea.getStyleClass().add("hex-dump");

        copyAllBtn = new Button("📋 Copy All");
        copyAllBtn.setOnAction(e -> copyAll());

        getChildren().addAll(headerLabel, protoBadge, detailsGrid, new Label("Hex Dump:"), hexArea, copyAllBtn);
    }

    public void showPacket(CapturedPacket packet) {
        detailsGrid.getChildren().clear();
        protoBadge.setText("");
        protoBadge.setStyle("");

        if (packet == null) {
            headerLabel.setText("No packet selected");
            hexArea.setText("");
            return;
        }
        headerLabel.setText("Packet #" + packet.getNumber() + " — " + packet.getProtocol());

        // Protocol color badge (match TablePanel colors)
        switch (packet.getProtocol()) {
            case "TCP" -> protoBadge.setStyle("-fx-background-color:#0094ff;-fx-text-fill:#fff;-fx-font-weight:bold;-fx-padding:2 10 2 10;-fx-background-radius:8;");
            case "UDP" -> protoBadge.setStyle("-fx-background-color:#32c8ff;-fx-text-fill:#1b1b1b;-fx-font-weight:bold;-fx-padding:2 10 2 10;-fx-background-radius:8;");
            case "ICMP" -> protoBadge.setStyle("-fx-background-color:#e4ff23;-fx-text-fill:#252b12;-fx-font-weight:bold;-fx-padding:2 10 2 10;-fx-background-radius:8;");
            case "ARP" -> protoBadge.setStyle("-fx-background-color:#ffaa00;-fx-text-fill:#282828;-fx-font-weight:bold;-fx-padding:2 10 2 10;-fx-background-radius:8;");
            case "HTTP" -> protoBadge.setStyle("-fx-background-color:#4caf50;-fx-text-fill:#fff;-fx-font-weight:bold;-fx-padding:2 10 2 10;-fx-background-radius:8;");
            case "HTTPS" -> protoBadge.setStyle("-fx-background-color:#2027e5;-fx-text-fill:#fff;-fx-font-weight:bold;-fx-padding:2 10 2 10;-fx-background-radius:8;");
            default -> protoBadge.setStyle("-fx-background-color:#bbbbbb;-fx-text-fill:#181e19;-fx-font-weight:bold;-fx-padding:2 10 2 10;-fx-background-radius:8;");
        }
        protoBadge.setText(packet.getProtocol());

        addField("Source IP", packet.getSourceIP() + ":" + packet.getSourcePort());
        addField("Dest IP", packet.getDestinationIP() + ":" + packet.getDestinationPort());
        addField("Source MAC", packet.getSourceMAC());
        addField("Dest MAC", packet.getDestinationMAC());
        addField("Packet Size", packet.getLength() + " bytes");

        // Risk badge
        double risk = packet.getRiskScore();
        String riskLabel;
        String riskColor;
        if (risk < 3.0)      { riskLabel = "Low";    riskColor = "#34c759"; }
        else if (risk < 7.0) { riskLabel = "Medium"; riskColor = "#ffd60a"; }
        else                 { riskLabel = "High";   riskColor = "#ff453a"; }
        Label riskBadge = new Label(riskLabel + " (" + String.format("%.1f", risk) + ")");
        riskBadge.setStyle("-fx-background-radius:8;-fx-background-color:" + riskColor + ";-fx-text-fill:#181e19;-fx-font-weight:bold;-fx-padding:2 12 2 12;");
        addField("Risk", riskBadge);

        // Hex dump, formatted with line numbers
        String[] lines = packet.getHexDump().split("\n");
        StringBuilder hexLines = new StringBuilder();
        for (int i = 0; i < lines.length; i++)
            hexLines.append(String.format("%02X: %s\n", i, lines[i]));
        hexArea.setText(hexLines.toString());
    }

    private void addField(String name, String value) {
        int row = detailsGrid.getRowCount();
        Label lbl = new Label(name + ":");
        lbl.setStyle("-fx-font-weight:bold;");
        TextField field = new TextField(value);
        field.setEditable(false);
        field.setFocusTraversable(false);
        field.setMaxWidth(290);
        Button copyBtn = new Button("📋");
        copyBtn.setFocusTraversable(false);
        copyBtn.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(value);
            Clipboard.getSystemClipboard().setContent(cc);
        });
        HBox hbox = new HBox(4, field, copyBtn);
        detailsGrid.add(lbl, 0, row);
        detailsGrid.add(hbox, 1, row);
    }

    private void addField(String name, Node valueNode) {
        int row = detailsGrid.getRowCount();
        Label lbl = new Label(name + ":");
        lbl.setStyle("-fx-font-weight:bold;");
        detailsGrid.add(lbl, 0, row);
        detailsGrid.add(valueNode, 1, row);
    }

    private void copyAll() {
        StringBuilder sb = new StringBuilder();
        for (Node n : detailsGrid.getChildren()) {
            if (n instanceof Label lbl && GridPane.getColumnIndex(lbl) == 0) {
                int row = GridPane.getRowIndex(lbl);
                Node valNode = detailsGrid.getChildren().stream()
                        .filter(x -> GridPane.getColumnIndex(x) == 1 && GridPane.getRowIndex(x) == row)
                        .findFirst().orElse(null);
                if (valNode != null) {
                    if (valNode instanceof HBox hbox && hbox.getChildren().get(0) instanceof TextField tf)
                        sb.append(lbl.getText()).append(" ").append(tf.getText()).append("\n");
                    else if (valNode instanceof Label l)
                        sb.append(lbl.getText()).append(" ").append(l.getText()).append("\n");
                }
            }
        }
        sb.append("Hex Dump:\n").append(hexArea.getText());
        ClipboardContent cc = new ClipboardContent();
        cc.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(cc);
    }

    public Node getNode() { return this; }
}
