package com.example.loadbalancer.scheduler;

import com.example.loadbalancer.config.AppConfig;
import com.example.loadbalancer.tracker.InstanceTracker;
import com.example.loadbalancer.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Scheduler class to periodically monitor the unhealthy instances and update the health of the instance.
 * Adds the instance back into the pool if healthy
 */
@Slf4j
@Component
@EnableScheduling
public class HealthCheckScheduler {

    private final InstanceTracker instanceTracker;
    private final WebClient webClient;
    private final AppConfig appConfig;

    public HealthCheckScheduler(InstanceTracker instanceTracker, WebClient webClient, AppConfig appConfig) {
        this.instanceTracker = instanceTracker;
        this.webClient = webClient;
        this.appConfig = appConfig;
    }

    @Scheduled(fixedRate = 5000)
    public void healthCheck() {
        log.info("Running scheduled health check for unhealthy instances");
        Set<String> unhealthyInstances = instanceTracker.getUnhealthyInstances();
        for (String instance : unhealthyInstances) {
            if (isInstanceHealthy(instance)) {
                instanceTracker.markInstanceHealthy(instance);
            }
        }
    }

    public boolean isInstanceHealthy(String workerUrl) {
        try {
            String healthUrl = workerUrl + appConfig.getWorkerHealthEndpoint();
            return Boolean.TRUE.equals(
                    webClient.get()
                            .uri(healthUrl)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .map(response -> Constants.HEALTH_STATUS_UP.equalsIgnoreCase(
                                    response.get(Constants.STATUS).toString()))
                            .block(Duration.ofSeconds(2))
            );
        } catch (Exception e) {
            log.warn("Health check failed for {}: {}", workerUrl, e.getMessage());
            return false;
        }
    }
}
