package com.mysite.core.models;

/**
 * One downloadable document in a {@link ResourceLibrary}.
 */
public interface ResourceEntry {

    String getTitle();

    String getDescription();

    /** @return the document's category, for example {@code Whitepaper} or {@code Data sheet} */
    String getCategory();

    /** @return the DAM path of the document */
    String getAssetPath();

    /**
     * @return {@code true} when the visitor must complete the lead form before the document
     *         is served
     */
    boolean isGated();

    /**
     * @return the href for an ungated document, or {@code null} when the document is gated.
     *         A gated entry deliberately has no direct URL to expose.
     */
    String getDownloadUrl();

    /** @return the file extension in upper case, for example {@code PDF}, or {@code null} */
    String getFormat();

    /** @return a human-readable file size, for example {@code 2.4 MB}, or {@code null} */
    String getSize();

    /**
     * @return the export-control classification of the document, or {@code null}. Shown next
     *         to the download so a visitor knows before they ask, and the reviewer who
     *         approved the page can see what they approved.
     */
    String getClassification();
}
