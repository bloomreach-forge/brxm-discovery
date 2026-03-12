package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

final class DefaultDiscoveryPixelTransport implements DiscoveryPixelTransport {

    private static final Logger log = LoggerFactory.getLogger(DefaultDiscoveryPixelTransport.class);

    private static final String PIXEL_PATH = "/pix.gif";
    private static final int PIXEL_MAX_SKUS = 20;

    private static final String PIXEL_RESOURCE_SPACE = "discoveryPixelAPI";
    private static final String PIXEL_RESOURCE_SPACE_EU = "discoveryPixelAPIEU";

    private static final String REQUEST_TYPE_SEARCH = "search";
    private static final String REQUEST_TYPE_CATEGORY = "category";
    private static final String PAGE_TYPE_PAGEVIEW = "pageview";
    private static final String PAGE_TYPE_EVENT = "event";
    private static final String PAGE_TYPE_WIDGET = "widget";
    private static final String PAGE_TYPE_VIEW = "view";

    private final DiscoveryResourceExecutor executor;

    DefaultDiscoveryPixelTransport(DiscoveryResourceExecutor executor) {
        this.executor = executor;
    }

    @Override
    public String buildSearchPixelPath(SearchQuery query, SearchResult result, DiscoveryCredentials credentials,
                                       String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendPixelCommonParams(builder, credentials);
        builder.queryParam("type", PAGE_TYPE_PAGEVIEW)
                .queryParam("ptype", REQUEST_TYPE_SEARCH);
        if (query.query() != null && !query.query().isBlank()) {
            builder.queryParam("search_term", query.query());
        }
        appendPixelTracking(builder, query.brUid2(), query.refUrl(), query.url(), clientIp);
        appendPixelSkus(builder, result.products());
        appendPixelFlags(builder, flags);
        return builder.build(false).toUriString();
    }

    @Override
    public String buildCategoryPixelPath(CategoryQuery query, SearchResult result, DiscoveryCredentials credentials,
                                         String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendPixelCommonParams(builder, credentials);
        builder.queryParam("type", PAGE_TYPE_PAGEVIEW)
                .queryParam("ptype", REQUEST_TYPE_CATEGORY);
        if (query.categoryId() != null && !query.categoryId().isBlank()) {
            builder.queryParam("cat_id", query.categoryId());
        }
        appendPixelTracking(builder, query.brUid2(), query.refUrl(), query.url(), clientIp);
        appendPixelSkus(builder, result.products());
        appendPixelFlags(builder, flags);
        return builder.build(false).toUriString();
    }

    @Override
    public String buildWidgetPixelPath(RecQuery query, RecommendationResult result, DiscoveryCredentials credentials,
                                       String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendPixelCommonParams(builder, credentials);
        builder.queryParam("type", PAGE_TYPE_EVENT)
                .queryParam("group", PAGE_TYPE_WIDGET)
                .queryParam("etype", PAGE_TYPE_VIEW);
        if (query.widgetId() != null && !query.widgetId().isBlank()) {
            builder.queryParam("wid", query.widgetId());
        }
        if (query.widgetType() != null && !query.widgetType().isBlank()) {
            builder.queryParam("wty", query.widgetType());
        }
        if (result.widgetResultId() != null && !result.widgetResultId().isBlank()) {
            builder.queryParam("wrid", result.widgetResultId());
        }
        if (query.contextProductId() != null && !query.contextProductId().isBlank()) {
            builder.queryParam("wq", query.contextProductId());
        }
        appendPixelTracking(builder, query.brUid2(), query.refUrl(), query.url(), clientIp);
        appendPixelSkus(builder, result.products());
        appendPixelFlags(builder, flags);
        return builder.build(false).toUriString();
    }

    @Override
    public String buildProductPageViewPixelPath(String pid, String prodName, String brUid2, String refUrl, String url,
                                                DiscoveryCredentials credentials, String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendPixelCommonParams(builder, credentials);
        builder.queryParam("type", PAGE_TYPE_PAGEVIEW)
                .queryParam("ptype", "product")
                .queryParam("prod_id", pid);
        if (prodName != null && !prodName.isBlank()) {
            builder.queryParam("prod_name", prodName);
        }
        appendPixelTracking(builder, brUid2, refUrl, url, clientIp);
        appendPixelFlags(builder, flags);
        return builder.build(false).toUriString();
    }

    @Override
    public void firePixelEvent(String pixelPath, ClientContext ctx, PixelFlags flags) {
        log.debug("Discovery pixel event: {}", pixelPath);
        try {
            executor.resolve(pixelResourceSpace(flags), pixelPath, ctx);
        } catch (ResourceException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("JSON processing error")) {
                log.debug("Discovery pixel event fired (non-JSON response ignored) — path={}", pixelPath);
            } else {
                log.warn("Discovery pixel event failed — path={}: {}", pixelPath, e.getMessage());
            }
        }
    }

    private static void appendPixelFlags(UriComponentsBuilder builder, PixelFlags flags) {
        if (flags.testData()) {
            builder.queryParam("test_data", "true");
        }
        if (flags.debug()) {
            builder.queryParam("debug", "true");
        }
    }

    private static String pixelResourceSpace(PixelFlags flags) {
        return "EU".equals(flags.region()) ? PIXEL_RESOURCE_SPACE_EU : PIXEL_RESOURCE_SPACE;
    }

    private static void appendPixelCommonParams(UriComponentsBuilder builder, DiscoveryCredentials credentials) {
        builder.queryParam("acct_id", credentials.accountId())
                .queryParam("domain_key", credentials.domainKey());
    }

    private static void appendPixelSkus(UriComponentsBuilder builder, List<ProductSummary> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        String skus = products.stream()
                .limit(PIXEL_MAX_SKUS)
                .map(ProductSummary::id)
                .filter(id -> id != null && !id.isBlank())
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
        if (skus != null) {
            builder.queryParam("sku", skus);
        }
    }

    private static void appendPixelTracking(UriComponentsBuilder builder, String brUid2, String refUrl,
                                            String url, String clientIp) {
        if (brUid2 != null && !brUid2.isBlank()) {
            String decodedBrUid2 = URLDecoder.decode(brUid2, StandardCharsets.UTF_8);
            builder.queryParam("cookie2", decodedBrUid2);
        }
        if (refUrl != null && !refUrl.isBlank()) {
            builder.queryParam("ref", refUrl);
        }
        if (url != null && !url.isBlank()) {
            int queryStart = url.indexOf('?');
            builder.queryParam("url", queryStart >= 0 ? url.substring(0, queryStart) : url);
        }
        builder.queryParam("version", "ss-v0.1")
                .queryParam("rand", UUID.randomUUID())
                .queryParam("client_ts", System.currentTimeMillis());
        if (clientIp != null && !clientIp.isBlank()) {
            builder.queryParam("client_ip", clientIp);
        }
    }
}
