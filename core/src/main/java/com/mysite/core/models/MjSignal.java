package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Investment Signal band: a statement of conviction set against a ledger
 * of figures.
 *
 * <p>The ledger is typographic, not a dashboard — the figures are set at display scale,
 * overlapping and running off the right edge, against a tall image column on the left.
 * The composition is the point: a reader should register the numbers as a mass before
 * reading any single one.</p>
 */
public interface MjSignal extends Component {

    String getEyebrow();

    /** @return the conviction statement, the section's own heading */
    String getStatement();

    List<MjSignalItem> getItems();

    /** @return source or as-of note printed under the ledger, or {@code null} */
    String getFootnote();

    /** @return DAM path of the tall image column, or {@code null} when the band is type only */
    String getFileReference();

    String getAlt();

    /**
     * @return the element this section's heading is written with, {@code h1}..{@code h4}.
     *         Authorable; left on Auto the opener of the page takes the {@code h1}.
     */
    String getHeadingTag();

    boolean isEmpty();
}
