package com.wirecat.core_capture;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;

import java.util.List;
import java.util.stream.Collectors;

public class SettingsView {
    private final CaptureService svc = new CaptureService();
    private final List<PcapNetworkInterface> devs = safeFind();

    private static List<PcapNetworkInterface> safeFind(){
        try { return Pcaps.findAllDevs(); }
        catch(Exception e){ return List.of(); }
    }

    public void show(Stage stage){
        Label title = new Label("WireCat Settings");
        ObservableList<String> opts = FXCollections.observableArrayList(
            devs.stream()
                .map(d->d.getDescription()!=null?d.getDescription():d.getName())
                .collect(Collectors.toList())
        );
        if(opts.isEmpty()) opts.add("❌ No interfaces found");
        ComboBox<String> cb = new ComboBox<>(opts);
        cb.getSelectionModel().selectFirst();

        TextField filter = new TextField();
        filter.setPromptText("BPF filter (opt)");

        Spinner<Integer> limit = new Spinner<>(0,100_000,0);

        Button go = new Button("Start Capture");
        go.setOnAction(e->{
            String sel = cb.getValue();
            PcapNetworkInterface dev = devs.stream()
                .filter(d->sel.equals(d.getDescription())||sel.equals(d.getName()))
                .findFirst().orElse(null);
            if(dev==null){
                new Alert(Alert.AlertType.ERROR,"Cannot resolve interface").show();
                return;
            }
            svc.startCapture(dev.getName(),filter.getText().trim(),limit.getValue());
            new MainView(svc).show(stage);
        });

        VBox box = new VBox(12,title,
            new Label("Interface:"),cb,
            new Label("Filter:"),filter,
            new Label("Limit (0=∞):"),limit,go
        );
        box.setPadding(new Insets(20)); box.setAlignment(Pos.TOP_LEFT);

        BorderPane root = new BorderPane(box);
        Scene sc = new Scene(root,360,400);
        sc.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        stage.setScene(sc);
        stage.setTitle("Settings – WireCat");
        stage.show();
    }
}
