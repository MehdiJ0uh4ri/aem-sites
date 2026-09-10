package com.mysite.core.models;

/**
 * One entry in an {@link MjIndex}, flattened from a Content Fragment.
 *
 * <p>The accessors are deliberately generic — {@code kicker}, {@code meta} — because the
 * same index renders Insights, Portfolio Companies and Deals. Which fragment element
 * feeds which accessor is decided by the index's variant, not by the markup.</p>
 */
public interface MjIndexItem {

    /** @return id unique within the page, derived from the parent component id */
    String getId();

    String getTitle();

    /** @return the classifying line: category, sector or investment type */
    String getKicker();

    /** @return the qualifying line: publication date, investment year or location */
    String getMeta();

    String getSummary();

    /** @return DAM path of the item's image, or {@code null} */
    String getFileReference();

    String getAlt();

    /** @return resolved href to the detail page, or {@code null} when the fragment has no page */
    String getUrl();

    /** @return status label such as {@code Active} or {@code Realised}, or {@code null} */
    String getStatus();

    /** @return the Adobe Client Data Layer entry for this item, or {@code null} */
    String getDataLayerJson();
}
