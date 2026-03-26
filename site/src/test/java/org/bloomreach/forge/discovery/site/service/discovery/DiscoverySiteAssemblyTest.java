package org.bloomreach.forge.discovery.site.service.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoverySiteAssemblyTest {

    @Test
    void addonAssembly_wiresCoreDiscoveryServices() throws Exception {
        String xml = new ClassPathResource("META-INF/hst-assembly/addon/brxm-discovery-site.xml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(xml.contains("id=\"brxmdis.resourceServiceBrokerFactory\""));
        assertTrue(xml.contains("id=\"brxmdis.resourceServiceBroker\""));
        assertTrue(xml.contains("<constructor-arg ref=\"brxmdis.resourceServiceBroker\"/>"));
        assertTrue(xml.contains("id=\"org.bloomreach.forge.discovery.site.platform.HstDiscoveryService\""));
        assertTrue(xml.contains("id=\"brxmdis.configProvider\""));
        assertTrue(xml.contains("id=\"brxmdis.configProviderServiceRegistration\""));
    }

    @Test
    void overrideAssembly_isOnlyACompatibilityShim() throws Exception {
        String xml = new ClassPathResource("META-INF/hst-assembly/overrides/brxm-discovery-site.xml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(!xml.contains("id=\"org.bloomreach.forge.discovery.site.platform.HstDiscoveryService\""));
    }
}
