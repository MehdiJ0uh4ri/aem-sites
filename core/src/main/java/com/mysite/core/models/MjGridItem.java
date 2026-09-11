package com.mysite.core.models;

/**
 * One company in the portfolio index.
 *
 * <p>The filter facets are exposed as their own accessors rather than being parsed out of
 * display copy. A normalised {@code stage} on the fragment is what lets the index filter
 * without guessing at prose, and it is why these fields exist on the content model.</p>
 */
public interface MjGridItem {

    String getId();

    String getName();

    String getSector();

    String getStage();

    String getRegion();

    String getYear();

    String getStatus();

    String getSummary();

    String getFileReference();

    /** @return href to the company's detail page, built from the agreed route */
    String getUrl();

    String getDataLayerJson();
}
