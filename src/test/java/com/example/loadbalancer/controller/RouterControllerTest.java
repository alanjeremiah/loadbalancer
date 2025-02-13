package com.example.loadbalancer.controller;

import com.example.loadbalancer.exception.InvalidRequestException;
import com.example.loadbalancer.exception.NoAvailableInstanceException;
import com.example.loadbalancer.service.LoadBalancerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

class RouterControllerTest {

    @Mock
    private LoadBalancerService loadBalancerService;

    @InjectMocks
    private RouterController routerController;

    private static final Map<String, Object> VALID_PAYLOAD = Map.of(
            "game", "Mobile Legends",
            "gamerID", "GYUTDTE",
            "points", 20
    );

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("Should return 200 OK when a valid request is routed")
    @Test
    void givenValidRequest_whenServiceSucceeds_thenReturns200() {
        when(loadBalancerService.routeRequest(VALID_PAYLOAD)).thenReturn(VALID_PAYLOAD);

        ResponseEntity<Map<String, Object>> response = routerController.routeRequest(VALID_PAYLOAD);

        assertAll(
                () -> assertEquals(200, response.getStatusCode().value(), "Status code should be 200"),
                () -> assertEquals(VALID_PAYLOAD, response.getBody(), "Response body should match request payload")
        );

        verify(loadBalancerService, times(1)).routeRequest(VALID_PAYLOAD);
    }

    @DisplayName("Should throw InvalidRequestException when request payload is empty")
    @Test
    void givenEmptyRequest_whenValidated_thenThrowsInvalidRequestException() {
        Map<String, Object> emptyPayload = Map.of();

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> routerController.routeRequest(emptyPayload)
        );

        assertEquals("Payload cannot be empty", exception.getMessage());
        verifyNoInteractions(loadBalancerService);
    }

    @DisplayName("Should return 500 Internal Server Error when service throws an unexpected exception")
    @Test
    void givenValidRequest_whenServiceThrowsException_thenReturns500() {
        when(loadBalancerService.routeRequest(VALID_PAYLOAD)).thenThrow(
                new RuntimeException("An unexpected error occurred"));

        Exception exception = assertThrows(
                Exception.class,
                () -> routerController.routeRequest(VALID_PAYLOAD)
        );

        assertEquals("An unexpected error occurred", exception.getMessage());
        verify(loadBalancerService, times(1)).routeRequest(VALID_PAYLOAD);
    }

    @DisplayName("Should throw NoAvailableInstanceException when no healthy instances are available")
    @Test
    void givenValidRequest_whenNoInstanceAvailable_thenThrowsNoAvailableInstanceException() {
        when(loadBalancerService.routeRequest(VALID_PAYLOAD)).thenThrow(
                new NoAvailableInstanceException("No healthy instance available"));

        NoAvailableInstanceException exception = assertThrows(
                NoAvailableInstanceException.class,
                () -> routerController.routeRequest(VALID_PAYLOAD)
        );

        assertEquals("No healthy instance available", exception.getMessage());
        verify(loadBalancerService, times(1)).routeRequest(VALID_PAYLOAD);
    }
}
