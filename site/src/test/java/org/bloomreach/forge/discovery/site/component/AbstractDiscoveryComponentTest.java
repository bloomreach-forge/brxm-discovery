package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.DiscoverySearchComponent;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link AbstractDiscoveryComponent} utility methods.
 */
@ExtendWith(MockitoExtension.class)
class AbstractDiscoveryComponentTest {

    @Mock HstRequest request;
    @Mock HstRequestContext requestContext;
    @Mock HstDiscoveryService discoveryService;
    @Mock HstContainerURL baseUrl;
    @Mock ResolvedSiteMapItem resolvedSiteMapItem;
    @Mock HstComponentConfiguration pageConfig;
    @Mock HstComponentConfiguration childConfig;

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

    // ── setModelAndAttribute ──────────────────────────────────────────────

    @Test
    void setModelAndAttribute_setsBoth() {
        new TestableComponent(discoveryService).setModelAndAttribute(request, "foo", "bar");

        verify(request).setModel("foo", "bar");
        verify(request).setAttribute("foo", "bar");
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
    void isBandConfiguredOnPage_ppr_matchingComponent_returnsTrue() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);
        when(requestContext.getBaseURL()).thenReturn(baseUrl);
        when(baseUrl.getComponentRenderingWindowReferenceNamespace()).thenReturn("ns");
        when(requestContext.getResolvedSiteMapItem()).thenReturn(resolvedSiteMapItem);
        when(resolvedSiteMapItem.getHstComponentConfiguration()).thenReturn(pageConfig);
        when(pageConfig.flattened()).thenReturn(Stream.of(childConfig));
        when(childConfig.getComponentClassName()).thenReturn(
                "org.bloomreach.forge.discovery.site.component.DiscoverySearchComponent");
        when(childConfig.getParameter("bandName")).thenReturn(null); // defaults to "default"

        assertTrue(new TestableComponent(discoveryService).isBandConfiguredOnPage(request, "default", DiscoverySearchComponent.class));
    }

    @Test
    void isBandConfiguredOnPage_ppr_wrongBand_returnsFalse() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);
        when(requestContext.getBaseURL()).thenReturn(baseUrl);
        when(baseUrl.getComponentRenderingWindowReferenceNamespace()).thenReturn("ns");
        when(requestContext.getResolvedSiteMapItem()).thenReturn(resolvedSiteMapItem);
        when(resolvedSiteMapItem.getHstComponentConfiguration()).thenReturn(pageConfig);
        when(pageConfig.flattened()).thenReturn(Stream.of(childConfig));
        when(childConfig.getComponentClassName()).thenReturn(
                "org.bloomreach.forge.discovery.site.component.DiscoverySearchComponent");
        when(childConfig.getParameter("bandName")).thenReturn("other");

        assertFalse(new TestableComponent(discoveryService).isBandConfiguredOnPage(request, "default", DiscoverySearchComponent.class));
    }

    @Test
    void isBandConfiguredOnPage_ppr_noMatchingClass_returnsFalse() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);
        when(requestContext.getBaseURL()).thenReturn(baseUrl);
        when(baseUrl.getComponentRenderingWindowReferenceNamespace()).thenReturn("ns");
        when(requestContext.getResolvedSiteMapItem()).thenReturn(resolvedSiteMapItem);
        when(resolvedSiteMapItem.getHstComponentConfiguration()).thenReturn(pageConfig);
        when(pageConfig.flattened()).thenReturn(Stream.of(childConfig));
        when(childConfig.getComponentClassName()).thenReturn("com.example.SomeOtherComponent");

        assertFalse(new TestableComponent(discoveryService).isBandConfiguredOnPage(request, "default", DiscoverySearchComponent.class));
    }

    @Test
    void isBandConfiguredOnPage_notPpr_returnsFalse() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);
        when(requestContext.getBaseURL()).thenReturn(baseUrl);
        when(baseUrl.getComponentRenderingWindowReferenceNamespace()).thenReturn(null); // full-page load

        assertFalse(new TestableComponent(discoveryService).isBandConfiguredOnPage(request, "default", DiscoverySearchComponent.class));
        verify(requestContext, never()).getResolvedSiteMapItem();
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
}
