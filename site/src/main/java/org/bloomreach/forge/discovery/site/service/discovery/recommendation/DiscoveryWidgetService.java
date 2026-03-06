package org.bloomreach.forge.discovery.site.service.discovery.recommendation;

import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.WidgetInfo;

import java.util.List;
import java.util.Optional;

public interface DiscoveryWidgetService {

    List<WidgetInfo> listWidgets(DiscoveryConfig config);

    Optional<WidgetInfo> findWidget(String widgetId, DiscoveryConfig config);

    List<WidgetInfo> findByType(String widgetType, DiscoveryConfig config);
}
