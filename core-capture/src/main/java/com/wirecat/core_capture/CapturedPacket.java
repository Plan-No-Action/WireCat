package com.wirecat.core_capture;

public class CapturedPacket {
    private final int number;
    private final String timestamp;       // human‑readable time string
    private final long timestampMs;       // epoch millis
    private long deltaTime;               // ms since previous packet
    private final String sourceMAC;
    private final String destinationMAC;
    private final String sourceIP;
    private final String destinationIP;
    private final String protocol;
    private final int sourcePort;
    private final int destinationPort;
    private final int length;
    private final String hexDump;
    private final String asciiDump;
    private final double riskScore;
    private final PacketDetail detail;    // parsed per‑layer detail

    public CapturedPacket(int number,
                          String timestamp,
                          long timestampMs,
                          long deltaTime,
                          String sourceMAC,
                          String destinationMAC,
                          String sourceIP,
                          String destinationIP,
                          String protocol,
                          int sourcePort,
                          int destinationPort,
                          int length,
                          String hexDump,
                          String asciiDump,
                          double riskScore,
                          PacketDetail detail) {
        this.number = number;
        this.timestamp = timestamp;
        this.timestampMs = timestampMs;
        this.deltaTime = deltaTime;
        this.sourceMAC = sourceMAC;
        this.destinationMAC = destinationMAC;
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.protocol = protocol;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.length = length;
        this.hexDump = hexDump;
        this.asciiDump = asciiDump;
        this.riskScore = riskScore;
        this.detail = detail;
    }

    // Properties for TableView
    public int getNumber()           { return number; }
    public String getTimestamp()     { return timestamp; }
    public long getDeltaTime()       { return deltaTime; }
    public String getSourceMAC()     { return sourceMAC; }
    public String getDestinationMAC(){ return destinationMAC; }
    public String getSourceIP()      { return sourceIP; }
    public String getDestinationIP() { return destinationIP; }
    public String getProtocol()      { return protocol; }
    public int getSourcePort()       { return sourcePort; }
    public int getDestinationPort()  { return destinationPort; }
    public int getLength()           { return length; }
    public String getHexDump()       { return hexDump; }
    public String getAsciiDump()     { return asciiDump; }
    public double getRiskScore()     { return riskScore; }

    // Millis timestamp for Δ Time
    public long getTimestampMs()     { return timestampMs; }
    public void setDeltaTime(long dt){ this.deltaTime = dt; }

    // Inspector detail
    public PacketDetail getDetail()  { return detail; }
    public String toAnalysisString() {
    return String.format(
        "Packet #%d\nProtocol: %s\nTimestamp: %s\n"
        + "Source: %s:%d (%s)\nDestination: %s:%d (%s)\n"
        + "Size: %d bytes\nRisk Score: %.1f\n"
        + "Hex Start: %s...\nASCII Start: %s...",
        number, protocol, timestamp,
        sourceIP, sourcePort, sourceMAC,
        destinationIP, destinationPort, destinationMAC,
        length, riskScore,
        hexDump.substring(0, Math.min(50, hexDump.length())),
        asciiDump.substring(0, Math.min(50, asciiDump.length()))
    );
}
}
