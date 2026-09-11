package com.mysite.core.models;

/**
 * One milestone in a {@link Timeline}.
 */
public interface TimelineEntry {

    /**
     * @return the period the milestone belongs to, as authored: a year, a quarter, or a
     *         span such as {@code 1994–97}. Kept as free text because programme histories
     *         are rarely precise to the day, and a date picker would force a false precision.
     */
    String getPeriod();

    String getTitle();

    String getDescription();

    /** @return DAM path of an accompanying still, or {@code null} */
    String getFileReference();

    String getAlt();

    /**
     * @return {@code true} when this milestone should be picked out as a turning point,
     *         for example a first flight or a certification
     */
    boolean isHighlighted();
}
