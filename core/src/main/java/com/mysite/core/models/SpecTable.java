package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Technical Specification Table.
 *
 * <p>Aerospace figures are published in both unit systems: an operator in Toulouse reads
 * metres and kilograms, one in Seattle reads feet and pounds. Rather than shipping two
 * tables, each row carries both values and the visitor switches between them. The switch is
 * a display concern only — both sets of figures are in the markup, so the table is complete
 * for search engines, print and anyone without JavaScript.</p>
 */
public interface SpecTable extends Component {

    /** Unit system identifier for the metric column. */
    String UNITS_METRIC = "metric";

    /** Unit system identifier for the imperial column. */
    String UNITS_IMPERIAL = "imperial";

    /** @return caption rendered as the table's accessible name */
    String getCaption();

    /** @return optional lead-in paragraph shown above the table */
    String getDescription();

    /**
     * @return the unit system shown first, {@link #UNITS_METRIC} or {@link #UNITS_IMPERIAL}.
     *         Defaults to metric.
     */
    String getDefaultUnits();

    /** @return {@code true} when at least one row has an imperial equivalent to switch to */
    boolean isUnitToggle();

    /** @return {@code true} when the visitor may reorder the table by clicking a header */
    boolean isSortable();

    /**
     * @return the export-control classification of these figures, for example
     *         {@code EAR99} or {@code ITAR — USML Category VIII}, or {@code null}
     */
    String getClassification();

    /** @return the rows in authored order, never {@code null} */
    List<SpecRow> getRows();

    /** @return {@code true} when there are no rows to render */
    boolean isEmpty();
}
