package org.bloomreach.forge.discovery.transport;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;

/** Creates the singleton {@link HttpClient} used by all Discovery API transports. */
public final class DiscoveryHttpClientFactory {

    private DiscoveryHttpClientFactory() {
    }

    /** Creates a new {@link HttpClient} configured for Discovery API calls. */
    public static HttpClient create() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
}
