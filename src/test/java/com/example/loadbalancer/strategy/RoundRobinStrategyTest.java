package com.example.loadbalancer.strategy;

import com.example.loadbalancer.exception.NoAvailableInstanceException;
import com.example.loadbalancer.tracker.InstanceTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class RoundRobinStrategyTest {

    @Mock
    private InstanceTracker instanceTracker;

    @InjectMocks
    private RoundRobinStrategy roundRobinStrategy;

    private static final List<String> INSTANCES = List.of(
            "http://localhost:8081",
            "http://localhost:8082",
            "http://localhost:8083"
    );

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void givenMultipleInstances_whenGetInstanceIsCalled_thenCyclesCorrectly() {

        when(instanceTracker.isInstanceUnhealthy(anyString())).thenReturn(false);

        assertEquals("http://localhost:8081", roundRobinStrategy.getInstanceUrl(INSTANCES));
        assertEquals("http://localhost:8082", roundRobinStrategy.getInstanceUrl(INSTANCES));
        assertEquals("http://localhost:8083", roundRobinStrategy.getInstanceUrl(INSTANCES));
        assertEquals("http://localhost:8081", roundRobinStrategy.getInstanceUrl(INSTANCES));
    }

    @Test
    void givenMultipleInstances_whenAllInstancesAreUnHealthy_throwsNoAvailableInstance() {
        when(instanceTracker.isInstanceUnhealthy(anyString())).thenReturn(true);

        NoAvailableInstanceException noAvailableInstanceException = assertThrows(
                NoAvailableInstanceException.class,
                () -> roundRobinStrategy.getInstanceUrl(INSTANCES)
        );

        assertEquals("No Healthy instance available", noAvailableInstanceException.getMessage());
    }

    @Test
    void givenMultipleInstances_whenAnInstanceIsCalled_thenSkipsUnhealthyInstance() {
        when(instanceTracker.isInstanceUnhealthy("http://localhost:8081")).thenReturn(true);
        when(instanceTracker.isInstanceUnhealthy("http://localhost:8082")).thenReturn(false);

        assertEquals("http://localhost:8082", roundRobinStrategy.getInstanceUrl(INSTANCES));
        verify(instanceTracker, times(2)).isInstanceUnhealthy(anyString());
    }

    @Test
    void givenEmptyInstances_whenGetInstanceIsCalled_throwsNoAvailableInstance() {
        NoAvailableInstanceException noAvailableInstanceException = assertThrows(
                NoAvailableInstanceException.class,
                () -> roundRobinStrategy.getInstanceUrl(List.of())
        );

        assertEquals("No Healthy instance available", noAvailableInstanceException.getMessage());
    }

    @Test
    void givenSingleInstance_whenGetInstanceIsCalled_thenReturnsSameInstance() {
        List<String> singleInstance = List.of("http://localhost:8081");

        when(instanceTracker.isInstanceUnhealthy("http://localhost:8081")).thenReturn(false);

        assertEquals("http://localhost:8081", roundRobinStrategy.getInstanceUrl(singleInstance));
        assertEquals("http://localhost:8081", roundRobinStrategy.getInstanceUrl(singleInstance));
    }

    @Test
    void givenCounterNearMax_whenGetInstanceUrl_thenResetsToZero() throws Exception {
        when(instanceTracker.isInstanceUnhealthy(anyString())).thenReturn(false);

        Field counterField = RoundRobinStrategy.class.getDeclaredField("counter");
        counterField.setAccessible(true);
        counterField.set(roundRobinStrategy, new AtomicInteger(Integer.MAX_VALUE - 1));

        int before = ( (AtomicInteger) counterField.get(roundRobinStrategy) ).get();
        assertEquals(Integer.MAX_VALUE - 1, before);

        roundRobinStrategy.getInstanceUrl(INSTANCES);

        int after = ( (AtomicInteger) counterField.get(roundRobinStrategy) ).get();
        assertEquals(0, after);
    }
}
