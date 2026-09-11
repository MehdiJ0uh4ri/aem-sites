package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Programme Teaser: a card linking to a programme landing page, carrying
 * enough of the aircraft's figures that a reader can tell the variants apart without
 * opening each one.
 */
public interface ProgramTeaser extends Component {

    /** @return the business unit or family the programme belongs to */
    String getEyebrow();

    String getTitle();

    String getDescription();

    /**
     * @return programme status, one of the values offered by the dialog — for example
     *         {@code in-service} — or {@code null}. Rendered as a badge and used as a
     *         CSS hook, so the value is a slug rather than the display text.
     */
    String getStatus();

    /** @return the display text for {@link #getStatus()}, or {@code null} */
    String getStatusLabel();

    /** @return DAM path of the programme still, or {@code null} */
    String getFileReference();

    String getAlt();

    /** @return the headline figures shown on the card, at most three, never {@code null} */
    List<StatsItem> getSpecs();

    /** @return resolved href for the card, {@code .html} appended for internal pages */
    String getLinkUrl();

    /** @return the call-to-action text; falls back to a generic label when unauthored */
    String getLinkText();

    /** @return {@code true} when there is neither a headline nor a link to render */
    boolean isEmpty();
}
