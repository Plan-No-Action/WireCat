package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.CapturedPacket;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public class InspectorPanel extends VBox {
    private final Label headerLabel;
    private final TabPane tabs;
    private final TextArea summaryArea;
    private final TextArea hexArea;

    public InspectorPanel() {
        getStyleClass().add("inspector-panel");
        setSpacing(10);
        setMinHeight(220);

        headerLabel = new Label("No packet selected");
        headerLabel.getStyleClass().add("header-label");

        summaryArea = new TextArea();
        summaryArea.setEditable(false);
        summaryArea.setWrapText(true);
        summaryArea.getStyleClass().add("details-area");

        hexArea = new TextArea();
        hexArea.setEditable(false);
        hexArea.setWrapText(false);
        hexArea.getStyleClass().add("hex-area");

        tabs = new TabPane();
        Tab summaryTab = new Tab("Summary", summaryArea);
        Tab hexTab = new Tab("Hex", hexArea);
        summaryTab.setClosable(false);
        hexTab.setClosable(false);

        tabs.getTabs().addAll(summaryTab, hexTab);
        getChildren().addAll(headerLabel, tabs);
    }

    public void showPacket(CapturedPacket packet) {
        if (packet == null) {
            headerLabel.setText("No packet selected");
            summaryArea.setText("");
            hexArea.setText("");
            return;
        }
        headerLabel.setText("Packet #" + packet.getNumber() + " — " + packet.getProtocol());
        summaryArea.setText(
                "Source: " + packet.getSourceIP() + ":" + packet.getSourcePort() + "\n" +
                        "Destination: " + packet.getDestinationIP() + ":" + packet.getDestinationPort() + "\n" +
                        "Length: " + packet.getLength() + " bytes\n" +
                        "Risk Score: " + packet.getRiskScore() + "\n\n"
        );
        hexArea.setText(packet.getHexDump());
    }
}
