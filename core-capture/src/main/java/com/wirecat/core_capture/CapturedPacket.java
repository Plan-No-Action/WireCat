package com.wirecat.core_capture;

import javafx.beans.property.*;

public class CapturedPacket {
    private final IntegerProperty number          = new SimpleIntegerProperty();
    private final StringProperty  timestamp       = new SimpleStringProperty();
    private final StringProperty  interfaceName   = new SimpleStringProperty();
    private final StringProperty  sourceMAC       = new SimpleStringProperty();
    private final StringProperty  destinationMAC  = new SimpleStringProperty();
    private final StringProperty  sourceIP        = new SimpleStringProperty();
    private final StringProperty  destinationIP   = new SimpleStringProperty();
    private final StringProperty  protocol        = new SimpleStringProperty();
    private final IntegerProperty sourcePort      = new SimpleIntegerProperty();
    private final IntegerProperty destinationPort = new SimpleIntegerProperty();
    private final IntegerProperty length          = new SimpleIntegerProperty();
    private final StringProperty  hexDump         = new SimpleStringProperty();
    private final StringProperty  asciiDump       = new SimpleStringProperty();
    private final DoubleProperty  riskScore       = new SimpleDoubleProperty();

    public CapturedPacket(int number, String timestamp, String iface,
                          String srcMac, String dstMac,
                          String srcIp,  String dstIp,
                          String proto,  int srcPort, int dstPort,
                          int len, String hex, String ascii, double risk) {

        this.number         .set(number);
        this.timestamp      .set(timestamp);
        this.interfaceName  .set(iface);
        this.sourceMAC      .set(srcMac);
        this.destinationMAC .set(dstMac);
        this.sourceIP       .set(srcIp);
        this.destinationIP  .set(dstIp);
        this.protocol       .set(proto);
        this.sourcePort     .set(srcPort);
        this.destinationPort.set(dstPort);
        this.length         .set(len);
        this.hexDump        .set(hex);
        this.asciiDump      .set(ascii);
        this.riskScore      .set(risk);
    }

    public int    getNumber()          { return number.get(); }
    public String getTimestamp()       { return timestamp.get(); }
    public String getInterfaceName()   { return interfaceName.get(); }
    public String getSourceMAC()       { return sourceMAC.get(); }
    public String getDestinationMAC()  { return destinationMAC.get(); }
    public String getSourceIP()        { return sourceIP.get(); }
    public String getDestinationIP()   { return destinationIP.get(); }
    public String getProtocol()        { return protocol.get(); }
    public int    getSourcePort()      { return sourcePort.get(); }
    public int    getDestinationPort() { return destinationPort.get(); }
    public int    getLength()          { return length.get(); }
    public String getHexDump()         { return hexDump.get(); }
    public String getAsciiDump()       { return asciiDump.get(); }
    public double getRiskScore()       { return riskScore.get(); }

    public IntegerProperty numberProperty()          { return number; }
    public StringProperty  timestampProperty()       { return timestamp; }
    public StringProperty  sourceMACProperty()       { return sourceMAC; }
    public StringProperty  destinationMACProperty()  { return destinationMAC; }
    public StringProperty  sourceIPProperty()        { return sourceIP; }
    public StringProperty  destinationIPProperty()   { return destinationIP; }
    public StringProperty  protocolProperty()        { return protocol; }
    public IntegerProperty sourcePortProperty()      { return sourcePort; }
    public IntegerProperty destinationPortProperty() { return destinationPort; }
    public IntegerProperty lengthProperty()          { return length; }
    public DoubleProperty  riskScoreProperty()       { return riskScore; }
}
