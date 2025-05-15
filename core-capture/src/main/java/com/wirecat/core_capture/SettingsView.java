package com.wirecat.core_capture;

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

        // Header
        Label header = new Label("Settings");
        header.getStyleClass().add("header-label");

        // Interface selection
        Label ifaceLabel = new Label("Network Interface:");
        ComboBox<String> ifaceCombo = new ComboBox<>();
        ifaceCombo.setPromptText("Select network interface");
        ObservableList<String> ifaceOptions = FXCollections.observableArrayList(
                interfaces.stream()
                        .map(dev -> dev.getDescription() != null ? dev.getDescription() : dev.getName())
                        .collect(Collectors.toList())
        );
        if (ifaceOptions.isEmpty()) {
            ifaceOptions.add("❌ No interfaces detected");
            ifaceCombo.setDisable(true);
        }
        ifaceCombo.setItems(ifaceOptions);
        ifaceCombo.getSelectionModel().selectFirst();

        // Filter input
        Label filterLabel = new Label("BPF Filter (optional):");
        TextField filterField = new TextField();
        filterField.setPromptText("e.g., tcp port 80");

        // Limit input
        Label limitLabel = new Label("Packet Limit:");
        Spinner<Integer> limitSpinner = new Spinner<>(0, Integer.MAX_VALUE, 0, 100);
        limitSpinner.setEditable(true);
        HBox limitBox = new HBox(5, limitSpinner, new Label("(0 = unlimited)"));
        limitBox.setAlignment(Pos.CENTER_LEFT);

        // Action buttons
        Button startBtn = new Button("Start Capture");
        startBtn.setDefaultButton(true);
        startBtn.getStyleClass().add("primary-button");
        startBtn.setOnAction(evt -> {
            String selected = ifaceCombo.getValue();
            PcapNetworkInterface chosen = interfaces.stream()
                    .filter(dev -> selected.equals(dev.getDescription()) || selected.equals(dev.getName()))
                    .findFirst().orElse(null);
            if (chosen == null) {
                new Alert(Alert.AlertType.ERROR, "Unable to resolve selected interface").showAndWait();
                return;
            }
            captureService.startCapture(chosen.getName(), filterField.getText().trim(), limitSpinner.getValue());
            new MainView(captureService).show(stage);
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(evt -> Platform.exit());

        HBox buttonBar = new HBox(10, cancelBtn, startBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        // Layout in GridPane
        GridPane grid = new GridPane();
        grid.setVgap(15);
        grid.setHgap(10);
        grid.setPadding(new Insets(20));
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setHalignment(HPos.RIGHT);
        ColumnConstraints controlCol = new ColumnConstraints();
        controlCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, controlCol);

        grid.add(header, 0, 0, 2, 1);
        grid.add(ifaceLabel, 0, 1);
        grid.add(ifaceCombo, 1, 1);
        grid.add(filterLabel, 0, 2);
        grid.add(filterField, 1, 2);
        grid.add(limitLabel, 0, 3);
        grid.add(limitBox, 1, 3);
        grid.add(buttonBar, 1, 4);

        BorderPane root = new BorderPane(grid);
        Scene scene = new Scene(root, 480, 320);
        scene.getStylesheets().add(getClass().getResource("/settings.css").toExternalForm());

        stage.setScene(scene);
        stage.show();
    }
}