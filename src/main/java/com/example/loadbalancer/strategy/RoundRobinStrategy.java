package com.example.loadbalancer.strategy;

import com.example.loadbalancer.exception.NoAvailableInstanceException;
import com.example.loadbalancer.tracker.InstanceTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements the LoadBalancingStrategy operations using the Round Robin Strategy
 * Distributes the requests evenly across the instances
 * and returns a healthy instance while skipping unhealthy instances
 */
@Slf4j
@Service
public class RoundRobinStrategy implements LoadBalancingStrategy{

    private final AtomicInteger counter;
    private final InstanceTracker instanceTracker;

    public RoundRobinStrategy(InstanceTracker instanceTracker) {
        this.counter = new AtomicInteger(0);
        this.instanceTracker = instanceTracker;
    }
    @Override
    public String getInstanceUrl(List<String> instances) {
        for (int i = 0; i < instances.size(); i++) {
            int index = counter.getAndUpdate(
                    current -> (current >= Integer.MAX_VALUE - 1) ? 0 : current + 1) % instances.size();
            String instance = instances.get(index);
            if (!instanceTracker.isInstanceUnhealthy(instance)) {
                return instance;
            }
        }
        throw new NoAvailableInstanceException("No Healthy instance available");
    }

    @Override
    public String getName() {
        return "roundrobin";
    }
}
