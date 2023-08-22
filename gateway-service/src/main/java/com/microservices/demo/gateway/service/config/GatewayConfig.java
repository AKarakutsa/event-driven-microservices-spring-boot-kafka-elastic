package com.microservices.demo.gateway.service.config;

import com.microservices.demo.config.GatewayServiceConfigData;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

@Configuration
public class GatewayConfig {

    private final GatewayServiceConfigData gatewayServiceConfigData;

    private static final String HEADER_FOR_KEY_RESOLVER = "Authorization";

    // Constructor injection of configuration data
    public GatewayConfig(GatewayServiceConfigData configData) {
        this.gatewayServiceConfigData = configData;
    }

    // Bean definition for KeyResolver, used in rate limiting
    @Bean(name = "authHeaderResolver")
    KeyResolver userKeyResolver() {
        // Retrieves the value of the "Authorization" header from the request
        return exchange -> Mono.just(Objects.requireNonNull(exchange
                .getRequest().getHeaders().getFirst(HEADER_FOR_KEY_RESOLVER)));
    }

    // Customizer for ReactiveResilience4JCircuitBreakerFactory, used to provide resilience patterns like circuit breaking
    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> circuitBreakerFactoryCustomizer() {
        return reactiveResilience4JCircuitBreakerFactory ->
                reactiveResilience4JCircuitBreakerFactory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                        // Configuring TimeLimiter to set a timeout duration for the circuit breaker
                        .timeLimiterConfig(TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofMillis(gatewayServiceConfigData.getTimeoutMs()))
                                .build())
                        // Configuring CircuitBreaker to detect failures and tripping the circuit
                        .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                .failureRateThreshold(gatewayServiceConfigData.getFailureRateThreshold())
                                .slowCallRateThreshold(gatewayServiceConfigData.getSlowCallRateThreshold())
                                .slowCallDurationThreshold(Duration.ofMillis(gatewayServiceConfigData
                                        .getSlowCallDurationThreshold()))
                                .permittedNumberOfCallsInHalfOpenState(gatewayServiceConfigData
                                        .getPermittedNumOfCallsInHalfOpenState())
                                .slidingWindowSize(gatewayServiceConfigData.getSlidingWindowSize())
                                .minimumNumberOfCalls(gatewayServiceConfigData.getMinNumberOfCalls())
                                .waitDurationInOpenState(Duration.ofMillis(gatewayServiceConfigData
                                        .getWaitDurationInOpenState()))
                                .build())
                        .build());
    }
}

