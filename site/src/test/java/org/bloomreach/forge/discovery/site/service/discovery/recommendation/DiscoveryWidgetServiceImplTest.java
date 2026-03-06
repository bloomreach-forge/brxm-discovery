package org.bloomreach.forge.discovery.site.service.discovery.recommendation;

import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.exception.ConfigurationException;
import org.bloomreach.forge.discovery.site.exception.SearchException;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.WidgetInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryWidgetServiceImplTest {

    @Mock DiscoveryClient client;

    private DiscoveryWidgetServiceImpl service;
    private DiscoveryConfig config;

    @BeforeEach
    void setUp() {
        service = new DiscoveryWidgetServiceImpl(client);
        config = new DiscoveryConfig(
                "acct123", "myDomain", "secret-key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                12, "");
    }

    @Test
    void listWidgets_happyPath() {
        List<WidgetInfo> expected = List.of(
                new WidgetInfo("w1", "Widget 1", "item", true, "Description"),
                new WidgetInfo("w2", "Widget 2", "keyword", false, null)
        );
        when(client.listWidgets(config)).thenReturn(expected);

        List<WidgetInfo> result = service.listWidgets(config);

        assertEquals(2, result.size());
        assertEquals("w1", result.get(0).id());
        assertEquals("keyword", result.get(1).type());
    }

    @Test
    void listWidgets_cachedResult_noDuplicateCall() {
        List<WidgetInfo> expected = List.of(new WidgetInfo("w1", "Widget 1", "item", true, null));
        when(client.listWidgets(config)).thenReturn(expected);

        service.listWidgets(config);
        List<WidgetInfo> result = service.listWidgets(config);

        assertEquals(1, result.size());
        verify(client, times(1)).listWidgets(config);
    }

    @Test
    void findWidget_exists_returnsPresent() {
        List<WidgetInfo> widgets = List.of(
                new WidgetInfo("w1", "Widget 1", "item", true, null),
                new WidgetInfo("w2", "Widget 2", "keyword", true, null)
        );
        when(client.listWidgets(config)).thenReturn(widgets);

        Optional<WidgetInfo> result = service.findWidget("w2", config);

        assertTrue(result.isPresent());
        assertEquals("Widget 2", result.get().name());
    }

    @Test
    void findWidget_missing_returnsEmpty() {
        when(client.listWidgets(config)).thenReturn(List.of());

        Optional<WidgetInfo> result = service.findWidget("nonexistent", config);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByType_filtersCorrectly() {
        List<WidgetInfo> widgets = List.of(
                new WidgetInfo("w1", "Widget 1", "item", true, null),
                new WidgetInfo("w2", "Widget 2", "keyword", true, null),
                new WidgetInfo("w3", "Widget 3", "item", true, null)
        );
        when(client.listWidgets(config)).thenReturn(widgets);

        List<WidgetInfo> result = service.findByType("item", config);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(w -> "item".equals(w.type())));
    }

    @Test
    void listWidgets_missingAccountId_throwsConfigurationException() {
        DiscoveryConfig badConfig = new DiscoveryConfig(
                null, "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                12, "");

        assertThrows(ConfigurationException.class, () -> service.listWidgets(badConfig));
        verifyNoInteractions(client);
    }

    @Test
    void listWidgets_clientThrows_propagates() {
        when(client.listWidgets(config)).thenThrow(new SearchException("CRISP failure"));

        assertThrows(SearchException.class, () -> service.listWidgets(config));
    }
}
