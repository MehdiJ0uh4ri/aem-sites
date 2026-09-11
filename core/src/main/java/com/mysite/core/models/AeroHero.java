package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Aerospace Hero: the full-bleed opening section of an AeroVance page.
 *
 * <p>The background is either a still from Assets or a muted, looping video with a poster
 * still behind it. An optional overlay panel carries up to three figures that animate on
 * scroll, reusing the {@link StatsItem} value parsing so "1,200+" and "$1.2M" behave the
 * same way here as they do in the Stats Bar.</p>
 */
public interface AeroHero extends Component {

    /** @return small label above the headline, typically the business unit */
    String getEyebrow();

    String getTitle();

    String getDescription();

    /** @return DAM path of the background still, or {@code null} when unauthored */
    String getFileReference();

    /**
     * @return alternative text for the background still; empty when the artwork is
     *         decorative, which is the default
     */
    String getAlt();

    /** @return DAM path of the background video, or {@code null} when unauthored */
    String getVideoReference();

    /** @return {@code true} when a video background should be rendered */
    boolean isVideo();

    /**
     * @return the still shown before the video can play. Falls back to the authored
     *         background image so a video hero never opens on an empty frame.
     */
    String getPosterReference();

    String getPrimaryCtaText();

    /** @return resolved href for the primary CTA, {@code .html} appended for internal pages */
    String getPrimaryCtaUrl();

    String getSecondaryCtaText();

    String getSecondaryCtaUrl();

    /** @return the overlay figures, at most three, never {@code null} */
    List<StatsItem> getStats();

    /**
     * @return an export-control notice to display over the hero (for example an ITAR
     *         advisory on a defence programme), or {@code null}
     */
    String getComplianceNotice();

    /** @return {@code true} when there is no headline to render */
    boolean isEmpty();
}
