package org.bloomreach.forge.discovery.site.service.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoverySiteAssemblyTest {

    @Test
    void siteAssembly_wiresClientThroughLocalBrokerFactory() throws Exception {
        String xml = new ClassPathResource("META-INF/hst-assembly/overrides/brxm-discovery-site.xml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(xml.contains("id=\"brxmdis.resourceServiceBrokerFactory\""));
        assertTrue(xml.contains("id=\"brxmdis.resourceServiceBroker\""));
        assertTrue(xml.contains("<constructor-arg ref=\"brxmdis.resourceServiceBroker\"/>"));
    }
}
