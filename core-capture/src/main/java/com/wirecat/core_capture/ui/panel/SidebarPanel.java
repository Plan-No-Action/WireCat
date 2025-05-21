package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.service.CaptureService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class SidebarPanel extends VBox {
    public SidebarPanel(Stage stage, CaptureService svc, Runnable onSettings) {
        getStyleClass().add("sidebar-panel");
        setSpacing(18);
        setPadding(new Insets(18, 12, 18, 12));

        // Network interface selector (placeholder values)
        Label ifaceLabel = new Label("Interface");
        ifaceLabel.getStyleClass().add("section-label");
        ComboBox<String> ifaceCombo = new ComboBox<>();
        ifaceCombo.getStyleClass().add("interface-selector");
        ifaceCombo.getItems().addAll("eth0", "wlan0", "lo");
        ifaceCombo.setValue("eth0");

        // Start/Stop buttons
        Button startBtn = new Button("Start Capture");
        startBtn.getStyleClass().add("sidebar-btn");
        startBtn.setOnAction(e -> svc.startCapture(ifaceCombo.getValue(), "", 0));

        Button stopBtn = new Button("Stop Capture");
        stopBtn.getStyleClass().add("sidebar-btn");
        stopBtn.setOnAction(e -> svc.stopCapture());

        // Settings button
        Button settingsBtn = new Button("Settings");
        settingsBtn.getStyleClass().add("sidebar-btn");
        settingsBtn.setOnAction(e -> {
            svc.stopCapture();
            if (onSettings != null) onSettings.run();
        });

        // (Optional) Filter section
        Label filterLabel = new Label("Filters");
        filterLabel.getStyleClass().add("section-label");
        // Placeholder for future filter controls
        VBox filterSection = new VBox();
        filterSection.setSpacing(8);
        filterSection.setMinHeight(40);

        getChildren().addAll(
                ifaceLabel, ifaceCombo,
                startBtn, stopBtn,
                new Separator(),
                settingsBtn,
                new Separator(),
                filterLabel, filterSection
        );
    }
}
