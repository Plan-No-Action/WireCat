package com.wirecat.core_capture.filter;

import com.wirecat.core_capture.Packet;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * Encapsulates all packet‐filtering logic.
 */
public class FilterEngine {
    private final ObservableList<Packet> masterList;

    public FilterEngine(ObservableList<Packet> masterList) {
        this.masterList = masterList;
    }

    /**
     * Returns a live FilteredList that only shows packets matching the given protocol.
     * Use "All" to disable filtering.
     */
    public FilteredList<Packet> filterByProtocol(String protocol) {
        FilteredList<Packet> filtered = new FilteredList<>(masterList, p -> true);
        if (protocol != null && !"All".equalsIgnoreCase(protocol)) {
            filtered.setPredicate(p -> p.getProto().equalsIgnoreCase(protocol));
        }
        return filtered;
    }
}
