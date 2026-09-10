package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Investment Signal band: a statement of conviction set against a ledger
 * of figures.
 *
 * <p>The ledger is typographic, not a dashboard — the figures are rendered as editorial
 * display type on a ruled baseline grid, so a reader takes the claim first and the
 * numbers as evidence.</p>
 */
public interface MjSignal extends Component {

    String getEyebrow();

    /** @return the conviction statement, the section's own heading */
    String getStatement();

    List<MjSignalItem> getItems();

    /** @return source or as-of note printed under the ledger, or {@code null} */
    String getFootnote();

    boolean isEmpty();
}
