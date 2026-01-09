package com.optinet.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Network Element Simulator Application
 * 
 * Simulates optical network elements (Transponders, Amplifiers, etc.)
 * with NETCONF protocol support over SSH.
 * 
 * This module demonstrates:
 * 1. NETCONF server implementation (RFC 6241)
 * 2. Network topology simulation
 * 3. Port connection management
 * 4. RPC request handling
 * 5. SSH authentication
 * 
 * Usage:
 *   gradle bootRun -p network-element-simulator
 * 
 * Test NETCONF connection:
 *   ssh -p 8830 admin@localhost
 *   (password: admin)
 * 
 * Send NETCONF RPC:
 *   <rpc message-id="1">
 *     <get/>
 *   </rpc>
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.optinet.simulator"
})
public class NeSimulatorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(NeSimulatorApplication.class, args);
    }
}
