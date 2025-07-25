package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.service.CaptureService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class SidebarPanel extends VBox {
    private final Label sessionStatus;
    private final Label interfaceLabel;
    private final Label errorStatus;
    private final Button startBtn;
    private final Button stopBtn;
    private final Button clearBtn;
    private final Button exportPcapBtn;
    private final Button exportCsvBtn;

    public SidebarPanel(Stage stage, CaptureService svc, Runnable onSettings, Runnable onStart, Runnable onStop) {
        getStyleClass().add("sidebar-panel");
        setSpacing(18);
        setPadding(new Insets(18, 14, 18, 15));
        setPrefWidth(196);

        // App Title
        Label appTitle = new Label("WireCat");
        appTitle.getStyleClass().add("sidebar-title");
        appTitle.setPadding(new Insets(2, 0, 8, 0));

        // Session Info
        interfaceLabel = new Label("Interface: N/A");
        interfaceLabel.getStyleClass().add("sidebar-meta");
        sessionStatus = new Label("Status: Stopped");
        sessionStatus.getStyleClass().add("sidebar-meta");

        // Controls
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

        startBtn = new Button("▶️ Start Capture");
        startBtn.getStyleClass().add("sidebar-btn");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setOnAction(e -> {
            if (onStart != null) onStart.run();
            updateSessionStatus("Live", "eth0");
        });

        stopBtn = new Button("⏹️ Stop Capture");
        stopBtn.getStyleClass().addAll("sidebar-btn", "stop-btn");
        stopBtn.setMaxWidth(Double.MAX_VALUE);
        stopBtn.setOnAction(e -> {
            if (onStop != null) onStop.run();
            updateSessionStatus("Stopped", null);
        });

        clearBtn = new Button("🧹 Clear Packets");
        clearBtn.getStyleClass().add("sidebar-btn");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> {
            svc.clearPackets();
            updateErrorStatus("Packets cleared");
        });

        controls.getChildren().addAll(settingsBtn, startBtn, stopBtn, clearBtn);

        // --- Export Controls ---
        exportPcapBtn = new Button("💾 Export PCAP");
        exportPcapBtn.getStyleClass().addAll("sidebar-btn", "export-btn");
        exportPcapBtn.setMaxWidth(Double.MAX_VALUE);

        exportCsvBtn = new Button("📑 Export CSV");
        exportCsvBtn.getStyleClass().addAll("sidebar-btn", "export-btn");
        exportCsvBtn.setMaxWidth(Double.MAX_VALUE);

        VBox exportBox = new VBox(7, exportPcapBtn, exportCsvBtn);
        exportBox.setPadding(new Insets(5, 0, 10, 0));
        exportBox.setFillWidth(true);

        // Error/status
        errorStatus = new Label();
        errorStatus.getStyleClass().add("sidebar-error");
        errorStatus.setPadding(new Insets(7, 0, 7, 0));

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // About/Version (Collapsible)
        TitledPane aboutPane = new TitledPane("About", new Label("WireCat Network Analyzer\nv1.0.0"));
        aboutPane.setCollapsible(true);
        aboutPane.setExpanded(false);
        aboutPane.getStyleClass().add("sidebar-about");

        // Add everything to sidebar
        getChildren().addAll(
                appTitle,
                interfaceLabel,
                sessionStatus,
                controls,
                exportBox,         // <<--- Export buttons grouped here!
                errorStatus,
                spacer,
                aboutPane
        );

        // Initial state
        updateSessionStatus("Stopped", null);
    }

    // Export button accessors for MainView
    public Button getExportPcapBtn() { return exportPcapBtn; }
    public Button getExportCsvBtn() { return exportCsvBtn; }
    public Button getClearBtn() { return clearBtn; }

    public void updateSessionStatus(String status, String iface) {
        sessionStatus.setText("Status: " + status);
        interfaceLabel.setText("Interface: " + (iface != null ? iface : "N/A"));
        sessionStatus.setStyle(
                "-fx-text-fill:" +
                        (status.equalsIgnoreCase("Live") ? "#38e37d" : "#fff176") +
                        "; -fx-font-weight:bold;"
        );
    }

    public void updateErrorStatus(String msg) {
        Platform.runLater(() -> {
            errorStatus.setText(msg == null ? "" : "⚠️ " + msg);
            errorStatus.setStyle("-fx-text-fill:#ff6767; -fx-font-size:11px; -fx-font-weight: bold;");
        });
    }
}
