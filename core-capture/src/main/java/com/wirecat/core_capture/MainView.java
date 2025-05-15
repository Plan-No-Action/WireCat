package com.wirecat.core_capture;

import com.wirecat.core_capture.filter.FilterEngine;
import com.wirecat.core_capture.inspector.PacketInspector;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
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
    private final ObservableList<CapturedPacket> packets = FXCollections.observableArrayList();
    private final FilteredList<CapturedPacket> view = new FilteredList<>(packets, p -> true);
    private final CaptureService svc;
    private final XYChart.Series<String, Number> protoSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> timeSeries = new XYChart.Series<>();

    private BorderPane root;
    private SplitPane centerSplit;
    private TableView<CapturedPacket> table;
    private PacketInspector inspector;
    private TextField searchField;
    private FlowPane filterChips;
    private ToggleButton autoScrollToggle;
    private ToggleButton asciiToggle;
    private ListView<String> convList;

    // conversation stats
    private final Map<String, AtomicInteger> convCounts = new HashMap<>();
    private LocalDateTime startTime;

    // pre-built views
    private VBox liveView;
    private VBox httpView;
    private VBox settingsView;

    public MainView(CaptureService svc) {
        this.svc = svc;
        protoSeries.setName("Protocols");
        timeSeries.setName("Rate (pkt/s)");
    }

    public void show(Stage stage) {
        startTime = LocalDateTime.now();
        inspector = new PacketInspector();

        // Build UI sections
        MenuBar menuBar = buildMenuBar();
        VBox sidebar = buildSidebar();
        ToolBar toolbar = buildToolBar();
        TitledPane filterPane = buildFilterPane();
        table = buildTable();

        // Live view content
        liveView = new VBox(toolbar, filterPane, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        // HTTP Objects placeholder
        httpView = new VBox(new Label("HTTP Objects will appear here"));
        httpView.setPadding(new Insets(10));

        // Settings placeholder
        settingsView = new VBox(new Label("Settings panel"));
        settingsView.setPadding(new Insets(10));

        // Inspector pane always visible
        VBox inspBox = buildInspectorPane();

        // Center split: [ content | inspector ]
        centerSplit = new SplitPane(liveView, inspBox);
        centerSplit.setDividerPositions(0.65);

        // Bottom status bar
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

        Scene scene = new Scene(root, 1600, 900);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/dark-theme.css")).toExternalForm());
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleGlobalShortcuts);

        stage.setScene(scene);
        stage.setTitle("WireCat - Live Packet Analysis");
        stage.show();

        startCapture();
    }

    private VBox buildInspectorPane() {
        asciiToggle = new ToggleButton("ASCII");
        asciiToggle.setOnAction(e -> inspector.setAsciiMode(asciiToggle.isSelected()));
        asciiToggle.setSelected(false);
        HBox header = new HBox(new Label("Inspector"), asciiToggle);
        header.setSpacing(10);
        header.setPadding(new Insets(8));

        VBox v = new VBox(header, inspector.getNode());
        v.setSpacing(4);
        v.setPadding(new Insets(8));
        v.getStyleClass().add("details-area");
        return v;
    }

    private MenuBar buildMenuBar() {
        Menu file = new Menu("File");
        MenuItem save = new MenuItem("Export PCAP");
        save.setOnAction(e -> executeCommand("export pcap"));
        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> Platform.exit());
        file.getItems().addAll(save, new SeparatorMenuItem(), exit);

        Menu view = new Menu("View");
        CheckMenuItem darkMode = new CheckMenuItem("Dark Mode");
        darkMode.setSelected(true);
        darkMode.setOnAction(e -> toggleTheme(darkMode.isSelected()));
        view.getItems().add(darkMode);

        Menu help = new Menu("Help");
        MenuItem shortcuts = new MenuItem("Keyboard Shortcuts");
        shortcuts.setOnAction(e -> showShortcuts());
        help.getItems().add(shortcuts);

        return new MenuBar(file, view, help);
    }

    private void toggleTheme(boolean dark) {
        var sheets = root.getScene().getStylesheets();
        sheets.clear();
        sheets.add(Objects.requireNonNull(getClass().getResource("/dark-theme.css")).toExternalForm());
    }

    private void showShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Available Shortcuts");
        alert.setContentText(
                """
                        Ctrl+K: Command Palette
                        Ctrl+F: Focus Search Field
                        Ctrl+S: Save PCAP
                        """
        );
        alert.showAndWait();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(260);

        Label logo = new Label("WireCat");
        logo.getStyleClass().add("logo");

        // Top Conversations list
        Label convLabel = new Label("Top Conversations");
        convLabel.getStyleClass().add("subtitle");
        convList = new ListView<>();
        convList.setPrefHeight(180);
        convList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> refreshPredicate());
        convList.setPlaceholder(new Label("No traffic yet"));

        // Navigation buttons
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
        // default select Live View
        ((ToggleButton)navBox.getChildren().get(0)).setSelected(true);

        autoScrollToggle = new ToggleButton("Auto-Scroll");
        autoScrollToggle.setSelected(true);

        sidebar.getChildren().addAll(logo, convLabel, convList, navBox, autoScrollToggle);
        return sidebar;
    }

    private void switchView(String viewName) {
        // always keep inspector on right
        switch (viewName) {
            case "Stop Capture": svc.stopCapture(); break;
            case "Live View": centerSplit.getItems().set(0, liveView); break;
            case "Follow Stream": centerSplit.getItems().set(0, inspector.getNode()); break;
            case "HTTP Objects": centerSplit.getItems().set(0, httpView); break;
            case "Settings": centerSplit.getItems().set(0, settingsView); break;
        }
    }

    private ToolBar buildToolBar() {
        searchField = new TextField();
        searchField.setPromptText("Filter IP / Port / URI");
        searchField.textProperty().addListener((o, old, txt) -> refreshPredicate());

        Button clearBtn = new Button();
        clearBtn.getStyleClass().add("icon-clear");
        clearBtn.setOnAction(e -> searchField.clear());

        Button palette = new Button();
        palette.getStyleClass().add("icon-palette");
        palette.setOnAction(e -> openCommandPalette());

        return new ToolBar(searchField, clearBtn, new Separator(), palette);
    }

    private void openCommandPalette() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Command Palette");
        dlg.setHeaderText("Type a command...");
        dlg.showAndWait().ifPresent(this::executeCommand);
    }

    private void executeCommand(String cmd) {
        switch (cmd.toLowerCase()) {
            case "export pcap" -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Save PCAP");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PCAP files", "*.pcap"));
                File file = fc.showSaveDialog(root.getScene().getWindow());
                if (file != null) svc.save(file);
            }
            case "toggle autoscroll" -> autoScrollToggle.setSelected(!autoScrollToggle.isSelected());
            case "focus search" -> searchField.requestFocus();
        }
    }

    private TitledPane buildFilterPane() {
        filterChips = new FlowPane(8, 8);
        filterChips.setPadding(new Insets(8));
        for (String proto : new String[]{"TCP", "UDP", "HTTP", "HTTPS", "ICMP", "ARP"}) {
            CheckBox cb = new CheckBox(proto);
            cb.setSelected(true);
            cb.setOnAction(e -> refreshPredicate());
            filterChips.getChildren().add(cb);
        }
        return new TitledPane("Protocol Filters", new ScrollPane(filterChips));
    }

    private TableView<CapturedPacket> buildTable() {
        TableView<CapturedPacket> tv = new TableView<>(view);
        addColumn(tv, "No", "number", 50);
        addColumn(tv, "Time", "timestamp", 120);
        addColumn(tv, "Src IP", "sourceIP", 140);
        addColumn(tv, "Dst IP", "destinationIP", 140);
        addColumn(tv, "Proto", "protocol", 80);

        tv.setRowFactory(tvRow -> {
            TableRow<CapturedPacket> row = new TableRow<>();
            row.setOnContextMenuRequested(evt -> {
                ContextMenu ctx = new ContextMenu();
                MenuItem follow = new MenuItem("Follow Stream");
                follow.setOnAction(a -> switchView("Follow Stream"));
                MenuItem copyHex = new MenuItem("Copy Hex Dump");
                copyHex.setOnAction(a -> {
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(row.getItem().getHexDump());
                    Clipboard.getSystemClipboard().setContent(cc);
                });
                ctx.getItems().addAll(follow, copyHex);
                ctx.show(row, evt.getScreenX(), evt.getScreenY());
            });
            return row;
        });
        tv.getSelectionModel().selectedItemProperty().addListener((o,old,sel) -> {
            if (sel != null) inspector.display(sel.getDetail());
        });
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return tv;
    }

    private <T> void addColumn(TableView<CapturedPacket> table, String title, String prop, int width) {
        TableColumn<CapturedPacket, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setPrefWidth(width);
        table.getColumns().add(col);
    }

    private void handleGlobalShortcuts(KeyEvent e) {
        if (e.isControlDown() && e.getCode() == KeyCode.K) openCommandPalette();
        if (e.isControlDown() && e.getCode() == KeyCode.F) searchField.requestFocus();
    }

    private void startCapture() {
        new AnimationTimer() {
            private long lastF = System.nanoTime(), count = 0;

            @Override public void handle(long now) {
                while (!svc.queue().isEmpty()) {
                    CapturedPacket cp = Objects.requireNonNull(svc.queue().poll()).toPacket();
                    if (!packets.isEmpty()) {
                        long prev = packets.get(packets.size()-1).getTimestampMs();
                        cp.setDeltaTime(cp.getTimestampMs() - prev);
                    }
                    packets.add(cp);
                    updateConversationStats(cp);
                    updateStats(cp);
                    count++;
                }
                if (now - lastF > 1_000_000_000L) {
                    double sec = (now - lastF) / 1e9;
                    timeSeries.getData().add(new XYChart.Data<>(
                            Duration.between(startTime, LocalDateTime.now()).toMillis()/1000.0,
                            count/sec
                    ));
                    lastF = now; count = 0;
                    refreshConversationList();
                }
                if (autoScrollToggle.isSelected() && !packets.isEmpty()) table.scrollTo(packets.size()-1);
            }
        }.start();
        svc.startCapture("eth0", "", 0);
    }

    private void updateConversationStats(CapturedPacket p) {
        String conv = p.getSourceIP() + " → " + p.getDestinationIP();
        convCounts.computeIfAbsent(conv, k -> new AtomicInteger()).incrementAndGet();
    }

    private void refreshConversationList() {
        var top = convCounts.entrySet().stream()
                .sorted((a,b) -> b.getValue().get() - a.getValue().get())
                .limit(5)
                .map(e -> String.format("%s (%d pkt)", e.getKey(), e.getValue().get()))
                .collect(Collectors.toList());
        Platform.runLater(() -> convList.setItems(FXCollections.observableArrayList(top)));
    }

    private void refreshPredicate() {
        Predicate<CapturedPacket> protoPred = p -> filterChips.getChildren().stream()
                .filter(node -> ((CheckBox) node).isSelected())
                .anyMatch(node -> ((CheckBox) node).getText().equalsIgnoreCase(p.getProtocol()));
        String txt = searchField.getText().trim();
        Predicate<CapturedPacket> ipPred = FilterEngine.byIp(txt.matches("\\d+(.\\d+){3}") ? txt : "");
        Predicate<CapturedPacket> portPred = FilterEngine.byPort(txt.matches("\\d+") ? Integer.parseInt(txt) : 0);
        String sel = convList.getSelectionModel().getSelectedItem();
        Predicate<CapturedPacket> convPred = p -> sel == null || sel.startsWith(p.getSourceIP() + " → " + p.getDestinationIP());
        view.setPredicate(FilterEngine.combine(protoPred, ipPred, portPred, convPred));
    }

    private void updateStats(CapturedPacket p) {
        protoSeries.getData().stream()
                .filter(d -> d.getXValue().equals(p.getProtocol()))
                .findFirst()
                .ifPresentOrElse(d -> d.setYValue(d.getYValue().intValue()+1),
                        () -> protoSeries.getData().add(new XYChart.Data<>(p.getProtocol(),1)));
    }
}