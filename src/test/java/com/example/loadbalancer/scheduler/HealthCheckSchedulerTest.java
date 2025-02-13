package com.example.loadbalancer.scheduler;

import com.example.loadbalancer.config.AppConfig;
import com.example.loadbalancer.tracker.InstanceTracker;
import com.example.loadbalancer.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

class HealthCheckSchedulerTest {

    @Mock
    private InstanceTracker instanceTracker;

    @Mock
    private WebClient webClient;

    @Mock
    private AppConfig appConfig;

    @InjectMocks
    private HealthCheckScheduler healthCheckScheduler;

    private static final String INSTANCE_1 = "http://localhost:8081";
    private static final String INSTANCE_2 = "http://localhost:8082";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void givenUnhealthyInstances_whenHealthCheck_thenMarkHealthyIfAvailable() {
        when(instanceTracker.getUnhealthyInstances()).thenReturn(Set.of(INSTANCE_1, INSTANCE_2));
        when(appConfig.getWorkerHealthEndpoint()).thenReturn("/actuator/health");

        WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        doReturn(requestHeadersUriSpec).when(webClient).get();

        doReturn(requestHeadersUriSpec).when(requestHeadersUriSpec).uri(INSTANCE_1 + "/actuator/health");
        doReturn(requestHeadersUriSpec).when(requestHeadersUriSpec).uri(INSTANCE_2 + "/actuator/health");
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(Map.class)).thenReturn(
                Mono.just(Map.of(Constants.STATUS, "UP")),
                Mono.just(Map.of(Constants.STATUS, "DOWN"))
        );

        healthCheckScheduler.healthCheck();

        verify(instanceTracker, times(1)).markInstanceHealthy(INSTANCE_1);
        verify(instanceTracker, never()).markInstanceHealthy(INSTANCE_2);
    }

    @Test
    void givenInstanceFailsHealthCheck_whenHealthCheckRuns_thenInstanceRemainsUnhealthy() {
        when(instanceTracker.getUnhealthyInstances()).thenReturn(Set.of(INSTANCE_1));
        when(appConfig.getWorkerHealthEndpoint()).thenReturn("/actuator/health");

        WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersUriSpec).when(requestHeadersUriSpec).uri(INSTANCE_1 + "/actuator/health");
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(Map.class)).thenReturn(
                Mono.just(Map.of(Constants.STATUS, "DOWN"))
        );

        healthCheckScheduler.healthCheck();

        verify(instanceTracker, never()).markInstanceHealthy(INSTANCE_1);
    }

    @Test
    void givenInstanceHealthCheckFailsWithException_whenHealthCheckRuns_thenInstanceRemainsUnhealthy() {
        when(instanceTracker.getUnhealthyInstances()).thenReturn(Set.of(INSTANCE_1));
        when(appConfig.getWorkerHealthEndpoint()).thenReturn("/actuator/health");

        WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersUriSpec).when(requestHeadersUriSpec).uri(INSTANCE_1 + "/actuator/health");
        when(requestHeadersUriSpec.retrieve()).thenThrow(new RuntimeException("Service unavailable"));

        healthCheckScheduler.healthCheck();

        verify(instanceTracker, never()).markInstanceHealthy(INSTANCE_1);
    }
}
