package com.wirecat.core_capture;

import javafx.beans.property.*;
import org.pcap4j.packet.*;

<<<<<<< Updated upstream
=======
import org.pcap4j.packet.ArpPacket;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.LinuxSllPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;

import java.time.LocalTime;
>>>>>>> Stashed changes
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PacketModel {

<<<<<<< Updated upstream
    private final IntegerProperty no   = new SimpleIntegerProperty();
    private final StringProperty  time = new SimpleStringProperty();
    private final StringProperty  src  = new SimpleStringProperty();
    private final StringProperty  dst  = new SimpleStringProperty();
    private final StringProperty  proto= new SimpleStringProperty();
    private final IntegerProperty srcPort = new SimpleIntegerProperty();
    private final IntegerProperty dstPort = new SimpleIntegerProperty();
    private final IntegerProperty len     = new SimpleIntegerProperty();
    private final StringProperty  hexDump = new SimpleStringProperty();
    private final StringProperty  asciiDump = new SimpleStringProperty();
    private final DoubleProperty  riskScore = new SimpleDoubleProperty();

    private String srcMac = "—";
    private String dstMac = "—";
    private final transient Packet raw;

    private PacketModel(int no, String time, String src, String dst, String proto,
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

    public static PacketModel fromRaw(Packet raw, int idx) {
        String ts = java.time.LocalTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.ROOT));

        String srcMac = "—", dstMac = "—";
        Packet l3 = raw;

        if (raw.contains(EthernetPacket.class)) {
            EthernetPacket eth = raw.get(EthernetPacket.class);
            srcMac = eth.getHeader().getSrcAddr().toString();
            dstMac = eth.getHeader().getDstAddr().toString();
            l3 = eth.getPayload();
        } else if (raw.contains(LinuxSllPacket.class)) {
            l3 = raw.get(LinuxSllPacket.class).getPayload();
        } // loopback frames fall through with l3 == raw

        String srcIp = "—", dstIp = "—", proto = "N/A";
        int sp = -1, dp = -1;
        double risk = 0;

        if (l3.contains(IpV4Packet.class)) {
            IpV4Packet ip4 = l3.get(IpV4Packet.class);
            srcIp = ip4.getHeader().getSrcAddr().getHostAddress();
            dstIp = ip4.getHeader().getDstAddr().getHostAddress();
            Packet l4 = ip4.getPayload();
            if (l4.contains(TcpPacket.class)) {
                TcpPacket t = l4.get(TcpPacket.class);
                proto = "TCP";
                sp = t.getHeader().getSrcPort().valueAsInt();
                dp = t.getHeader().getDstPort().valueAsInt();
                risk = (t.getHeader().getSyn() && !t.getHeader().getAck()) ? .7 : .1;
            } else if (l4.contains(UdpPacket.class)) {
                UdpPacket u = l4.get(UdpPacket.class);
                proto = "UDP";
                sp = u.getHeader().getSrcPort().valueAsInt();
                dp = u.getHeader().getDstPort().valueAsInt();
            } else proto = ip4.getHeader().getProtocol().name();
        } else if (l3.contains(IpV6Packet.class)) {
            IpV6Packet ip6 = l3.get(IpV6Packet.class);
            srcIp = ip6.getHeader().getSrcAddr().getHostAddress();
            dstIp = ip6.getHeader().getDstAddr().getHostAddress();
            proto = ip6.getHeader().getNextHeader().name();
        } else if (l3.contains(ArpPacket.class)) {
            ArpPacket arp = l3.get(ArpPacket.class);
            srcIp = arp.getHeader().getSrcProtocolAddr().getHostAddress();
            dstIp = arp.getHeader().getDstProtocolAddr().getHostAddress();
            proto = "ARP";
        }

        byte[] bytes = raw.getRawData();
        StringBuilder h = new StringBuilder(bytes.length * 3);
        StringBuilder a = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            h.append(String.format("%02X ", b));
            a.append((b >= 32 && b <= 126) ? (char) b : '.');
        }

        PacketModel pm = new PacketModel(idx, ts, srcIp, dstIp, proto, sp, dp,
                bytes.length, h.toString().trim(), a.toString(), risk, raw);
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
    public int    getNo()        { return no.get(); }
    public String getTime()      { return time.get(); }
    public String getSrc()       { return src.get(); }
    public String getDst()       { return dst.get(); }
    public String getProto()     { return proto.get(); }
    public int    getSrcPort()   { return srcPort.get(); }
    public int    getDstPort()   { return dstPort.get(); }
    public int    getLen()       { return len.get(); }
    public String getHexDump()   { return hexDump.get(); }
    public String getAsciiDump() { return asciiDump.get(); }
    public double getRiskScore() { return riskScore.get(); }
}
=======
    private final IntegerProperty no;
    private final StringProperty time;
    private final StringProperty src;
    private final StringProperty dst;
    private final StringProperty proto;
    private final IntegerProperty len;
    private final StringProperty hexDump;
    private final StringProperty asciiDump;
    private final Packet rawPacket;

    public PacketModel(int no,
                       String time,
                       String src,
                       String dst,
                       String proto,
                       int len,
                       String hexDump,
                       String asciiDump,
                       Packet rawPacket) {
        this.no        = new SimpleIntegerProperty(no);
        this.time      = new SimpleStringProperty(time);
        this.src       = new SimpleStringProperty(src);
        this.dst       = new SimpleStringProperty(dst);
        this.proto     = new SimpleStringProperty(proto);
        this.len       = new SimpleIntegerProperty(len);
        this.hexDump   = new SimpleStringProperty(hexDump);
        this.asciiDump = new SimpleStringProperty(asciiDump);
        this.rawPacket = rawPacket;
    }

    // UI getters
    public int    getNo()       { return no.get();    }
    public String getTime()     { return time.get();  }
    public String getSrc()      { return src.get();   }
    public String getDst()      { return dst.get();   }
    public String getProto()    { return proto.get(); }
    public int    getLen()      { return len.get();   }
    public String getHexDump()  { return hexDump.get();}
    public String getAsciiDump(){ return asciiDump.get();}

    // Expose raw for inspector
    public Packet getRawPacket() { return rawPacket; }

    /**
     * Build flat table model from raw packet with debug logging.
     */
    public static PacketModel fromRaw(Packet pkt, int index) {
        System.out.println("----- Packet #" + index + " -----");
        System.out.println("Initial Packet class: " + pkt.getClass().getSimpleName());

        String timestamp = LocalTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

        String srcAddr = "Unknown";
        String dstAddr = "Unknown";
        String protocol = "Unknown";

        // 1) Direct IPv4
        IpV4Packet ipv4 = pkt.get(IpV4Packet.class);
        if (ipv4 != null) {
            srcAddr = ipv4.getHeader().getSrcAddr().getHostAddress();
            dstAddr = ipv4.getHeader().getDstAddr().getHostAddress();
            if (ipv4.contains(TcpPacket.class))      protocol = "TCP";
            else if (ipv4.contains(UdpPacket.class)) protocol = "UDP";
            else                                     protocol = ipv4.getHeader().getProtocol().name();
            System.out.println("Parsed via direct IPv4: " + srcAddr + " → " + dstAddr + " proto=" + protocol);
        } else {
            // 2) Direct IPv6
            IpV6Packet ipv6 = pkt.get(IpV6Packet.class);
            if (ipv6 != null) {
                srcAddr = ipv6.getHeader().getSrcAddr().getHostAddress();
                dstAddr = ipv6.getHeader().getDstAddr().getHostAddress();
                protocol = ipv6.getHeader().getNextHeader().name();
                System.out.println("Parsed via direct IPv6: " + srcAddr + " → " + dstAddr + " nextHeader=" + protocol);
            } else {
                // 3) Ethernet peel
                EthernetPacket eth = pkt.get(EthernetPacket.class);
                if (eth != null) {
                    System.out.println("Ethernet header found");
                    Packet payload = eth.getPayload();
                    srcAddr = eth.getHeader().getSrcAddr().toString();
                    dstAddr = eth.getHeader().getDstAddr().toString();
                    IpV4Packet ipInEth = payload.get(IpV4Packet.class);
                    if (ipInEth != null) {
                        protocol = ipInEth.getHeader().getProtocol().name();
                    } else if (payload.contains(TcpPacket.class)) {
                        protocol = "TCP";
                    } else if (payload.contains(UdpPacket.class)) {
                        protocol = "UDP";
                    }
                    System.out.println("After Ethernet peel: src=" + srcAddr
                            + ", dst=" + dstAddr + ", proto=" + protocol);
                } else {
                    // 4) Linux SLL peel
                    LinuxSllPacket sll = pkt.get(LinuxSllPacket.class);
                    if (sll != null) {
                        System.out.println("Linux SLL header found");
                        Packet payload = sll.getPayload();
                        IpV4Packet ipInSll = payload.get(IpV4Packet.class);
                        if (ipInSll != null) {
                            srcAddr = ipInSll.getHeader().getSrcAddr().getHostAddress();
                            dstAddr = ipInSll.getHeader().getDstAddr().getHostAddress();
                            protocol = ipInSll.getHeader().getProtocol().name();
                        }
                        System.out.println("After SLL peel: src=" + srcAddr
                                + ", dst=" + dstAddr + ", proto=" + protocol);
                    } else {
                        // 5) ARP fallback
                        ArpPacket arp = pkt.get(ArpPacket.class);
                        if (arp != null) {
                            srcAddr = arp.getHeader().getSrcProtocolAddr().getHostAddress();
                            dstAddr = arp.getHeader().getDstProtocolAddr().getHostAddress();
                            protocol = "ARP";
                            System.out.println("Parsed via ARP: " + srcAddr + " → " + dstAddr);
                        } else {
                            System.out.println("No known header matched");
                        }
                    }
                }
            }
        }

        // Hex & ASCII dump
        byte[] data = pkt.getRawData();
        StringBuilder hexB   = new StringBuilder();
        StringBuilder asciiB = new StringBuilder();
        for (byte b : data) {
            hexB.append(String.format("%02X ", b));
            char c = (char) b;
            asciiB.append((c >= 32 && c <= 126) ? c : '.');
        }

        System.out.println("Final resolve: src=" + srcAddr
                + ", dst=" + dstAddr + ", proto=" + protocol);
        System.out.println("-------------------------------");

        return new PacketModel(
                index,
                timestamp,
                srcAddr,
                dstAddr,
                protocol,
                data.length,
                hexB.toString().trim(),
                asciiB.toString(),
                pkt
        );
    }

    /**
     * Dissect into layers for the inspector panel.
     */
    public static PacketDetail dissect(Packet rawPacket) {
        // (You can add debug here similarly if needed)
        StringBuilder frame     = new StringBuilder();
        StringBuilder eth       = new StringBuilder();
        StringBuilder ip        = new StringBuilder();
        StringBuilder transport = new StringBuilder();
        StringBuilder app       = new StringBuilder();

        byte[] data = rawPacket.getRawData();
        frame.append("Frame: ").append(data.length).append(" bytes on wire");

        EthernetPacket ethPkt = rawPacket.get(EthernetPacket.class);
        if (ethPkt != null) {
            eth.append("Ethernet II\n")
                    .append("  Src MAC: ").append(ethPkt.getHeader().getSrcAddr()).append("\n")
                    .append("  Dst MAC: ").append(ethPkt.getHeader().getDstAddr()).append("\n")
                    .append("  Type: ").append(ethPkt.getHeader().getType());
        }

        IpV4Packet ipv4 = rawPacket.get(IpV4Packet.class);
        if (ipv4 != null) {
            ip.append("IPv4\n")
                    .append("  Src IP: ").append(ipv4.getHeader().getSrcAddr()).append("\n")
                    .append("  Dst IP: ").append(ipv4.getHeader().getDstAddr()).append("\n")
                    .append("  Protocol: ").append(ipv4.getHeader().getProtocol());
        } else {
            IpV6Packet ipv6 = rawPacket.get(IpV6Packet.class);
            if (ipv6 != null) {
                ip.append("IPv6\n")
                        .append("  Src IP: ").append(ipv6.getHeader().getSrcAddr()).append("\n")
                        .append("  Dst IP: ").append(ipv6.getHeader().getDstAddr()).append("\n")
                        .append("  Next Header: ").append(ipv6.getHeader().getNextHeader());
            }
        }

        TcpPacket tcp = rawPacket.get(TcpPacket.class);
        if (tcp != null) {
            transport.append("TCP\n")
                    .append("  Src Port: ").append(tcp.getHeader().getSrcPort()).append("\n")
                    .append("  Dst Port: ").append(tcp.getHeader().getDstPort()).append("\n")
                    .append("  Seq Num: ").append(tcp.getHeader().getSequenceNumber()).append("\n")
                    .append("  Ack Num: ").append(tcp.getHeader().getAcknowledgmentNumber());
        }

        UdpPacket udp = rawPacket.get(UdpPacket.class);
        if (udp != null) {
            transport.append("UDP\n")
                    .append("  Src Port: ").append(udp.getHeader().getSrcPort()).append("\n")
                    .append("  Dst Port: ").append(udp.getHeader().getDstPort());
        }

        if (tcp != null && tcp.getPayload() != null) {
            byte[] tdata = tcp.getPayload().getRawData();
            if (tdata.length > 0 && (tdata[0] & 0xFF) == 0x16) {
                app.append("TLSv1.x Record Detected (0x16)");
            } else {
                app.append("TCP Payload: ").append(tdata.length).append(" bytes");
            }
        }

        return new PacketDetail(
                frame.toString(),
                eth.toString(),
                ip.toString(),
                transport.toString(),
                app.toString()
        );
    }

    /** Convert to UI Packet for the TableView. */
    public com.wirecat.core_capture.Packet toPacket() {
        return new com.wirecat.core_capture.Packet(
                getNo(), getTime(), getSrc(), getDst(),
                getProto(), getLen(), getHexDump(), getAsciiDump()
        );
    }
}
>>>>>>> Stashed changes
