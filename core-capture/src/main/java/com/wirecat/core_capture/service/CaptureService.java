package com.wirecat.core_capture.service;

import com.wirecat.core_capture.model.PacketModel;
import org.pcap4j.core.*;
import org.pcap4j.packet.namednumber.DataLinkType;
import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class CaptureService {
    private PcapHandle handle;
    private Thread captureThread;
    private final LinkedTransferQueue<PacketModel> queue = new LinkedTransferQueue<>();
    private final CopyOnWriteArrayList<PacketModel> capturedPackets = new CopyOnWriteArrayList<>();
    private final AIAnalysisService aiService = new AIAnalysisService();

    private Consumer<String> statusConsumer;
    public void onStatus(Consumer<String> c) { statusConsumer = c; }
    private void emitStatus(String s) { if (statusConsumer != null) statusConsumer.accept(s); }

    private Consumer<PacketModel> packetListener;
    public void setOnPacketCaptured(Consumer<PacketModel> listener) { packetListener = listener; }

    public LinkedTransferQueue<PacketModel> queue() { return queue; }
    public List<PacketModel> getCapturedPackets() { return List.copyOf(capturedPackets); }

    public void startCapture(String ifaceName, String bpfFilter, int limit) {
        try {
            PcapNetworkInterface device = Pcaps.findAllDevs().stream()
                .filter(d -> d.getName().equals(ifaceName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Interface not found"));

            handle = device.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 50);
            
            if (bpfFilter != null && !bpfFilter.isBlank()) {
                handle.setFilter(bpfFilter, BpfProgram.BpfCompileMode.OPTIMIZE);
            }

            captureThread = new Thread(() -> {
                emitStatus("▶ Capturing on " + ifaceName);
                AtomicInteger counter = new AtomicInteger();

                PacketListener listener = rawPkt -> {
                    int idx = counter.incrementAndGet();
                    PacketModel pm = PacketModel.fromRaw(rawPkt, idx);
                    
                    // Store packet immediately
                    capturedPackets.add(pm);
                    queue.offer(pm);
                    if (packetListener != null) packetListener.accept(pm);

                    // Initiate AI analysis
                    aiService.analyzeAsync(pm, analyzedPm -> {
                        // Fixed replacement logic
                        for (int i = 0; i < capturedPackets.size(); i++) {
                            if (capturedPackets.get(i).getId() == analyzedPm.getId()) {
                                capturedPackets.set(i, analyzedPm);
                                break;
                            }
                        }
                        if (packetListener != null) packetListener.accept(analyzedPm);
                    });

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
                    if (handle.isOpen()) handle.close();
                    emitStatus("■ Capture stopped");
                }
            }, "WireCat-Capture-Thread");

            captureThread.setDaemon(true);
            captureThread.start();

        } catch (Exception e) {
            emitStatus("❌ Initialization failed: " + e.getMessage());
        }
    }

    public void stopCapture() {
        try {
            if (handle != null && handle.isOpen()) {
                handle.breakLoop();
                handle.close();
            }
            aiService.shutdown();
        } catch (Exception e) {
            emitStatus("❌ Stop error: " + e.getMessage());
        }
        if (captureThread != null) {
            captureThread.interrupt();
        }
    }

    public void save(File outFile) {
        try (PcapHandle dead = Pcaps.openDead(DataLinkType.EN10MB, 65536);
             PcapDumper dumper = dead.dumpOpen(outFile.getAbsolutePath())) {
            
            for (PacketModel pm : capturedPackets) {
                dumper.dump(pm.getRaw(), new Timestamp(pm.getTimestampMs()));
            }
            emitStatus("💾 Saved to " + outFile.getAbsolutePath());
            
        } catch (Exception e) {
            emitStatus("❌ Save failed: " + e.getMessage());
        }
    }

    public void clearPackets() {
        capturedPackets.clear();
        queue.clear();
        emitStatus("🧹 Packets cleared");
    }
}