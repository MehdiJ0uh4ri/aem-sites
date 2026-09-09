package com.mysite.core.models;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Hero component: a full-bleed opening section with artwork,
 * a headline and up to two calls to action.
 */
public interface Hero extends Component {

    String getEyebrow();

    String getTitle();

    String getDescription();

    /** @return DAM path of the background artwork, or {@code null} when unauthored */
    String getFileReference();

    String getAlt();

    String getPrimaryCtaText();

    /** @return resolved href for the primary CTA, {@code .html} appended for internal pages */
    String getPrimaryCtaUrl();

    String getSecondaryCtaText();

    String getSecondaryCtaUrl();

    /** @return {@code true} when there is no headline to render */
    boolean isEmpty();
}
