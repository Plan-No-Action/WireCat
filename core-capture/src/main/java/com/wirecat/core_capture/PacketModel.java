package com.wirecat.core_capture;

import javafx.beans.property.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.TcpPort;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
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

    private String httpInfo = null; // 🆕 HTTP metadata
    private String aiExplanation;

    private final int id;  // Add this field
    private long timestampMs;  // Add this field

    // Add these methods
    public int getId() { return id; }
    public long getTimestampMs() { return timestampMs; }


    public void setAiExplanation(String explanation) {
            this.aiExplanation = explanation;
        }
    
    public String getAiExplanation() {
        return aiExplanation != null ? aiExplanation : "Analyzing...";
    }
    public PacketModel(int no, String time, String src, String dst, String proto,
                       int sp, int dp, int len, String hex, String ascii, double risk, Packet raw) {
        this.id = no;
        this.timestampMs = System.currentTimeMillis();
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
    
    public static PacketModel fromRaw(Packet raw, int idx) {
        String ts = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

        String srcMac = "—", dstMac = "—", srcIp = "—", dstIp = "—", protocol = "N/A";
        int sp = -1, dp = -1;
        double risk = 0.0;
        String httpInfo = null;

        Packet layer3 = null;
        try {
            // Ethernet
            if (raw.contains(EthernetPacket.class)) {
                EthernetPacket eth = raw.get(EthernetPacket.class);
                srcMac = eth.getHeader().getSrcAddr().toString();
                dstMac = eth.getHeader().getDstAddr().toString();
                layer3 = eth.getPayload();
                // ARP
                if (layer3 != null && layer3.contains(ArpPacket.class)) {
                    ArpPacket arp = layer3.get(ArpPacket.class);
                    srcIp = arp.getHeader().getSrcProtocolAddr().getHostAddress();
                    dstIp = arp.getHeader().getDstProtocolAddr().getHostAddress();
                    protocol = "ARP";
                }
            } else {
                layer3 = raw;
            }

            // IPv4
            if (layer3 != null && layer3.contains(IpV4Packet.class)) {
                IpV4Packet ip4 = layer3.get(IpV4Packet.class);
                srcIp = ip4.getHeader().getSrcAddr().getHostAddress();
                dstIp = ip4.getHeader().getDstAddr().getHostAddress();
                Packet layer4 = ip4.getPayload();
                if (layer4 != null && layer4.contains(TcpPacket.class)) {
                    TcpPacket tcp = layer4.get(TcpPacket.class);
                    sp = tcp.getHeader().getSrcPort().valueAsInt();
                    dp = tcp.getHeader().getDstPort().valueAsInt();
                    protocol = "TCP";
                    if (tcp.getHeader().getSyn() && !tcp.getHeader().getAck()) {
                        risk = 0.7;
                    } else {
                        risk = 0.1;
                    }
                    byte[] tcpPayload = tcp.getPayload() != null ? tcp.getPayload().getRawData() : new byte[0];
                    String payloadStr = new String(tcpPayload, StandardCharsets.UTF_8);
                    if (sp == 80 || dp == 80 || payloadStr.startsWith("GET") || payloadStr.startsWith("POST")) {
                        protocol = "HTTP";
                        httpInfo = extractHttpInfo(payloadStr);
                    } else if (sp == 443 || dp == 443) {
                        protocol = "HTTPS";
                    }
                } else if (layer4 != null && layer4.contains(UdpPacket.class)) {
                    UdpPacket udp = layer4.get(UdpPacket.class);
                    sp = udp.getHeader().getSrcPort().valueAsInt();
                    dp = udp.getHeader().getDstPort().valueAsInt();
                    protocol = "UDP";
                } else if (layer4 != null && layer4.contains(IcmpV4CommonPacket.class)) {
                    protocol = "ICMPv4";
                } else {
                    protocol = ip4.getHeader().getProtocol().name();
                }
            }
            // IPv6
            else if (layer3 != null && layer3.contains(IpV6Packet.class)) {
                IpV6Packet ip6 = layer3.get(IpV6Packet.class);
                srcIp = ip6.getHeader().getSrcAddr().getHostAddress();
                dstIp = ip6.getHeader().getDstAddr().getHostAddress();
                Packet layer4 = ip6.getPayload();
                if (layer4 != null && layer4.contains(TcpPacket.class)) {
                    TcpPacket tcp = layer4.get(TcpPacket.class);
                    sp = tcp.getHeader().getSrcPort().valueAsInt();
                    dp = tcp.getHeader().getDstPort().valueAsInt();
                    protocol = "TCPv6";
                    byte[] tcpPayload = tcp.getPayload() != null ? tcp.getPayload().getRawData() : new byte[0];
                    String payloadStr = new String(tcpPayload, StandardCharsets.UTF_8);
                    if (sp == 80 || dp == 80 || payloadStr.startsWith("GET") || payloadStr.startsWith("POST")) {
                        protocol = "HTTP";
                        httpInfo = extractHttpInfo(payloadStr);
                    } else if (sp == 443 || dp == 443) {
                        protocol = "HTTPS";
                    }
                } else if (layer4 != null && layer4.contains(UdpPacket.class)) {
                    UdpPacket udp = layer4.get(UdpPacket.class);
                    sp = udp.getHeader().getSrcPort().valueAsInt();
                    dp = udp.getHeader().getDstPort().valueAsInt();
                    protocol = "UDPv6";
                } else if (layer4 != null && layer4.contains(IcmpV6CommonPacket.class)) {
                    protocol = "ICMPv6";
                } else {
                    protocol = ip6.getHeader().getNextHeader().name();
                }
            }
        } catch (Exception ex) {
            // Log error for debugging
            System.err.println("[PacketModel] Error parsing packet: " + ex.getMessage());
            ex.printStackTrace();
        }

        // Create hex/ascii dump
        byte[] rawBytes = raw.getRawData();
        StringBuilder hex = new StringBuilder(rawBytes.length * 3);
        StringBuilder ascii = new StringBuilder(rawBytes.length);
        for (byte b : rawBytes) {
            hex.append(String.format("%02X ", b));
            ascii.append((b >= 32 && b <= 126) ? (char) b : '.');
        }

        PacketModel pm = new PacketModel(idx, ts, srcIp, dstIp, protocol,
                sp, dp, rawBytes.length, hex.toString().trim(), ascii.toString(), risk, raw);
        pm.srcMac = srcMac;
        pm.dstMac = dstMac;
        pm.httpInfo = httpInfo;
        return pm;
    }

    private static String extractHttpInfo(String data) {
        int end = data.indexOf("\r\n");
        return (end > 0) ? data.substring(0, end) : data.trim();
    }

    public CapturedPacket toPacket() {
        long tsMs = System.currentTimeMillis();
        Map<String, String> info = new HashMap<>();
        info.put("SrcPort", String.valueOf(getSrcPort()));
        info.put("DstPort", String.valueOf(getDstPort()));
        if (httpInfo != null) info.put("HTTP", httpInfo);

        return new CapturedPacket(getNo(), getTime(), tsMs, 0,
                srcMac, dstMac, getSrc(), getDst(), getProto(),
                getSrcPort(), getDstPort(), getLen(), getHexDump(), getAsciiDump(),
                getRiskScore(), new PacketDetail(srcMac, dstMac, "Ethernet",
                        getSrc(), getDst(), getProto(), getProto(), info));
    }

    public String getHttpInfo() { return httpInfo; }

    // Returns a canonical stream key for TCP/HTTP session tracking
    public String getStreamKey() {
        if (("TCP".equals(getProto()) || "TCPv6".equals(getProto())) && getSrcPort() > 0 && getDstPort() > 0) {
            String a = getSrc() + ":" + getSrcPort();
            String b = getDst() + ":" + getDstPort();
            return (a.compareTo(b) <= 0) ? a + "-" + b : b + "-" + a;
        }
        return null;
    }
    // --- Getters for JavaFX binding (required for TableView) ---
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
