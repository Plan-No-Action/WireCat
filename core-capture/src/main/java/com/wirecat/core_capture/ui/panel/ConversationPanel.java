package com.wirecat.core_capture.ui.panel;

import com.wirecat.core_capture.model.Conversation;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

public class ConversationPanel extends VBox {

    public ConversationPanel(ObservableList<Conversation> convList, Consumer<Conversation> onSelect) {
        getStyleClass().add("conversation-panel");
        setPadding(new Insets(4, 4, 4, 4));
        setSpacing(2);

        Label title = new Label("🗨️ Conversations");
        title.getStyleClass().add("label");

        TableView<Conversation> table = new TableView<>();
        table.setItems(convList);
        table.setFixedCellSize(21);
        table.setStyle("-fx-font-size: 10.5px; -fx-cell-size: 20px;");

        // --- Source MAC with Vendor
        TableColumn<Conversation, String> srcMacCol = new TableColumn<>("Src");
        srcMacCol.setCellValueFactory(c -> new SimpleStringProperty(
                macWithVendor(c.getValue().getSrcMAC())
        ));
        srcMacCol.setPrefWidth(120);

        // --- Destination MAC with Vendor
        TableColumn<Conversation, String> dstMacCol = new TableColumn<>("Dst");
        dstMacCol.setCellValueFactory(c -> new SimpleStringProperty(
                macWithVendor(c.getValue().getDstMAC())
        ));
        dstMacCol.setPrefWidth(120);

        // --- Number of Packets
        TableColumn<Conversation, Number> pktCol = new TableColumn<>("#");
        pktCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getPacketCount()));
        pktCol.setPrefWidth(40);

        table.getColumns().addAll(srcMacCol, dstMacCol, pktCol);

        // --- FIX: this wires the callback so clicking a conversation filters the main table ---
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> onSelect.accept(sel));
        // ---------------------------------------------------------------------------------------

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(title, table);
        VBox.setVgrow(this, Priority.ALWAYS);
    }

    // Basic vendor detection: replace with a proper OUI lookup as needed
    private String macWithVendor(String mac) {
        if (mac == null || mac.length() < 8) return mac;
        String oui = mac.replace(":", "").substring(0, 6).toUpperCase();
        return switch (oui) {
            case "FC349E", "D89EF3", "70E72C", "B827EB", "3C0754", "FCECDA", "A4B197", "C85B76", "28CFDA" -> mac + " (Apple)";
            // Dell
            case "F4E9D4", "A4BADB", "00219B", "0016D3", "68F728", "54E1AD", "B4B676" -> mac + " (Dell)";
            // Lenovo/IBM
            case "64A651", "74E50B", "1008B1", "00216D" -> mac + " (Lenovo)";
            // HP
            case "3C52E1", "E0DB55", "F0D5BF", "347E5C", "6CB7F4", "88532E" -> mac + " (HP)";
            // Asus
            case "98E743", "F8CAB8", "68D925", "A44E31" -> mac + " (Asus)";
            // Acer
            case "08EDB9", "C0E54E" -> mac + " (Acer)";
            // Microsoft
            case "BC83A7", "5C514F", "3497F6" -> mac + " (Microsoft)";
            // Samsung
            case "A088B4", "8C7712", "B86B23" -> mac + " (Samsung)";
            // Huawei
            case "F4F5E8", "001E10", "6C1A84", "4C1FCC", "E4956E" -> mac + " (Huawei)";
            // Xiaomi
            case "7C1EB3", "30B5C2", "38D269", "54E6FC", "2C4D54" -> mac + " (Xiaomi)";
            // Google
            case "A0B4A5", "5CBAEF", "64006A" -> mac + " (Google)";
            // TP-Link
            case "50C7BF", "B0BE76", "F4F26D", "A044E2" -> mac + " (TP-Link)";
            // Cisco
            case "00163E", "F4CE46", "001B54", "0023AE", "0021A0" -> mac + " (Cisco)";
            // Netgear
            case "388345", "C02506" -> mac + " (Netgear)";
            // Ubiquiti
            case "24A43C", "44D9E7", "7825AD" -> mac + " (Ubiquiti)";
            // VirtualBox
            case "080027" -> mac + " (VirtualBox)";
            // VMware
            case "000C29", "000569", "005056", "001C14", "0050F2" -> mac + " (VMware)";
            // Parallels
            case "001C42" -> mac + " (Parallels VM)";
            // QEMU/KVM
            case "525400" -> mac + " (QEMU/KVM)";
            // Intel
            case "001B21", "F8BC12" -> mac + " (Intel)";
            // Realtek
            case "001C25", "E894F6" -> mac + " (Realtek)";
            // Zyxel
            case "00235A", "000C43" -> mac + " (Zyxel)";
            // D-Link
            case "001C0F", "001D0F" -> mac + " (D-Link)";
            // Linksys/Belkin
            case "001839", "F4EC38" -> mac + " (Linksys/Belkin)";
            // Arduino
            case "04E548" -> mac + " (Arduino)";
            // Amazon (Echo, Fire TV)
            case "F0D2F1", "441CA8", "B4F1DA" -> mac + " (Amazon)";
            // Roku
            case "0806C8", "000D4B" -> mac + " (Roku)";
            // Broadcom (set-top, IoT, TVs)
            case "00065B", "001018" -> mac + " (Broadcom)";
            // Sony
            case "B0C559", "DC2B2A", "F05A09" -> mac + " (Sony)";
            // LG
            case "00256C", "AC7A4D", "5C497D" -> mac + " (LG)";
            // Philips
            case "00146D", "20E52A" -> mac + " (Philips)";
            // Murata (IoT, embedded)
            case "0022FA" -> mac + " (Murata)";
            // Bosch (IoT)
            case "0012F2" -> mac + " (Bosch)";
            // Bosch Rexroth
            case "00260A" -> mac + " (Bosch Rexroth)";
            // Segway/Ninebot (scooter IoT)
            case "A0A868" -> mac + " (Ninebot)";
            // Juniper
            case "000173", "001D45" -> mac + " (Juniper)";
            // Aruba
            case "002233", "000D28" -> mac + " (Aruba)";
            // Fortinet
            case "0019E0" -> mac + " (Fortinet)";
            // MikroTik
            case "4C5E0C", "6C3B6B" -> mac + " (MikroTik)";
            // OpenWRT
            case "A4D18F" -> mac + " (OpenWRT)";
            // UniFi
            case "18E829" -> mac + " (UniFi)";
            // Ruckus Wireless
            case "884AE9" -> mac + " (Ruckus)";
            // Avaya
            case "001A4B" -> mac + " (Avaya)";
            // Brother (printers)
            case "402E28" -> mac + " (Brother)";
            // Canon (printers)
            case "D8E0E1" -> mac + " (Canon)";
            // Epson (printers)
            case "C8A030" -> mac + " (Epson)";
            // Zebra (barcode printers)
            case "44A842" -> mac + " (Zebra)";
            // Ricoh
            case "001F29" -> mac + " (Ricoh)";
            // Honeywell
            case "485B39" -> mac + " (Honeywell)";
            // Chicony
            case "AC3FA4" -> mac + " (Chicony)";
            // NVIDIA (Jetson, SHIELD TV)
            case "6C8FB5", "04E5B8" -> mac + " (NVIDIA)";
            // Espressif (ESP32, IoT)
            case "24A160", "24DC39" -> mac + " (Espressif)";
            // Tuya/SmartLife IoT
            case "A4C138", "7C49EB" -> mac + " (Tuya)";
            // Foscam (cams)
            case "8853D4" -> mac + " (Foscam)";
            // TPV (Philips TV)
            case "C8F650" -> mac + " (TPV/Philips)";
            // Nintendo
            case "A0F3C1", "0023CC" -> mac + " (Nintendo)";
            // ...add more as you want
            default -> mac;
        };
    }
}
