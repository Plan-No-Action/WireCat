// src/main/java/com/wirecat/core_capture/Packet.java
package com.wirecat.core_capture.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CustomPacket {
    private final IntegerProperty no;
    private final StringProperty time;
    private final StringProperty src;
    private final StringProperty dst;
    private final StringProperty proto;
    private final IntegerProperty len;
    private final StringProperty hexDump;
    private final StringProperty asciiDump;

    public CustomPacket(int no, String time, String src, String dst, String proto, int len,
                  String hexDump, String asciiDump) {
        this.no        = new SimpleIntegerProperty(no);
        this.time      = new SimpleStringProperty(time);
        this.src       = new SimpleStringProperty(src);
        this.dst       = new SimpleStringProperty(dst);
        this.proto     = new SimpleStringProperty(proto);
        this.len       = new SimpleIntegerProperty(len);
        this.hexDump   = new SimpleStringProperty(hexDump);
        this.asciiDump = new SimpleStringProperty(asciiDump);
    }

    public int    getNo()       { return no.get(); }
    public String getTime()     { return time.get(); }
    public String getSrc()      { return src.get(); }
    public String getDst()      { return dst.get(); }
    public String getProto()    { return proto.get(); }
    public int    getLen()      { return len.get(); }
    public String getHexDump()  { return hexDump.get(); }
    public String getAsciiDump(){ return asciiDump.get(); }
}
