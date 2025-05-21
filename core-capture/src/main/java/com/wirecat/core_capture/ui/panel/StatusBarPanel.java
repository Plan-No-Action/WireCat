package com.wirecat.core_capture.ui.panel;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class StatusBarPanel extends HBox {
    private final Label statusLabel;

    public StatusBarPanel() {
        getStyleClass().add("status-bar-panel");
        setSpacing(10);
        setMinHeight(32);

        statusLabel = new Label("Idle");
        statusLabel.getStyleClass().add("status-label");

        getChildren().add(statusLabel);
    }

    public void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    public String getStatus() {
        return statusLabel.getText();
    }
}
