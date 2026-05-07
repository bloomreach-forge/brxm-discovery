package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryPixelTransport;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.PixelEvent;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.SearchPageView;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.TrackingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryPixelServiceImplTest {

    @Mock DiscoveryPixelTransport transport;

    private DiscoveryPixelServiceImpl service;
    private DiscoveryCredentials credentials;
    private PixelEvent event;

    private static final PixelFlags ENABLED      = new PixelFlags(true,  false, false, "US");
    private static final ClientContext BROWSER    = new ClientContext("Mozilla/5.0 (Macintosh)", null, null);
    private static final ClientContext AXIOS_CTX  = new ClientContext("axios/1.13.5", null, null);
    private static final ClientContext NULL_UA    = new ClientContext(null, null, null);

    @BeforeEach
    void setUp() {
        service     = new DiscoveryPixelServiceImpl(transport, Runnable::run);
        credentials = new DiscoveryCredentials("acct", "domain", "key", null, "PRODUCTION");
        event       = new SearchPageView(new TrackingContext(null, null, null, null, null), "shoes", List.of());
    }

    @Test
    void fire_enabled_browserUA_buildPathAndFireCalled() {
        when(transport.buildPath(event, credentials, null, ENABLED)).thenReturn("/pix.gif?type=pageview");

        service.fire(event, credentials, null, BROWSER, ENABLED);

        verify(transport).buildPath(event, credentials, null, ENABLED);
        verify(transport).fire("/pix.gif?type=pageview", BROWSER, ENABLED);
    }

    @Test
    void fire_disabled_nothingCalled() {
        service.fire(event, credentials, null, BROWSER, PixelFlags.DISABLED);

        verifyNoInteractions(transport);
    }

    @Test
    void fire_axiosUA_blocked_nothingCalled() {
        service.fire(event, credentials, null, AXIOS_CTX, ENABLED);

        verifyNoInteractions(transport);
    }

    @Test
    void fire_nullUA_allowed_pixelFires() {
        when(transport.buildPath(event, credentials, null, ENABLED)).thenReturn("/pix.gif");

        service.fire(event, credentials, null, NULL_UA, ENABLED);

        verify(transport).buildPath(event, credentials, null, ENABLED);
    }

    @Test
    void fire_passesClientIpToTransport() {
        when(transport.buildPath(eq(event), eq(credentials), eq("10.0.0.1"), eq(ENABLED))).thenReturn("/pix.gif");

        service.fire(event, credentials, "10.0.0.1", BROWSER, ENABLED);

        verify(transport).buildPath(event, credentials, "10.0.0.1", ENABLED);
    }

    @Test
    void fire_transportFireThrows_doesNotPropagate() {
        when(transport.buildPath(any(), any(), nullable(String.class), any())).thenReturn("/pix.gif");
        doThrow(new RuntimeException("broker down")).when(transport).fire(anyString(), any(), any());

        assertDoesNotThrow(() -> service.fire(event, credentials, null, BROWSER, ENABLED));
    }

    @Test
    void fire_transportBuildPathThrows_doesNotPropagate() {
        when(transport.buildPath(any(), any(), nullable(String.class), any()))
                .thenThrow(new IllegalStateException("bad state"));

        assertDoesNotThrow(() -> service.fire(event, credentials, null, BROWSER, ENABLED));

        verify(transport, never()).fire(anyString(), any(), any());
    }

    @Test
    void fire_rejectedExecution_doesNotPropagate() {
        DiscoveryPixelServiceImpl asyncService =
                new DiscoveryPixelServiceImpl(transport, task -> { throw new RejectedExecutionException("queue full"); });

        assertDoesNotThrow(() -> asyncService.fire(event, credentials, null, BROWSER, ENABLED));

        verifyNoInteractions(transport);
    }
}
