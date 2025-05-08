package com.wirecat.core_capture;

import javafx.beans.property.*;
import org.pcap4j.packet.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PacketModel {

    private final IntegerProperty no = new SimpleIntegerProperty();
    private final StringProperty time = new SimpleStringProperty();
    private final StringProperty src = new SimpleStringProperty();
    private final StringProperty dst = new SimpleStringProperty();
    private final StringProperty proto = new SimpleStringProperty();
    private final IntegerProperty srcPort = new SimpleIntegerProperty();
    private final IntegerProperty dstPort = new SimpleIntegerProperty();
    private final IntegerProperty len = new SimpleIntegerProperty();
    private final StringProperty hexDump = new SimpleStringProperty();
    private final StringProperty asciiDump = new SimpleStringProperty();
    private final DoubleProperty riskScore = new SimpleDoubleProperty();

    private String srcMac = "—";
    private String dstMac = "—";
    private final transient Packet raw;

    public PacketModel(int no, String time, String src, String dst, String proto,
                        int sp, int dp, int len,
                        String hex, String ascii, double risk,
                        Packet raw) {

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
        String ts = java.time.LocalTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.ROOT));

        String srcMac = "—", dstMac = "—";
        org.pcap4j.packet.Packet l3 = raw;

        if (raw instanceof org.pcap4j.packet.EthernetPacket) {
            org.pcap4j.packet.EthernetPacket eth = (org.pcap4j.packet.EthernetPacket) raw;
            srcMac = eth.getHeader().getSrcAddr().toString();
            dstMac = eth.getHeader().getDstAddr().toString();
            l3 = eth.getPayload();
        } else if (raw instanceof org.pcap4j.packet.LinuxSllPacket) {
            org.pcap4j.packet.LinuxSllPacket sll = (org.pcap4j.packet.LinuxSllPacket) raw;
            l3 = sll.getPayload();
        }

        String srcIp = "—", dstIp = "—", proto = "N/A";
        int sp = -1, dp = -1;
        double risk = 0;
        if (l3 instanceof org.pcap4j.packet.IpV4Packet) {
            org.pcap4j.packet.IpV4Packet ip4 = (org.pcap4j.packet.IpV4Packet) l3;
            srcIp = ip4.getHeader().getSrcAddr().getHostAddress();
            dstIp = ip4.getHeader().getDstAddr().getHostAddress();
            org.pcap4j.packet.Packet l4 = ip4.getPayload();
            if (l4 instanceof org.pcap4j.packet.TcpPacket) {
                org.pcap4j.packet.TcpPacket t = (org.pcap4j.packet.TcpPacket) l4;
                proto = "TCP";
                sp = t.getHeader().getSrcPort().valueAsInt();
                dp = t.getHeader().getDstPort().valueAsInt();
                risk = (t.getHeader().getSyn() && !t.getHeader().getAck()) ? .7 : .1;
            } else if (l4 instanceof org.pcap4j.packet.UdpPacket) {
                org.pcap4j.packet.UdpPacket u = (org.pcap4j.packet.UdpPacket) l4;
                proto = "UDP";
                sp = u.getHeader().getSrcPort().valueAsInt();
                dp = u.getHeader().getDstPort().valueAsInt();
            } else {
                proto = ip4.getHeader().getProtocol().name();
            }
        } else if (l3 instanceof org.pcap4j.packet.IpV6Packet) {
            org.pcap4j.packet.IpV6Packet ip6 = (org.pcap4j.packet.IpV6Packet) l3;
            srcIp = ip6.getHeader().getSrcAddr().getHostAddress();
            dstIp = ip6.getHeader().getDstAddr().getHostAddress();
            proto = ip6.getHeader().getNextHeader().name();
        } else if (l3 instanceof org.pcap4j.packet.ArpPacket) {
            org.pcap4j.packet.ArpPacket arp = (org.pcap4j.packet.ArpPacket) l3;
            srcIp = arp.getHeader().getSrcProtocolAddr().getHostAddress();
            dstIp = arp.getHeader().getDstProtocolAddr().getHostAddress();
            proto = "ARP";
        }
        byte[] bytes = raw.getRawData(); // Ensure raw is of type org.pcap4j.packet.Packet
        StringBuilder h = new StringBuilder(bytes.length * 3);
        StringBuilder a = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            h.append(String.format("%02X ", b));
            a.append((b >= 32 && b <= 126) ? (char) b : '.');
        }

        PacketModel pm = new PacketModel(idx, ts, srcIp, dstIp, proto, sp, dp,
            bytes.length, h.toString().trim(), a.toString(), risk, (Packet) raw);
        pm.srcMac = srcMac;
        pm.dstMac = dstMac;
        return pm;
    }

    public CapturedPacket toPacket() {
        return new CapturedPacket(getNo(), getTime(), "—",
                srcMac, dstMac, getSrc(), getDst(), getProto(),
                getSrcPort(), getDstPort(), getLen(),
                getHexDump(), getAsciiDump(), getRiskScore());
    }

    public Packet getRaw() { return raw; }
    public int getNo() { return no.get(); }
    public String getTime() { return time.get(); }
    public String getSrc() { return src.get(); }
    public String getDst() { return dst.get(); }
    public String getProto() { return proto.get(); }
    public int getSrcPort() { return srcPort.get(); }
    public int getDstPort() { return dstPort.get(); }
    public int getLen() { return len.get(); }
    public String getHexDump() { return hexDump.get(); }
    public String getAsciiDump() { return asciiDump.get(); }
    public double getRiskScore() { return riskScore.get(); }
}
