package com.optinet.simulator.config;

import com.optinet.simulator.auth.AuthenticationManager;
import com.optinet.simulator.topology.NetworkTopology;
import com.optinet.simulator.topology.TopologyBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the Network Element Simulator
 */
@Configuration
public class SimulatorConfiguration {
    
    /**
     * Create and initialize the network topology
     */
    @Bean
    public NetworkTopology networkTopology() {
        // Start with a long-haul topology for demonstration
        return TopologyBuilder.buildLongHaulTopology();
    }
    
    /**
     * Create authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new AuthenticationManager();
    }
}
