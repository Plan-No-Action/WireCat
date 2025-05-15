package com.wirecat.core_capture.filter;

import com.wirecat.core_capture.CapturedPacket;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FilterEngine {
    public static Predicate<CapturedPacket> byProtocol(String p) {
        if ("All".equalsIgnoreCase(p)) return pkt -> true;
        return pkt -> pkt.getProtocol().equalsIgnoreCase(p);
    }

    public static Predicate<CapturedPacket> byIp(String ip) {
        if (ip == null || ip.isBlank()) return pkt -> true;
        return pkt -> pkt.getSourceIP().equals(ip) ||
                      pkt.getDestinationIP().equals(ip);
    }

    public static Predicate<CapturedPacket> byPort(int port) {
        if (port <= 0) return pkt -> true;
        return pkt -> pkt.getSourcePort() == port ||
                      pkt.getDestinationPort() == port;
    }

    @SafeVarargs
    public static Predicate<CapturedPacket> combine(
            Predicate<CapturedPacket>... ps) {
        return Stream.of(ps)
                     .reduce(x -> true, Predicate::and);
    }
}
