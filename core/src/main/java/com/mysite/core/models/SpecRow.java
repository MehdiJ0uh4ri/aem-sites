package com.mysite.core.models;

/**
 * One line of a {@link SpecTable}: a named characteristic and its value in both unit systems.
 */
public interface SpecRow {

    /** @return the characteristic being stated, for example "Maximum take-off weight" */
    String getLabel();

    /** @return optional grouping, for example "Dimensions" or "Powerplant" */
    String getCategory();

    /** @return the value in metric units, without the unit itself */
    String getMetricValue();

    /** @return the metric unit symbol, for example {@code kg} */
    String getMetricUnit();

    /** @return the value in imperial units, or {@code null} when the figure is unitless */
    String getImperialValue();

    /** @return the imperial unit symbol, for example {@code lb} */
    String getImperialUnit();

    /**
     * @return {@code true} when this row has an imperial equivalent. Unitless figures
     *         (seat count, engine count) read the same in both systems and are rendered
     *         once rather than duplicated.
     */
    boolean isDualUnit();

    /** @return a qualifier shown beneath the value, for example "at ISA +15 °C" */
    String getNote();

    /**
     * @return the leading number of the metric value as a sortable double, or {@code null}
     *         when the value is not numeric. Lets the table sort 980 before 12,400 instead
     *         of the other way round, which is what a string comparison would give.
     */
    Double getSortKey();
}
