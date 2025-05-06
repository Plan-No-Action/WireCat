package com.wirecat.core_capture;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;

import java.util.Collections;
import java.util.List;

public class SettingsView {

    private final CaptureService captureService;
    private final List<PcapNetworkInterface> devices;

    public SettingsView() {
        this.captureService = new CaptureService();
        List<PcapNetworkInterface> devs;
        try {
            devs = Pcaps.findAllDevs();
        } catch (Exception e) {
            e.printStackTrace();
            devs = Collections.emptyList();
        }
        this.devices = devs;
    }

    public void show(Stage stage) {
        // Title
        Label title = new Label("WireCat Settings");
        title.getStyleClass().add("settings-title");

        // Interface selection
        Label ifaceLabel = new Label("Capture Interface:");
        ComboBox<String> ifaceComboBox = new ComboBox<>();

        // Populate descriptions
        ObservableList<String> interfaceDescriptions = FXCollections.observableArrayList();
        if (devices.isEmpty()) {
            interfaceDescriptions.add("No interfaces found");
        } else {
            for (PcapNetworkInterface dev : devices) {
                String desc = dev.getDescription() != null
                        ? dev.getDescription()
                        : dev.getName();
                interfaceDescriptions.add(desc);
            }
        }
        ifaceComboBox.setItems(interfaceDescriptions);
        ifaceComboBox.getStyleClass().add("protocol-combo");
        if (!interfaceDescriptions.isEmpty()) {
            ifaceComboBox.getSelectionModel().selectFirst();
        }

        // Optional capture filter
        Label filterLabel = new Label("Filter (BPF, optional):");
        TextField filterField = new TextField();
        filterField.getStyleClass().add("filter-field");

        // Packet limit
        Label limitLabel = new Label("Packet Limit:");
        Spinner<Integer> packetLimit = new Spinner<>(1, 100000, 1000);
        packetLimit.getStyleClass().add("limit-spinner");

        // Start button
        Button startButton = new Button("Start Capture");
        startButton.getStyleClass().add("start-button");
        startButton.setOnAction(e -> {
            String selected = ifaceComboBox.getValue();
            if (selected == null || selected.startsWith("No ")) {
                new Alert(Alert.AlertType.ERROR, "Select a valid interface.")
                        .show();
                return;
            }
            String iface = devices.stream()
                    .filter(dev -> selected.equals(dev.getDescription())
                            || selected.equals(dev.getName()))
                    .map(PcapNetworkInterface::getName)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Interface not found: " + selected));

            String filter = filterField.getText().trim();
            int limit = packetLimit.getValue();

            // Launch capture and switch to main view
            captureService.startCapture(iface, filter, limit);
            new MainView(captureService).show(stage);
        });

        // Layout form
        VBox form = new VBox(12,
                title,
                ifaceLabel, ifaceComboBox,
                filterLabel, filterField,
                limitLabel, packetLimit,
                startButton
        );
        form.setAlignment(Pos.TOP_LEFT);
        form.setPadding(new Insets(20));

        BorderPane root = new BorderPane(form);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 360, 400);
        scene.getStylesheets().add(
                getClass().getResource("/dark-theme.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.setTitle("Settings - WireCat");
        stage.show();
    }
}
