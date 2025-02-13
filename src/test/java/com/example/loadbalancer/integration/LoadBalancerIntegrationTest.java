package com.example.loadbalancer.integration;

import com.example.loadbalancer.tracker.InstanceTracker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class LoadBalancerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebClient webClient;

    @Autowired
    private InstanceTracker instanceTracker;

    @RegisterExtension
    static WireMockExtension wireMockInstance1 = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    @RegisterExtension
    static WireMockExtension wireMockInstance2 = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("loadbalancer.instances",
                () -> wireMockInstance1.baseUrl() + "," + wireMockInstance2.baseUrl());
    }

    private static final Map<String, Object> requestPayload = Map.of(
            "game", "Mobile Legends",
            "gamerID", "GYUTDTE",
            "points", 20
    );

    private static final Map<String, Object> errorResponse = Map.of(
            "status", "500",
            "error", "Server Down"
    );

    private static final Map<String, Object> healthUpResponse = Map.of(
            "status", "UP"
    );

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setupMocks() throws JsonProcessingException {
        instanceTracker.getUnhealthyInstances().clear();

        wireMockInstance1.resetAll();
        wireMockInstance2.resetAll();

        String jsonPayload = objectMapper.writeValueAsString(requestPayload);

        wireMockInstance1.stubFor(post(urlEqualTo("/process"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonPayload)));

        wireMockInstance2.stubFor(post(urlEqualTo("/process"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonPayload)));
    }

    @Test
    void givenValidRequest_whenRouteIsSuccess_thenReturnsResponseSameAsRequest() throws JsonProcessingException {
        String baseUrl = "http://localhost:" + port + "/route";

        Map<String, Object> responseBody = webClient.post()
                .uri(baseUrl)
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        assertEquals(requestPayload, responseBody);
    }

    @Test
    void givenValidRequest_whenNoInstanceIsAvailable_thenReturns503() throws JsonProcessingException {
        String baseUrl = "http://localhost:" + port + "/route";

        String errorResponseJson = objectMapper.writeValueAsString(errorResponse);

        wireMockInstance1.stubFor(post(urlEqualTo("/process"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(errorResponseJson)));

        wireMockInstance2.stubFor(post(urlEqualTo("/process"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(errorResponseJson)));

        WebClientResponseException exception = assertThrows(
                WebClientResponseException.class,
                () -> webClient.post()
                        .uri(baseUrl)
                        .bodyValue(requestPayload)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block()
        );

        assertEquals(503, exception.getStatusCode().value());
    }

    @Test
    void givenInvalidRequest_whenRouteIsCalled_thenReturns400() {
        String baseUrl = "http://localhost:" + port + "/route";

        Map<String, Object> emptyPayload = Map.of();

        WebClientResponseException exception = assertThrows(
                WebClientResponseException.class,
                () -> webClient.post()
                        .uri(baseUrl)
                        .bodyValue(emptyPayload)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block()
        );

        assertEquals(400, exception.getStatusCode().value());
    }


    @Test
    void givenOneInstanceFails_whenAnotherInstanceWorks_thenRoutesSuccessfully() throws JsonProcessingException {
        String baseUrl = "http://localhost:" + port + "/route";

        wireMockInstance1.stubFor(post(urlEqualTo("/process"))
                .willReturn(aResponse()
                        .withStatus(500)));

        Map<String, Object> responseBody = webClient.post()
                .uri(baseUrl)
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        assertEquals(requestPayload, responseBody);
    }

    @Test
    void givenOneInstanceFails_whenHealthIsRestored_thenInstanceIsRecovered()
            throws JsonProcessingException, InterruptedException {
        String baseUrl = "http://localhost:" + port + "/route";

        wireMockInstance1.stubFor(post(urlEqualTo("/process"))
                .willReturn(aResponse()
                        .withStatus(500)));

        wireMockInstance1.stubFor(get(urlEqualTo("/actuator/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(healthUpResponse))));

        webClient.post().uri(baseUrl).bodyValue(requestPayload).retrieve().bodyToMono(Map.class).block();

        assertTrue(instanceTracker.isInstanceUnhealthy(wireMockInstance1.baseUrl()));
        Thread.sleep(6000);
        assertFalse(instanceTracker.isInstanceUnhealthy(wireMockInstance1.baseUrl()));
    }
}
