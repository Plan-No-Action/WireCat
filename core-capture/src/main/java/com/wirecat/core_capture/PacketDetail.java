package com.wirecat.core_capture;

import java.util.Map;

public class PacketDetail {
    private final String sourceMAC;
    private final String destinationMAC;
    private final String ethernetType;
    private final String sourceIP;
    private final String destinationIP;
    private final String protocol;
    private final String transportName;
    private final Map<String,String> transportInfo;

    public PacketDetail(String sourceMAC,
                        String destinationMAC,
                        String ethernetType,
                        String sourceIP,
                        String destinationIP,
                        String protocol,
                        String transportName,
                        Map<String,String> transportInfo) {
        this.sourceMAC = sourceMAC;
        this.destinationMAC = destinationMAC;
        this.ethernetType = ethernetType;
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.protocol = protocol;
        this.transportName = transportName;
        this.transportInfo = transportInfo;
    }

    public String getSourceMAC()      { return sourceMAC; }
    public String getDestinationMAC() { return destinationMAC; }
    public String getEthernetType()   { return ethernetType; }
    public String getSourceIP()       { return sourceIP; }
    public String getDestinationIP()  { return destinationIP; }
    public String getProtocol()       { return protocol; }
    public String getTransportName()  { return transportName; }
    public Map<String,String> getTransportInfo() {
        return transportInfo;
    }
}