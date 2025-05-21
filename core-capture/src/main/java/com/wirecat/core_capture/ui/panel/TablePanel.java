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
    private final Set<String> selectedProtocols = new HashSet<>();
    private boolean autoScroll = true;

    public TablePanel(FilteredList<CapturedPacket> filteredList, Consumer<CapturedPacket> onRowSelected) {
        this.filteredData = filteredList;
        this.tableView = new TableView<>();
        this.tableView.setItems(filteredData);
        this.getStyleClass().add("table-panel");
        this.setSpacing(0);
        this.setStyle("-fx-background-color:transparent;");

        // Table Columns - improved headers, padding, right/left align, truncation
        tableView.getColumns().addAll(
                col("No", "number", 48, "center"),
                col("Time", "timestamp", 110, "center"),
                col("Δ Time", "deltaTime", 60, "center"),
                colWithTooltip("Src MAC", "sourceMAC", 118, "left"),
                colWithTooltip("Dst MAC", "destinationMAC", 118, "left"),
                colWithTooltip("Src IP", "sourceIP", 108, "left"),
                colWithTooltip("Dst IP", "destinationIP", 108, "left"),
                col("Proto", "protocol", 60, "center"),
                col("Src Port", "sourcePort", 55, "right"),
                col("Dst Port", "destinationPort", 55, "right"),
                col("Len", "length", 44, "right"),
                riskCol("Risk", "riskScore", 80)
        );

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tableView.setPlaceholder(new Label("No packets captured."));
        tableView.setFocusTraversable(true);

        // Row selection highlight
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (onRowSelected != null) onRowSelected.accept(selected);
        });

        // Row hover highlight
        tableView.setRowFactory(tv -> {
            TableRow<CapturedPacket> row = new TableRow<>();
            row.hoverProperty().addListener((obs, wasHovered, isHovered) -> {
                row.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("hover"), isHovered);
            });
            return row;
        });

        // Context menu (copy row, copy IP)
        ContextMenu menu = new ContextMenu();
        MenuItem copyRow = new MenuItem("Copy Row");
        copyRow.setOnAction(e -> copySelectedRow(false));
        MenuItem copySrcIP = new MenuItem("Copy Source IP");
        copySrcIP.setOnAction(e -> copySelectedRow(true));
        menu.getItems().addAll(copyRow, copySrcIP);
        tableView.setContextMenu(menu);

        this.getChildren().add(tableView);
        this.setFillWidth(true);

        selectedProtocols.addAll(Set.of("TCP", "UDP", "ICMP", "ARP", "HTTP", "HTTPS"));
        updatePredicate();
    }

    public TableView<CapturedPacket> getTableView() { return tableView; }

    // --- Filtering logic
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
        if (autoScroll && !tableView.getItems().isEmpty())
            tableView.scrollTo(tableView.getItems().size() - 1);
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

    // --- Column Factories

    private <T> TableColumn<CapturedPacket, T> col(String title, String prop, int w, String align) {
        TableColumn<CapturedPacket, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        c.setStyle("-fx-alignment:" +
                switch (align) {
                    case "right" -> "CENTER-RIGHT;";
                    case "center" -> "CENTER;";
                    default -> "CENTER-LEFT;";
                }
        );
        // Text truncation and padding
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toString());
                setTooltip(empty || item == null ? null : new Tooltip(item.toString()));
                setStyle("-fx-padding: 0 8 0 8; -fx-font-size:13px;");
                if (!empty && item != null && item.toString().length() > 20)
                    setText(item.toString().substring(0, 17) + "…");
            }
        });
        return c;
    }

    // Tooltip and left-align for IP/MACs
    private <T> TableColumn<CapturedPacket, T> colWithTooltip(String title, String prop, int w, String align) {
        return col(title, prop, w, align);
    }

    // Modern color-coded risk badge
    private TableColumn<CapturedPacket, Double> riskCol(String title, String prop, int w) {
        TableColumn<CapturedPacket, Double> c = new TableColumn<>(title);
        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null); setStyle("");
                } else {
                    String label; String color;
                    if (item < 3.0)      { label = "Low";    color = "#35e171"; }
                    else if (item < 7.0) { label = "Med";    color = "#ffd60a"; }
                    else                 { label = "High";   color = "#ff453a"; }
                    Label badge = new Label(label + " (" + String.format("%.1f", item) + ")");
                    badge.getStyleClass().add("risk-badge");
                    setGraphic(badge); setText(null); setStyle("-fx-alignment:CENTER;");
                }
            }
        });
        return c;
    }

    // --- Context menu actions
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
