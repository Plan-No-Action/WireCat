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

import java.util.List;

public class SettingsView {
    private final CaptureService svc = new CaptureService();
    private final List<PcapNetworkInterface> devs = safeFind();

    private static List<PcapNetworkInterface> safeFind(){
        try { return Pcaps.findAllDevs(); }
        catch(Exception e){ return List.of(); }
    }

    public void show(Stage s){
        Label title = new Label("WireCat Settings");
        ComboBox<String> cb = new ComboBox<>();
        ObservableList<String> opts = FXCollections.observableArrayList();
        if(devs.isEmpty()) opts.add("❌ No interfaces found");
        else devs.forEach(d -> opts.add(d.getDescription()!=null?d.getDescription():d.getName()));
        cb.setItems(opts); if(!opts.isEmpty()) cb.getSelectionModel().selectFirst();

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
    }
    private static void alert(String m){ new Alert(Alert.AlertType.ERROR,m).show(); }
}
