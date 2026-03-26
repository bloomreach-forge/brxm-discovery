package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.platform.SearchRequestOptions;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentsException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.HstServices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link AbstractDiscoveryComponent} utility methods.
 */
@ExtendWith(MockitoExtension.class)
class AbstractDiscoveryComponentTest {

    @Mock HstRequest request;
    @Mock HstRequestContext requestContext;
    @Mock HstResponse response;
    @Mock HstDiscoveryService discoveryService;
    @Mock HstContainerURL baseUrl;
    @Mock ResolvedSiteMapItem resolvedSiteMapItem;
    @Mock HstComponentConfiguration pageConfig;
    @Mock HstComponentConfiguration childConfig;

    @AfterEach
    void tearDown() {
        HstServices.setComponentManager(null);
    }

    // ── parseIntOrDefault ─────────────────────────────────────────────────

    @Test
    void parseIntOrDefault_validNumber_returnsIt() {
        assertEquals(42, AbstractDiscoveryComponent.parseIntOrDefault("42", 10));
    }

    @Test
    void parseIntOrDefault_null_returnsDefault() {
        assertEquals(10, AbstractDiscoveryComponent.parseIntOrDefault(null, 10));
    }

    @Test
    void parseIntOrDefault_blank_returnsDefault() {
        assertEquals(10, AbstractDiscoveryComponent.parseIntOrDefault("  ", 10));
    }

    @Test
    void parseIntOrDefault_invalid_returnsDefault() {
        assertEquals(10, AbstractDiscoveryComponent.parseIntOrDefault("abc", 10));
    }

    @Test
    void parseIntOrDefault_negativeNumber_returnsIt() {
        assertEquals(-5, AbstractDiscoveryComponent.parseIntOrDefault("-5", 10));
    }

    @Test
    void parseIntOrDefault_whitespace_trimmed() {
        assertEquals(7, AbstractDiscoveryComponent.parseIntOrDefault("  7  ", 10));
    }

    // ── isEditMode ────────────────────────────────────────────────────────

    @Test
    void isEditMode_true_whenChannelManagerPreviewRequest() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        assertTrue(new TestableComponent(discoveryService).isEditMode(request));
    }

    @Test
    void isEditMode_false_whenNotPreviewRequest() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);

        assertFalse(new TestableComponent(discoveryService).isEditMode(request));
    }

    @Test
    void doBeforeRender_setsEditModeModel() throws HstComponentException {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        new TestableBaseRenderComponent().doBeforeRender(request, response);

        verify(request).setModel("editMode", true);
    }

    // ── getPublicRequestParameterAsInt ────────────────────────────────────

    @Test
    void getPublicRequestParameterAsInt_parsesParam() {
        TestableComponent component = new TestableComponent(discoveryService, "limit", "20");

        assertEquals(20, component.getPublicRequestParameterAsInt(request, "limit", 10));
    }

    @Test
    void getPublicRequestParameterAsInt_usesDefaultWhenAbsent() {
        TestableComponent component = new TestableComponent(discoveryService, "limit", null);

        assertEquals(10, component.getPublicRequestParameterAsInt(request, "limit", 10));
    }

    // ── isBandConfiguredOnPage ────────────────────────────────────────────

    @Test
    void isBandConfiguredOnPage_matchingComponent_returnsTrue() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getResolvedSiteMapItem()).thenReturn(resolvedSiteMapItem);
        when(resolvedSiteMapItem.getHstComponentConfiguration()).thenReturn(pageConfig);
        when(pageConfig.flattened()).thenReturn(Stream.of(childConfig));
        when(childConfig.getComponentClassName()).thenReturn(
                "org.bloomreach.forge.discovery.site.component.DiscoverySearchComponent");
        when(childConfig.getParameter("label")).thenReturn(null); // defaults to "default"

        assertTrue(new TestableComponent(discoveryService).isBandConfiguredOnPage(request, "default", DiscoverySearchComponent.class));
    }

    @Test
    void isBandConfiguredOnPage_wrongBand_returnsFalse() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getResolvedSiteMapItem()).thenReturn(resolvedSiteMapItem);
        when(resolvedSiteMapItem.getHstComponentConfiguration()).thenReturn(pageConfig);
        when(pageConfig.flattened()).thenReturn(Stream.of(childConfig));
        when(childConfig.getComponentClassName()).thenReturn(
                "org.bloomreach.forge.discovery.site.component.DiscoverySearchComponent");
        when(childConfig.getParameter("label")).thenReturn("other");

        assertFalse(new TestableComponent(discoveryService).isBandConfiguredOnPage(request, "default", DiscoverySearchComponent.class));
    }

    @Test
    void isBandConfiguredOnPage_noMatchingClass_returnsFalse() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getResolvedSiteMapItem()).thenReturn(resolvedSiteMapItem);
        when(resolvedSiteMapItem.getHstComponentConfiguration()).thenReturn(pageConfig);
        when(pageConfig.flattened()).thenReturn(Stream.of(childConfig));
        when(childConfig.getComponentClassName()).thenReturn("com.example.SomeOtherComponent");

        assertFalse(new TestableComponent(discoveryService).isBandConfiguredOnPage(request, "default", DiscoverySearchComponent.class));
    }

    @Test
    void isBandConfiguredOnPage_nullContext_returnsFalse() {
        when(request.getRequestContext()).thenReturn(null);

        assertFalse(new TestableComponent(discoveryService).isBandConfiguredOnPage(request, "default", DiscoverySearchComponent.class));
    }

    @Test
    void isBandConfiguredOnPage_noResolvedSiteMapItem_returnsFalse() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getResolvedSiteMapItem()).thenReturn(null);

        assertFalse(new TestableComponent(discoveryService).isBandConfiguredOnPage(request, "default", DiscoverySearchComponent.class));
    }

    // ── isIsolatedComponentRender ─────────────────────────────────────────

    @Test
    void isIsolatedComponentRender_withNamespace_returnsTrue() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);
        when(requestContext.getBaseURL()).thenReturn(baseUrl);
        when(baseUrl.getComponentRenderingWindowReferenceNamespace()).thenReturn("ns");

        assertTrue(new TestableComponent(discoveryService).isIsolatedComponentRender(request));
    }

    @Test
    void isIsolatedComponentRender_withoutNamespace_returnsFalse() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);
        when(requestContext.getBaseURL()).thenReturn(baseUrl);
        when(baseUrl.getComponentRenderingWindowReferenceNamespace()).thenReturn(null);

        assertFalse(new TestableComponent(discoveryService).isIsolatedComponentRender(request));
    }

    @Test
    void isIsolatedComponentRender_liveMode_returnsFalse() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);

        assertFalse(new TestableComponent(discoveryService).isIsolatedComponentRender(request));
        verify(requestContext, never()).getBaseURL();
    }

    // ── getDiscoveryService ───────────────────────────────────────────────

    @Test
    void getDiscoveryService_delegatesToLookupService() {
        TestableComponent component = new TestableComponent(discoveryService);

        assertSame(discoveryService, component.getDiscoveryService());
    }

    @Test
    void getDiscoveryService_usesTypedLookupWhenBeanNameLookupWouldMiss() {
        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getComponent(HstDiscoveryService.class)).thenReturn(discoveryService);
        HstServices.setComponentManager(componentManager);

        assertSame(discoveryService, new TestableLookupComponent().getDiscoveryService());
    }

    @Test
    void getDiscoveryService_fallsBackToBeanNameLookupWhenTypedLookupFails() {
        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getComponent(HstDiscoveryService.class))
                .thenThrow(new ComponentsException("typed lookup failed"));
        when(componentManager.getComponent(HstDiscoveryService.class.getName())).thenReturn(discoveryService);
        HstServices.setComponentManager(componentManager);

        assertSame(discoveryService, new TestableLookupComponent().getDiscoveryService());
    }

    @Test
    void getDiscoveryService_fallsBackToAddonModuleLookupWhenRootContextMisses() {
        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getComponent(HstDiscoveryService.class))
                .thenThrow(new ComponentsException("typed lookup failed"));
        when(componentManager.getComponent(HstDiscoveryService.class.getName())).thenReturn(null);
        when(componentManager.getComponent(HstDiscoveryService.class, "org.bloomreach.forge.discovery.site"))
                .thenReturn(discoveryService);
        HstServices.setComponentManager(componentManager);

        assertSame(discoveryService, new TestableLookupComponent().getDiscoveryService());
    }

    // ── backfillSearchResponse ────────────────────────────────────────────

    /**
     * Issue #1 regression: when a CategoryComponent config is found but categoryId is blank
     * (no document, no URL param), the method must NOT return empty — it must fall through
     * to the search branch and return the search result if a query is present.
     */
    @Test
    void backfillSearchResponse_catConfigPresentButBlankCategoryId_fallsThroughToSearch() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getResolvedSiteMapItem()).thenReturn(resolvedSiteMapItem);
        when(resolvedSiteMapItem.getHstComponentConfiguration()).thenReturn(pageConfig);

        // Category config: blank document param → no categoryId resolved
        HstComponentConfiguration catConfig = mock(HstComponentConfiguration.class);
        when(catConfig.getComponentClassName()).thenReturn(DiscoveryCategoryComponent.class.getName());
        when(catConfig.getParameter("label")).thenReturn(null); // defaults to "default"
        when(catConfig.getParameter("document")).thenReturn(null);

        // Search config: present + all optional params null
        when(childConfig.getComponentClassName()).thenReturn(DiscoverySearchComponent.class.getName());
        when(childConfig.getParameter("label")).thenReturn(null); // defaults to "default"
        when(childConfig.getParameter("pageSize")).thenReturn(null);
        when(childConfig.getParameter("defaultSort")).thenReturn(null);
        when(childConfig.getParameter("catalogName")).thenReturn(null);
        when(childConfig.getParameter("statsFields")).thenReturn(null);
        when(childConfig.getParameter("segment")).thenReturn(null);
        when(childConfig.getParameter("exclusionFilter")).thenReturn(null);

        // flattened() is called twice: once for cat lookup, once for search lookup
        when(pageConfig.flattened())
                .thenReturn(Stream.of(catConfig, childConfig))
                .thenReturn(Stream.of(catConfig, childConfig));

        SearchResult result = new SearchResult(List.of(), 1L, 0, 12, Map.of());
        SearchResponse mockResponse = new SearchResponse(result, SearchMetadata.empty());
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(mockResponse);

        // "category" URL param blank → no cat id; "q" param present → search executes
        Map<String, String> params = Map.of("q", "sneakers");
        TestableMultiParamComponent component = new TestableMultiParamComponent(discoveryService, params);
        var response = component.backfillSearchResponse(request, "default");

        assertTrue(response.isPresent(), "Expected search fallback result when cat categoryId is blank");
        verify(discoveryService, never()).browse(any(), any(), any(SearchRequestOptions.class));
    }

    // ── testable subclass ─────────────────────────────────────────────────

    private static class TestableComponent extends AbstractDiscoveryComponent {

        private final HstDiscoveryService service;
        private final String paramName;
        private final String paramValue;

        TestableComponent(HstDiscoveryService service) {
            this(service, null, null);
        }

        TestableComponent(HstDiscoveryService service, String paramName, String paramValue) {
            this.service = service;
            this.paramName = paramName;
            this.paramValue = paramValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            return name.equals(paramName) ? paramValue : null;
        }

        @Override
        public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
            // no-op — only testing base-class helpers
        }
    }

    private static class TestableMultiParamComponent extends AbstractDiscoveryComponent {

        private final HstDiscoveryService service;
        private final Map<String, String> params;

        TestableMultiParamComponent(HstDiscoveryService service, Map<String, String> params) {
            this.service = service;
            this.params = params;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            return params.get(name);
        }

        @Override
        public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
            // no-op — only testing base-class helpers
        }
    }

    private static class TestableLookupComponent extends AbstractDiscoveryComponent {

        @Override
        public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
            // no-op — exercising base lookupService implementation
        }
    }

    private static class TestableBaseRenderComponent extends AbstractDiscoveryComponent {
    }
}
