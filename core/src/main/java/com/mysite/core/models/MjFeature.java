package com.mysite.core.models;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the single-fragment feature module used by Featured Insight and by the
 * Case Study section.
 *
 * <p>It reads one Content Fragment and gives it the space of a magazine opener. Authored
 * overrides win over the fragment's own fields so an editor can retitle a feature on the
 * homepage without touching the source fragment.</p>
 */
public interface MjFeature extends Component {

    /** A published article: category, date, standfirst. */
    String VARIANT_INSIGHT = "insight";

    /** A case study: company, challenge and outcome set as a two-beat story. */
    String VARIANT_CASESTUDY = "casestudy";

    /** @return one of {@link #VARIANT_INSIGHT}, {@link #VARIANT_CASESTUDY} */
    String getVariant();

    String getEyebrow();

    /** @return the classifying line — category, or the company for a case study */
    String getKicker();

    String getTitle();

    /** @return the standfirst, or the case study's challenge */
    String getStandfirst();

    /** @return the outcome line; case study variant only, {@code null} otherwise */
    String getOutcome();

    /** @return publication date or investment year, already formatted for display */
    String getMeta();

    String getFileReference();

    String getAlt();

    String getCtaText();

    String getCtaUrl();

    String getCtaId();

    String getCtaDataLayerJson();

    /**
     * @return the element this section's heading is written with, {@code h1}..{@code h4}.
     *         Authorable; left on Auto the opener of the page takes the {@code h1}.
     */
    String getHeadingTag();

    boolean isEmpty();
}
