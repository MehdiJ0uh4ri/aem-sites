package com.mysite.core.models;

/**
 * One piece of related content, resolved from a reference on the source fragment.
 */
public interface MjRelatedItem {

    /** @return id unique within the page, derived from the parent component id */
    String getId();

    String getTitle();

    /**
     * @return what kind of thing this is — "Sector", "Portfolio company", "Insight" —
     *         derived from the fragment's own model, not from a list in the markup
     */
    String getKind();

    String getSummary();

    /** @return href to the detail page, or {@code null} when that type has no detail page */
    String getUrl();

    /** @return the Adobe Client Data Layer entry for this item, or {@code null} */
    String getDataLayerJson();
}
