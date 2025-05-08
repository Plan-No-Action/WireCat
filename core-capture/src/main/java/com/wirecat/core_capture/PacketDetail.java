// src/main/java/com/wirecat/core_capture/PacketDetail.java
package com.wirecat.core_capture;

public class PacketDetail {
    private final String frame;
    private final String ethernet;
    private final String ip;
    private final String transport;
    private final String application;

    public PacketDetail(String frame, String ethernet, String ip,
                        String transport, String application) {
        this.frame       = frame;
        this.ethernet    = ethernet;
        this.ip          = ip;
        this.transport   = transport;
        this.application = application;
    }

    public String getFrame()       { return frame; }
    public String getEthernet()    { return ethernet; }
    public String getIp()          { return ip; }
    public String getTransport()   { return transport; }
    public String getApplication() { return application; }
}
