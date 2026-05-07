package org.bloomreach.forge.discovery.site.service.discovery.pixel.event;

import org.bloomreach.forge.discovery.search.model.ProductSummary;

import java.util.List;
import java.util.Map;

public sealed interface PixelEvent
        permits SearchPageView, CategoryPageView, ProductPageView,
                WidgetView, WidgetClick, SearchSubmit, SuggestClick,
                ClickAdd, Quickview {

    TrackingContext tracking();

    String type();

    default String group() { return null; }

    default String etype() { return null; }

    String ptype();

    default boolean keepSuggestQuery() { return false; }

    default List<ProductSummary> products() { return List.of(); }

    Map<String, String> typeParams();

    static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
