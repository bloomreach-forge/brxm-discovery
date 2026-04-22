package org.bloomreach.forge.discovery.cms.rest;

import org.bloomreach.forge.discovery.cms.rest.dto.PickerItemDto;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerSearchResponseDto;
import org.bloomreach.forge.discovery.config.model.DiscoverySchemaConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveryPickerResponseMapperTest {

    private static final String STANDARD_JSON = """
            {
              "response": {
                "numFound": 1,
                "docs": [{
                  "pid": "p1",
                  "title": "Running Shoe",
                  "thumb_image": "https://img.example.com/shoe.jpg",
                  "url": "/shoe",
                  "price": 49.99
                }]
              }
            }
            """;

    private static final String CUSTOM_FIELD_JSON = """
            {
              "response": {
                "numFound": 1,
                "docs": [{
                  "sku": "sku-1",
                  "name": "Fancy Shoe",
                  "image_url": "https://img.example.com/fancy.jpg",
                  "product_url": "/fancy",
                  "list_price": 89.99
                }]
              }
            }
            """;

    // ── default field mapping ─────────────────────────────────────────────

    @Test
    void toSearchResponse_defaultMapping_mapsStandardFields() {
        DiscoveryPickerResponseMapper mapper = new DiscoveryPickerResponseMapper(DiscoverySchemaConfig.DEFAULT);

        PickerSearchResponseDto result = mapper.toSearchResponse(STANDARD_JSON, 0, 12);

        assertEquals(1, result.total());
        assertEquals(1, result.items().size());
        PickerItemDto item = result.items().get(0);
        assertEquals("p1", item.id());
        assertEquals("Running Shoe", item.title());
        assertEquals("https://img.example.com/shoe.jpg", item.imageUrl());
        assertEquals("49.99", item.price());
    }

    @Test
    void toSearchResponse_defaultMapping_missingField_returnsNull() {
        String json = """
                {
                  "response": { "numFound": 1, "docs": [{"pid": "p1"}] }
                }
                """;
        DiscoveryPickerResponseMapper mapper = new DiscoveryPickerResponseMapper(DiscoverySchemaConfig.DEFAULT);

        PickerItemDto item = mapper.toSearchResponse(json, 0, 12).items().get(0);

        assertEquals("p1", item.id());
        assertNull(item.title());
        assertNull(item.imageUrl());
        assertNull(item.price());
    }

    // ── custom field mapping ──────────────────────────────────────────────

    @Test
    void toSearchResponse_customMapping_mapsAltFields() {
        DiscoverySchemaConfig schema = new DiscoverySchemaConfig(
                "sku,name,image_url,product_url,list_price",
                null,
                "sku", "name", "image_url", "list_price");
        DiscoveryPickerResponseMapper mapper = new DiscoveryPickerResponseMapper(schema);

        PickerSearchResponseDto result = mapper.toSearchResponse(CUSTOM_FIELD_JSON, 0, 12);

        assertEquals(1, result.total());
        PickerItemDto item = result.items().get(0);
        assertEquals("sku-1", item.id());
        assertEquals("Fancy Shoe", item.title());
        assertEquals("https://img.example.com/fancy.jpg", item.imageUrl());
        assertEquals("89.99", item.price());
    }

    @Test
    void toSearchResponse_customMapping_standardFieldsReturnNull() {
        DiscoverySchemaConfig schema = new DiscoverySchemaConfig(
                "sku,name,image_url,product_url,list_price",
                null,
                "sku", "name", "image_url", "list_price");
        DiscoveryPickerResponseMapper mapper = new DiscoveryPickerResponseMapper(schema);

        // JSON has standard fields (pid/title/thumb_image) but mapper reads custom names
        PickerItemDto item = mapper.toSearchResponse(STANDARD_JSON, 0, 12).items().get(0);

        assertNull(item.id());
        assertNull(item.title());
        assertNull(item.imageUrl());
    }

    // ── pagination passthrough ────────────────────────────────────────────

    @Test
    void toSearchResponse_paginationPassthrough() {
        DiscoveryPickerResponseMapper mapper = new DiscoveryPickerResponseMapper(DiscoverySchemaConfig.DEFAULT);

        PickerSearchResponseDto result = mapper.toSearchResponse(STANDARD_JSON, 2, 10);

        assertEquals(2, result.page());
        assertEquals(10, result.pageSize());
    }

    // ── non-numeric price ─────────────────────────────────────────────────

    @Test
    void toSearchResponse_nonNumericPriceField_returnsNull() {
        String json = """
                {
                  "response": { "numFound": 1, "docs": [{"pid": "p1", "price": "N/A"}] }
                }
                """;
        DiscoveryPickerResponseMapper mapper = new DiscoveryPickerResponseMapper(DiscoverySchemaConfig.DEFAULT);

        assertNull(mapper.toSearchResponse(json, 0, 12).items().get(0).price());
    }

    // ── multiple docs ─────────────────────────────────────────────────────

    @Test
    void toSearchResponse_multipleDocs_allMapped() {
        String json = """
                {
                  "response": {
                    "numFound": 2,
                    "docs": [
                      {"pid": "p1", "title": "Shoe"},
                      {"pid": "p2", "title": "Boot"}
                    ]
                  }
                }
                """;
        DiscoveryPickerResponseMapper mapper = new DiscoveryPickerResponseMapper(DiscoverySchemaConfig.DEFAULT);

        List<PickerItemDto> items = mapper.toSearchResponse(json, 0, 12).items();

        assertEquals(2, items.size());
        assertEquals("p1", items.get(0).id());
        assertEquals("p2", items.get(1).id());
    }
}
