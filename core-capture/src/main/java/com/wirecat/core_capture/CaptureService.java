package com.wirecat.core_capture;

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

public class CaptureService {
    private PcapHandle handle;
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
    }
}
