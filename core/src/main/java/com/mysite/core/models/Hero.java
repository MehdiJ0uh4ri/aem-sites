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

    /** @return id for the primary CTA's data layer entry, or {@code null} */
    String getPrimaryCtaId();

    /** @return Adobe Client Data Layer entry for the primary CTA, or {@code null} */
    String getPrimaryCtaDataLayerJson();

    String getSecondaryCtaText();

    String getSecondaryCtaUrl();

    /** @return id for the secondary CTA's data layer entry, or {@code null} */
    String getSecondaryCtaId();

    /** @return Adobe Client Data Layer entry for the secondary CTA, or {@code null} */
    String getSecondaryCtaDataLayerJson();

    /** @return {@code true} when there is no headline to render */
    boolean isEmpty();
}
