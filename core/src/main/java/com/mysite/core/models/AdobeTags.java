package com.mysite.core.models;

/**
 * View model for the Adobe Experience Platform Tags embed, consumed by the page's
 * {@code customheaderlibs.html}.
 */
public interface AdobeTags {

    /** @return {@code true} when the embed script should be rendered */
    boolean isEnabled();

    /** @return the Tags embed script URL */
    String getEmbedUrl();

    /** @return whether the script tag carries {@code async} */
    boolean isAsync();
}
