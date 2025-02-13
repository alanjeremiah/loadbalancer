package com.example.loadbalancer.factory;

import com.example.loadbalancer.strategy.LoadBalancingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoadBalancerFactoryTest {

    @Mock
    private LoadBalancingStrategy roundRobinStrategy;

    @Mock
    private LoadBalancingStrategy customStrategy;

    private LoadBalancerFactory loadBalancerFactory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(roundRobinStrategy.getName()).thenReturn("roundrobin");
        when(customStrategy.getName()).thenReturn("custom");

        loadBalancerFactory = new LoadBalancerFactory(List.of(roundRobinStrategy, customStrategy));
    }

    @Test
    void givenValidStrategyName_whenGetStrategy_thenReturnsCorrectStrategy() {
        LoadBalancingStrategy strategy = loadBalancerFactory.getStrategy("custom");
        assertEquals(customStrategy, strategy);
    }

    @Test
    void givenInvalidStrategyName_whenGetStrategy_thenReturnsDefault() {
        LoadBalancingStrategy strategy = loadBalancerFactory.getStrategy("nonexistent");
        assertEquals(roundRobinStrategy, strategy);
    }

    @Test
    void givenNoValidStrategy_whenGetStrategy_thenThrowsException() {
        LoadBalancerFactory emptyFactory = new LoadBalancerFactory(List.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> emptyFactory.getStrategy("random"));

        assertEquals("No valid load balancing strategy available", exception.getMessage());
    }
}
