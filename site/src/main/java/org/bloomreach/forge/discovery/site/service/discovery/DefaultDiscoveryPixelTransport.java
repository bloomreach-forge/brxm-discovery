package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;

import org.bloomreach.forge.discovery.exception.DiscoveryException;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.PixelEvent;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.TrackingContext;
import org.bloomreach.forge.discovery.transport.DiscoveryTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


final class DefaultDiscoveryPixelTransport implements DiscoveryPixelTransport {

    private static final Logger log = LoggerFactory.getLogger(DefaultDiscoveryPixelTransport.class);

    private static final String PIXEL_PATH = "/pix.gif";
    private static final int PIXEL_MAX_SKUS = 20;

    private final DiscoveryTransport transport;
    private final DiscoveryConfigProvider configProvider;

    DefaultDiscoveryPixelTransport(DiscoveryTransport transport, DiscoveryConfigProvider configProvider) {
        this.transport = transport;
        this.configProvider = configProvider;
    }

    @Override
    public String buildPath(PixelEvent event, DiscoveryCredentials credentials, String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendCommonParams(builder, credentials);
        builder.queryParam("type", event.type());
        if (event.group() != null) builder.queryParam("group", event.group());
        if (event.etype() != null) builder.queryParam("etype", event.etype());
        builder.queryParam("ptype", event.ptype());
        event.typeParams().forEach(builder::queryParam);
        appendTracking(builder, event.tracking(), clientIp, event.keepSuggestQuery());
        appendSkus(builder, event.products());
        appendFlags(builder, flags);
        return builder.build(false).toUriString();
    }

    @Override
    public void fire(String path, ClientContext ctx, PixelFlags flags) {
        log.debug("Discovery pixel event: {}", redactedPath(path));
        try {
            URI uri = DiscoveryRequestHeaders.buildUri(pixelBaseUri(flags), path);
            transport.execute(DiscoveryRequestHeaders.forPixel(uri, ctx));
        } catch (DiscoveryException e) {
            log.warn("Discovery pixel event failed - path={}: {}", redactedPath(path), e.getMessage());
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────────

    private static void appendCommonParams(UriComponentsBuilder builder, DiscoveryCredentials credentials) {
        builder.queryParam("acct_id", credentials.accountId())
                .queryParam("domain_key", credentials.domainKey());
    }

    private static void appendTracking(UriComponentsBuilder builder, TrackingContext ctx,
                                       String clientIp, boolean keepSuggestQuery) {
        if (notBlank(ctx.title())) builder.queryParam("title", ctx.title());
        if (notBlank(ctx.brUid2())) {
            builder.queryParam("cookie2", URLDecoder.decode(ctx.brUid2(), StandardCharsets.UTF_8));
        } else {
            log.warn("Discovery pixel firing without cookie2 (br_uid_2 absent) - events will be anonymous");
        }
        if (notBlank(ctx.refUrl())) builder.queryParam("ref", ctx.refUrl());
        if (notBlank(ctx.origRefUrl())) builder.queryParam("orig_ref_url", ctx.origRefUrl());
        if (notBlank(ctx.url())) builder.queryParam("url", normalizeUrl(ctx.url(), keepSuggestQuery));
        builder.queryParam("version", "ss-v0.1")
                .queryParam("rand", UUID.randomUUID())
                .queryParam("client_ts", System.currentTimeMillis() * 1000L);
        if (notBlank(clientIp)) builder.queryParam("client_ip", clientIp);
    }

    private static void appendSkus(UriComponentsBuilder builder, List<ProductSummary> products) {
        if (products == null || products.isEmpty()) return;
        String skus = products.stream()
                .limit(PIXEL_MAX_SKUS)
                .map(ProductSummary::id)
                .filter(DefaultDiscoveryPixelTransport::notBlank)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
        if (skus != null) builder.queryParam("sku", skus);
    }

    private static void appendFlags(UriComponentsBuilder builder, PixelFlags flags) {
        if (flags.testData()) builder.queryParam("test_data", "true");
        if (flags.debug()) builder.queryParam("debug", "true");
    }

    private String pixelBaseUri(PixelFlags flags) {
        return "EU".equals(flags.region()) ? configProvider.settings().pixelBaseUriEU() : configProvider.settings().pixelBaseUri();
    }

    private static String redactedPath(String path) {
        return UriComponentsBuilder.fromUriString(path)
                .replaceQueryParam("cookie2",      "[redacted]")
                .replaceQueryParam("ref",          "[redacted]")
                .replaceQueryParam("orig_ref_url", "[redacted]")
                .replaceQueryParam("url",          "[redacted]")
                .replaceQueryParam("client_ip",    "[redacted]")
                .build(false).toUriString();
    }

    private static String normalizeUrl(String url, boolean keepSuggestQuery) {
        int queryStart = url.indexOf('?');
        if (queryStart < 0) return url;
        String base = url.substring(0, queryStart);
        if (!keepSuggestQuery) return base;
        return Arrays.stream(url.substring(queryStart + 1).split("&"))
                .filter(part -> part.startsWith("_br_psugg_q="))
                .findFirst()
                .map(part -> base + "?" + part)
                .orElse(base);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
