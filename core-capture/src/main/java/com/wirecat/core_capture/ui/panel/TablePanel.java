package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.CapturedPacket;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TablePanel extends VBox {
    public final TableView<CapturedPacket> table;

    public TablePanel(FilteredList<CapturedPacket> view, Consumer<CapturedPacket> onRowSelected) {
        this.table = new TableView<>();
        table.setItems(view);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Columns (copy from your old code)
        table.getColumns().addAll(
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

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (onRowSelected != null && selected != null) {
                onRowSelected.accept(selected);
            }
        });

        table.setContextMenu(buildContextMenu());

        getChildren().add(table);
        setSpacing(0);
    }

    private <T> TableColumn<CapturedPacket, T> col(String title, String prop, int w) {
        TableColumn<CapturedPacket, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    private ContextMenu buildContextMenu() {
        MenuItem copy = new MenuItem("Copy rows");
        copy.setOnAction(e -> {
            var sel = table.getSelectionModel().getSelectedItems();
            ClipboardContent cc = new ClipboardContent();
            cc.putString(sel.stream()
                    .map(p -> p.getNumber()+"\t"+p.getTimestamp()+"\t"+p.getSourceIP()+"→"+p.getDestinationIP())
                    .collect(Collectors.joining("\n")));
            Clipboard.getSystemClipboard().setContent(cc);
        });
        return new ContextMenu(copy);
    }

    public TableView<CapturedPacket> getTableView() {
        return table;
    }
}
