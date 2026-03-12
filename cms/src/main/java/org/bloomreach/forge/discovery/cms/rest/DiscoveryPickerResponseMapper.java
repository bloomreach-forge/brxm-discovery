package org.bloomreach.forge.discovery.cms.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.InternalServerErrorException;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerCategoryDto;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerItemDto;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerSearchResponseDto;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerWidgetDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class DiscoveryPickerResponseMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    PickerSearchResponseDto toSearchResponse(String json, int page, int pageSize) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode response = root.path("response");
            long total = response.path("numFound").asLong(0);
            List<PickerItemDto> items = new ArrayList<>();
            for (JsonNode doc : response.path("docs")) {
                String price = doc.path("price").isNumber() ? doc.path("price").asText() : null;
                items.add(new PickerItemDto(
                        doc.path("pid").asText(null),
                        doc.path("title").asText(null),
                        doc.path("thumb_image").asText(null),
                        doc.path("url").asText(null),
                        price));
            }
            return new PickerSearchResponseDto(items, total, page, pageSize);
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to parse Discovery API response", e);
        }
    }

    List<PickerWidgetDto> toWidgets(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode widgets = root.path("response").path("widgets");
            List<PickerWidgetDto> result = new ArrayList<>();
            for (JsonNode widget : widgets) {
                result.add(new PickerWidgetDto(
                        widget.path("id").asText(null),
                        widget.path("name").asText(null),
                        widget.path("type").asText(null),
                        widget.path("enabled").asBoolean(false),
                        widget.path("description").asText(null)));
            }
            return result;
        } catch (IOException e) {
            return List.of();
        }
    }

    List<PickerCategoryDto> toCategories(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode categoryMap = root.path("category_map");
            List<PickerCategoryDto> result = new ArrayList<>();
            categoryMap.fields().forEachRemaining(entry -> {
                String id = entry.getKey();
                String name = entry.getValue().asText(id);
                result.add(new PickerCategoryDto(id, name));
            });
            return result;
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to parse category_map response", e);
        }
    }
}
