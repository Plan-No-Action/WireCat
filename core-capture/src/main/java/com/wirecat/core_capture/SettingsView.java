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
import javafx.util.StringConverter;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;

import java.util.List;
import java.util.stream.Collectors;

public class SettingsView {
    private final CaptureService svc = new CaptureService();
    private final List<PcapNetworkInterface> devs = safeFind();

<<<<<<< Updated upstream
    private static List<PcapNetworkInterface> safeFind(){
        try { return Pcaps.findAllDevs(); }
        catch(Exception e){ return List.of(); }
=======
    private final CaptureService captureService;
    private final List<PcapNetworkInterface> devices;

    public SettingsView() {
        this.captureService = new CaptureService();

        List<PcapNetworkInterface> allDevs;
        try {
            allDevs = Pcaps.findAllDevs();
        } catch (Exception e) {
            e.printStackTrace();
            allDevs = List.of();
        }

        // Keep only interfaces with a MAC (link-layer) address, filter out loopback/minipor
        this.devices = allDevs.stream()
                .filter(dev -> !dev.getLinkLayerAddresses().isEmpty())
                .filter(dev -> !dev.getName().toLowerCase().contains("loopback"))
                .filter(dev -> {
                    String desc = dev.getDescription() == null ? "" : dev.getDescription().toLowerCase();
                    return !desc.contains("network monitor") && !desc.contains("adapter for loopback");
                })
                .collect(Collectors.toList());
>>>>>>> Stashed changes
    }

    public void show(Stage s){
        Label title = new Label("WireCat Settings");
        ComboBox<String> cb = new ComboBox<>();
        ObservableList<String> opts = FXCollections.observableArrayList();
        if(devs.isEmpty()) opts.add("❌ No interfaces found");
        else devs.forEach(d -> opts.add(d.getDescription()!=null?d.getDescription():d.getName()));
        cb.setItems(opts); if(!opts.isEmpty()) cb.getSelectionModel().selectFirst();

<<<<<<< Updated upstream
        TextField filter = new TextField();
        Spinner<Integer> limit = new Spinner<>(1,100_000,1000);
        Button go = new Button("Start Capture");
        go.setOnAction(e -> {
            String sel = cb.getValue();
            if(sel==null || sel.startsWith("❌")){ alert("Select valid interface"); return; }
            PcapNetworkInterface d = devs.stream().filter(v -> sel.equals(v.getDescription())||sel.equals(v.getName())).findFirst().orElse(null);
            if(d==null){ alert("Cannot resolve interface"); return; }
            svc.start(d, filter.getText().trim(), limit.getValue());
            new MainView(svc).show(s);
        });

        VBox form = new VBox(12,title,new Label("Interface:"),cb,
                new Label("BPF filter (opt):"),filter,
                new Label("Packet limit:"),limit,go);
        form.setPadding(new Insets(20)); form.setAlignment(Pos.TOP_LEFT);

        Scene sc = new Scene(new BorderPane(form),360,400);
        sc.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        s.setScene(sc); s.setTitle("Settings – WireCat"); s.show();
=======
        // Interface combo
        Label ifaceLabel = new Label("Capture Interface:");
        ComboBox<PcapNetworkInterface> ifaceCombo = new ComboBox<>();

        if (devices.isEmpty()) {
            ifaceCombo.setPromptText("No valid interfaces found");
            ifaceCombo.setDisable(true);
        } else {
            ObservableList<PcapNetworkInterface> items = FXCollections.observableArrayList(devices);
            ifaceCombo.setItems(items);

            // Auto-select Realtek if present
            devices.stream()
                    .filter(dev -> {
                        String desc = dev.getDescription() != null ? dev.getDescription().toLowerCase() : "";
                        return desc.contains("realtek");
                    })
                    .findFirst()
                    .ifPresent(ifaceCombo.getSelectionModel()::select);

            // Otherwise select first
            if (ifaceCombo.getSelectionModel().isEmpty()) {
                ifaceCombo.getSelectionModel().selectFirst();
            }

            // Display "name — description"
            ifaceCombo.setConverter(new StringConverter<>() {
                @Override
                public String toString(PcapNetworkInterface dev) {
                    if (dev == null) return "";
                    String desc = dev.getDescription() != null ? dev.getDescription() : "no desc";
                    return dev.getName() + " — " + desc;
                }
                @Override
                public PcapNetworkInterface fromString(String s) {
                    return null; // not needed
                }
            });
        }
        ifaceCombo.getStyleClass().add("protocol-combo");

        // Optional BPF filter
        Label filterLabel = new Label("Filter (BPF, optional):");
        TextField filterField = new TextField();
        filterField.getStyleClass().add("filter-field");

        // Packet limit
        Label limitLabel = new Label("Packet Limit (0=infinite):");
        Spinner<Integer> packetLimit = new Spinner<>(0, Integer.MAX_VALUE, 0);
        packetLimit.getStyleClass().add("limit-spinner");

        // Start button
        Button startButton = new Button("Start Capture");
        startButton.getStyleClass().add("start-button");
        startButton.setOnAction(e -> {
            PcapNetworkInterface sel = ifaceCombo.getValue();
            if (sel == null) {
                new Alert(Alert.AlertType.ERROR, "Select a valid interface.").show();
                return;
            }
            String ifaceName = sel.getName();
            System.out.println("▶️ Starting capture on interface: " + ifaceName
                    + " (" + sel.getDescription() + ")");

            String filter = filterField.getText().trim();
            int limit = packetLimit.getValue();

            captureService.startCapture(ifaceName, filter, limit);
            new MainView(captureService).show(stage);
        });

        // Layout form
        VBox form = new VBox(12,
                title,
                ifaceLabel, ifaceCombo,
                filterLabel, filterField,
                limitLabel, packetLimit,
                startButton
        );
        form.setAlignment(Pos.TOP_LEFT);
        form.setPadding(new Insets(20));

        BorderPane root = new BorderPane(form);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 380, 420);
        scene.getStylesheets().add(
                getClass().getResource("/dark-theme.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.setTitle("Settings - WireCat");
        stage.show();
>>>>>>> Stashed changes
    }
    private static void alert(String m){ new Alert(Alert.AlertType.ERROR,m).show(); }
}
