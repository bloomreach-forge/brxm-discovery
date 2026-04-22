package org.bloomreach.forge.discovery.site.component.info;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class DiscoverySortOptionsProviderTest {

    // ── parseValue / parseLabel static helpers ───────────────────────────

    @Test
    void parseValue_withEqualsSeparator_returnsValuePart() {
        assertEquals("price asc", DiscoverySortOptionsProvider.parseValue("price asc=Price: Low to High"));
    }

    @Test
    void parseValue_noEqualsSeparator_returnsTrimmedEntry() {
        assertEquals("relevance", DiscoverySortOptionsProvider.parseValue("relevance"));
    }

    @Test
    void parseLabel_withEqualsSeparator_returnsLabelPart() {
        assertEquals("Price: Low to High", DiscoverySortOptionsProvider.parseLabel("price asc=Price: Low to High"));
    }

    @Test
    void parseLabel_noEqualsSeparator_returnsTrimmedEntry() {
        assertEquals("name asc", DiscoverySortOptionsProvider.parseLabel("name asc"));
    }

    @Test
    void parseValue_trimsWhitespace() {
        assertEquals("price desc", DiscoverySortOptionsProvider.parseValue("  price desc = Price: High to Low  "));
    }

    @Test
    void parseLabel_trimsWhitespace() {
        assertEquals("Price: High to Low", DiscoverySortOptionsProvider.parseLabel("  price desc = Price: High to Low  "));
    }

    // ── getValues ────────────────────────────────────────────────────────

    @Test
    void getValues_returnsValuePart() {
        DiscoverySortOptionsProvider provider = new DiscoverySortOptionsProvider(List.of(
                "price asc=Price: Low to High",
                "price desc=Price: High to Low",
                "name asc"));

        List<String> values = provider.getValues();

        assertEquals(List.of("price asc", "price desc", "name asc"), values);
    }

    @Test
    void getValues_emptyEntries_returnsEmptyList() {
        assertEquals(List.of(), new DiscoverySortOptionsProvider(List.of()).getValues());
    }

    // ── getDisplayValue ───────────────────────────────────────────────────

    @Test
    void getDisplayValue_knownValue_returnsLabel() {
        DiscoverySortOptionsProvider provider = new DiscoverySortOptionsProvider(List.of(
                "price asc=Price: Low to High", "name asc=Name A-Z"));

        assertEquals("Price: Low to High", provider.getDisplayValue("price asc"));
    }

    @Test
    void getDisplayValue_unknownValue_returnsValueAsLabel() {
        DiscoverySortOptionsProvider provider = new DiscoverySortOptionsProvider(List.of(
                "price asc=Price: Low to High"));

        assertEquals("unknown", provider.getDisplayValue("unknown"));
    }

    @Test
    void getDisplayValue_noLabelPart_returnsValueItself() {
        DiscoverySortOptionsProvider provider = new DiscoverySortOptionsProvider(List.of("name asc"));

        assertEquals("name asc", provider.getDisplayValue("name asc"));
    }

    @Test
    void getDisplayValue_withLocale_sameAsWithoutLocale() {
        DiscoverySortOptionsProvider provider = new DiscoverySortOptionsProvider(List.of(
                "price asc=Price: Low to High"));

        assertEquals(
                provider.getDisplayValue("price asc"),
                provider.getDisplayValue("price asc", Locale.FRENCH));
    }
}
