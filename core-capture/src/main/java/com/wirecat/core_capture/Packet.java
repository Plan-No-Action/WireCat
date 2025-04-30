package com.wirecat.core_capture;
import javafx.beans.property.*;

public class Packet {
    private final IntegerProperty no = new SimpleIntegerProperty();
    private final StringProperty time = new SimpleStringProperty();
    private final StringProperty src = new SimpleStringProperty();
    private final StringProperty dst = new SimpleStringProperty();
    private final StringProperty proto = new SimpleStringProperty();
    private final IntegerProperty len = new SimpleIntegerProperty();

    public Packet(int no, String time, String src, String dst, String proto, int len) {
        this.no.set(no);
        this.time.set(time);
        this.src.set(src);
        this.dst.set(dst);
        this.proto.set(proto);
        this.len.set(len);
    }

    public IntegerProperty noProperty() { return no; }
    public StringProperty timeProperty() { return time; }
    public StringProperty srcProperty() { return src; }
    public StringProperty dstProperty() { return dst; }
    public StringProperty protoProperty() { return proto; }
    public IntegerProperty lenProperty() { return len; }
}
