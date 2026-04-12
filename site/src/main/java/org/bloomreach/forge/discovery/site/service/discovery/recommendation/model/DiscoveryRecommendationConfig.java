package org.bloomreach.forge.discovery.site.service.discovery.recommendation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Typed representation of the JSON stored in {@code brxdis:config} on a
 * {@code brxdis:recommendationDocument}.
 *
 * <p>Produced by the recommendation wizard Open UI extension and consumed by
 * {@link org.bloomreach.forge.discovery.site.beans.DiscoveryRecommendationBean}
 * at site-delivery time.
 *
 * <p>Null context fields mean "fall back to the URL parameter at runtime":
 * <ul>
 *   <li>{@code contextProductId == null} → use {@code ?pid=} for item/recs/mlt widgets</li>
 *   <li>{@code contextCategoryId == null} → use {@code ?category=} for category widgets</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscoveryRecommendationConfig(
        String widgetId,
        String widgetName,
        String widgetType,
        String contextProductId,
        String contextProductName,
        String contextCategoryId,
        String contextCategoryName
) {}
