package com.optinet.simulator.netconf;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * NETCONF server that handles client connections and processes RPC messages.
 * 
 * NETCONF Protocol Flow:
 * 1. Client connects via SSH
 * 2. Server sends capabilities
 * 3. Client sends hello with capabilities
 * 4. Exchange RPC messages
 * 5. Each message ends with "]]>]]>"
 * 
 * This is a simplified NETCONF 1.0 implementation for simulation purposes.
 */
public class NetconfSession {
    private static final String NETCONF_1_0_CAPABILITY = 
        "urn:ietf:params:netconf:base:1.0";
    private static final String MESSAGE_DELIMITER = "]]>]]>";
    
    private final InputStream input;
    private final PrintWriter output;
    private final String sessionId;
    private boolean helloReceived = false;
    
    public NetconfSession(InputStream input, OutputStream output, String sessionId) {
        this.input = input;
        this.output = new PrintWriter(
            new OutputStreamWriter(output, StandardCharsets.UTF_8),
            true
        );
        this.sessionId = sessionId;
    }
    
    /**
     * Send server capabilities after client connects
     */
    public void sendCapabilities() {
        String capabilities = 
            "<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
            "  <capabilities>\n" +
            "    <capability>" + NETCONF_1_0_CAPABILITY + "</capability>\n" +
            "    <capability>urn:ietf:params:netconf:capability:startup:1.0</capability>\n" +
            "    <capability>urn:ietf:params:netconf:capability:url:1.0</capability>\n" +
            "  </capabilities>\n" +
            "  <session-id>" + sessionId + "</session-id>\n" +
            "</hello>\n" +
            MESSAGE_DELIMITER;
        
        output.print(capabilities);
        output.flush();
    }
    
    /**
     * Receive and parse client hello
     */
    public void receiveHello() throws IOException {
        String message = readMessage();
        if (message.contains("<hello")) {
            helloReceived = true;
        } else {
            throw new IOException("Expected hello message");
        }
    }
    
    /**
     * Read a complete NETCONF message from client
     * Messages are delimited by ]]>]]>
     */
    public String readMessage() throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(input, StandardCharsets.UTF_8)
        );
        
        StringBuilder message = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            if (line.contains(MESSAGE_DELIMITER)) {
                // Remove delimiter and return
                int delimiterIndex = line.indexOf(MESSAGE_DELIMITER);
                message.append(line, 0, delimiterIndex);
                return message.toString();
            }
            message.append(line).append("\n");
        }
        
        throw new IOException("End of stream reached without message delimiter");
    }
    
    /**
     * Send RPC reply to client
     */
    public void sendReply(String reply) {
        output.print(reply);
        output.flush();
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public boolean isHelloReceived() {
        return helloReceived;
    }
    
    public void close() {
        output.close();
    }
}
