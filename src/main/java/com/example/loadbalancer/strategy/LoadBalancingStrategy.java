package com.example.loadbalancer.strategy;

import java.util.List;

/**
 *  Interface responsible for defining the operations of a Strategy Handler
 */
public interface LoadBalancingStrategy {

    String getInstanceUrl(List<String> instances);
    String getName();
}
