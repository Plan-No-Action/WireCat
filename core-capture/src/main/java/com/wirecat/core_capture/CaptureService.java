package com.wirecat.core_capture;

import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.namednumber.DataLinkType;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Live‐capture service; feeds PacketModel into capturedPackets for UI.
 */
public class CaptureService {

    private PcapHandle handle;
    private Thread captureThread;
    private final LinkedTransferQueue<PacketModel> queue = new LinkedTransferQueue<>();

    /** Expose the internal queue of PacketModel **/
    public LinkedTransferQueue<PacketModel> queue() {
        return queue;
    }

    // Shared list observed by UI
    private final CopyOnWriteArrayList<PacketModel> capturedPackets = new CopyOnWriteArrayList<>();
    // New queue for MainView to poll packets

    // For status updates in UI
    private Consumer<String> statusConsumer;
    public void onStatus(Consumer<String> c){ statusConsumer = c; }
    private void emitStatus(String s){
        if(statusConsumer != null) statusConsumer.accept(s);
    }

    // Packet callback for UI charts/stats
    private Consumer<PacketModel> packetListener;
    public void setOnPacketCaptured(Consumer<PacketModel> listener){
        packetListener = listener;
    }

    /** Expose the live list as an unmodifiable view */
    public List<PacketModel> getCapturedPackets(){
        return List.copyOf(capturedPackets);
    }


    /**
     * Start live capture on the given interface name.
     * @param ifaceName  network interface name (from Pcaps.findAllDevs())
     * @param bpfFilter  BPF filter string; null or empty for none
     * @param limit      max packets to capture; 0 = unlimited
     */
    public void startCapture(String ifaceName, String bpfFilter, int limit){
        // 1) Lookup the interface
        PcapNetworkInterface device;
        try {
            device = Pcaps.findAllDevs().stream()
                .filter(d -> d.getName().equals(ifaceName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Interface not found"));
        } catch (Exception e) {
            emitStatus("❌ Error listing devices: " + e.getMessage());
            return;
        }

        // 2) Open live with snaplen=65536, promisc=true, timeout=50ms
        try {
            handle = device.openLive(
                65536,
                PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,
                50
            );
        } catch (PcapNativeException e) {
            emitStatus("❌ Failed to open interface: " + e.getMessage());
            return;
        }

        // 3) Apply BPF filter if provided
        if (bpfFilter != null && !bpfFilter.isBlank()) {
            try {
                handle.setFilter(bpfFilter, BpfProgram.BpfCompileMode.OPTIMIZE);
            } catch (PcapNativeException | NotOpenException e) {
                emitStatus("❌ BPF filter failed: " + e.getMessage());
            }
        }

        // 4) Start background thread for capture loop
        captureThread = new Thread(() -> {
            emitStatus("▶ Capturing on " + ifaceName);
            AtomicInteger counter = new AtomicInteger(0);

            PacketListener listener = rawPkt -> {
                int idx = counter.incrementAndGet();
                // Build model and enqueue
                PacketModel pm = PacketModel.fromRaw(rawPkt, idx);
                capturedPackets.add(pm);
                queue.offer(pm);
                if (packetListener != null) {
                    packetListener.accept(pm);
                }
                // Stop if limit reached
                if (limit > 0 && idx >= limit) {
                    try { handle.breakLoop(); }
                    catch (NotOpenException ignored) {}
                }
            };

            try {
                handle.loop(-1, listener);
            } catch (Exception e) {
                emitStatus("❌ Capture error: " + e.getMessage());
            } finally {
                if (handle.isOpen()) {
                    handle.close();
                }
                emitStatus("■ Capture stopped");
            }
        }, "WireCat-Capture-Thread");

        captureThread.setDaemon(true);
        captureThread.start();
    }

    /** Stop capture early */
    public void stopCapture(){
        if (handle != null && handle.isOpen()) {
            try { handle.breakLoop(); }
            catch (NotOpenException ignored) {}
        }
        if (captureThread != null) {
            captureThread.interrupt();
        }
    }

    /** Export to PCAP file */
    public void save(File outFile){
        try (PcapHandle dead = Pcaps.openDead(DataLinkType.EN10MB, 65536);
             PcapDumper dumper = dead.dumpOpen(outFile.getAbsolutePath())) {

            for (PacketModel pm : capturedPackets) {
                dumper.dump(pm.getRaw(), new Timestamp(System.currentTimeMillis()));
            }
            emitStatus("💾 Saved to " + outFile.getAbsolutePath());
        } catch (Exception e){
            emitStatus("❌ Save failed: " + e.getMessage());
        }
    }
    
}
