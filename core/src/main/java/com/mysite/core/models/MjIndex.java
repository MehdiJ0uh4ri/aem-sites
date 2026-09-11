package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the dynamic index that drives Portfolio, Deals and Insights.
 *
 * <p>One component rather than three: the three sections differ in typography and in
 * which Content Fragment elements they surface, not in behaviour. The variant selects
 * both the element mapping and the layout, which keeps a single dialog, a single model
 * and a single stylesheet for what would otherwise be three near-identical components.</p>
 */
public interface MjIndex extends Component {

    /** Cinematic cards scrolled horizontally — Portfolio. */
    String VARIANT_PORTFOLIO = "portfolio";

    /** A ruled ledger of transactions — Deals. */
    String VARIANT_DEALS = "deals";

    /** An editorial list with a lead item — Insights. */
    String VARIANT_INSIGHTS = "insights";

    String getEyebrow();

    String getHeading();

    String getIntro();

    /** @return one of {@link #VARIANT_PORTFOLIO}, {@link #VARIANT_DEALS}, {@link #VARIANT_INSIGHTS} */
    String getVariant();

    List<MjIndexItem> getItems();

    /**
     * @return DAM path of the image held beside the list, or {@code null}. The deals
     *         variant pins it as a full-height column while the ledger scrolls past it.
     */
    String getFileReference();

    String getAlt();

    String getCtaText();

    /** @return resolved href for the "see all" link, or {@code null} */
    String getCtaUrl();

    boolean isEmpty();
}
