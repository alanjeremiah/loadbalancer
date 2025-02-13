package com.example.loadbalancer.service.impl;

import com.example.loadbalancer.config.AppConfig;
import com.example.loadbalancer.exception.NoAvailableInstanceException;
import com.example.loadbalancer.factory.LoadBalancerFactory;
import com.example.loadbalancer.service.LoadBalancerService;
import com.example.loadbalancer.strategy.LoadBalancingStrategy;
import com.example.loadbalancer.tracker.InstanceTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Service class for implementing the LoadBalancer operation for routing the requests
 * Fetches the appropriate instance by the configured strategy, retrying on failure
 */
@Slf4j
@Service
public class LoadBalancerServiceImpl implements LoadBalancerService {

    private final LoadBalancerFactory loadBalancerFactory;
    private final AppConfig appConfig;
    private final WebClient webClient;
    private final InstanceTracker instanceTracker;

    public LoadBalancerServiceImpl(LoadBalancerFactory loadBalancerFactory, AppConfig appConfig,
                                   WebClient webClient, InstanceTracker instanceTracker) {
        this.loadBalancerFactory = loadBalancerFactory;
        this.appConfig = appConfig;
        this.webClient = webClient;
        this.instanceTracker = instanceTracker;
    }

    @Override
    public Map<String, Object> routeRequest(Map<String, Object> payload) {
        LoadBalancingStrategy strategy = loadBalancerFactory.getStrategy(appConfig.getAlgorithm());
        int attempts = 0;
        int instancesSize = appConfig.getInstances().size();

        while (attempts < instancesSize) {
            String instanceUrl = strategy.getInstanceUrl(appConfig.getInstances());
            try {
                log.info("Routing request to: {}", instanceUrl);
                return webClient.post()
                        .uri(instanceUrl + appConfig.getWorkerApiEndpoint())
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .timeout(Duration.ofSeconds(5))
                        .block();
            } catch (Exception e) {
                log.error("Failed to reach worker instance {}: {}", instanceUrl, e.getMessage());
                instanceTracker.markInstanceUnHealthy(instanceUrl);
            }
            attempts++;
        }
        throw new NoAvailableInstanceException("No healthy instance available to route the request");
    }
}
