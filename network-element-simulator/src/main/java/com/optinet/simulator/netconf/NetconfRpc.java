package com.optinet.simulator.netconf;

import java.util.*;

/**
 * Represents a NETCONF RPC message.
 * NETCONF (Network Configuration Protocol) uses XML-based RPC for communication.
 * This class wraps incoming RPC requests and manages responses.
 * Example RPC:
 * <rpc message-id="1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
 *   <get>
 *     <filter type="subtree">
 *       <system>
 *         <status/>
 *       </system>
 *     </filter>
 *   </get>
 * </rpc>
 */
public class NetconfRpc {
    private final String messageId;
    private final String operation;
    private final String requestXml;
    private final Map<String, String> filterMap;
    
    public NetconfRpc(String messageId, String operation, String requestXml) {
        this.messageId = messageId;
        this.operation = operation;
        this.requestXml = requestXml;
        this.filterMap = parseFilter(requestXml);
    }
    
    private Map<String, String> parseFilter(String xml) {
        Map<String, String> map = new HashMap<>();
        // Simple XML parsing for common NETCONF operations
        if (xml.contains("<get>")) {
            map.put("type", "get");
        } else if (xml.contains("<get-config>")) {
            map.put("type", "get-config");
        } else if (xml.contains("<edit-config>")) {
            map.put("type", "edit-config");
        } else if (xml.contains("<rpc-reply>")) {
            map.put("type", "rpc-reply");
        }
        
        // Extract source/target
        if (xml.contains("<source>")) {
            int start = xml.indexOf("<source>") + 8;
            int end = xml.indexOf("</source>");
            if (start < end) {
                map.put("source", xml.substring(start, end).trim());
            }
        }
        
        return map;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public String getRequestXml() {
        return requestXml;
    }
    
    public boolean isGet() {
        return operation.equals("get");
    }
    
    public boolean isGetConfig() {
        return operation.equals("get-config");
    }
    
    public boolean isEditConfig() {
        return operation.equals("edit-config");
    }
    
    public String getFilterValue(String key) {
        return filterMap.get(key);
    }
    
    /**
     * Build RPC-REPLY XML response
     */
    public String buildReply(String data) {
        return String.format(
            "<rpc-reply message-id=\"%s\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
            "%s\n" +
            "</rpc-reply>\n" +
            "]]>]]>",
            messageId,
            data
        );
    }
    
    /**
     * Build RPC-REPLY for OK response (e.g., edit-config success)
     */
    public String buildOkReply() {
        return String.format(
            "<rpc-reply message-id=\"%s\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
            "  <ok/>\n" +
            "</rpc-reply>\n" +
            "]]>]]>",
            messageId
        );
    }
    
    /**
     * Build RPC-REPLY for error response
     */
    public String buildErrorReply(String errorType, String errorTag, String errorMessage) {
        return String.format(
            "<rpc-reply message-id=\"%s\" xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
            "  <rpc-error>\n" +
            "    <error-type>%s</error-type>\n" +
            "    <error-tag>%s</error-tag>\n" +
            "    <error-message>%s</error-message>\n" +
            "  </rpc-error>\n" +
            "</rpc-reply>\n" +
            "]]>]]>",
            messageId,
            errorType,
            errorTag,
            errorMessage
        );
    }
}
