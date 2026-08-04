package org.bloomreach.forge.discovery.transport;

import org.bloomreach.forge.discovery.config.ConfigDefaults;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscoveryResilienceConfigTest {

    @AfterEach
    void clearSysProps() {
        System.clearProperty(ConfigDefaults.CB_FAILURE_RATE_THRESHOLD_SYS);
        System.clearProperty(ConfigDefaults.CB_SLIDING_WINDOW_SIZE_SYS);
        System.clearProperty(ConfigDefaults.CB_MINIMUM_NUMBER_OF_CALLS_SYS);
        System.clearProperty(ConfigDefaults.CB_WAIT_DURATION_IN_OPEN_STATE_SECONDS_SYS);
    }

    private static Function<String, String> noEnv() {
        return ignored -> null;
    }

    @Test
    void noEnvNoSys_usesCodedDefaults() {
        DiscoveryResilienceConfig config = DiscoveryResilienceConfig.fromEnvironment(noEnv());

        assertEquals(ConfigDefaults.CB_FAILURE_RATE_THRESHOLD_DEFAULT, config.failureRateThreshold());
        assertEquals(ConfigDefaults.CB_SLIDING_WINDOW_SIZE_DEFAULT, config.slidingWindowSize());
        assertEquals(ConfigDefaults.CB_MINIMUM_NUMBER_OF_CALLS_DEFAULT, config.minimumNumberOfCalls());
        assertEquals(ConfigDefaults.CB_WAIT_DURATION_IN_OPEN_STATE_SECONDS_DEFAULT, config.waitDurationInOpenStateSeconds());
    }

    @Test
    void sysPropOverridesDefault_whenEnvAbsent() {
        System.setProperty(ConfigDefaults.CB_FAILURE_RATE_THRESHOLD_SYS, "75");

        DiscoveryResilienceConfig config = DiscoveryResilienceConfig.fromEnvironment(noEnv());

        assertEquals(75, config.failureRateThreshold());
    }

    @Test
    void envOverridesSysAndDefault() {
        System.setProperty(ConfigDefaults.CB_SLIDING_WINDOW_SIZE_SYS, "99");
        Function<String, String> env = Map.of(ConfigDefaults.CB_SLIDING_WINDOW_SIZE_ENV, "42")::get;

        DiscoveryResilienceConfig config = DiscoveryResilienceConfig.fromEnvironment(env);

        assertEquals(42, config.slidingWindowSize());
    }

    @Test
    void invalidEnvValue_fallsThroughToSys() {
        System.setProperty(ConfigDefaults.CB_MINIMUM_NUMBER_OF_CALLS_SYS, "5");
        Function<String, String> env = Map.of(ConfigDefaults.CB_MINIMUM_NUMBER_OF_CALLS_ENV, "not-a-number")::get;

        DiscoveryResilienceConfig config = DiscoveryResilienceConfig.fromEnvironment(env);

        assertEquals(5, config.minimumNumberOfCalls());
    }

    @Test
    void invalidEnvAndSys_fallsThroughToDefault() {
        System.setProperty(ConfigDefaults.CB_WAIT_DURATION_IN_OPEN_STATE_SECONDS_SYS, "also-not-a-number");
        Function<String, String> env = Map.of(ConfigDefaults.CB_WAIT_DURATION_IN_OPEN_STATE_SECONDS_ENV, "nope")::get;

        DiscoveryResilienceConfig config = DiscoveryResilienceConfig.fromEnvironment(env);

        assertEquals(ConfigDefaults.CB_WAIT_DURATION_IN_OPEN_STATE_SECONDS_DEFAULT, config.waitDurationInOpenStateSeconds());
    }
}
