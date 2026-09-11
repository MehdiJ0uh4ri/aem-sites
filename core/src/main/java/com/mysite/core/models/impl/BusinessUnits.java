package com.mysite.core.models.impl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The company's business units, as the slug stored in the repository mapped to its display
 * text.
 *
 * <p>Components store the slug, never the label. Renaming a unit — which happens after every
 * reorganisation — then means editing this one map rather than migrating every facility,
 * programme and job listing that referenced the old wording. The iteration order is the order
 * the units are presented in across the site, so filter controls read consistently wherever
 * they appear.</p>
 */
final class BusinessUnits {

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put("commercial-aviation", "Commercial Aviation");
        LABELS.put("defence-space", "Defence & Space");
        LABELS.put("mro-services", "MRO & Services");
        LABELS.put("digital-autonomous", "Digital & Autonomous Systems");
        LABELS.put("corporate", "Corporate");
    }

    private BusinessUnits() {
        // static helper
    }

    /**
     * @return the slug if it is one the company actually has, otherwise {@code null}. An
     *         unrecognised value would produce a filter button matching nothing.
     */
    static String slugOrNull(String slug) {
        return LABELS.containsKey(slug) ? slug : null;
    }

    /** @return the display text for a slug, or {@code null} when it is not a known unit */
    static String label(String slug) {
        return LABELS.get(slug);
    }

    /** @return every unit, slug to label, in presentation order */
    static Map<String, String> all() {
        return java.util.Collections.unmodifiableMap(LABELS);
    }
}
