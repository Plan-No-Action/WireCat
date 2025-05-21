package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.service.CaptureService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class SidebarPanel extends VBox {

    private final Button btnDashboard;
    private final Button btnSettings;
    private final Button btnAbout;
    private final Button btnDocs;
    private final Button btnStop;
    private final Button btnExport;
    private final Button btnFeedback;

    public SidebarPanel(Stage stage, CaptureService svc, Runnable onSettings) {
        getStyleClass().add("sidebar-panel");
        setSpacing(10);
        setPadding(new Insets(20, 8, 20, 8));
        setPrefWidth(180);
        setAlignment(Pos.TOP_CENTER);

        // 1. Logo/Brand
        Label logo = new Label("🐾 WireCat");
        logo.getStyleClass().add("sidebar-logo");
        logo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        logo.setPadding(new Insets(8,0,18,0));

        // 2. Navigation section
        btnDashboard = new SidebarButton("🏠", "Dashboard");
        btnDashboard.setOnAction(e -> {
            // TODO: Future home/dashboard view
        });

        btnSettings = new SidebarButton("⚙️", "Settings");
        btnSettings.setOnAction(e -> {
            svc.stopCapture();
            if (onSettings != null) onSettings.run();
        });

        btnAbout = new SidebarButton("ℹ️", "About");
        btnAbout.setOnAction(e -> {
            // TODO: Open about dialog
        });

        btnDocs = new SidebarButton("📚", "Docs");
        btnDocs.setOnAction(e -> {
            // TODO: Open documentation, maybe external link
        });

        // 3. Separator
        Separator sep1 = new Separator();
        sep1.setPrefWidth(130);

        // 4. Actions section
        btnStop = new SidebarButton("⏹️", "Stop Capture");
        btnStop.getStyleClass().add("sidebar-stop-btn");
        btnStop.setOnAction(e -> svc.stopCapture());

        btnExport = new SidebarButton("💾", "Export");
        btnExport.setOnAction(e -> {
            // TODO: Open export menu/dialog
        });

        btnFeedback = new SidebarButton("✉️", "Feedback");
        btnFeedback.setOnAction(e -> {
            // TODO: Open feedback form/modal
        });

        // 5. Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // 6. Profile (optional)
        Label userProfile = new Label("Guest\nuser@wirecat");
        userProfile.getStyleClass().add("sidebar-profile");
        userProfile.setAlignment(Pos.CENTER_LEFT);
        userProfile.setPadding(new Insets(12, 6, 6, 6));
        userProfile.setStyle("-fx-text-fill: #888;-fx-font-size:12px;");

        getChildren().addAll(
                logo,
                btnDashboard,
                btnSettings,
                btnAbout,
                btnDocs,
                sep1,
                btnStop,
                btnExport,
                btnFeedback,
                spacer,
                userProfile
        );
    }

    // Custom Button: Icon + Text, Modern Styles
    private static class SidebarButton extends Button {
        public SidebarButton(String icon, String text) {
            super(icon + "  " + text);
            getStyleClass().add("sidebar-btn");
            setPrefWidth(160);
            setMinHeight(36);
            setAlignment(Pos.CENTER_LEFT);
        }
    }
}
