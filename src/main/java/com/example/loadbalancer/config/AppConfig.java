package com.example.loadbalancer.config;

import io.netty.channel.ChannelOption;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;

/**
 * Configuration class for load balancer settings and WebClient setup.
 * <p>
 * This class reads application properties prefixed with `loadbalancer`
 * and provides a WebClient bean configured with connection timeouts.
 * </p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "loadbalancer")
public class AppConfig {

    private String algorithm;
    private List<String> instances;
    private String workerApiEndpoint;
    private String workerHealthEndpoint;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                                .responseTimeout(Duration.ofSeconds(5))
                ))
                .build();
    }
}
