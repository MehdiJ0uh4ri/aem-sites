package com.mysite.core.models;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the MJ &amp; Company editorial hero.
 *
 * <p>Deliberately not a text-left / image-right hero: the headline is set as two
 * typographic lines on an asymmetric baseline, and the artwork — when authored at all —
 * sits as a narrow cinematic band rather than a background wash. Sections that carry
 * conviction in words alone leave the artwork empty.</p>
 */
public interface MjHero extends Component {

    String getEyebrow();

    /** @return first headline line, set flush left */
    String getLineOne();

    /** @return second headline line, indented into the type column */
    String getLineTwo();

    /** @return the standfirst paragraph below the rule */
    String getStandfirst();

    /** @return DAM path of the cinematic band, or {@code null} when the hero is typographic only */
    String getFileReference();

    String getAlt();

    String getCtaText();

    /** @return resolved href for the call to action, {@code .html} appended for internal pages */
    String getCtaUrl();

    /** @return id for the call to action's data layer entry, or {@code null} */
    String getCtaId();

    /** @return Adobe Client Data Layer entry for the call to action, or {@code null} */
    String getCtaDataLayerJson();

    /** @return label for the scroll cue, or {@code null} when the cue is suppressed */
    String getScrollCue();

    boolean isEmpty();
}
