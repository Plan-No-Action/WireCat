package com.wirecat.core_capture.util;

import java.io.*;
import java.util.*;

public class MacVendorLookup {
    private static final Map<String, String> OUI_MAP = new HashMap<>();

    static {
        try (InputStream is = MacVendorLookup.class.getResourceAsStream("/oui.csv");
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] arr = line.split(",", 2);
                if (arr.length == 2) {
                    OUI_MAP.put(arr[0].toUpperCase(), arr[1]);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load OUI mapping: " + e.getMessage());
        }
    }

    public static String lookup(String mac) {
        if (mac == null || mac.length() < 8) return "Unknown";
        String[] parts = mac.split(":");
        if (parts.length < 3) return "Unknown";
        String oui = (parts[0] + ":" + parts[1] + ":" + parts[2]).toUpperCase();
        return OUI_MAP.getOrDefault(oui, "Unknown");
    }
}
