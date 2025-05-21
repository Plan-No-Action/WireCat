package com.wirecat.core_capture.service;

import com.wirecat.core_capture.model.PacketModel;

import java.util.concurrent.*;
import java.util.function.Consumer; // Add this import

public class AIAnalysisService {
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    
    public void analyzeAsync(PacketModel packet, Consumer<PacketModel> callback) {
        executor.submit(() -> {
            try {
                // Call static method correctly
                String explanation = GeminiClient.analyzePacket(packet.toString());
                packet.setAiExplanation(explanation);
                callback.accept(packet);
            } catch (Exception e) {
                packet.setAiExplanation("Analysis failed: " + e.getMessage());
                callback.accept(packet);
            }
        });
    }
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}