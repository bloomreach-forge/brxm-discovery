package org.bloomreach.forge.discovery.site.service.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrispResourceResolverAssemblyTest {

    private static final String SIMPLE_RESOLVER_CLASS =
            "org.onehippo.cms7.crisp.core.resource.jackson.SimpleJacksonRestTemplateResourceResolver";
    private static final String CONFIG_BACKED_RESOLVER_CLASS =
            "org.bloomreach.forge.discovery.crisp.ConfigBackedDiscoveryResourceResolver";

    @Test
    void siteFallbackResolvers_useGenericSpacesAndConfigBackedApiResolvers() throws Exception {
        String xml = new ClassPathResource("META-INF/hst-assembly/addon/crisp/overrides/brxdis-resource-resolvers.xml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertEquals(5, count(xml, "parent=\"abstractCrispSimpleJacksonRestTemplateResourceResolver\""));
        assertEquals(3, count(xml, "class=\"" + CONFIG_BACKED_RESOLVER_CLASS + "\""));
        assertEquals(2, count(xml, "class=\"" + SIMPLE_RESOLVER_CLASS + "\""));
        assertEquals(5, count(xml, "lazy-init=\"true\""));
        assertEquals(5, count(xml, "<property name=\"cacheEnabled\" value=\"false\"/>"));
        assertEquals(3, count(xml, "<property name=\"configProvider\" ref=\"brxmdis.configProvider\"/>"));
        assertTrue(!xml.contains("discoverySearchAPIStaging"));
        assertTrue(!xml.contains("discoveryPathwaysAPIStaging"));
        assertTrue(!xml.contains("discoveryAutosuggestAPIStaging"));
        assertTrue(xml.contains("Repository-backed resolver nodes are platform-scoped"),
                "assembly comment should describe the current platform-scoped model");
    }

    private static int count(String text, String needle) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
