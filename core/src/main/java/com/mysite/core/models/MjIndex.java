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

    /** The same rail as Portfolio, reading Sector fragments — Investment Approach. */
    String VARIANT_SECTORS = "sectors";

    String getEyebrow();

    String getHeading();

    String getIntro();

    /**
     * @return one of {@link #VARIANT_PORTFOLIO}, {@link #VARIANT_DEALS},
     *         {@link #VARIANT_INSIGHTS}, {@link #VARIANT_SECTORS}
     */
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

    /**
     * @return the element this section's heading is written with, {@code h1}..{@code h4}.
     *         Authorable; left on Auto the opener of the page takes the {@code h1}.
     */
    String getHeadingTag();

    boolean isEmpty();
}
