package com.wirecat.core_capture;

import org.pcap4j.packet.TcpPacket;
import java.util.*;

/**
 * Tracks and reconstructs TCP streams (for Follow Stream feature).
 */
public class TcpStreamReassembler {
    // Key: stream ID (srcIP:srcPort-dstIP:dstPort, both directions)
    private final Map<String, List<PacketModel>> streams = new HashMap<>();

    public void addPacket(PacketModel pm) {
        if (!"TCP".equals(pm.getProto()) && !"TCPv6".equals(pm.getProto())) return;
        String key = getStreamKey(pm);
        streams.computeIfAbsent(key, k -> new ArrayList<>()).add(pm);
    }

    public List<PacketModel> getStream(String streamKey) {
        return streams.getOrDefault(streamKey, Collections.emptyList());
    }

    public Set<String> getAllStreamKeys() {
        return streams.keySet();
    }

    // Helper: canonicalize stream key (bidirectional)
    public static String getStreamKey(PacketModel pm) {
        String a = pm.getSrc() + ":" + pm.getSrcPort();
        String b = pm.getDst() + ":" + pm.getDstPort();
        return (a.compareTo(b) <= 0) ? a + "-" + b : b + "-" + a;
    }
}
