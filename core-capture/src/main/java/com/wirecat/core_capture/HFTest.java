package com.wirecat.core_capture;

import com.wirecat.core_capture.service.GeminiClient;

public class HFTest {
    public static void main(String[] args) {
        try {
            String testPacket = "[TCP] 192.168.1.100 → 10.0.0.3 [SYN]";
            String explanation = GeminiClient.analyzePacket(testPacket);
            System.out.println("Gemini Explanation:\n" + explanation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}