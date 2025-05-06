package com.wirecat.core_capture;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.pcap4j.packet.Packet;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.LinuxSllPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.ArpPacket;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Transforms raw Pcap4J Packet into a UI-friendly model,
 * peeling off link-layer headers (Ethernet/SLL) and extracting IPv4, IPv6 or ARP.
 */
public class PacketModel {

    private final IntegerProperty no;
    private final StringProperty time;
    private final StringProperty src;
    private final StringProperty dst;
    private final StringProperty proto;
    private final IntegerProperty len;
    private final StringProperty hexDump;
    private final StringProperty asciiDump;

    public PacketModel(int no,
                       String time,
                       String src,
                       String dst,
                       String proto,
                       int len,
                       String hexDump,
                       String asciiDump) {
        this.no        = new SimpleIntegerProperty(no);
        this.time      = new SimpleStringProperty(time);
        this.src       = new SimpleStringProperty(src);
        this.dst       = new SimpleStringProperty(dst);
        this.proto     = new SimpleStringProperty(proto);
        this.len       = new SimpleIntegerProperty(len);
        this.hexDump   = new SimpleStringProperty(hexDump);
        this.asciiDump = new SimpleStringProperty(asciiDump);
    }

    public int    getNo()       { return no.get();       }
    public String getTime()     { return time.get();     }
    public String getSrc()      { return src.get();      }
    public String getDst()      { return dst.get();      }
    public String getProto()    { return proto.get();    }
    public int    getLen()      { return len.get();      }
    public String getHexDump()  { return hexDump.get();  }
    public String getAsciiDump(){ return asciiDump.get();}

    /**
     * Build a PacketModel from a raw Pcap4J Packet:
     * 1) Peel off Ethernet or Linux SLL link-layer
     * 2) Extract IPv4, IPv6, or ARP header fields
     * 3) Generate hex + ASCII dump of the payload
     */
    public static PacketModel fromRaw(Packet rawPacket, int index) {
        // 1) Peel off link-layer
        Packet payload = rawPacket;
        EthernetPacket eth = rawPacket.get(EthernetPacket.class);
        if (eth != null) {
            payload = eth.getPayload();
        } else {
            LinuxSllPacket sll = rawPacket.get(LinuxSllPacket.class);
            if (sll != null) {
                payload = sll.getPayload();
            }
        }

        // 2) Attempt to extract network-layer info
        String srcAddr = "Unknown";
        String dstAddr = "Unknown";
        String protocol = "Unknown";

        IpV4Packet ipv4 = payload.get(IpV4Packet.class);
        if (ipv4 != null) {
            srcAddr  = ipv4.getHeader().getSrcAddr().getHostAddress();
            dstAddr  = ipv4.getHeader().getDstAddr().getHostAddress();
            if      (payload.contains(TcpPacket.class)) { protocol = "TCP"; }
            else if (payload.contains(UdpPacket.class)) { protocol = "UDP"; }
            else                                       { protocol = ipv4.getHeader().getProtocol().name(); }
        } else {
            IpV6Packet ipv6 = payload.get(IpV6Packet.class);
            if (ipv6 != null) {
                srcAddr  = ipv6.getHeader().getSrcAddr().getHostAddress();
                dstAddr  = ipv6.getHeader().getDstAddr().getHostAddress();
                protocol = ipv6.getHeader().getNextHeader().name();
            } else {
                ArpPacket arp = payload.get(ArpPacket.class);
                if (arp != null) {
                    srcAddr  = arp.getHeader().getSrcProtocolAddr().getHostAddress();
                    dstAddr  = arp.getHeader().getDstProtocolAddr().getHostAddress();
                    protocol = "ARP";
                }
            }
        }

        // 3) Timestamp
        String timestamp = LocalTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

        // 4) Hex + ASCII dump
        byte[] data = rawPacket.getRawData();
        StringBuilder hexBuilder  = new StringBuilder();
        StringBuilder asciiBuilder= new StringBuilder();
        for (byte b : data) {
            hexBuilder.append(String.format("%02X ", b));
            char c = (char) b;
            asciiBuilder.append((c >= 32 && c <= 126) ? c : '.');
        }

        return new PacketModel(
                index,
                timestamp,
                srcAddr,
                dstAddr,
                protocol,
                data.length,
                hexBuilder.toString().trim(),
                asciiBuilder.toString()
        );
    }

    /** Wraps this model in the JavaFX Packet class for display. */
    public com.wirecat.core_capture.Packet toPacket() {
        return new com.wirecat.core_capture.Packet(
                getNo(), getTime(), getSrc(), getDst(), getProto(), getLen(),
                getHexDump(), getAsciiDump()
        );
    }
}
