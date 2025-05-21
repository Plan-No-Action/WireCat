package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.PacketDetail;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;

public class PacketInspector {
    private final TreeView<String> tree = new TreeView<>();

    public Node getNode() {
        VBox box = new VBox(new Label("Packet Dissection"), tree);
        box.setSpacing(6);
        return box;
    }
    @SuppressWarnings("unchecked")
    public void display(PacketDetail d) {
        if (d == null) return;
        TreeItem<String> root = new TreeItem<>("Frame");
        root.setExpanded(true);

        TreeItem<String> eth = new TreeItem<>("Ethernet");
        eth.getChildren().addAll(
            new TreeItem<>("Src MAC: " + d.getSourceMAC()),
            new TreeItem<>("Dst MAC: " + d.getDestinationMAC()),
            new TreeItem<>("Type:    " + d.getEthernetType())
        );

        TreeItem<String> ip = new TreeItem<>("IP");
        ip.getChildren().addAll(
            new TreeItem<>("Src IP: " + d.getSourceIP()),
            new TreeItem<>("Dst IP: " + d.getDestinationIP()),
            new TreeItem<>("Proto:  " + d.getProtocol())
        );

        root.getChildren().addAll(eth, ip);

        if (d.getTransportInfo() != null && !d.getTransportInfo().isEmpty()) {
            TreeItem<String> tr = new TreeItem<>(d.getTransportName());
            d.getTransportInfo()
             .forEach((k,v)-> tr.getChildren().add(new TreeItem<>(k + ": " + v)));
            root.getChildren().add(tr);
        }

        tree.setRoot(root);
    }
    public void setAsciiMode(boolean selected) {
        if (selected) {
            tree.setShowRoot(false);
            tree.setFixedCellSize(20);
        } else {
            tree.setShowRoot(true);
            tree.setFixedCellSize(24);
        }
    }
}