package com.wirecat.core_capture.inspector;

import com.wirecat.core_capture.CapturedPacket;
import javafx.scene.control.TextArea;

/**
 * Responsible for rendering detailed packet dumps into two panes:
 * one for hexadecimal, one for ASCII.
 */
public class PacketInspector {
    private final TextArea hexArea;
    private final TextArea asciiArea;

    public PacketInspector(TextArea hexArea, TextArea asciiArea) {
        this.hexArea   = hexArea;
        this.asciiArea = asciiArea;
    }

    /**  
     * Populate the two TextAreas based on the selected Packet’s dumps.
     */
    public void showPacketDetails(CapturedPacket pkt) {
        if (pkt == null) return;
        hexArea.setText(pkt.getHexDump());
        asciiArea.setText(pkt.getAsciiDump());
    }
}
