package org.bloomreach.forge.discovery.site.service.discovery.autosuggest;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.exception.DiscoveryException;
import org.bloomreach.forge.discovery.request.DiscoveryRequestFactory;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryRequestHeaders;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryRequestLogging;
import org.bloomreach.forge.discovery.transport.DiscoveryTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class AutosuggestApiClientImpl implements AutosuggestApiClient {

    private static final Logger log = LoggerFactory.getLogger(AutosuggestApiClientImpl.class);

    private final DiscoveryTransport transport;
    private final DiscoveryConfigProvider configProvider;
    private final AutosuggestResponseMapper responseMapper;
    private final DiscoveryRequestFactory requestFactory;

    public AutosuggestApiClientImpl(DiscoveryTransport transport, DiscoveryConfigProvider configProvider,
                                    AutosuggestResponseMapper responseMapper, DiscoveryRequestFactory requestFactory) {
        this.transport = transport;
        this.configProvider = configProvider;
        this.responseMapper = responseMapper;
        this.requestFactory = requestFactory;
    }

    @Override
    public AutosuggestResult autosuggest(AutosuggestQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = requestFactory.autosuggest(query, credentials).toRelativePath();
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery autosuggest [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            URI uri = DiscoveryRequestHeaders.buildUri(configProvider.settings().autosuggestBaseUri(), path);
            String json = transport.execute(DiscoveryRequestHeaders.forSearch(uri, ctx));
            AutosuggestResult result = responseMapper.toAutosuggestResult(json);
            log.debug("Discovery autosuggest returned {} query suggestions [request_id={}]",
                    result.querySuggestions().size(), requestLog.requestId());
            return result;
        } catch (DiscoveryException e) {
            log.error("Discovery autosuggest failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw e;
        }
    }
}
