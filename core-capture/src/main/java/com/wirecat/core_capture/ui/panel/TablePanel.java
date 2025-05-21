package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.CapturedPacket;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

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

        tableView.getColumns().addAll(
                col("No", "number", 60),
                col("Time", "timestamp", 120),
                col("Δ Time", "deltaTime", 80),
                col("Src MAC", "sourceMAC", 140),
                col("Dst MAC", "destinationMAC", 140),
                col("Src IP", "sourceIP", 140),
                col("Dst IP", "destinationIP", 140),
                col("Proto", "protocol", 80),
                col("Src Port", "sourcePort", 80),
                col("Dst Port", "destinationPort", 80),
                col("Len", "length", 60),
                col("Risk", "riskScore", 60)
        );
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tableView.setPlaceholder(new Label("No packets captured."));

        // Row selection
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (onRowSelected != null) onRowSelected.accept(selected);
        });

        this.getChildren().add(tableView);
        this.setFillWidth(true);

        // Defaults
        selectedProtocols.addAll(Set.of("TCP","UDP","ICMP","ARP","HTTP","HTTPS"));
        updatePredicate();
    }

    public TableView<CapturedPacket> getTableView() { return tableView; }

    // API for MainView/TopBarPanel:
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
    private <T> TableColumn<CapturedPacket, T> col(String title, String prop, int w) {
        TableColumn<CapturedPacket, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }
}
