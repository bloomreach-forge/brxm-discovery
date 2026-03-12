package org.bloomreach.forge.discovery.site.service.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrispResourceResolverAssemblyTest {

    private static final String RESOLVER_CLASS =
            "org.onehippo.cms7.crisp.core.resource.jackson.SimpleJacksonRestTemplateResourceResolver";

    @Test
    void siteFallbackResolvers_inheritCrispParentAndDeclareConcreteClass() throws Exception {
        String xml = new ClassPathResource("META-INF/hst-assembly/addon/crisp/overrides/brxdis-resource-resolvers.xml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertEquals(8, count(xml, "parent=\"abstractCrispSimpleJacksonRestTemplateResourceResolver\""));
        assertEquals(8, count(xml, "class=\"" + RESOLVER_CLASS + "\""));
        assertEquals(8, count(xml, "lazy-init=\"true\""));
        assertEquals(8, count(xml, "<property name=\"cacheEnabled\" value=\"false\"/>"));
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
