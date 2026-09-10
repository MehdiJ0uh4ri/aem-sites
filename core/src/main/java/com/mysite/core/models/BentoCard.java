package com.mysite.core.models;

/**
 * A single tile in the {@link Bento} grid.
 */
public interface BentoCard {

    /** @return id unique within the page, derived from the parent component id */
    String getId();

    /**
     * @return the Adobe Client Data Layer entry for this tile, or {@code null} when the
     *         data layer is switched off for the page
     */
    String getDataLayerJson();

    String getTitle();

    String getDescription();

    String getFileReference();

    String getAlt();

    /** @return resolved href, or {@code null} when the tile is not a link */
    String getUrl();

    /**
     * @return grid footprint: {@code wide}, {@code tall} or {@code normal}.
     *         Drives which modifier class the tile receives.
     */
    String getSpan();
}
