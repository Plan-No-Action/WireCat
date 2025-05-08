// src/main/java/com/wirecat/core_capture/inspector/PacketInspector.java
package com.wirecat.core_capture.inspector;

<<<<<<< Updated upstream
import com.wirecat.core_capture.CapturedPacket;
import javafx.scene.control.TextArea;
=======
import com.wirecat.core_capture.PacketDetail;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
>>>>>>> Stashed changes

public class PacketInspector {
    private final Accordion accordion = new Accordion();

    public void display(PacketDetail detail) {
        TextArea f = new TextArea(detail.getFrame());
        TextArea e = new TextArea(detail.getEthernet());
        TextArea i = new TextArea(detail.getIp());
        TextArea t = new TextArea(detail.getTransport());
        TextArea a = new TextArea(detail.getApplication());

        for (TextArea ta : new TextArea[]{f, e, i, t, a}) {
            ta.setEditable(false);
            ta.setWrapText(true);
        }

        accordion.getPanes().setAll(
                new TitledPane("Frame",       f),
                new TitledPane("Ethernet",    e),
                new TitledPane("IP",          i),
                new TitledPane("Transport",   t),
                new TitledPane("Application", a)
        );
    }

<<<<<<< Updated upstream
    /**  
     * Populate the two TextAreas based on the selected Packet’s dumps.
     */
    public void showPacketDetails(CapturedPacket pkt) {
        if (pkt == null) return;
        hexArea.setText(pkt.getHexDump());
        asciiArea.setText(pkt.getAsciiDump());
=======
    public Node getNode() {
        VBox box = new VBox(new Label("Packet Dissection"), accordion);
        box.setSpacing(6);
        return box;
>>>>>>> Stashed changes
    }
}
