package org.bloomreach.forge.discovery.site.component;

import jakarta.servlet.http.HttpServletRequest;
import org.bloomreach.forge.discovery.exception.DiscoveryException;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentsException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @Mock HttpServletRequest servletRequest;

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

    // ── doBeforeRender - DiscoveryException handling ──────────────────────

    @Test
    void doBeforeRender_discoveryException_doesNotPropagate() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);

        assertDoesNotThrow(() ->
                new ThrowingComponent(new SearchException("API down")).doBeforeRender(request, response));
    }

    @Test
    void doBeforeRender_discoveryException_setsWarningInEditMode() throws HstComponentException {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        new ThrowingComponent(new SearchException("API down")).doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), anyString());
    }

    @Test
    void doBeforeRender_discoveryException_noWarningInLiveMode() throws HstComponentException {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);

        new ThrowingComponent(new SearchException("API down")).doBeforeRender(request, response);

        verify(request, never()).setAttribute(anyString(), any());
    }

    // ── getPathSegmentParam ───────────────────────────────────────────────

    @Test
    void getPathSegmentParam_returnsValue_whenLabelPrecedesValueInPath() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getPathInfo()).thenReturn("/product/blue-chair/pid/SKU-123");

        assertEquals("SKU-123", AbstractDiscoveryComponent.getPathSegmentParam(request, "pid"));
    }

    @Test
    void getPathSegmentParam_returnsNull_whenServletRequestNull() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getServletRequest()).thenReturn(null);

        assertNull(AbstractDiscoveryComponent.getPathSegmentParam(request, "pid"));
    }

    @Test
    void getPathSegmentParam_returnsNull_whenLabelAbsentFromPath() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getPathInfo()).thenReturn("/product/blue-chair");

        assertNull(AbstractDiscoveryComponent.getPathSegmentParam(request, "pid"));
    }

    @Test
    void getPathSegmentParam_returnsNull_whenLabelIsLastSegment() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getPathInfo()).thenReturn("/product/blue-chair/pid");

        assertNull(AbstractDiscoveryComponent.getPathSegmentParam(request, "pid"));
    }

    // ── resolveUrlParam ───────────────────────────────────────────────────

    @Test
    void resolveUrlParam_pathInfoContainsLabel_returnsPathValue() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getPathInfo()).thenReturn("/product/blue-chair/pid/SKU-123");

        assertEquals("SKU-123", new TestableComponent(discoveryService).resolveUrlParam(request, "pid"));
    }

    @Test
    void resolveUrlParam_pathInfoLabelAbsent_returnsQueryParam() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getPathInfo()).thenReturn("/product/blue-chair");

        assertEquals("SKU-456", new TestableComponent(discoveryService, "pid", "SKU-456").resolveUrlParam(request, "pid"));
    }

    // ── getDiscoveryService ───────────────────────────────────────────────

    @Test
    void getDiscoveryService_delegatesToLookupService() {
        TestableComponent component = new TestableComponent(discoveryService);

        assertSame(discoveryService, component.getDiscoveryService());
    }

    @Test
    void getDiscoveryService_usesModuleAwareLookup() {
        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getComponent(HstDiscoveryService.class, AbstractDiscoveryComponent.MODULE_NAME))
                .thenReturn(discoveryService);
        HstServices.setComponentManager(componentManager);

        assertSame(discoveryService, new TestableLookupComponent().getDiscoveryService());
    }

    @Test
    void getDiscoveryService_throwsWhenModuleLookupFails() {
        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getComponent(HstDiscoveryService.class, AbstractDiscoveryComponent.MODULE_NAME))
                .thenThrow(new ComponentsException("module not found"));
        HstServices.setComponentManager(componentManager);

        assertThrows(org.bloomreach.forge.discovery.exception.ConfigurationException.class,
                () -> new TestableLookupComponent().getDiscoveryService());
    }

    @Test
    void lookupService_moduleNotFoundRuntimeException_convertsToConfigurationException() {
        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getComponent(HstDiscoveryService.class, AbstractDiscoveryComponent.MODULE_NAME))
                .thenThrow(new RuntimeException("ModuleNotFoundException: module not registered"));
        HstServices.setComponentManager(componentManager);

        assertThrows(org.bloomreach.forge.discovery.exception.ConfigurationException.class,
                () -> new TestableLookupComponent().getDiscoveryService());
    }

    @Test
    void doBeforeRender_unexpectedRuntimeException_doesNotPropagate() {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);

        assertDoesNotThrow(() ->
                new ThrowingRuntimeComponent(new IllegalStateException("broker unavailable"))
                        .doBeforeRender(request, response));
    }

    @Test
    void doBeforeRender_unexpectedRuntimeException_setsWarningInEditMode() throws HstComponentException {
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        new ThrowingRuntimeComponent(new IllegalStateException("broker unavailable"))
                .doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), anyString());
    }

    @Test
    void getDiscoveryService_throwsWhenModuleLookupReturnsNull() {
        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getComponent(HstDiscoveryService.class, AbstractDiscoveryComponent.MODULE_NAME))
                .thenReturn(null);
        HstServices.setComponentManager(componentManager);

        assertThrows(org.bloomreach.forge.discovery.exception.ConfigurationException.class,
                () -> new TestableLookupComponent().getDiscoveryService());
    }

    // ── testable subclasses ───────────────────────────────────────────────

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
    }

    private static class TestableLookupComponent extends AbstractDiscoveryComponent {
    }

    private static class TestableBaseRenderComponent extends AbstractDiscoveryComponent {
    }

    private static class ThrowingComponent extends AbstractDiscoveryComponent {
        private final DiscoveryException ex;

        ThrowingComponent(DiscoveryException ex) {
            this.ex = ex;
        }

        @Override
        protected void doDiscoveryBeforeRender(HstRequest request, HstResponse response) {
            throw ex;
        }
    }

    private static class ThrowingRuntimeComponent extends AbstractDiscoveryComponent {
        private final RuntimeException ex;

        ThrowingRuntimeComponent(RuntimeException ex) {
            this.ex = ex;
        }

        @Override
        protected void doDiscoveryBeforeRender(HstRequest request, HstResponse response) {
            throw ex;
        }
    }
}
