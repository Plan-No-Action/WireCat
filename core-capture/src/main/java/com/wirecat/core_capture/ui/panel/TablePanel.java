package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.CapturedPacket;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class TablePanel extends VBox {
    private final TableView<CapturedPacket> tableView;
    private final FilteredList<CapturedPacket> filteredData;

    private String searchText = "";
    private Set<String> selectedProtocols = new HashSet<>();
    private boolean autoScroll = true;

    public TablePanel(FilteredList<CapturedPacket> filteredList, Consumer<CapturedPacket> onRowSelected) {
        this.filteredData = filteredList;
        this.tableView = new TableView<>();
        this.tableView.setItems(filteredData);
        this.getStyleClass().add("table-panel");
        this.setSpacing(2);

        // Table Columns
        tableView.getColumns().addAll(
                col("No", "number", 50),
                col("Time", "timestamp", 110),
                col("Δ Time", "deltaTime", 60),
                colWithTooltip("Src MAC", "sourceMAC", 125),
                colWithTooltip("Dst MAC", "destinationMAC", 125),
                colWithTooltip("Src IP", "sourceIP", 120),
                colWithTooltip("Dst IP", "destinationIP", 120),
                col("Proto", "protocol", 70),
                col("Src Port", "sourcePort", 65),
                col("Dst Port", "destinationPort", 65),
                col("Len", "length", 60),
                riskCol("Risk", "riskScore", 65)
        );

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tableView.setPlaceholder(new Label("No packets captured."));
        tableView.setFocusTraversable(true);

        // Row selection
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (onRowSelected != null) onRowSelected.accept(selected);
        });

        // Context menu
        ContextMenu menu = new ContextMenu();
        MenuItem copyRow = new MenuItem("Copy Row");
        copyRow.setOnAction(e -> copySelectedRow(false));
        MenuItem copySrcIP = new MenuItem("Copy Source IP");
        copySrcIP.setOnAction(e -> copySelectedRow(true));
        menu.getItems().addAll(copyRow, copySrcIP);
        tableView.setContextMenu(menu);

        // Keyboard nav: focus highlight
        tableView.setOnKeyPressed(event -> tableView.requestFocus());

        this.getChildren().add(tableView);
        this.setFillWidth(true);

        // Defaults
        selectedProtocols.addAll(Set.of("TCP","UDP","ICMP","ARP","HTTP","HTTPS"));
        updatePredicate();
    }

    public TableView<CapturedPacket> getTableView() { return tableView; }

    public void filterBySearch(String search) {
        this.searchText = search == null ? "" : search.trim();
        updatePredicate();
    }
    public void filterByProtocols(java.util.List<String> protocols) {
        this.selectedProtocols.clear();
        if (protocols != null) this.selectedProtocols.addAll(protocols);
        updatePredicate();
    }
    public void setAutoScroll(boolean enabled) { this.autoScroll = enabled; }
    public void scrollToBottom() {
        if (autoScroll && !tableView.getItems().isEmpty()) {
            tableView.scrollTo(tableView.getItems().size() - 1);
            // Optional: Highlight new row for 1 second
            int last = tableView.getItems().size() - 1;
            TableRow<CapturedPacket> row = getRow(last);
            if (row != null) {
                row.getStyleClass().add("new-row");
                new Thread(() -> {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> row.getStyleClass().remove("new-row"));
                }).start();
            }
        }
    }

    // -- Helper for getting TableRow --
    private TableRow<CapturedPacket> getRow(int index) {
        for (Object obj : tableView.lookupAll(".table-row-cell")) {
            TableRow<CapturedPacket> row = (TableRow<CapturedPacket>) obj;
            if (row.getIndex() == index) return row;
        }
        return null;
    }

    private void updatePredicate() {
        filteredData.setPredicate(p -> {
            boolean protoMatch = selectedProtocols.isEmpty() || selectedProtocols.contains(p.getProtocol());
            boolean searchMatch = searchText.isEmpty() ||
                    p.getSourceIP().contains(searchText) ||
                    p.getDestinationIP().contains(searchText) ||
                    String.valueOf(p.getSourcePort()).contains(searchText) ||
                    String.valueOf(p.getDestinationPort()).contains(searchText);
            return protoMatch && searchMatch;
        });
    }

    private <T> TableColumn<CapturedPacket, T> col(String title, String prop, int w) {
        TableColumn<CapturedPacket, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    // Column with Tooltip
    private <T> TableColumn<CapturedPacket, T> colWithTooltip(String title, String prop, int w) {
        TableColumn<CapturedPacket, T> c = col(title, prop, w);
        c.setCellFactory(tc -> {
            TableCell<CapturedPacket, T> cell = new TableCell<>() {
                @Override protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item == null ? "" : item.toString());
                    setTooltip(empty || item == null ? null : new Tooltip(item.toString()));
                }
            };
            return cell;
        });
        return c;
    }

    // Risk Badge Column
    private TableColumn<CapturedPacket, Double> riskCol(String title, String prop, int w) {
        TableColumn<CapturedPacket, Double> c = new TableColumn<>(title);
        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String label;
                    String color;
                    if (item < 3.0)      { label = "Low";    color = "#34c759"; }
                    else if (item < 7.0) { label = "Medium"; color = "#ffd60a"; }
                    else                 { label = "High";   color = "#ff453a"; }
                    setText(label + " (" + String.format("%.1f", item) + ")");
                    setStyle("-fx-background-radius:7;-fx-background-color:" + color + ";-fx-text-fill:#181e19; -fx-font-weight:bold;");
                }
            }
        });
        return c;
    }

    private void copySelectedRow(boolean onlyIP) {
        CapturedPacket p = tableView.getSelectionModel().getSelectedItem();
        if (p != null) {
            String text = onlyIP ? p.getSourceIP() :
                    String.join("\t",
                            "" + p.getNumber(), p.getTimestamp(),
                            p.getSourceIP() + ":" + p.getSourcePort(),
                            p.getDestinationIP() + ":" + p.getDestinationPort(),
                            p.getProtocol(), "" + p.getLength(), "" + p.getRiskScore()
                    );
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }
}
