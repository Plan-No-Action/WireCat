package com.wirecat.core_capture;

import com.wirecat.core_capture.filter.FilterEngine;
import com.wirecat.core_capture.inspector.PacketInspector;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MainView {
    // Packet capture
    private final ObservableList<CapturedPacket> packets = FXCollections.observableArrayList();
    private final FilteredList<CapturedPacket> view = new FilteredList<>(packets, p -> true);
    private final CaptureService svc;
    private final XYChart.Series<String, Number> protoSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> timeSeries = new XYChart.Series<>();

    // UI components
    private BorderPane root;
    private SplitPane centerSplit;
    private TableView<CapturedPacket> table;
    private PacketInspector inspector;
    private TextArea hexArea;
    private TextField searchField;
    private FlowPane filterChips;
    private ToggleButton autoScrollToggle;
    private ToggleButton asciiToggle;

    // Conversation stats table
    private TableView<Conversation> convTable;
    private final ObservableList<Conversation> convData = FXCollections.observableArrayList();
    private final Map<String, AtomicInteger> convCounts = new HashMap<>();

    // Views
    private VBox liveView;
    private VBox httpView;
    private VBox settingsView;
    private LocalDateTime startTime;

    public MainView(CaptureService svc) {
        this.svc = svc;
        protoSeries.setName("Protocols");
        timeSeries.setName("Rate (pkt/s)");
    }

    public void show(Stage stage) {
        startTime = LocalDateTime.now();
        inspector = new PacketInspector();

        // Build sections
        MenuBar menuBar = buildMenuBar();
        VBox sidebar = buildSidebar();
        ToolBar toolbar = buildToolBar();
        TitledPane filterPane = buildFilterPane();
        table = buildTable();

        // Live view: toolbar, filters, packet table
        liveView = new VBox(toolbar, filterPane, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        // HTTP & Settings placeholders
        httpView = new VBox(new Label("HTTP Objects will appear here"));
        httpView.setPadding(new Insets(10));
        settingsView = new VBox(new Label("Settings panel"));
        settingsView.setPadding(new Insets(10));

        // Inspector + Hex pane
        VBox inspBox = buildInspectorPane();

        // Center split
        centerSplit = new SplitPane(liveView, inspBox);
        centerSplit.setDividerPositions(0.65);

        // Status bar
        Label status = new Label("Idle");
        svc.onStatus(msg -> Platform.runLater(() -> status.setText(msg)));
        HBox bottomBar = new HBox(status);
        bottomBar.setPadding(new Insets(6));
        bottomBar.getStyleClass().add("status-bar");

        // Root layout
        root = new BorderPane();
        root.setTop(menuBar);
        root.setLeft(sidebar);
        root.setCenter(centerSplit);
        root.setBottom(bottomBar);

        // Scene
        Scene scene = new Scene(root, 1600, 900);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/dark-theme.css")).toExternalForm());
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleGlobalShortcuts);

        stage.setScene(scene);
        stage.setTitle("WireCat - Live Packet Analysis");
        stage.show();

        startCapture();
    }

    private MenuBar buildMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem exportPcap = new MenuItem("Export PCAP");
        exportPcap.setOnAction(e -> executeCommand("export pcap"));
        fileMenu.getItems().add(exportPcap);
        menuBar.getMenus().add(fileMenu);
        return menuBar;
    }

    private TitledPane buildFilterPane() {
        filterChips = new FlowPane();
        filterChips.setHgap(10);
        filterChips.setVgap(5);
        filterChips.setPadding(new Insets(10));
        filterChips.getStyleClass().add("filter-chips");

        // Protocol checkboxes
        for (String proto : new String[]{"TCP", "UDP", "ICMP", "HTTP", "ARP"}) {
            CheckBox cb = new CheckBox(proto);
            cb.setSelected(true);
            cb.setOnAction(e -> refreshPredicate());
            filterChips.getChildren().add(cb);
        }

        TitledPane pane = new TitledPane("Filters", filterChips);
        pane.setCollapsible(false);
        return pane;
    }

    private VBox buildInspectorPane() {
        asciiToggle = new ToggleButton("ASCII");
        asciiToggle.setOnAction(e -> inspector.setAsciiMode(asciiToggle.isSelected()));
        asciiToggle.setSelected(true); // default ASCII view

        // Hex area
        hexArea = new TextArea();
        hexArea.setEditable(false);
        hexArea.setWrapText(false);
        hexArea.getStyleClass().add("details-area");
        hexArea.setPrefRowCount(10);
        hexArea.setFont(javafx.scene.text.Font.font("Consolas", 11));

        HBox header = new HBox(new Label("Inspector"), asciiToggle);
        header.setSpacing(10);
        header.setPadding(new Insets(8));

        VBox v = new VBox(header, inspector.getNode(), new Label("Hex Dump:"), hexArea);
        v.setSpacing(4);
        v.setPadding(new Insets(8));
        v.getStyleClass().add("details-area");
        return v;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(300);

        Label logo = new Label("WireCat");
        logo.getStyleClass().add("logo");

        Label convLabel = new Label("Conversations");
        convLabel.getStyleClass().add("subtitle");

        // Conversation table
        convTable = new TableView<>(convData);
        convTable.setPlaceholder(new Label("No traffic yet"));
        convTable.setPrefHeight(200);
        TableColumn<Conversation, String> convCol = new TableColumn<>("Conversation");
        convCol.setCellValueFactory(new PropertyValueFactory<>("conversation"));
        convCol.setPrefWidth(200);
        TableColumn<Conversation, Integer> countCol = new TableColumn<>("Packets");
        countCol.setCellValueFactory(new PropertyValueFactory<>("count"));
        countCol.setPrefWidth(80);
        convTable.getColumns().addAll(convCol, countCol);
        convTable.getSelectionModel().selectedItemProperty().addListener((obs,old,sel)->refreshPredicate());

        ToggleGroup navGroup = new ToggleGroup();
        VBox navBox = new VBox(8);
        for (String t : new String[]{"Live View", "Follow Stream", "HTTP Objects", "Settings", "Stop Capture"}) {
            ToggleButton btn = new ToggleButton(t);
            btn.setToggleGroup(navGroup);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.getStyleClass().add("sidebar-button");
            btn.setOnAction(e -> switchView(t));
            navBox.getChildren().add(btn);
        }
        ((ToggleButton)navBox.getChildren().get(0)).setSelected(true);

        autoScrollToggle = new ToggleButton("Auto-Scroll");
        autoScrollToggle.setSelected(true);

        sidebar.getChildren().addAll(logo, convLabel, convTable, navBox, autoScrollToggle);
        return sidebar;
    }

    private ToolBar buildToolBar() {
        searchField = new TextField();
        searchField.setPromptText("Filter IP / Port / URI");
        searchField.textProperty().addListener((o,old,txt)->refreshPredicate());

        Button clearBtn = new Button(); clearBtn.getStyleClass().add("icon-clear"); clearBtn.setOnAction(e->searchField.clear());
        Button palette = new Button(); palette.getStyleClass().add("icon-palette"); palette.setOnAction(e->openCommandPalette());

        return new ToolBar(searchField, clearBtn, new Separator(), palette);
    }

    private TableView<CapturedPacket> buildTable() {
        TableView<CapturedPacket> tv = new TableView<>(view);
        addColumn(tv, "No", "number", 50);
        addColumn(tv, "Time", "timestamp", 120);
        addColumn(tv, "Src IP", "sourceIP", 140);
        addColumn(tv, "Dst IP", "destinationIP", 140);
        addColumn(tv, "Proto", "protocol", 80);

        tv.setRowFactory(row->{
            TableRow<CapturedPacket> r = new TableRow<>();
            r.setOnContextMenuRequested(evt->{
                ContextMenu ctx=new ContextMenu();
                MenuItem follow=new MenuItem("Follow Stream"); follow.setOnAction(a->switchView("Follow Stream"));
                MenuItem copyHex=new MenuItem("Copy Hex Dump"); copyHex.setOnAction(a->{
                    ClipboardContent cc=new ClipboardContent(); cc.putString(r.getItem().getHexDump());
                    Clipboard.getSystemClipboard().setContent(cc);
                });
                ctx.getItems().addAll(follow, copyHex);
                ctx.show(r, evt.getScreenX(), evt.getScreenY());
            });
            return r;
        });
        tv.getSelectionModel().selectedItemProperty().addListener((o,old,sel)->{
            if(sel!=null) {
                inspector.display(sel.getDetail());
                hexArea.setText(sel.getHexDump());
            }
        });
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return tv;
    }

    private void switchView(String followStream) {
        switch (followStream) {
            case "Live View" -> centerSplit.getItems().set(0, liveView);
            case "Follow Stream" -> centerSplit.getItems().set(0, new VBox(new Label("Follow Stream Placeholder")));
            case "HTTP Objects" -> centerSplit.getItems().set(0, httpView);
            case "Settings" -> centerSplit.getItems().set(0, settingsView);
            case "Stop Capture" -> {
                svc.stopCapture();
                centerSplit.getItems().set(0, new VBox(new Label("Capture Stopped")));
            }
        }
    }

    private <T> void addColumn(TableView<CapturedPacket> table,String title,String prop,int w){
        TableColumn<CapturedPacket,T> col=new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(prop)); col.setPrefWidth(w);
        table.getColumns().add(col);
    }

    private void startCapture(){
        new AnimationTimer(){
            private long lastF=System.nanoTime(), count=0;
            @Override public void handle(long now){
                while(!svc.queue().isEmpty()){
                    CapturedPacket cp= Objects.requireNonNull(svc.queue().poll()).toPacket();
                    if(!packets.isEmpty()) cp.setDeltaTime(cp.getTimestampMs()-packets.get(packets.size()-1).getTimestampMs());
                    packets.add(cp);
                    updateConversationStats(cp);
                    updateStats(cp);
                    count++;
                }
                if(now-lastF>1_000_000_000L){
                    timeSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(
                            Duration.between(startTime,LocalDateTime.now()).toMillis()/1000.0, count/(double)((now-lastF)/1e9)
                    ));
                    lastF=now; count=0; refreshConversationList();
                }
                if(autoScrollToggle.isSelected()&& !packets.isEmpty()) table.scrollTo(packets.size()-1);
            }
        }.start();
        svc.startCapture("eth0","",0);
    }

    private void updateConversationStats(CapturedPacket p){
        String conv=p.getSourceIP()+" → "+p.getDestinationIP();
        convCounts.computeIfAbsent(conv,k->new AtomicInteger()).incrementAndGet();
    }

    private void refreshConversationList(){
        var top=convCounts.entrySet().stream()
                .sorted((a,b)->b.getValue().get()-a.getValue().get())
                .limit(10)
                .map(e->new Conversation(e.getKey(), e.getValue().get()))
                .collect(Collectors.toList());
        Platform.runLater(() ->{
            convData.setAll(top);
        });
    }

    private void refreshPredicate(){
        Predicate<CapturedPacket> protoPred=p->filterChips.getChildren().stream()
                .filter(n->((CheckBox)n).isSelected())
                .anyMatch(n->((CheckBox)n).getText().equalsIgnoreCase(p.getProtocol()));
        String txt=searchField.getText().trim();
        Predicate<CapturedPacket> ipPred=FilterEngine.byIp(txt.matches("\\d+(.\\d+){3}")?txt:"");
        Predicate<CapturedPacket> portPred=FilterEngine.byPort(txt.matches("\\d+")?Integer.parseInt(txt):0);
        Conversation sel=convTable.getSelectionModel().getSelectedItem();
        Predicate<CapturedPacket> convPred=p->sel==null || sel.getConversation().equals(p.getSourceIP()+" → "+p.getDestinationIP());
        view.setPredicate(FilterEngine.combine(protoPred, ipPred, portPred, convPred));
    }

    private void handleGlobalShortcuts(KeyEvent e){
        if(e.isControlDown()&&e.getCode()==KeyCode.K) openCommandPalette();
        if(e.isControlDown()&&e.getCode()==KeyCode.F) searchField.requestFocus();
    }

    private void openCommandPalette(){
        TextInputDialog dlg=new TextInputDialog();
        dlg.setTitle("Command Palette"); dlg.setHeaderText("Type a command...");
        dlg.showAndWait().ifPresent(this::executeCommand);
    }

    private void executeCommand(String cmd){
        switch(cmd.toLowerCase()){
            case "export pcap": {
                FileChooser fc=new FileChooser(); fc.setTitle("Save PCAP");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PCAP files","*.pcap"));
                File file=fc.showSaveDialog(root.getScene().getWindow()); if(file!=null) svc.save(file);
                break;
            }
            case "toggle autoscroll": autoScrollToggle.setSelected(!autoScrollToggle.isSelected()); break;
            case "focus search": searchField.requestFocus(); break;
        }
    }

    private void updateStats(CapturedPacket p){
        protoSeries.getData().stream().filter(d->d.getXValue().equals(p.getProtocol()))
                .findFirst().ifPresentOrElse(d->d.setYValue(d.getYValue().intValue()+1),
                        ()->protoSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(p.getProtocol(),1)));
    }

    public static class Conversation {
        private final SimpleStringProperty conversation;
        private final SimpleIntegerProperty count;
        public Conversation(String conv, int cnt){
            conversation=new SimpleStringProperty(conv);
            count=new SimpleIntegerProperty(cnt);
        }
        public String getConversation(){return conversation.get();}
        public int getCount(){return count.get();}
    }
}
