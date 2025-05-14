package com.wirecat.core_capture;

import java.util.HashMap;
import java.util.Map;

import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;

import javafx.beans.property.*;

public class PacketModel {

    private final IntegerProperty no       = new SimpleIntegerProperty();
    private final StringProperty  time     = new SimpleStringProperty();
    private final StringProperty  src      = new SimpleStringProperty();
    private final StringProperty  dst      = new SimpleStringProperty();
    private final StringProperty  proto    = new SimpleStringProperty();
    private final IntegerProperty srcPort  = new SimpleIntegerProperty();
    private final IntegerProperty dstPort  = new SimpleIntegerProperty();
    private final IntegerProperty len      = new SimpleIntegerProperty();
    private final StringProperty  hexDump  = new SimpleStringProperty();
    private final StringProperty  asciiDump= new SimpleStringProperty();
    private final DoubleProperty  riskScore= new SimpleDoubleProperty();

    // Populated during fromRaw()
    private String srcMac = "—";
    private String dstMac = "—";
    private final transient org.pcap4j.packet.Packet raw;

    public PacketModel(int no,
                       String time,
                       String src,
                       String dst,
                       String proto,
                       int sp,
                       int dp,
                       int len,
                       String hex,
                       String ascii,
                       double risk,
                       org.pcap4j.packet.Packet raw) {
        this.no.set(no);
        this.time.set(time);
        this.src.set(src);
        this.dst.set(dst);
        this.proto.set(proto);
        this.srcPort.set(sp);
        this.dstPort.set(dp);
        this.len.set(len);
        this.hexDump.set(hex);
        this.asciiDump.set(ascii);
        this.riskScore.set(risk);
        this.raw = raw;
    }

    public static PacketModel fromRaw(org.pcap4j.packet.Packet raw, int idx) {
        // timestamp string
        String ts = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

        // default MACs & payload
        String srcMac="—", dstMac="—";
        org.pcap4j.packet.Packet payload = raw;
        if (raw.contains(EthernetPacket.class)) {
            EthernetPacket eth = raw.get(EthernetPacket.class);
            srcMac = eth.getHeader().getSrcAddr().toString();
            dstMac = eth.getHeader().getDstAddr().toString();
            payload = eth.getPayload();
        }

        // defaults
        String srcIp="—", dstIp="—", protocol="N/A";
        int sp=-1, dp=-1;
        double risk=0.0;

        // IPv4 with TCP/UDP
        if (payload.contains(org.pcap4j.packet.IpV4Packet.class)) {
            var ip4 = payload.get(org.pcap4j.packet.IpV4Packet.class);
            srcIp = ip4.getHeader().getSrcAddr().getHostAddress();
            dstIp = ip4.getHeader().getDstAddr().getHostAddress();
            var l4 = ip4.getPayload();
            if (l4!=null && l4.contains(TcpPacket.class)) {
                var tcp = l4.get(TcpPacket.class);
                protocol="TCP";
                sp = tcp.getHeader().getSrcPort().valueAsInt();
                dp = tcp.getHeader().getDstPort().valueAsInt();
                // risk: SYN w/o ACK
                risk = (tcp.getHeader().getSyn() && !tcp.getHeader().getAck())?0.7:0.1;
            } else if (l4!=null && l4.contains(UdpPacket.class)) {
                var udp = l4.get(UdpPacket.class);
                protocol="UDP";
                sp = udp.getHeader().getSrcPort().valueAsInt();
                dp = udp.getHeader().getDstPort().valueAsInt();
            } else {
                protocol = ip4.getHeader().getProtocol().name();
            }
        }

        byte[] rawData = raw.getRawData();
        // hex + ascii
        StringBuilder hb = new StringBuilder(rawData.length*3);
        StringBuilder ab = new StringBuilder(rawData.length);
        for (byte b: rawData) {
            hb.append(String.format("%02X ", b));
            ab.append((b>=32 && b<127)?(char)b:'.');
        }

        PacketModel pm = new PacketModel(
            idx, ts, srcIp, dstIp, protocol,
            sp, dp, rawData.length,
            hb.toString().trim(), ab.toString(), risk, raw
        );
        pm.srcMac = srcMac;
        pm.dstMac = dstMac;
        return pm;
    }

    public CapturedPacket toPacket() {
        long tsMs = System.currentTimeMillis();

        // build transport info map
        Map<String,String> tinfo = new HashMap<>();
        if ("TCP".equals(proto.get())) {
            var tcp = raw.get(TcpPacket.class).getHeader();
            StringBuilder flags = new StringBuilder();
            if (tcp.getSyn()) flags.append("SYN ");
            if (tcp.getAck()) flags.append("ACK ");
            if (tcp.getFin()) flags.append("FIN ");
            if (tcp.getRst()) flags.append("RST ");
            tinfo.put("Flags", flags.toString().trim());
        }
        tinfo.put("SrcPort", String.valueOf(srcPort.get()));
        tinfo.put("DstPort", String.valueOf(dstPort.get()));

        PacketDetail detail = new PacketDetail(
            srcMac, dstMac,
            raw.contains(EthernetPacket.class)
              ? raw.get(EthernetPacket.class).getHeader().getType().name()
              : "N/A",
            getSrc(), getDst(), getProto(),
            getProto(), // use proto as transport name
            tinfo
        );

        return new CapturedPacket(
            getNo(), getTime(), tsMs, 0,
            srcMac, dstMac,
            getSrc(), getDst(), getProto(),
            getSrcPort(), getDstPort(),
            getLen(), getHexDump(), getAsciiDump(),
            getRiskScore(), detail
        );
    }

    // getters for TableView binding...

    public org.pcap4j.packet.Packet getRaw()    { return raw; }
    public int   getNo()     { return no.get(); }
    public String getTime()  { return time.get(); }
    public String getSrc()   { return src.get(); }
    public String getDst()   { return dst.get(); }
    public String getProto() { return proto.get(); }
    public int   getSrcPort(){ return srcPort.get(); }
    public int   getDstPort(){ return dstPort.get(); }
    public int   getLen()    { return len.get(); }
    public String getHexDump(){ return hexDump.get(); }
    public String getAsciiDump(){ return asciiDump.get(); }
    public double getRiskScore(){ return riskScore.get(); }
}
