package org.bloomreach.forge.discovery.transport;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * {@link DiscoveryTransport} decorator that fails fast once an upstream host is unhealthy,
 * instead of paying the full request timeout on every call.
 * <p>
 * One {@link CircuitBreaker} per upstream host (search/pathways/autosuggest are independent
 * services - one being down should not trip calls to the others). 4xx responses are excluded
 * from the failure-rate sample entirely: a client error says nothing about upstream health and
 * must not let a burst of bad queries trip the breaker for everyone.
 */
public final class CircuitBreakerDiscoveryTransport implements DiscoveryTransport {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerDiscoveryTransport.class);

    private final DiscoveryTransport delegate;
    private final CircuitBreakerRegistry registry;

    public CircuitBreakerDiscoveryTransport(DiscoveryTransport delegate, DiscoveryResilienceConfig config) {
        this.delegate = delegate;
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(config.failureRateThreshold())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(config.slidingWindowSize())
                .minimumNumberOfCalls(config.minimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofSeconds(config.waitDurationInOpenStateSeconds()))
                .ignoreException(CircuitBreakerDiscoveryTransport::isClientError)
                .build();
        this.registry = CircuitBreakerRegistry.of(cbConfig);
    }

    @Override
    public String execute(DiscoveryTransportRequest request) {
        String host = request.uri().getHost();
        CircuitBreaker breaker = registry.circuitBreaker(host);
        try {
            return breaker.executeSupplier(() -> delegate.execute(request));
        } catch (CallNotPermittedException e) {
            log.warn("Discovery API circuit breaker open for host '{}' - failing fast", host);
            throw new SearchException("Discovery API circuit breaker open for host '" + host + "'", e);
        }
    }

    private static boolean isClientError(Throwable t) {
        return t instanceof SearchException se && se.statusCode() >= 400 && se.statusCode() < 500;
    }
}
