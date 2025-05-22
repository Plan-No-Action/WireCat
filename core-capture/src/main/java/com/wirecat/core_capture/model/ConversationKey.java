package com.wirecat.core_capture.model;

import java.util.Objects;

public class ConversationKey {
    private final String srcIP, dstIP, proto;
    private final int srcPort, dstPort;

    public ConversationKey(String srcIP, int srcPort, String dstIP, int dstPort, String proto) {
        this.srcIP = srcIP; this.srcPort = srcPort;
        this.dstIP = dstIP; this.dstPort = dstPort;
        this.proto = proto;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversationKey k)) return false;
        return srcPort == k.srcPort && dstPort == k.dstPort &&
                Objects.equals(srcIP, k.srcIP) && Objects.equals(dstIP, k.dstIP) &&
                Objects.equals(proto, k.proto);
    }
    @Override public int hashCode() {
        return Objects.hash(srcIP, srcPort, dstIP, dstPort, proto);
    }
}
