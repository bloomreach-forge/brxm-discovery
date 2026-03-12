package org.bloomreach.forge.discovery.search.model;

public record PaginationModel(long total, int page, int pageSize, int totalPages) {

    public PaginationModel(long total, int page, int pageSize) {
        this(total, page, pageSize, pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0);
    }
}
