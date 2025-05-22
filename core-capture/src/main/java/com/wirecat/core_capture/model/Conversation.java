package com.wirecat.core_capture.model;

public class Conversation {
    private final String srcIP;
    private final int srcPort;
    private final String dstIP;
    private final int dstPort;
    private final String proto;

    private final String srcMAC;
    private final String dstMAC;

    private long firstTimestamp;
    private long lastTimestamp;
    private int packetCount;
    private long totalBytes;

    public Conversation(
            String srcIP, int srcPort, String dstIP, int dstPort, String proto,
            long firstTimestamp, int firstLen,
            String sourceMAC, String destinationMAC) {
        this.srcIP = srcIP;
        this.srcPort = srcPort;
        this.dstIP = dstIP;
        this.dstPort = dstPort;
        this.proto = proto;
        this.srcMAC = sourceMAC;       // FIXED!
        this.dstMAC = destinationMAC;  // FIXED!
        this.firstTimestamp = this.lastTimestamp = firstTimestamp;
        this.packetCount = 1;
        this.totalBytes = firstLen;
    }

    public void addPacket(int len, long timestamp) {
        packetCount++;
        totalBytes += len;
        lastTimestamp = timestamp;
    }

    public String getProto() { return proto; }
    public String getSrcIP() { return srcIP; }
    public int getSrcPort() { return srcPort; }
    public String getDstIP() { return dstIP; }
    public int getDstPort() { return dstPort; }
    public long getFirstTimestamp() { return firstTimestamp; }
    public long getLastTimestamp() { return lastTimestamp; }
    public int getPacketCount() { return packetCount; }
    public long getTotalBytes() { return totalBytes; }

    // MAC getters
    public String getSrcMAC() { return srcMAC; }
    public String getDstMAC() { return dstMAC; }
}
