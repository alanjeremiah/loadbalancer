package com.example.loadbalancer.factory;

import com.example.loadbalancer.strategy.LoadBalancingStrategy;
import com.example.loadbalancer.util.Constants;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory class responsible for managing load-balancing strategies.
 * It maps strategy names to their corresponding implementations.
 */
@Service
public class LoadBalancerFactory {

    private final Map<String, LoadBalancingStrategy> strategyMap;

    public LoadBalancerFactory(List<LoadBalancingStrategy> strategies) {
        strategyMap = strategies.stream().collect(Collectors.toMap(
                LoadBalancingStrategy::getName,
                strategy -> strategy
        ));
    }

    /**
     * Fetches the passed LoadBalancing Strategy by name, returns the strategy if present
     * or returns the default strategy
     *
     * @param strategyName, the configured strategy is passed here
     * @return, The corresponding Strategy Handler is returned
     * @Throws, IllegalStateException in case no handlers are configured
     */
    public LoadBalancingStrategy getStrategy(String strategyName) {
        LoadBalancingStrategy strategy = strategyMap.get(strategyName.toLowerCase());

        if (strategy == null) {
            strategy = strategyMap.get(Constants.DEFAULT_ALGORITHM);
        }

        if (strategy == null) {
            throw new IllegalStateException("No valid load balancing strategy available");
        }

        return strategy;
    }

}
