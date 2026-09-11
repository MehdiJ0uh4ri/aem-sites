package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Portfolio Grid: the full, browsable index of portfolio companies.
 *
 * <p>Distinct from the {@code portfolio} variant of {@link MjIndex}, which is a rail — a
 * preview of a handful, scrolled sideways, with a lead card that reads as editorial
 * selection. That is the wrong form for a complete set: on an index a featured card asks
 * a question the page cannot answer, and a card that changes size when a filter is applied
 * makes the result set harder to read, not easier.</p>
 *
 * <p>The facet lists are derived from the companies actually present, so a sector added to
 * the DAM appears as a filter without anyone editing a dialog.</p>
 */
public interface MjGrid extends Component {

    String getEyebrow();

    String getHeading();

    String getIntro();

    List<MjGridItem> getItems();

    /** @return every sector present in the data, in display order */
    List<String> getSectors();

    /** @return every stage present, ordered by how early it is rather than alphabetically */
    List<String> getStages();

    /** @return every investment year present, most recent first */
    List<String> getYears();

    /** @return whether the secondary Stage and Year controls are shown */
    boolean isShowSecondaryFilters();

    /** @return the message shown when a filter combination matches nothing */
    String getEmptyText();

    boolean isEmpty();
}
