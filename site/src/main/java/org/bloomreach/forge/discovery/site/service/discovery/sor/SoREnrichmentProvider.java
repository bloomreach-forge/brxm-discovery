package org.bloomreach.forge.discovery.site.service.discovery.sor;

import org.bloomreach.forge.discovery.search.model.ProductSummary;

import java.util.List;

/**
 * Optional SPI for enriching Discovery product summaries with live commerce data
 * (e.g. price, stock) from a System of Record at render time.
 * <p>
 * Integrators register a Spring bean with the FQCN as ID to activate enrichment:
 * <pre>
 *   &lt;bean id="org.bloomreach.forge.discovery.site.service.discovery.sor.SoREnrichmentProvider"
 *         class="com.example.MyEnrichmentProvider"/&gt;
 * </pre>
 * The plugin provides no built-in implementation — pass {@code null} to
 * {@link org.bloomreach.forge.discovery.site.platform.HstDiscoveryService} to disable.
 */
public interface SoREnrichmentProvider {

    List<ProductSummary> enrich(List<ProductSummary> products);
}
