package org.bloomreach.forge.discovery.cms.rest;

import org.bloomreach.forge.discovery.cms.rest.dto.PickerCategoryDto;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerSearchResponseDto;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerWidgetDto;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigReader;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.BadRequestException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryPickerResourceTest {

    @Mock Session session;
    @Mock Node configNode;
    @Mock DiscoveryConfigReader configReader;
    @Mock Function<String, String> httpGateway;

    DiscoveryPickerResource resource;

    static final String CONFIG_PATH = "/content/documents/discovery-config";
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
                BASE_URI, "https://pathways.dxpapi.com", "PRODUCTION",
                12, "");
    }

    @BeforeEach
    void setUp() throws RepositoryException {
        resource = new DiscoveryPickerResource(session, configReader, httpGateway);
        // lenient: some tests short-circuit before touching JCR
        lenient().when(session.getNode(CONFIG_PATH)).thenReturn(configNode);
        lenient().when(configReader.read(configNode)).thenReturn(dummyConfig());
    }

    // ---- search() -------------------------------------------------------

    @Test
    void search_delegatesToHttpGatewayAndParsesResult() {
        when(httpGateway.apply(anyString())).thenReturn(ONE_RESULT_JSON);

        PickerSearchResponseDto resp = resource.search(CONFIG_PATH, "shirt", 0, 12);

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

        resource.search(CONFIG_PATH, "shirt", 0, 12);

        verify(httpGateway).apply(argThat(url ->
                url.contains("account_id=acc1") && url.contains("q=shirt")));
    }

    @Test
    void search_emptyResults_returnsZeroItems() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        PickerSearchResponseDto resp = resource.search(CONFIG_PATH, "*", 0, 12);

        assertEquals(0, resp.items().size());
        assertEquals(0L, resp.total());
    }

    @Test
    void search_throwsBadRequestWhenConfigPathBlank() {
        assertThrows(BadRequestException.class, () -> resource.search("", "shirt", 0, 12));
        assertThrows(BadRequestException.class, () -> resource.search(null, "shirt", 0, 12));
        verifyNoInteractions(session);
    }

    @Test
    void search_throwsBadRequestWhenConfigPathInvalid() {
        // path traversal attempt
        assertThrows(BadRequestException.class,
                () -> resource.search("../../etc/passwd", "q", 0, 12));
        // relative path (no leading slash)
        assertThrows(BadRequestException.class,
                () -> resource.search("content/documents/config", "q", 0, 12));
        verifyNoInteractions(session);
    }

    @Test
    void search_capsPageSizeAtMaximum() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.search(CONFIG_PATH, "*", 0, 99999);

        verify(httpGateway).apply(argThat(url -> url.contains("rows=100")));
    }

    @Test
    void search_encodesSpecialCharsInQuery() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.search(CONFIG_PATH, "shirt&auth_key=injected", 0, 12);

        // The injected value must be percent-encoded, not treated as a separator
        verify(httpGateway).apply(argThat(url ->
                !url.contains("auth_key=injected") && url.contains("shirt")));
    }

    @Test
    void search_wrapsRepositoryExceptionAsServerError() throws RepositoryException {
        when(session.getNode(CONFIG_PATH)).thenThrow(new RepositoryException("node not found"));

        assertThrows(jakarta.ws.rs.InternalServerErrorException.class,
                () -> resource.search(CONFIG_PATH, "q", 0, 12));
    }

    @Test
    void search_httpGatewayThrows_propagatesAsServerError() {
        when(httpGateway.apply(anyString()))
                .thenThrow(new jakarta.ws.rs.InternalServerErrorException("Discovery API returned HTTP 503"));

        assertThrows(jakarta.ws.rs.InternalServerErrorException.class,
                () -> resource.search(CONFIG_PATH, "shirt", 0, 12));
    }

    // ---- items() --------------------------------------------------------

    @Test
    void items_returnsEmptyResponseWhenIdsBlank() {
        PickerSearchResponseDto resp = resource.items(CONFIG_PATH, "");
        assertEquals(0, resp.items().size());
        assertEquals(0L, resp.total());
        verifyNoInteractions(session);
    }

    @Test
    void items_returnsEmptyResponseWhenIdsNull() {
        PickerSearchResponseDto resp = resource.items(CONFIG_PATH, null);
        assertEquals(0, resp.items().size());
        verifyNoInteractions(session);
    }

    @Test
    void items_buildsSearchQueryWithPidFilter() {
        when(httpGateway.apply(anyString())).thenReturn(ONE_RESULT_JSON);

        PickerSearchResponseDto resp = resource.items(CONFIG_PATH, "p1");

        assertEquals(1, resp.items().size());
        verify(httpGateway).apply(argThat(url -> url.contains("fq=pid")));
    }

    @Test
    void items_stripsWhitespaceFromCommaSeparatedIds() {
        when(httpGateway.apply(anyString())).thenReturn(EMPTY_JSON);

        resource.items(CONFIG_PATH, "p1 , p2, p3");

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

        List<PickerCategoryDto> result = resource.categories(CONFIG_PATH);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(c -> "cat1".equals(c.id()) && "Shirts".equals(c.name())));
        assertTrue(result.stream().anyMatch(c -> "cat2".equals(c.id()) && "Pants".equals(c.name())));
    }

    @Test
    void categories_emptyMap_returnsEmpty() {
        when(httpGateway.apply(anyString())).thenReturn("{\"category_map\":{}}");

        List<PickerCategoryDto> result = resource.categories(CONFIG_PATH);

        assertTrue(result.isEmpty());
    }

    @Test
    void categories_urlContainsSearchTypeCategoryAndRows0WithEmptyQ() {
        when(httpGateway.apply(anyString())).thenReturn("{\"category_map\":{}}");

        resource.categories(CONFIG_PATH);

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

        List<PickerCategoryDto> result = resource.categories(CONFIG_PATH);

        assertEquals(1, result.size());
        assertEquals("cat_no_name", result.get(0).id());
        assertEquals("", result.get(0).name());
    }

    // ---- widgets() ------------------------------------------------------

    @Test
    void widgets_urlUsesAccountIdAndDomainKey() {
        when(httpGateway.apply(anyString())).thenReturn("{\"response\":{\"widgets\":[]}}");

        resource.listWidgets(CONFIG_PATH);

        verify(httpGateway).apply(argThat(url ->
                url.contains("/api/v1/merchant/widgets")
                && url.contains("account_id=acc1")
                && url.contains("domain_key=domain1")));
    }

    @Test
    void widgets_returnsAvailableWidgets() {
        String json = """
                {"response":{"widgets":[
                  {"id":"w1","name":"Homepage Recs","type":"item","enabled":true,"description":"Homepage widget"}
                ]}}
                """;
        when(httpGateway.apply(anyString())).thenReturn(json);

        List<PickerWidgetDto> result = resource.listWidgets(CONFIG_PATH);

        assertEquals(1, result.size());
        PickerWidgetDto w = result.get(0);
        assertEquals("w1", w.id());
        assertEquals("Homepage Recs", w.name());
        assertEquals("item", w.type());
        assertTrue(w.enabled());
        assertEquals("Homepage widget", w.description());
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

        PickerSearchResponseDto resp = resource.search(CONFIG_PATH, "*", 0, 12);

        assertNull(resp.items().get(0).price());
    }

    @Test
    void search_missingResponseNode_returnsEmptyList() {
        when(httpGateway.apply(anyString())).thenReturn("{}");

        PickerSearchResponseDto resp = resource.search(CONFIG_PATH, "*", 0, 12);

        assertEquals(0, resp.items().size());
        assertEquals(0L, resp.total());
    }
}
