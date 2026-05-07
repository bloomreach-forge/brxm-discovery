package org.bloomreach.forge.discovery.site.service.discovery.dto;

import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.VariantSummary;

import java.util.List;

public final class ProductDocMapper {

    private ProductDocMapper() {
    }

    public static ProductSummary toProductSummary(ProductDoc doc) {
        List<VariantSummary> variants = doc.variants().stream().map(ProductDocMapper::toVariantSummary).toList();
        return new ProductSummary(doc.pid(), doc.title(), doc.url(), doc.thumbImage(),
                doc.price(), doc.currency(), doc.extras(), variants);
    }

    public static VariantSummary toVariantSummary(VariantDoc v) {
        List<String> thumbs   = v.skuThumbImages()  != null ? List.copyOf(v.skuThumbImages())  : List.of();
        List<String> large    = v.skuLargeImages()  != null ? List.copyOf(v.skuLargeImages())  : List.of();
        List<String> swatches = v.skuSwatchImages() != null ? List.copyOf(v.skuSwatchImages()) : List.of();
        return new VariantSummary(v.skuId(), v.skuColor(), v.skuColorGroup(), v.skuSize(),
                v.skuPrice(), v.skuSalePrice(), thumbs, large, swatches, v.extras());
    }
}
