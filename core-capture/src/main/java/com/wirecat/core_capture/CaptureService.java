package com.wirecat.core_capture;

import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * CaptureService handles live packet capture via Pcap4J,
 * streams PacketModel instances to the UI, and provides
 * a stub for saving captured data to a file.
 */
public class CaptureService {

    private PcapHandle handle;
    private Thread captureThread;
    private Consumer<PacketModel> packetListener;

    /**
     * Register a callback to receive PacketModel objects on each captured packet.
     *
     * @param listener a Consumer that processes PacketModel instances
     */
    public void setOnPacketCaptured(Consumer<PacketModel> listener) {
        this.packetListener = listener;
    }

    /**
     * Starts live capture on the specified network interface.
     *
     * @param ifaceName the name of the interface (e.g., "eth0")
     * @param filter    an optional BPF filter string (empty for none)
     * @param limit     the maximum number of packets to capture before stopping
     */
    public void startCapture(String ifaceName, String filter, int limit) {
        captureThread = new Thread(() -> {
            try {
                // Discover network interfaces
                List<PcapNetworkInterface> devices = Pcaps.findAllDevs();
                PcapNetworkInterface device = devices.stream()
                        .filter(dev -> dev.getName().equals(ifaceName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Interface not found: " + ifaceName));

                // Open live capture handle
                handle = device.openLive(65536,
                        PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,
                        10
                );

                // Apply BPF filter if provided
                if (filter != null && !filter.isBlank()) {
                    handle.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE);
                }

                AtomicInteger count = new AtomicInteger(0);

                // Loop indefinitely; break when count >= limit
                handle.loop(-1, (PacketListener) rawPacket -> {
                    int current = count.incrementAndGet();
                    PacketModel model = PacketModel.fromRaw(rawPacket, current);

                    if (packetListener != null) {
                        packetListener.accept(model);
                    }

                    if (current >= limit) {
                        try {
                            handle.breakLoop();
                        } catch (NotOpenException e) {
                            e.printStackTrace();
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Ensure handle is closed
                if (handle != null && handle.isOpen()) {
                    handle.close();
                }
            }
        }, "WireCat-Capture-Thread");

        captureThread.setDaemon(true);
        captureThread.start();
    }

    /**
     * Stops the active capture session and interrupts the capture thread.
     */
    public void stopCapture() {
        if (handle != null && handle.isOpen()) {
            try {
                handle.breakLoop();
                handle.close();
            } catch (NotOpenException e) {
                e.printStackTrace();
            } finally {
                handle = null;
            }
        }
        if (captureThread != null) {
            captureThread.interrupt();
        }
    }

    /**
     * Stub for saving captured packets to a PCAP file.
     * To be implemented: buffer packets in memory and
     * write them out with PcapDumper.
     *
     * @param file the destination .pcap file
     */
    public void saveCapture(File file) {
        // TODO: implement real PCAP dumping using Pcaps.openDead()
        // and PcapDumper.dumpOpen(), then iterate your packet buffer.
        throw new UnsupportedOperationException("saveCapture() not yet implemented");
    }
}
