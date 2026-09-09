package com.mysite.core.models;

/**
 * A single statistic rendered by the {@link StatsBar} component.
 *
 * <p>The authored value is a free-form string (for example {@code 98%}, {@code $1.2M} or
 * {@code 1,200+}). It is split into a numeric part and its surrounding decoration so that
 * the client-side count-up animation can interpolate the number without having to re-parse
 * the rendered markup.</p>
 */
public interface StatsItem {

    /**
     * @return the authored value exactly as entered, never {@code null} for a valid item
     */
    String getValue();

    /**
     * @return the numeric part of {@link #getValue()}, or {@code null} when the value
     *         contains no number (for example {@code N/A}) and must not be animated
     */
    Double getNumber();

    /**
     * @return the characters preceding the number, for example {@code $}; empty when there are none
     */
    String getPrefix();

    /**
     * @return the characters following the number, for example {@code %}; empty when there are none
     */
    String getSuffix();

    /**
     * @return the short caption shown underneath the value
     */
    String getLabel();

    /**
     * @return an optional longer explanation, or {@code null} when not authored
     */
    String getDescription();
}
