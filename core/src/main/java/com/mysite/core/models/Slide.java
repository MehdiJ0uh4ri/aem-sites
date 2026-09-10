package com.mysite.core.models;

/**
 * A single slide in the {@link Slides} showcase.
 *
 * <p>Unlike the other item models these carry their own data layer identity, so
 * Adobe Analytics / Experience Platform can attribute a click to the individual
 * slide rather than only to the section around it.</p>
 */
public interface Slide {

    /** @return id unique within the page, derived from the parent component id */
    String getId();

    String getTitle();

    String getDescription();

    String getFileReference();

    String getAlt();

    String getCtaText();

    /** @return resolved href, or {@code null} when the slide has no call to action */
    String getCtaUrl();

    /**
     * @return the Adobe Client Data Layer entry for this slide, or {@code null}
     *         when the data layer is switched off for the page
     */
    String getDataLayerJson();
}
