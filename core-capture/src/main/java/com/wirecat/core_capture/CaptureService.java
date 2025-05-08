// src/main/java/com/wirecat/core_capture/CaptureService.java

package com.wirecat.core_capture;

<<<<<<< Updated upstream
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.namednumber.DataLinkType;

import java.io.File;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

=======
import org.pcap4j.core.Pcaps;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.BpfProgram;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Live‐capture service; stores PacketModel list for UI + inspector lookup.
 */
>>>>>>> Stashed changes
public class CaptureService {
    private PcapHandle handle;
<<<<<<< Updated upstream
    private Thread producer;

    private final LinkedTransferQueue<PacketModel> queue = new LinkedTransferQueue<>();
    private final Map<String, AtomicInteger> stats = new ConcurrentHashMap<>();

    private Consumer<String> status;
    public  void onStatus(Consumer<String> l){ status=l; }
    private void emit(String s){ if(status!=null) status.accept(s); }

    public LinkedTransferQueue<PacketModel> queue(){ return queue; }
    public Map<String,AtomicInteger> stats(){ return stats; }

    public void start(PcapNetworkInterface dev, String bpf, int limit) {
        producer = new Thread(() -> {
            try {
                emit("▶ Capturing on "+dev.getName());
                handle = dev.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
                if(bpf!=null && !bpf.isBlank()) handle.setFilter(bpf, BpfProgram.BpfCompileMode.OPTIMIZE);
                AtomicInteger idx = new AtomicInteger();
                handle.loop(-1, (PacketListener) p -> {
                    PacketModel pm = PacketModel.fromRaw(p, idx.incrementAndGet());
                    queue.offer(pm);
                    stats.computeIfAbsent(pm.getProto(), k -> new AtomicInteger())
                            .incrementAndGet();

                    if (limit > 0 && idx.get() >= limit) {
                        try { handle.breakLoop(); }         // ← catch the checked exception
                        catch (NotOpenException ignored) { }
                    }
                });


            } catch(Exception e){ emit("❌ "+e.getMessage()); }
            finally{
                if(handle!=null && handle.isOpen()) handle.close();
                emit("■ Capture stopped");
            }
        },"WireCat-Capture");
        producer.setDaemon(true);
        producer.start();
    }

    public void stop(){
        if(handle!=null && handle.isOpen()) try{ handle.breakLoop(); }catch(NotOpenException ignored){}
        if(producer!=null) producer.interrupt();
    }

    public void save(File f){
        try (PcapHandle dead = Pcaps.openDead(DataLinkType.EN10MB,65536);
             PcapDumper d = dead.dumpOpen(f.getAbsolutePath())) {

            queue.forEach(pm -> {
                Packet p = pm.getRaw();
                if(p!=null){
                    try{ d.dump(p, new Timestamp(System.currentTimeMillis())); }
                    catch(Exception ignored){}
                }
            });
            emit("💾 Saved → "+f.getAbsolutePath());
        } catch(Exception e){ emit("❌ Save failed: "+e.getMessage()); }
=======
    private Thread captureThread;
    private Consumer<PacketModel> packetListener;
    private final List<PacketModel> capturedPackets = new CopyOnWriteArrayList<>();

    public void setOnPacketCaptured(Consumer<PacketModel> listener) {
        this.packetListener = listener;
    }

    public void startCapture(String ifaceName, String filter, int limit) {
        captureThread = new Thread(() -> {
            try {
                List<PcapNetworkInterface> devs = Pcaps.findAllDevs();
                PcapNetworkInterface device = devs.stream()
                        .filter(d -> d.getName().equals(ifaceName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Interface not found"));

                handle = device.openLive(65536,
                        PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,
                        10);

                if (filter != null && !filter.isBlank()) {
                    handle.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE);
                }

                AtomicInteger count = new AtomicInteger(0);
                handle.loop(-1, (PacketListener) rawPkt -> {
                    int idx = count.incrementAndGet();
                    PacketModel model = PacketModel.fromRaw(rawPkt, idx);
                    capturedPackets.add(model);
                    if (packetListener != null) packetListener.accept(model);
                    if (idx >= limit) {
                        try { handle.breakLoop(); }
                        catch (NotOpenException ignore) {}
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (handle!=null && handle.isOpen()) handle.close();
            }
        }, "WireCat-Capture-Thread");

        captureThread.setDaemon(true);
        captureThread.start();
    }

    public void stopCapture() {
        if (handle!=null && handle.isOpen()) {
            try { handle.breakLoop(); handle.close(); }
            catch (NotOpenException ignore) {}
            finally { handle = null; }
        }
        if (captureThread!=null) captureThread.interrupt();
    }

    public void saveCapture(File file) {
        throw new UnsupportedOperationException("saveCapture() not yet implemented");
>>>>>>> Stashed changes
    }

    /** Retrieve the PacketModel by 1-based table index. */
    public PacketModel getRawPacketByIndex(int index) {
        if (index<=0 || index>capturedPackets.size()) return null;
        return capturedPackets.get(index-1);
    }
}
