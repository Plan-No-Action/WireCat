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
        setSpacing(18);
        setPadding(new Insets(20, 16, 20, 18));
        setPrefWidth(180);

        // App Title / Identity
        Label appTitle = new Label("🐾 WireCat");
        appTitle.getStyleClass().add("sidebar-title");
        appTitle.setPadding(new Insets(0, 0, 16, 0));

        // Controls section
        VBox controls = new VBox(10);
        controls.setFillWidth(true);
        controls.getStyleClass().add("sidebar-controls");

        Button settingsBtn = new Button("⚙️  Settings");
        settingsBtn.getStyleClass().add("sidebar-btn");
        settingsBtn.setMaxWidth(Double.MAX_VALUE);
        settingsBtn.setOnAction(e -> {
            svc.stopCapture();
            if (onSettings != null) onSettings.run();
        });

        Button stopBtn = new Button("🛑 Stop Capture");
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

        // Filler and bottom version area
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label versionLabel = new Label("v1.0.0\nby Yacine");
        versionLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#8e9eac; -fx-padding:8 2 0 2;");
        versionLabel.setAlignment(Pos.CENTER);

        getChildren().addAll(appTitle, controls, spacer, versionLabel);
    }
}
