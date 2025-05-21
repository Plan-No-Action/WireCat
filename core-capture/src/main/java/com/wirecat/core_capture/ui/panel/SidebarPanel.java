package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.service.CaptureService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class SidebarPanel extends VBox {
    public SidebarPanel(Stage stage, CaptureService svc, Runnable onSettings) {
        getStyleClass().add("sidebar-panel");
        setSpacing(14);
        setPadding(new Insets(18, 12, 18, 16));
        setPrefWidth(168);

        // App Title / Brand
        Label appTitle = new Label("WireCat");
        appTitle.getStyleClass().add("sidebar-title");
        appTitle.setStyle("-fx-font-size: 1.21em; -fx-font-weight: bold; -fx-text-fill: #7ffe9c; -fx-padding: 0 0 12 2;");

        // Controls section
        VBox controls = new VBox(9);
        controls.setFillWidth(true);

        Button settingsBtn = new Button("⚙️  Settings");
        settingsBtn.getStyleClass().add("sidebar-btn");
        settingsBtn.setMaxWidth(Double.MAX_VALUE);
        settingsBtn.setOnAction(e -> {
            svc.stopCapture();
            if (onSettings != null) onSettings.run();
        });

        Button stopBtn = new Button("⏹ Stop Capture");
        stopBtn.getStyleClass().addAll("sidebar-btn", "stop-btn");
        stopBtn.setMaxWidth(Double.MAX_VALUE);
        stopBtn.setOnAction(e -> svc.stopCapture());

        Button aboutBtn = new Button("❓ About");
        aboutBtn.getStyleClass().add("sidebar-btn");
        aboutBtn.setMaxWidth(Double.MAX_VALUE);
        aboutBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "WireCat Network Analyzer\n© 2025 Yacine\nVersion 1.0.0", ButtonType.OK);
            alert.setHeaderText("About");
            alert.showAndWait();
        });

        controls.getChildren().addAll(settingsBtn, stopBtn, aboutBtn);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bottom info
        VBox bottomBox = new VBox();
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        bottomBox.setSpacing(2);

        Label credit = new Label("v1.0.0  •  by Yacine");
        credit.setStyle("-fx-font-size: 10px; -fx-text-fill: #96b0a7; -fx-padding: 4 0 0 1;");

        bottomBox.getChildren().add(credit);

        // Compose
        getChildren().setAll(appTitle, controls, spacer, bottomBox);
        setStyle("-fx-background-color: #1a2022; -fx-border-color: #2a2f31; -fx-border-width: 0 1 0 0; -fx-effect: dropshadow(gaussian,#222,2,0,0,1);");
    }
}
