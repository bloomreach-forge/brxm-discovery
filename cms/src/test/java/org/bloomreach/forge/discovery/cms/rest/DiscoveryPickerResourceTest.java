package org.bloomreach.forge.discovery.cms.rest;

import org.bloomreach.forge.discovery.cms.rest.dto.PickerCategoryDto;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerSearchResponseDto;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerWidgetDto;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.List;
import java.util.function.Function;

import static org.mockito.Mockito.mock;

import jakarta.ws.rs.BadRequestException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryPickerResourceTest {

    @Mock Session session;
    @Mock Node rootNode;
    @Mock Node hstRootNode;
    @Mock NodeIterator hstRootIterator;
    @Mock DiscoveryConfigProvider configProvider;
    @Mock Function<String, String> httpGateway;

    DiscoveryPickerResource resource;

    static final String BASE_URI = "https://core.dxpapi.com";

    static final String ONE_RESULT_JSON = """
            {"response":{"numFound":1,"docs":[
              {"pid":"p1","title":"Widget","thumb_image":"http://img","url":"http://url","price":9.99}
            ]}}
            """;
    static final String EMPTY_JSON = """
            {"response":{"numFound":0,"docs":[]}}
            """;

    DiscoveryConfig dummyConfig() {
        return new DiscoveryConfig(
                "acc1", "domain1", "key1", null,
                BASE_URI, "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION",
                12, "");
    }

    @BeforeEach
    void setUp() {
        resource = new DiscoveryPickerResource(session, configProvider, httpGateway);

        lenient().when(configProvider.get(session)).thenReturn(dummyConfig());
    }

    // ---- search() -------------------------------------------------------

    // ── Part 1B: query length guard ───────────────────────────────────────

    @Test
    void search_queryExceeds1000Chars_throwsBadRequest() {
        String tooLong = "a".repeat(1001);

        assertThrows(BadRequestException.class, () -> resource.search("", "", tooLong, 0, 12, ""));
        verifyNoInteractions(httpGateway);
    }

    @Test
    void search_queryOf1000Chars_acceptedNormally() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);
        String exactly1000 = "a".repeat(1000);

        assertDoesNotThrow(() -> resource.search("", "", exactly1000, 0, 12, ""));
    }

    @Test
    void search_delegatesToHttpGatewayAndParsesResult() {
        when(httpGateway.apply(anyString())).thenReturn(ONE_RESULT_JSON);

        PickerSearchResponseDto resp = resource.search("", "", "shirt", 0, 12, "");

        assertEquals(1, resp.items().size());
        assertEquals("p1", resp.items().get(0).id());
        assertEquals("Widget", resp.items().get(0).title());
        assertEquals("http://img", resp.items().get(0).imageUrl());
        assertEquals("9.99", resp.items().get(0).price());
        assertEquals(1L, resp.total());
    }

    @Test
    void search_urlContainsAccountIdAndQuery() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.search("", "", "shirt", 0, 12, "");

        verify(httpGateway).apply(argThat(url ->
                url.contains("account_id=acc1") && url.contains("q=shirt")));
    }

    @Test
    void search_emptyResults_returnsZeroItems() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        PickerSearchResponseDto resp = resource.search("", "", "*", 0, 12, "");

        assertEquals(0, resp.items().size());
        assertEquals(0L, resp.total());
    }

    @Test
    void search_capsPageSizeAtMaximum() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.search("", "", "*", 0, 99999, "");

        verify(httpGateway).apply(argThat(url -> url.contains("rows=100")));
    }

    @Test
    void search_encodesSpecialCharsInQuery() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.search("", "", "shirt&auth_key=injected", 0, 12, "");

        // The injected value must be percent-encoded, not treated as a separator
        verify(httpGateway).apply(argThat(url ->
                !url.contains("auth_key=injected") && url.contains("shirt")));
    }

    @Test
    void search_wrapsProviderFailureAsServerError() {
        when(configProvider.get(session)).thenThrow(new IllegalStateException("node not found"));

        assertThrows(jakarta.ws.rs.InternalServerErrorException.class,
                () -> resource.search("", "", "q", 0, 12, ""));
    }

    @Test
    void search_httpGatewayThrows_propagatesAsServerError() {
        when(httpGateway.apply(anyString()))
                .thenThrow(new jakarta.ws.rs.InternalServerErrorException("Discovery API returned HTTP 503"));

        assertThrows(jakarta.ws.rs.InternalServerErrorException.class,
                () -> resource.search("", "", "shirt", 0, 12, ""));
    }

    @Test
    void search_withCatId_forwardsCatIdFilterInUrl() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.search("", "", "*", 0, 12, "women");

        // filters map produces fq=cat_id:"women" (URL-encoded: fq=cat_id%3A%22women%22)
        verify(httpGateway).apply(argThat(url -> url.contains("cat_id") && url.contains("women")));
    }

    @Test
    void search_withBlankCatId_omitsCatIdFilter() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.search("", "", "*", 0, 12, "");

        verify(httpGateway).apply(argThat(url -> !url.contains("cat_id")));
    }

    // ---- items() --------------------------------------------------------

    @Test
    void items_returnsEmptyResponseWhenIdsBlank() {
        PickerSearchResponseDto resp = resource.items("", "", "");
        assertEquals(0, resp.items().size());
        assertEquals(0L, resp.total());
        verifyNoInteractions(configProvider);
    }

    @Test
    void items_returnsEmptyResponseWhenIdsNull() {
        PickerSearchResponseDto resp = resource.items("", "", null);
        assertEquals(0, resp.items().size());
        verifyNoInteractions(configProvider);
    }

    @Test
    void items_buildsSearchQueryWithPidFilter() {
        when(httpGateway.apply(anyString())).thenReturn(ONE_RESULT_JSON);

        PickerSearchResponseDto resp = resource.items("", "", "p1");

        assertEquals(1, resp.items().size());
        verify(httpGateway).apply(argThat(url -> url.contains("fq=pid")));
    }

    @Test
    void items_stripsWhitespaceFromCommaSeparatedIds() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.items("", "", "p1 , p2, p3");

        // All three pids should produce fq params
        verify(httpGateway).apply(argThat(url -> {
            int count = 0;
            int idx = 0;
            while ((idx = url.indexOf("fq=pid", idx)) != -1) {
                count++;
                idx++;
            }
            return count == 3;
        }));
    }

    // ---- categories() ---------------------------------------------------

    @Test
    void categories_returnsListFromCategoryMap() {
        String json = """
                {"category_map":{
                  "cat1":"Shirts",
                  "cat2":"Pants"
                }}
                """;
        when(httpGateway.apply(anyString())).thenReturn(json);

        List<PickerCategoryDto> result = resource.categories("", "");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(c -> "cat1".equals(c.id()) && "Shirts".equals(c.name())));
        assertTrue(result.stream().anyMatch(c -> "cat2".equals(c.id()) && "Pants".equals(c.name())));
    }

    @Test
    void categories_emptyMap_returnsEmpty() {
        when(httpGateway.apply(anyString())).thenReturn("{\"category_map\":{}}");

        List<PickerCategoryDto> result = resource.categories("", "");

        assertTrue(result.isEmpty());
    }

    @Test
    void categories_urlContainsSearchTypeCategoryAndRows0WithEmptyQ() {
        when(httpGateway.apply(anyString())).thenReturn("{\"category_map\":{}}");

        resource.categories("", "");

        verify(httpGateway).apply(argThat(url ->
                url.contains("search_type=category") && url.contains("rows=0")
                && url.contains("fl=") && url.contains("start=0")));
    }

    @Test
    void categories_fallsBackToKeyWhenNameAbsent() {
        String json = """
                {"category_map":{
                  "cat_no_name":""
                }}
                """;
        when(httpGateway.apply(anyString())).thenReturn(json);

        List<PickerCategoryDto> result = resource.categories("", "");

        assertEquals(1, result.size());
        assertEquals("cat_no_name", result.get(0).id());
        assertEquals("", result.get(0).name());
    }

    // ---- browse() -------------------------------------------------------

    @Test
    void browse_urlContainsSearchTypeCategoryAndCatId() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.browse("", "", "women", 0, 9);

        verify(httpGateway).apply(argThat(url ->
                url.contains("search_type=category") && url.contains("q=women")));
    }

    @Test
    void browse_usesRowsFromPageSize() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.browse("", "", "women", 0, 6);

        verify(httpGateway).apply(argThat(url -> url.contains("rows=6")));
    }

    @Test
    void browse_parsesProductsFromResponse() {
        when(httpGateway.apply(anyString())).thenReturn(ONE_RESULT_JSON);

        PickerSearchResponseDto resp = resource.browse("", "", "women", 0, 9);

        assertEquals(1, resp.items().size());
        assertEquals("p1", resp.items().get(0).id());
    }

    // ---- widgets() ------------------------------------------------------

    @Test
    void widgets_urlUsesAccountIdAndDomainKey() {
        when(httpGateway.apply(anyString())).thenReturn("{\"response\":{\"widgets\":[]}}");

        resource.listWidgets("", "");

        verify(httpGateway).apply(argThat(url ->
                url.contains("/api/v1/merchant/widgets")
                && url.contains("account_id=acc1")
                && url.contains("domain_key=domain1")
                && url.contains("auth_key=key1")));
    }

    @Test
    void widgets_returnsAvailableWidgets() {
        String json = """
                {"response":{"widgets":[
                  {"id":"w1","name":"Homepage Recs","type":"item","enabled":true,"description":"Homepage widget"}
                ]}}
                """;
        when(httpGateway.apply(anyString())).thenReturn(json);

        List<PickerWidgetDto> result = resource.listWidgets("", "");

        assertEquals(1, result.size());
        PickerWidgetDto w = result.get(0);
        assertEquals("w1", w.id());
        assertEquals("Homepage Recs", w.name());
        assertEquals("item", w.type());
        assertTrue(w.enabled());
        assertEquals("Homepage widget", w.description());
    }

    // ---- resolveChannelFromDocument path validation ----------------------

    @Test
    void search_withDocumentId_nonStandardPath_usesGlobalConfig() throws RepositoryException {
        // Path like /apps/config/mysite/doc has parts[3]="mysite" but is NOT under /content/documents/
        // Before fix: attempts channel lookup with the wrong index value
        // After fix: returns "" immediately and uses global config
        Node docNode = mock(Node.class);
        when(session.getNodeByIdentifier("doc-uuid")).thenReturn(docNode);
        when(docNode.getPath()).thenReturn("/apps/config/mysite/document");
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.search("", "doc-uuid", "*", 0, 12, "");

        verify(httpGateway).apply(argThat(url -> url.contains("account_id=acc1")));
    }

    @Test
    void search_withDocumentId_tooShortPath_usesGlobalConfig() throws RepositoryException {
        Node docNode = mock(Node.class);
        when(session.getNodeByIdentifier("doc-uuid")).thenReturn(docNode);
        when(docNode.getPath()).thenReturn("/content/doc"); // only 2 segments after root
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.search("", "doc-uuid", "*", 0, 12, "");

        verify(httpGateway).apply(argThat(url -> url.contains("account_id=acc1")));
    }

    // ---- categoryProducts() ---------------------------------------------

    @Test
    void categoryProducts_directCategoryId_skipJcrAndBrowses() throws RepositoryException {
        // categoryId passed directly (live pre-save value from browser) - no JCR variant read needed
        when(httpGateway.apply(anyString())).thenReturn(ONE_RESULT_JSON);

        List<?> items = resource.categoryProducts("", "women", 2, "");

        assertEquals(1, items.size());
        verify(httpGateway).apply(argThat(url ->
                url.contains("search_type=category") && url.contains("q=women") && url.contains("rows=2")));
        // getNodeByIdentifier must NOT be called (no JCR lookup for categoryId)
        verify(session, never()).getNodeByIdentifier(anyString());
    }

    @Test
    void categoryProducts_fallsBackToJcrWhenNoCategoryIdParam() throws RepositoryException {
        // no direct categoryId → read from JCR variant child
        Node handle = mock(Node.class);
        Node variant = mock(Node.class);
        NodeIterator variantIter = mock(NodeIterator.class);
        when(session.getNodeByIdentifier("doc-uuid")).thenReturn(handle);
        when(handle.getPath()).thenReturn("/apps/config/doc"); // non-standard → no channel override
        when(handle.getNodes()).thenReturn(variantIter);
        when(variantIter.hasNext()).thenReturn(true, false);
        when(variantIter.nextNode()).thenReturn(variant);
        Property catIdProp = mock(Property.class);
        when(variant.hasProperty("brxdis:categoryId")).thenReturn(true);
        when(variant.getProperty("brxdis:categoryId")).thenReturn(catIdProp);
        when(catIdProp.getString()).thenReturn("women");

        when(httpGateway.apply(anyString())).thenReturn(ONE_RESULT_JSON);

        List<?> items = resource.categoryProducts("doc-uuid", "", 2, "");

        assertEquals(1, items.size());
        verify(httpGateway).apply(argThat(url ->
                url.contains("search_type=category") && url.contains("q=women") && url.contains("rows=2")));
    }

    @Test
    void categoryProducts_missingCategoryId_returnsEmptyList() throws RepositoryException {
        // handle with a variant that has no brxdis:categoryId, no direct param
        Node handle = mock(Node.class);
        Node variant = mock(Node.class);
        NodeIterator variantIter = mock(NodeIterator.class);
        when(session.getNodeByIdentifier("doc-uuid")).thenReturn(handle);
        when(handle.getNodes()).thenReturn(variantIter);
        when(variantIter.hasNext()).thenReturn(true, false);
        when(variantIter.nextNode()).thenReturn(variant);
        when(variant.hasProperty("brxdis:categoryId")).thenReturn(false);

        List<?> items = resource.categoryProducts("doc-uuid", "", 2, "");

        assertTrue(items.isEmpty());
        verifyNoInteractions(httpGateway);
    }

    // ---- recommendationProducts() ---------------------------------------

    static final String REC_ONE_RESULT_JSON = """
            {"response":{"numFound":1,"docs":[
              {"pid":"p1","title":"Widget","thumb_image":"http://img","url":"http://url","price":9.99}
            ]}}
            """;
    static final String GLOBAL_CONFIG_JSON =
            "{\"widgetId\":\"w1\",\"widgetName\":\"Trending\",\"widgetType\":\"global\"," +
            "\"contextProductId\":null,\"contextCategoryId\":null}";
    static final String ITEM_WITH_PID_CONFIG_JSON =
            "{\"widgetId\":\"w2\",\"widgetName\":\"PDP Recs\",\"widgetType\":\"item\"," +
            "\"contextProductId\":\"pid123\",\"contextCategoryId\":null}";
    static final String ITEM_NO_PID_CONFIG_JSON =
            "{\"widgetId\":\"w3\",\"widgetName\":\"PDP Recs\",\"widgetType\":\"item\"," +
            "\"contextProductId\":null,\"contextCategoryId\":null}";
    static final String CATEGORY_WITH_CATID_CONFIG_JSON =
            "{\"widgetId\":\"w4\",\"widgetName\":\"Cat Recs\",\"widgetType\":\"category\"," +
            "\"contextProductId\":null,\"contextCategoryId\":\"cat1\"}";
    static final String CATEGORY_NO_CATID_CONFIG_JSON =
            "{\"widgetId\":\"w5\",\"widgetName\":\"Cat Recs\",\"widgetType\":\"category\"," +
            "\"contextProductId\":null,\"contextCategoryId\":null}";

    @Test
    void recommendationProducts_blankConfigAndDocumentId_returnsEmpty() {
        List<?> items = resource.recommendationProducts("", "", 4, "");
        assertTrue(items.isEmpty());
        verifyNoInteractions(httpGateway);
    }

    @Test
    void recommendationProducts_invalidJson_returnsEmpty() {
        List<?> items = resource.recommendationProducts("", "not-json", 4, "");
        assertTrue(items.isEmpty());
        verifyNoInteractions(httpGateway);
    }

    @Test
    void recommendationProducts_blankWidgetId_returnsEmpty() {
        String cfg = "{\"widgetId\":\"\",\"widgetType\":\"global\"}";
        List<?> items = resource.recommendationProducts("", cfg, 4, "");
        assertTrue(items.isEmpty());
        verifyNoInteractions(httpGateway);
    }

    @Test
    void recommendationProducts_itemTypeNoPid_returnsEmpty() {
        List<?> items = resource.recommendationProducts("", ITEM_NO_PID_CONFIG_JSON, 4, "");
        assertTrue(items.isEmpty());
        verifyNoInteractions(httpGateway);
    }

    @Test
    void recommendationProducts_categoryTypeNoCatId_returnsEmpty() {
        List<?> items = resource.recommendationProducts("", CATEGORY_NO_CATID_CONFIG_JSON, 4, "");
        assertTrue(items.isEmpty());
        verifyNoInteractions(httpGateway);
    }

    @Test
    void recommendationProducts_globalType_callsApiAndReturnsItems() {
        when(httpGateway.apply(anyString())).thenReturn(REC_ONE_RESULT_JSON);
        List<?> items = resource.recommendationProducts("", GLOBAL_CONFIG_JSON, 4, "");
        assertEquals(1, items.size());
        verify(httpGateway).apply(argThat(url -> url.contains("/api/v2/widgets/global/w1")));
    }

    @Test
    void recommendationProducts_itemTypeWithPid_includesItemIdsInUrl() {
        when(httpGateway.apply(anyString())).thenReturn(REC_ONE_RESULT_JSON);
        resource.recommendationProducts("", ITEM_WITH_PID_CONFIG_JSON, 4, "");
        verify(httpGateway).apply(argThat(url ->
                url.contains("/api/v2/widgets/item/w2") && url.contains("item_ids=pid123")));
    }

    @Test
    void recommendationProducts_categoryType_includesCatIdInUrl() {
        when(httpGateway.apply(anyString())).thenReturn(REC_ONE_RESULT_JSON);
        resource.recommendationProducts("", CATEGORY_WITH_CATID_CONFIG_JSON, 4, "");
        verify(httpGateway).apply(argThat(url ->
                url.contains("/api/v2/widgets/category/w4") && url.contains("cat_id=cat1")));
    }

    @Test
    void recommendationProducts_withoutAuthKey_usesCoreBaseUri() {
        when(httpGateway.apply(anyString())).thenReturn(REC_ONE_RESULT_JSON);
        resource.recommendationProducts("", GLOBAL_CONFIG_JSON, 4, "");
        verify(httpGateway).apply(argThat(url -> url.startsWith(BASE_URI)));
    }

    @Test
    void recommendationProducts_withAuthKey_usesPathwaysBaseUri() {
        DiscoveryConfig v2Config = new DiscoveryConfig(
                "acc1", "domain1", "key1", "authKey1",
                BASE_URI, "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION",
                12, "");
        when(configProvider.get(session)).thenReturn(v2Config);
        when(httpGateway.apply(anyString())).thenReturn(REC_ONE_RESULT_JSON);

        resource.recommendationProducts("", GLOBAL_CONFIG_JSON, 4, "");

        verify(httpGateway).apply(argThat(url -> url.startsWith("https://pathways.dxpapi.com")));
    }

    @Test
    void recommendationProducts_liveConfigJsonWinsOverJcr() throws RepositoryException {
        // configJson is non-blank → resolveConfigJsonFromDocument is not called (handle.getNodes not stubbed).
        // If the live config were ignored and JCR was consulted instead, getNodes() would throw NPE and fail.
        Node handle = mock(Node.class);
        when(session.getNodeByIdentifier("doc-uuid")).thenReturn(handle);
        when(handle.getPath()).thenReturn("/apps/config/doc");
        when(httpGateway.apply(anyString())).thenReturn(REC_ONE_RESULT_JSON);

        List<?> items = resource.recommendationProducts("doc-uuid", GLOBAL_CONFIG_JSON, 4, "");

        assertEquals(1, items.size()); // non-empty → live configJson was used, not JCR
    }

    @Test
    void recommendationProducts_usesJcrWhenNoLiveConfigJson() throws RepositoryException {
        Node handle = mock(Node.class);
        Node variant = mock(Node.class);
        NodeIterator variantIter = mock(NodeIterator.class);
        when(session.getNodeByIdentifier("doc-uuid")).thenReturn(handle);
        when(handle.getPath()).thenReturn("/apps/config/doc");
        when(handle.getNodes()).thenReturn(variantIter);
        when(variantIter.hasNext()).thenReturn(true, false);
        when(variantIter.nextNode()).thenReturn(variant);
        Property cfgProp = mock(Property.class);
        when(variant.hasProperty("brxdis:config")).thenReturn(true);
        when(variant.getProperty("brxdis:config")).thenReturn(cfgProp);
        when(cfgProp.getString()).thenReturn(GLOBAL_CONFIG_JSON);
        when(httpGateway.apply(anyString())).thenReturn(REC_ONE_RESULT_JSON);

        List<?> items = resource.recommendationProducts("doc-uuid", "", 4, "");

        assertEquals(1, items.size()); // config read from JCR → API called
    }

    // ---- productDetail() ------------------------------------------------

    @Test
    void productDetail_directProductId_skipJcrAndFetchesProduct() throws RepositoryException {
        when(httpGateway.apply(anyString())).thenReturn(ONE_RESULT_JSON);

        List<?> items = resource.productDetail("", "pid1", "");

        assertEquals(1, items.size());
        verify(httpGateway).apply(argThat(url -> url.contains("fq=pid") && url.contains("pid1")));
        verify(session, never()).getNodeByIdentifier(anyString());
    }

    @Test
    void productDetail_fallsBackToJcrWhenNoProductIdParam() throws RepositoryException {
        Node handle = mock(Node.class);
        Node variant = mock(Node.class);
        NodeIterator variantIter = mock(NodeIterator.class);
        when(session.getNodeByIdentifier("doc-uuid")).thenReturn(handle);
        when(handle.getPath()).thenReturn("/apps/config/doc");
        when(handle.getNodes()).thenReturn(variantIter);
        when(variantIter.hasNext()).thenReturn(true, false);
        when(variantIter.nextNode()).thenReturn(variant);
        Property pidProp = mock(Property.class);
        when(variant.hasProperty("brxdis:productId")).thenReturn(true);
        when(variant.getProperty("brxdis:productId")).thenReturn(pidProp);
        when(pidProp.getString()).thenReturn("pid1");
        when(httpGateway.apply(anyString())).thenReturn(ONE_RESULT_JSON);

        List<?> items = resource.productDetail("doc-uuid", "", "");

        assertEquals(1, items.size());
        verify(httpGateway).apply(argThat(url -> url.contains("fq=pid") && url.contains("pid1")));
    }

    @Test
    void productDetail_missingProductId_returnsEmptyList() throws RepositoryException {
        Node handle = mock(Node.class);
        Node variant = mock(Node.class);
        NodeIterator variantIter = mock(NodeIterator.class);
        when(session.getNodeByIdentifier("doc-uuid")).thenReturn(handle);
        when(handle.getNodes()).thenReturn(variantIter);
        when(variantIter.hasNext()).thenReturn(true, false);
        when(variantIter.nextNode()).thenReturn(variant);
        when(variant.hasProperty("brxdis:productId")).thenReturn(false);

        List<?> items = resource.productDetail("doc-uuid", "", "");

        assertTrue(items.isEmpty());
        verifyNoInteractions(httpGateway);
    }

    @Test
    void productDetail_liveProductIdWinsOverJcr() throws RepositoryException {
        // productId is non-blank → resolveProductIdFromDocument not called (handle.getNodes not stubbed)
        Node handle = mock(Node.class);
        when(session.getNodeByIdentifier("doc-uuid")).thenReturn(handle);
        when(handle.getPath()).thenReturn("/apps/config/doc");
        when(httpGateway.apply(anyString())).thenReturn(ONE_RESULT_JSON);

        List<?> items = resource.productDetail("doc-uuid", "live-pid", "");

        assertEquals(1, items.size());
    }

    @Test
    void productDetail_blankDocumentIdAndProductId_returnsEmpty() {
        List<?> items = resource.productDetail("", "", "");
        assertTrue(items.isEmpty());
        verifyNoInteractions(httpGateway);
    }

    // ---- JSON parsing ---------------------------------------------------

    @Test
    void search_nullPriceFieldParsedAsNull() {
        String json = """
                {"response":{"numFound":1,"docs":[
                  {"pid":"p2","title":"Free Item","thumb_image":null,"url":"http://u","price":null}
                ]}}
                """;
        when(httpGateway.apply(anyString())).thenReturn(json);

        PickerSearchResponseDto resp = resource.search("", "", "*", 0, 12, "");

        assertNull(resp.items().get(0).price());
    }

    @Test
    void search_missingResponseNode_returnsEmptyList() {
        when(httpGateway.apply(anyString())).thenReturn("{}");

        PickerSearchResponseDto resp = resource.search("", "", "*", 0, 12, "");

        assertEquals(0, resp.items().size());
        assertEquals(0L, resp.total());
    }

    // ---- session refresh -------------------------------------------------

    @Test
    void search_refreshesSessionBeforeResolvingConfig() throws RepositoryException {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.search("", "", "*", 0, 12, "");

        verify(session).refresh(false);
    }

    // ---- channelId overrides -----------------------------------------------

    @Test
    void search_withBlankChannelId_usesGlobalConfig() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.search("", "", "*", 0, 12, "");

        verify(httpGateway).apply(argThat(url -> url.contains("account_id=acc1")));
    }

    @Test
    void search_withDocumentId_appliesChannelCredentials(@Mock Node channelInfoNode,
                                                         @Mock Node docHandleNode,
                                                         @Mock Property accountProp,
                                                         @Mock Property domainProp,
                                                         @Mock Property apiKeyEnvVarProp) throws RepositoryException {
        // Create resource with seam envResolver so CHAN_API_KEY resolves to a known value
        DiscoveryPickerResource resourceWithSeam = new DiscoveryPickerResource(
                session, configProvider, httpGateway,
                new DiscoveryPickerResponseMapper(),
                name -> "CHAN_API_KEY".equals(name) ? "chan-api-key" : null);

        // documentId resolves to channelId via document path: /content/documents/{siteName}/...
        when(session.getNodeByIdentifier("doc-uuid")).thenReturn(docHandleNode);
        when(docHandleNode.getPath()).thenReturn("/content/documents/myChannel/folder/doc");

        String channelId = "myChannel";
        String path = "/hst:myproject/hst:configurations/" + channelId + "/hst:workspace/hst:channel/hst:channelinfo";
        when(session.getRootNode()).thenReturn(rootNode);
        when(rootNode.getNodes("hst:*")).thenReturn(hstRootIterator);
        when(hstRootIterator.hasNext()).thenReturn(true, false);
        when(hstRootIterator.nextNode()).thenReturn(hstRootNode);
        when(hstRootNode.getPath()).thenReturn("/hst:myproject");
        when(session.nodeExists(path)).thenReturn(true);
        when(session.getNode(path)).thenReturn(channelInfoNode);

        when(channelInfoNode.hasProperty("discoveryAccountId")).thenReturn(true);
        when(channelInfoNode.getProperty("discoveryAccountId")).thenReturn(accountProp);
        when(accountProp.getString()).thenReturn("chan-acct");

        when(channelInfoNode.hasProperty("discoveryDomainKey")).thenReturn(true);
        when(channelInfoNode.getProperty("discoveryDomainKey")).thenReturn(domainProp);
        when(domainProp.getString()).thenReturn("chan-domain");

        when(channelInfoNode.hasProperty("discoveryApiKeyEnvVar")).thenReturn(true);
        when(channelInfoNode.getProperty("discoveryApiKeyEnvVar")).thenReturn(apiKeyEnvVarProp);
        when(apiKeyEnvVarProp.getString()).thenReturn("CHAN_API_KEY");

        when(channelInfoNode.hasProperty("discoveryAuthKeyEnvVar")).thenReturn(false);

        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        // channelId="" but documentId="doc-uuid" → server derives channelId from document path
        resourceWithSeam.search("", "doc-uuid", "*", 0, 12, "");

        // channel credentials replace global → URL contains channel account_id and domain_key
        verify(httpGateway).apply(argThat(url ->
                url.contains("account_id=chan-acct") && url.contains("domain_key=chan-domain")));
    }
}
