package com.example.loadbalancer.tracker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the health status of backend instances.
 * Stores the instances that are marked as unhealthy.
 */
@Slf4j
@Component
public class InstanceTracker {

    private final Set<String> unhealthyInstances = ConcurrentHashMap.newKeySet();

    public void markInstanceUnHealthy(String workerUrl) {
        unhealthyInstances.add(workerUrl);
        log.warn("Marked instance {} as unhealthy", workerUrl);
    }

    public void markInstanceHealthy(String workerUrl) {
        unhealthyInstances.remove(workerUrl);
        log.info("Recovered instance {} and added back to available instances", workerUrl);
    }

    public boolean isInstanceUnhealthy(String workerUrl) {
        return unhealthyInstances.contains(workerUrl);
    }

    public Set<String> getUnhealthyInstances() {
        return unhealthyInstances;
    }
}
