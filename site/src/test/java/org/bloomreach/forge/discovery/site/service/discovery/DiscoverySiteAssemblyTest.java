package org.bloomreach.forge.discovery.site.service.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoverySiteAssemblyTest {

    @Test
    void moduleDescriptor_declaresCorrectNameAndConfigLocations() throws Exception {
        String xml = new ClassPathResource("META-INF/hst-assembly/addon/module.xml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(xml.contains("org.bloomreach.forge.discovery.site"));
        assertTrue(xml.contains("classpath*:META-INF/hst-assembly/addon/brxm-discovery/*.xml"));
        assertTrue(xml.contains("classpath*:META-INF/hst-assembly/addon/brxm-discovery/overrides/*.xml"));
    }

    @Test
    void addonAssembly_wiresCoreDiscoveryServices() throws Exception {
        String xml = new ClassPathResource("META-INF/hst-assembly/addon/brxm-discovery/brxm-discovery-site.xml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(xml.contains("id=\"brxmdis.discoveryClient\""));
        assertTrue(xml.contains("id=\"brxmdis.searchApiClient\""));
        assertTrue(xml.contains("id=\"brxmdis.autosuggestApiClient\""));
        assertTrue(xml.contains("id=\"brxmdis.recommendationApiClient\""));
        assertTrue(xml.contains("id=\"org.bloomreach.forge.discovery.site.platform.HstDiscoveryService\""));
        assertTrue(xml.contains("id=\"brxmdis.configProvider\""));

        assertFalse(xml.contains("id=\"brxmdis.resourceServiceBroker\""));
        assertFalse(xml.contains("factory-method=\"getDefaultResourceServiceBroker\""));
        assertFalse(xml.contains("id=\"brxmdis.resourceServiceBrokerFactory\""));
        assertFalse(xml.contains("id=\"brxmdis.configProviderServiceRegistration\""));
    }
}
