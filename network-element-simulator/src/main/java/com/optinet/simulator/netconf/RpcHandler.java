package com.optinet.simulator.netconf;

import java.util.*;
import java.util.concurrent.*;

/**
 * NETCONF RPC Request Handler
 * 
 * Processes incoming NETCONF RPC requests and generates appropriate responses.
 * Supports:
 * - <get> : retrieve running operational data
 * - <get-config> : retrieve configuration
 * - <edit-config> : modify configuration
 * 
 * This is a simplified handler for simulation. In production, this would
 * interface with actual network element hardware/firmware.
 */
public class RpcHandler {
    private final String neId;
    private final String neType;
    private final Map<String, Object> config;
    private final Map<String, Object> operationalData;
    
    public RpcHandler(String neId, String neType) {
        this.neId = neId;
        this.neType = neType;
        this.config = new ConcurrentHashMap<>();
        this.operationalData = new ConcurrentHashMap<>();
        initializeDefaultData();
    }
    
    /**
     * Initialize default configuration and operational data
     */
    private void initializeDefaultData() {
        // Default configuration
        config.put("hostname", neId);
        config.put("ntp-enabled", "true");
        config.put("logging-level", "info");
        
        // Operational data (system info, interfaces, etc.)
        operationalData.put("system-status", "operational");
        operationalData.put("uptime", System.currentTimeMillis());
        operationalData.put("ne-type", neType);
    }
    
    /**
     * Handle <get> RPC: retrieve operational data
     */
    public String handleGet() {
        StringBuilder xml = new StringBuilder();
        xml.append("  <data>\n");
        xml.append("    <system>\n");
        xml.append(String.format("      <hostname>%s</hostname>\n", neId));
        xml.append(String.format("      <ne-type>%s</ne-type>\n", neType));
        xml.append(String.format("      <status>%s</status>\n", operationalData.get("system-status")));
        xml.append("      <interfaces>\n");
        
        // Add interface data
        for (int i = 1; i <= getInterfaceCount(); i++) {
            xml.append(String.format("        <interface>\n"));
            xml.append(String.format("          <name>eth%d</name>\n", i));
            xml.append(String.format("          <admin-status>up</admin-status>\n"));
            xml.append(String.format("          <oper-status>up</oper-status>\n"));
            xml.append(String.format("          <mtu>1500</mtu>\n"));
            xml.append(String.format("        </interface>\n"));
        }
        
        xml.append("      </interfaces>\n");
        xml.append("    </system>\n");
        xml.append("  </data>\n");
        
        return xml.toString();
    }
    
    /**
     * Handle <get-config> RPC: retrieve configuration
     */
    public String handleGetConfig() {
        StringBuilder xml = new StringBuilder();
        xml.append("  <data>\n");
        xml.append("    <configuration>\n");
        
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            xml.append(String.format("      <%s>%s</%s>\n", 
                entry.getKey(), 
                entry.getValue(), 
                entry.getKey()));
        }
        
        xml.append("    </configuration>\n");
        xml.append("  </data>\n");
        
        return xml.toString();
    }
    
    /**
     * Handle <edit-config> RPC: modify configuration
     */
    public String handleEditConfig(String configXml) {
        // Simple parsing: look for key-value pairs in XML
        if (configXml.contains("<hostname>")) {
            int start = configXml.indexOf("<hostname>") + 10;
            int end = configXml.indexOf("</hostname>");
            if (start < end) {
                String hostname = configXml.substring(start, end);
                config.put("hostname", hostname);
            }
        }
        
        return ""; // OK response is handled separately
    }
    
    /**
     * Get number of interfaces for this NE type
     */
    private int getInterfaceCount() {
        return switch (neType.toLowerCase()) {
            case "transponder" -> 10;      // 10 optical ports
            case "amplifier" -> 4;         // 4 amplifier stages
            case "switch" -> 32;           // 32 ports
            case "cross-connect" -> 16;    // 16 wavelength paths
            default -> 8;
        };
    }
    
    public String getNeId() {
        return neId;
    }
    
    public String getNeType() {
        return neType;
    }
}
