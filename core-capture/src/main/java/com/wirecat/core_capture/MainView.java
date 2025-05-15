package com.wirecat.core_capture;
import com.wirecat.core_capture.filter.FilterEngine;        
import com.wirecat.core_capture.inspector.PacketInspector;  
import javafx.scene.Node;                                    
import javafx.scene.control.Alert; 

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Predicate;

public class MainView {

    private final ObservableList<CapturedPacket> packets = FXCollections.observableArrayList();
    private final FilteredList<CapturedPacket> view = new FilteredList<>(packets, p -> true);
    private final CaptureService svc;
    private final XYChart.Series<String, Number> protoSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> timeSeries = new XYChart.Series<>();
    private String selectedProto = "All";
    private String searchText = "";
    private int searchPort = 0;
    private String searchIp = "";
    private boolean autoScroll = true;
    private TableView<CapturedPacket> table;
    private LocalDateTime startTime;
    private PacketInspector inspector = new PacketInspector();

    public MainView(CaptureService svc) {
        this.svc = svc;
        protoSeries.setName("Protocols");
        timeSeries.setName("Packets/sec");
    }

    public void show(Stage stage) {
        startTime = LocalDateTime.now();

        // Sparkline
        NumberAxis sx = new NumberAxis();
        NumberAxis sy = new NumberAxis();
        LineChart<Number, Number> spark = new LineChart<>(sx, sy);
        spark.getData().add(timeSeries);
        spark.setLegendVisible(false);
        spark.setAnimated(false);
        spark.setPrefHeight(80);

        // Top controls
        Label title = new Label("WIRECAT");
        ComboBox<String> protoFilter = new ComboBox<>(
            FXCollections.observableArrayList("All", "TCP", "UDP", "ICMP", "ARP")
        );
        protoFilter.setValue("All");
        protoFilter.valueProperty().addListener((o,old,n)-> {
            selectedProto = n;
            refreshPredicate();
        });

        TextField searchField = new TextField();
        searchField.setPromptText("IP or Port…");
        searchField.textProperty().addListener((o,old,n)-> {
            String t = n.trim();
            if (t.matches("\\d+")) {
                searchPort = Integer.parseInt(t);
                searchIp = "";
            } else {
                searchIp = t;
                searchPort = 0;
            }
            refreshPredicate();
        });
        CheckBox auto = new CheckBox("Auto‑scroll");
        auto.setSelected(true);
        auto.selectedProperty().addListener((o,old,v)-> autoScroll = v);

        HBox topControls = new HBox(10, spark, title, new Region(), searchField, protoFilter, auto);
        HBox.setHgrow(topControls.getChildren().get(2), Priority.ALWAYS);
        topControls.setPadding(new Insets(10));

        // Left
        Button settingsBtn = new Button("Settings");
        settingsBtn.setOnAction(e->{
            svc.stopCapture();
            new SettingsView().show(stage);
        });
        Button stopBtn = new Button("Stop");
        stopBtn.setOnAction(e->svc.stopCapture());
        VBox left = new VBox(12, settingsBtn, stopBtn);
        left.setPadding(new Insets(15));

        // Table
        table = buildTable();
        table.setItems(view);
        table.setFixedCellSize(24);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setContextMenu(buildContextMenu());

        // Inspector pane
        Node inspNode = inspector.getNode();

        SplitPane center = new SplitPane(table, inspNode);
        center.setOrientation(Orientation.VERTICAL);
        center.setDividerPositions(0.6);

        // Stats
        BarChart<String, Number> chart = createBarChart();
        Label totalLabel = new Label();
        totalLabel.textProperty().bind(Bindings.size(packets).asString("Total Packets: %d"));
        Button savePcap = new Button("Export PCAP");
        savePcap.setOnAction(e->savePcap(stage));
        Button saveCsv = new Button("Export CSV");
        saveCsv.setOnAction(e->saveCsv(stage));
        VBox right = new VBox(20, new Label("Statistics"), chart, totalLabel, savePcap, saveCsv);
        right.setPadding(new Insets(15));
        right.setPrefWidth(260);

        // Status
        Label status = new Label("Idle");
        svc.onStatus(msg->Platform.runLater(()->status.setText(msg)));
        HBox bottom = new HBox(status);
        bottom.setPadding(new Insets(6,10,6,10));
        bottom.getStyleClass().add("status-bar");

        // Scene
        BorderPane root = new BorderPane(center, topControls, right, bottom, left);
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("WireCat");
        stage.show();

        // Capture loop
        new AnimationTimer(){
            private long last = System.nanoTime();
            private int count = 0;
            @Override public void handle(long now){
                while(!svc.queue().isEmpty()){
                    CapturedPacket cp = svc.queue().poll().toPacket();
                    // compute ΔTime
                    if(!packets.isEmpty()){
                        long prev = packets.get(packets.size()-1).getTimestampMs();
                        cp.setDeltaTime(cp.getTimestampMs()-prev);
                    }
                    packets.add(cp);
                    updateStats(cp);
                    count++;
                }
                // sparkline once/sec
                if(now - last > 1_000_000_000L){
                    double sec = (now - last)/1e9;
                    double rate = count/sec;
                    double x = Duration.between(startTime, LocalDateTime.now()).toMillis()/1000.0;
                    timeSeries.getData().add(new XYChart.Data<>(x, rate));
                    last = now; count = 0;
                }
                if(autoScroll && !packets.isEmpty()) table.scrollTo(packets.size()-1);
            }
        }.start();

        svc.startCapture("eth0","",0);
    }

    private void refreshPredicate(){
        Predicate<CapturedPacket> p1 = FilterEngine.byProtocol(selectedProto);
        Predicate<CapturedPacket> p2 = FilterEngine.byIp(searchIp);
        Predicate<CapturedPacket> p3 = FilterEngine.byPort(searchPort);
        view.setPredicate(FilterEngine.combine(p1,p2,p3));
    }

    private TableView<CapturedPacket> buildTable(){
        TableView<CapturedPacket> tv = new TableView<>();
        addColumn(tv,"No","number",60);
        addColumn(tv,"Time","timestamp",120);
        addColumn(tv,"Δ Time","deltaTime",80);
        addColumn(tv,"Src MAC","sourceMAC",140);
        addColumn(tv,"Dst MAC","destinationMAC",140);
        addColumn(tv,"Src IP","sourceIP",140);
        addColumn(tv,"Dst IP","destinationIP",140);
        addColumn(tv,"Proto","protocol",80);
        addColumn(tv,"Src Port","sourcePort",80);
        addColumn(tv,"Dst Port","destinationPort",80);
        addColumn(tv,"Len","length",60);
        addColumn(tv,"Risk","riskScore",60);
        // row coloring
        tv.setRowFactory(t->new TableRow<>(){
            @Override protected void updateItem(CapturedPacket p, boolean e){
                super.updateItem(p,e);
                if(p==null||e) { setStyle(""); return; }
                switch(p.getProtocol()){
                    case "TCP": setStyle("-fx-background-color:#31363b;"); break;
                    case "UDP": setStyle("-fx-background-color:#2d3135;"); break;
                    default:    setStyle("");
                }
            }
        });
        // on‑click open inspector tree
        tv.getSelectionModel().selectedItemProperty().addListener((o,old,sel)->{
            if(sel!=null) inspector.display(sel.getDetail());
        });
        return tv;
    }

    private <T> void addColumn(TableView<CapturedPacket> t, String title, String prop, int w){
        TableColumn<CapturedPacket,T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        t.getColumns().add(c);
    }

    private ContextMenu buildContextMenu(){
        MenuItem copy = new MenuItem("Copy row(s)");
        copy.setOnAction(e->{
            StringBuilder sb=new StringBuilder();
            for(CapturedPacket p:table.getSelectionModel().getSelectedItems()){
                sb.append(p.getNumber()).append('\t')
                  .append(p.getTimestamp()).append('\t')
                  .append(p.getSourceIP()).append("→")
                  .append(p.getDestinationIP()).append('\n');
            }
            ClipboardContent cc=new ClipboardContent();
            cc.putString(sb.toString());
            Clipboard.getSystemClipboard().setContent(cc);
        });
        return new ContextMenu(copy);
    }

    private BarChart<String,Number> createBarChart(){
        CategoryAxis x=new CategoryAxis();
        NumberAxis y=new NumberAxis();
        BarChart<String,Number> c=new BarChart<>(x,y);
        c.getData().add(protoSeries);
        c.setLegendVisible(false);
        c.setAnimated(false);
        return c;
    }

    private void updateStats(CapturedPacket p){
        for(XYChart.Data<String,Number> d:protoSeries.getData()){
            if(d.getXValue().equals(p.getProtocol())){
                d.setYValue(d.getYValue().intValue()+1);
                return;
            }
        }
        protoSeries.getData().add(new XYChart.Data<>(p.getProtocol(),1));
    }

    private void savePcap(Stage s){
        FileChooser fc=new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PCAP","*.pcap"));
        File f=fc.showSaveDialog(s);
        if(f!=null) svc.save(f);
    }

    private void saveCsv(Stage s){
        FileChooser fc=new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV","*.csv"));
        File f=fc.showSaveDialog(s);
        if(f==null) return;
        try(BufferedWriter w=new BufferedWriter(new FileWriter(f))){
            w.write("No,Time,ΔTime,SrcMAC,DstMAC,SrcIP,DstIP,Proto,SrcPort,DstPort,Len,Risk\n");
            for(CapturedPacket p:packets){
                w.write(String.format("%d,%s,%d,%s,%s,%s,%s,%s,%d,%d,%d,%.2f\n",
                    p.getNumber(),p.getTimestamp(),p.getDeltaTime(),
                    p.getSourceMAC(),p.getDestinationMAC(),
                    p.getSourceIP(),p.getDestinationIP(),
                    p.getProtocol(),p.getSourcePort(),
                    p.getDestinationPort(),p.getLength(),
                    p.getRiskScore()
                ));
            }
        }catch(Exception ex){
            new Alert(Alert.AlertType.ERROR,"CSV export failed:"+ex.getMessage()).show();
        }
    }
}
