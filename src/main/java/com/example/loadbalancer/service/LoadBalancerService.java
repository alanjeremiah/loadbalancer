package com.example.loadbalancer.service;

import java.util.Map;

/**
 *  Interface responsible for processing the load balancer operations
 */
public interface LoadBalancerService {

    /**
     * Method to route the request to the appropriate instance using the configured strategy
     *
     * @param payload, The incoming request is passed as a payload.
     * @return, Returns the response from the appropriate instance handler
     */
    Map<String, Object> routeRequest(Map<String, Object> payload);
}
