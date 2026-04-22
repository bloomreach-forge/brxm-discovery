package org.bloomreach.forge.discovery.search.model;

/** A user-selected range filter for a single facet field (e.g. price $10–$100). */
public record RangeSelection(Double start, Double end) {}
