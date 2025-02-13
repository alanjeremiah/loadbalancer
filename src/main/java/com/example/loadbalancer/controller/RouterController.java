package com.example.loadbalancer.controller;

import com.example.loadbalancer.exception.InvalidRequestException;
import com.example.loadbalancer.service.LoadBalancerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 *  RouterController class for routing requests to handlers via load balancer strategies
 *  Validates the request payload and delegates the routing to {@link LoadBalancerService}
 */
@Slf4j
@RestController
@RequestMapping("/route")
public class RouterController {

    private final LoadBalancerService loadBalancerService;

    public RouterController(LoadBalancerService loadBalancerService) {
        this.loadBalancerService = loadBalancerService;
    }

    /**
     * Endpoint to handle the incoming requests and routes to the appropriate instance
     *
     * @param payload, The incoming request is passed as a payload
     * @return, The response handled by the appropriate instance
     * @throws InvalidRequestException in case of invalid payload
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> routeRequest(@RequestBody Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new InvalidRequestException("Payload cannot be empty");
        }
        Map<String, Object> response = loadBalancerService.routeRequest(payload);
        log.info("Reached here: {}" , response);
        return ResponseEntity.ok(response);
    }
}
