package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.service.CaptureService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SettingsView {
    private final CaptureService captureService;
    private final List<PcapNetworkInterface> interfaces = fetchInterfaces();

    public SettingsView() {
        this.captureService = new CaptureService();
    }

    private static List<PcapNetworkInterface> fetchInterfaces() {
        try {
            return Pcaps.findAllDevs();
        } catch (Exception e) {
            return List.of();
        }
    }

    public void show(Stage stage) {
        stage.setTitle("WireCat – Settings");

        // HEADER
        Label header = new Label("Settings");
        header.getStyleClass().add("header-label");

        // --- INTERFACE PICKER ---
        Label ifaceLabel = new Label("Network Interface:");
        ComboBox<String> ifaceCombo = new ComboBox<>();
        ifaceCombo.setPromptText("Select a network interface");
        ifaceCombo.setPrefWidth(320); // Wider for details
        ObservableList<String> ifaceOptions = FXCollections.observableArrayList(
                interfaces.stream()
                        .map(dev -> {
                            String desc = dev.getDescription();
                            String name = dev.getName();
                            return desc != null ? desc + "  (" + name + ")" : name;
                        })
                        .collect(Collectors.toList())
        );
        if (ifaceOptions.isEmpty()) {
            ifaceOptions.add("❌ No interfaces detected");
            ifaceCombo.setDisable(true);
        }
        ifaceCombo.setItems(ifaceOptions);
        ifaceCombo.getSelectionModel().selectFirst();
        ifaceCombo.setTooltip(new Tooltip("Select the network interface to capture from."));

        // --- BPF FILTER ---
        Label filterLabel = new Label("BPF Filter (optional):");
        TextField filterField = new TextField();
        filterField.setPromptText("e.g., tcp port 80");
        filterField.setTooltip(new Tooltip("Use Berkeley Packet Filter syntax, e.g., \"ip and tcp\"."));
        Label filterHint = new Label("Leave empty to capture all packets.");
        filterHint.getStyleClass().add("field-hint");

        // --- PACKET LIMIT ---
        Label limitLabel = new Label("Packet Limit:");
        Spinner<Integer> limitSpinner = new Spinner<>(0, Integer.MAX_VALUE, 0, 100);
        limitSpinner.setEditable(true);
        limitSpinner.setPrefWidth(120);
        limitSpinner.setTooltip(new Tooltip("Set to 0 for unlimited packet capture."));
        HBox limitBox = new HBox(5, limitSpinner, new Label("(0 = unlimited)"));
        limitBox.setAlignment(Pos.CENTER_LEFT);

        // --- BUTTONS ---
        Button startBtn = new Button("Start Capture");
        startBtn.setDefaultButton(true);
        startBtn.getStyleClass().add("primary-button");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("cancel-button");

        startBtn.setOnAction(evt -> {
            String selected = ifaceCombo.getValue();
            PcapNetworkInterface chosen = interfaces.stream()
                    .filter(dev -> selected.contains(dev.getName()))
                    .findFirst().orElse(null);
            if (chosen == null) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Unable to resolve selected interface");
                a.showAndWait();
                return;
            }
            // Maybe validate filterField here, show error if invalid
            captureService.startCapture(chosen.getName(), filterField.getText().trim(), limitSpinner.getValue());
            new MainView(captureService).show(stage);
        });

        cancelBtn.setOnAction(evt -> Platform.exit());

        HBox buttonBar = new HBox(12, cancelBtn, startBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.getStyleClass().add("button-bar");

        // --- MAIN LAYOUT ("Card") ---
        GridPane grid = new GridPane();
        grid.setVgap(15);
        grid.setHgap(12);
        grid.setPadding(new Insets(18, 22, 18, 22));
        grid.setStyle("-fx-background-color: linear-gradient(to bottom, #232829 93%, #1e2226); -fx-background-radius: 14; -fx-effect: dropshadow(gaussian,#2c372c33,8,0.12,0,3);");

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setHalignment(HPos.RIGHT);
        labelCol.setPrefWidth(135);
        ColumnConstraints controlCol = new ColumnConstraints();
        controlCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, controlCol);

        grid.add(header, 0, 0, 2, 1);

        grid.add(ifaceLabel, 0, 1);
        grid.add(ifaceCombo, 1, 1);

        grid.add(filterLabel, 0, 2);
        VBox filterBox = new VBox(filterField, filterHint);
        filterBox.setSpacing(2);
        grid.add(filterBox, 1, 2);

        grid.add(limitLabel, 0, 3);
        grid.add(limitBox, 1, 3);

        grid.add(buttonBar, 1, 4);

        BorderPane root = new BorderPane(grid);
        root.setPadding(new Insets(14));
        Scene scene = new Scene(root, 500, 320);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/components/settings.css")).toExternalForm());

        stage.setScene(scene);
        stage.show();
    }
}