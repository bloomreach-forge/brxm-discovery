package org.bloomreach.forge.discovery.cms.picker;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrispRepositoryConfigTest {

    private static final String RESOLVER_CLASS =
            "org.onehippo.cms7.crisp.core.resource.jackson.SimpleJacksonRestTemplateResourceResolver";

    @Test
    void platformScopedRepositoryResolvers_declareConcreteClassAndScope() throws Exception {
        String yaml = new ClassPathResource("hcm-config/brxdis-crisp.yaml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertEquals(5, count(yaml, "crisp:sitescopes:\n        - platform"));
        assertEquals(5, count(yaml, "parent=\"abstractCrispSimpleJacksonRestTemplateResourceResolver\""));
        assertEquals(5, count(yaml, "class=\"" + RESOLVER_CLASS + "\""));
        assertEquals(5, count(yaml, "<property name=\"cacheEnabled\" value=\"false\"/>"));
        assertEquals(5, count(yaml, "crisp:propvalues:"));
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
