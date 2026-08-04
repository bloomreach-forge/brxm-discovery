package org.bloomreach.forge.discovery.transport;

import org.bloomreach.forge.discovery.config.ConfigDefaults;

import java.util.function.Function;

/**
 * Circuit breaker tuning for {@link CircuitBreakerDiscoveryTransport}.
 * Ops/infra-level knobs, resolved env var -&gt; sys prop -&gt; coded default (no JCR/content authoring),
 * matching how connect timeouts and executor pool sizes are already configured in this codebase.
 */
public record DiscoveryResilienceConfig(
        int failureRateThreshold,
        int slidingWindowSize,
        int minimumNumberOfCalls,
        int waitDurationInOpenStateSeconds
) {

    public static DiscoveryResilienceConfig fromEnvironment() {
        return fromEnvironment(System::getenv);
    }

    /** Package-private for testing - pass a fake env lookup to avoid mutating real environment variables. */
    static DiscoveryResilienceConfig fromEnvironment(Function<String, String> envLookup) {
        return new DiscoveryResilienceConfig(
                resolveInt(envLookup, ConfigDefaults.CB_FAILURE_RATE_THRESHOLD_ENV,
                        ConfigDefaults.CB_FAILURE_RATE_THRESHOLD_SYS, ConfigDefaults.CB_FAILURE_RATE_THRESHOLD_DEFAULT),
                resolveInt(envLookup, ConfigDefaults.CB_SLIDING_WINDOW_SIZE_ENV,
                        ConfigDefaults.CB_SLIDING_WINDOW_SIZE_SYS, ConfigDefaults.CB_SLIDING_WINDOW_SIZE_DEFAULT),
                resolveInt(envLookup, ConfigDefaults.CB_MINIMUM_NUMBER_OF_CALLS_ENV,
                        ConfigDefaults.CB_MINIMUM_NUMBER_OF_CALLS_SYS, ConfigDefaults.CB_MINIMUM_NUMBER_OF_CALLS_DEFAULT),
                resolveInt(envLookup, ConfigDefaults.CB_WAIT_DURATION_IN_OPEN_STATE_SECONDS_ENV,
                        ConfigDefaults.CB_WAIT_DURATION_IN_OPEN_STATE_SECONDS_SYS,
                        ConfigDefaults.CB_WAIT_DURATION_IN_OPEN_STATE_SECONDS_DEFAULT)
        );
    }

    private static int resolveInt(Function<String, String> envLookup, String envVar, String sysProp, int codedDefault) {
        Integer fromEnv = parseOrNull(envLookup.apply(envVar));
        if (fromEnv != null) return fromEnv;
        Integer fromSys = parseOrNull(System.getProperty(sysProp));
        if (fromSys != null) return fromSys;
        return codedDefault;
    }

    private static Integer parseOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
