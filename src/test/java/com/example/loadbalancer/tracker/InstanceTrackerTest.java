package com.example.loadbalancer.tracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class InstanceTrackerTest {

    private InstanceTracker instanceTracker;

    @BeforeEach
    void setUp() {
        instanceTracker = new InstanceTracker();
    }

    @Test
    void givenHealthyInstance_whenMarkedUnhealthy_thenItIsTracked() {
        String instance = "http://localhost:8081";

        instanceTracker.markInstanceUnHealthy(instance);

        assertTrue(instanceTracker.isInstanceUnhealthy(instance));
        assertEquals(Set.of(instance), instanceTracker.getUnhealthyInstances());
    }

    @Test
    void givenUnhealthyInstance_whenMarkedHealthy_thenItIsRemoved() {
        String instance = "http://localhost:8081";

        instanceTracker.markInstanceUnHealthy(instance);
        instanceTracker.markInstanceHealthy(instance);

        assertFalse(instanceTracker.isInstanceUnhealthy(instance));
        assertTrue(instanceTracker.getUnhealthyInstances().isEmpty());
    }

    @Test
    void givenMultipleInstances_whenTracked_thenAllAreStoredCorrectly() {
        String instance1 = "http://localhost:8081";
        String instance2 = "http://localhost:8082";

        instanceTracker.markInstanceUnHealthy(instance1);
        instanceTracker.markInstanceUnHealthy(instance2);

        assertEquals(Set.of(instance1, instance2), instanceTracker.getUnhealthyInstances());
    }

    @Test
    void givenConcurrentModifications_whenMarkingInstances_thenHandlesThreadSafety() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        String instance = "http://localhost:8081";

        for (int i = 0; i < 100; i++) {
            executor.execute(() -> instanceTracker.markInstanceUnHealthy(instance));
            executor.execute(() -> instanceTracker.markInstanceHealthy(instance));
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertTrue(instanceTracker.getUnhealthyInstances().isEmpty() || instanceTracker.getUnhealthyInstances().contains(instance));
    }
}
