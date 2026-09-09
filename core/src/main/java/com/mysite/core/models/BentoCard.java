package com.mysite.core.models;

/**
 * A single tile in the {@link Bento} grid.
 */
public interface BentoCard {

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
