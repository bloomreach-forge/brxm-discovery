package org.bloomreach.forge.discovery.cms.picker;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrispRepositoryConfigTest {

    private static final String SIMPLE_RESOLVER_CLASS =
            "org.onehippo.cms7.crisp.core.resource.jackson.SimpleJacksonRestTemplateResourceResolver";
    private static final String CONFIG_BACKED_RESOLVER_CLASS =
            "org.bloomreach.forge.discovery.crisp.ConfigBackedDiscoveryResourceResolver";

    @Test
    void platformScopedRepositoryResolvers_declareConcreteClassAndScope() throws Exception {
        String yaml = new ClassPathResource("hcm-config/brxdis-crisp.yaml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertEquals(5, count(yaml, "crisp:sitescopes:\n        - platform"));
        assertEquals(5, count(yaml, "parent=\"abstractCrispSimpleJacksonRestTemplateResourceResolver\""));
        assertEquals(3, count(yaml, "class=\"" + CONFIG_BACKED_RESOLVER_CLASS + "\""));
        assertEquals(2, count(yaml, "class=\"" + SIMPLE_RESOLVER_CLASS + "\""));
        assertEquals(5, count(yaml, "<property name=\"cacheEnabled\" value=\"false\"/>"));
        assertEquals(2, count(yaml, "crisp:propvalues:"));
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
