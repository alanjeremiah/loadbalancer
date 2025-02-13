package com.example.loadbalancer.service.impl;

import com.example.loadbalancer.config.AppConfig;
import com.example.loadbalancer.exception.NoAvailableInstanceException;
import com.example.loadbalancer.factory.LoadBalancerFactory;
import com.example.loadbalancer.strategy.LoadBalancingStrategy;
import com.example.loadbalancer.tracker.InstanceTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class LoadBalancerServiceImplTest {

    @Mock
    private LoadBalancerFactory loadBalancerFactory;

    @Mock
    private AppConfig appConfig;

    @Mock
    private WebClient webClient;

    @Mock
    private InstanceTracker instanceTracker;

    @Mock
    private LoadBalancingStrategy loadBalancingStrategy;

    @InjectMocks
    private LoadBalancerServiceImpl loadBalancerService;

    private static final List<String> INSTANCES = List.of(
            "http://localhost:8081",
            "http://localhost:8082"
    );

    private static final Map<String, Object> REQUEST_PAYLOAD = Map.of(
            "game", "Mobile Legends",
            "gamerID", "GYUTDTE",
            "points", 20
    );

    private static final String ROUND_ROBIN_ALGORITHM = "roundrobin";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        webClient = mock(WebClient.class, RETURNS_DEEP_STUBS);
        loadBalancerService = new LoadBalancerServiceImpl(loadBalancerFactory, appConfig, webClient, instanceTracker);
    }

    @Test
    void givenValidRequest_whenServiceIsHealthy_thenReturnsResponse() {
        when(appConfig.getInstances()).thenReturn(INSTANCES);
        when(appConfig.getAlgorithm()).thenReturn(ROUND_ROBIN_ALGORITHM);
        when(appConfig.getWorkerApiEndpoint()).thenReturn("/process");
        when(loadBalancerFactory.getStrategy(ROUND_ROBIN_ALGORITHM)).thenReturn(loadBalancingStrategy);
        when(loadBalancingStrategy.getInstanceUrl(INSTANCES)).thenReturn("http://localhost:8081");

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("http://localhost:8081/process"))).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(eq(REQUEST_PAYLOAD));
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(REQUEST_PAYLOAD));

        Map<String, Object> response = loadBalancerService.routeRequest(REQUEST_PAYLOAD);

        assertEquals(REQUEST_PAYLOAD, response);
        verify(loadBalancerFactory, times(1)).getStrategy(ROUND_ROBIN_ALGORITHM);
    }

    @Test
    void givenFirstInstanceFails_whenSecondInstanceWorks_thenReturnsResponse() {
        when(appConfig.getInstances()).thenReturn(INSTANCES);
        when(appConfig.getAlgorithm()).thenReturn(ROUND_ROBIN_ALGORITHM);
        when(appConfig.getWorkerApiEndpoint()).thenReturn("/process");
        when(loadBalancerFactory.getStrategy(ROUND_ROBIN_ALGORITHM)).thenReturn(loadBalancingStrategy);
        when(loadBalancingStrategy.getInstanceUrl(INSTANCES))
                .thenReturn("http://localhost:8081")
                .thenReturn("http://localhost:8082");

        // Separate mocks for first and second attempts
        WebClient.RequestBodyUriSpec firstRequestUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodyUriSpec secondRequestUriSpec = mock(WebClient.RequestBodyUriSpec.class);

        WebClient.RequestBodySpec firstRequestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestBodySpec secondRequestBodySpec = mock(WebClient.RequestBodySpec.class);

        WebClient.RequestHeadersSpec<?> firstRequestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.RequestHeadersSpec<?> secondRequestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);

        WebClient.ResponseSpec firstResponseSpec = mock(WebClient.ResponseSpec.class);
        WebClient.ResponseSpec secondResponseSpec = mock(WebClient.ResponseSpec.class);

        // First request setup (which fails)
        when(webClient.post()).thenReturn(firstRequestUriSpec, secondRequestUriSpec);
        when(firstRequestUriSpec.uri(eq("http://localhost:8081/process"))).thenReturn(firstRequestBodySpec);
        doReturn(firstRequestHeadersSpec).when(firstRequestBodySpec).bodyValue(eq(REQUEST_PAYLOAD));
        when(firstRequestHeadersSpec.retrieve()).thenReturn(firstResponseSpec);
        when(firstResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Instance down"));

        // Second request setup (which succeeds)
        when(secondRequestUriSpec.uri(eq("http://localhost:8082/process"))).thenReturn(secondRequestBodySpec);
        doReturn(secondRequestHeadersSpec).when(secondRequestBodySpec).bodyValue(eq(REQUEST_PAYLOAD));
        when(secondRequestHeadersSpec.retrieve()).thenReturn(secondResponseSpec);
        when(secondResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(REQUEST_PAYLOAD));

        Map<String, Object> response = loadBalancerService.routeRequest(REQUEST_PAYLOAD);

        assertEquals(REQUEST_PAYLOAD, response);
        verify(instanceTracker, times(1)).markInstanceUnHealthy("http://localhost:8081");
        verify(webClient, times(2)).post();
    }

    @Test
    void givenValidRequest_whenAllInstancesFail_throwsNoAvailableInstance() {
        when(appConfig.getInstances()).thenReturn(INSTANCES);
        when(appConfig.getAlgorithm()).thenReturn(ROUND_ROBIN_ALGORITHM);
        when(appConfig.getWorkerApiEndpoint()).thenReturn("/process");
        when(loadBalancerFactory.getStrategy(ROUND_ROBIN_ALGORITHM)).thenReturn(loadBalancingStrategy);
        when(loadBalancingStrategy.getInstanceUrl(INSTANCES))
                .thenReturn("http://localhost:8081")
                .thenReturn("http://localhost:8082");

        // Separate mocks for first and second attempts
        WebClient.RequestBodyUriSpec firstRequestUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodyUriSpec secondRequestUriSpec = mock(WebClient.RequestBodyUriSpec.class);

        WebClient.RequestBodySpec firstRequestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestBodySpec secondRequestBodySpec = mock(WebClient.RequestBodySpec.class);

        WebClient.RequestHeadersSpec<?> firstRequestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.RequestHeadersSpec<?> secondRequestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);

        WebClient.ResponseSpec firstResponseSpec = mock(WebClient.ResponseSpec.class);
        WebClient.ResponseSpec secondResponseSpec = mock(WebClient.ResponseSpec.class);

        // First request setup (which fails)
        when(webClient.post()).thenReturn(firstRequestUriSpec, secondRequestUriSpec);
        when(firstRequestUriSpec.uri(eq("http://localhost:8081/process"))).thenReturn(firstRequestBodySpec);
        doReturn(firstRequestHeadersSpec).when(firstRequestBodySpec).bodyValue(eq(REQUEST_PAYLOAD));
        when(firstRequestHeadersSpec.retrieve()).thenReturn(firstResponseSpec);
        when(firstResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Instance down"));

        // Second request setup (which fails)
        when(secondRequestUriSpec.uri(eq("http://localhost:8082/process"))).thenReturn(secondRequestBodySpec);
        doReturn(secondRequestHeadersSpec).when(secondRequestBodySpec).bodyValue(eq(REQUEST_PAYLOAD));
        when(secondRequestHeadersSpec.retrieve()).thenReturn(secondResponseSpec);
        when(secondResponseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Instance down"));

        NoAvailableInstanceException noAvailableInstanceException = assertThrows(
                NoAvailableInstanceException.class,
                () -> loadBalancerService.routeRequest(REQUEST_PAYLOAD)
        );

        assertEquals("No healthy instance available to route the request",
                noAvailableInstanceException.getMessage());
        verify(instanceTracker, times(2)).markInstanceUnHealthy(anyString());
        verify(webClient, times(2)).post();
    }

    @Test
    void givenInstanceFails_whenServiceRetries_thenMarksInstanceUnhealthy() {
        when(appConfig.getInstances()).thenReturn(INSTANCES);
        when(appConfig.getAlgorithm()).thenReturn(ROUND_ROBIN_ALGORITHM);
        when(appConfig.getWorkerApiEndpoint()).thenReturn("/process");
        when(loadBalancerFactory.getStrategy(ROUND_ROBIN_ALGORITHM)).thenReturn(loadBalancingStrategy);
        when(loadBalancingStrategy.getInstanceUrl(INSTANCES))
                .thenReturn("http://localhost:8081")
                .thenReturn("http://localhost:8082");

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("http://localhost:8081/process"))).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(eq(REQUEST_PAYLOAD));
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Instance down"));

        assertThrows(NoAvailableInstanceException.class, () -> loadBalancerService.routeRequest(REQUEST_PAYLOAD));

        verify(instanceTracker, times(1)).markInstanceUnHealthy("http://localhost:8081");
    }

    @Test
    void givenAllInstancesFail_whenServiceRetries_thenRetriesExpectedTimes() {
        when(appConfig.getInstances()).thenReturn(INSTANCES);
        when(appConfig.getAlgorithm()).thenReturn(ROUND_ROBIN_ALGORITHM);
        when(appConfig.getWorkerApiEndpoint()).thenReturn("/process");
        when(loadBalancerFactory.getStrategy(ROUND_ROBIN_ALGORITHM)).thenReturn(loadBalancingStrategy);
        when(loadBalancingStrategy.getInstanceUrl(INSTANCES))
                .thenReturn("http://localhost:8081")
                .thenReturn("http://localhost:8082");

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(eq(REQUEST_PAYLOAD));
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Instance down"));

        assertThrows(NoAvailableInstanceException.class, () -> loadBalancerService.routeRequest(REQUEST_PAYLOAD));

        verify(loadBalancingStrategy, times(2)).getInstanceUrl(INSTANCES);
    }
}
