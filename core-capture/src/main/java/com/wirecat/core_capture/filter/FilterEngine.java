package com.wirecat.core_capture.filter;

import com.wirecat.core_capture.CapturedPacket;
import java.util.function.Predicate;

public class FilterEngine {
    public static Predicate<CapturedPacket> byProtocol(String p){
        String u = p.toUpperCase();
        return c -> c.getProtocol().equalsIgnoreCase(u);
    }
}
