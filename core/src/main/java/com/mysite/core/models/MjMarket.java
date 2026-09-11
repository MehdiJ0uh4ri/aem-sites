package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Market Intelligence band.
 *
 * <p>Metrics come from Content Fragments so the data is authored once and reused across
 * the homepage and the Market Intelligence page. Each metric carries its own commentary:
 * the section is written as market comment illustrated by figures, not as a dashboard.</p>
 */
public interface MjMarket extends Component {

    String getEyebrow();

    String getHeading();

    String getIntro();

    List<MjMetric> getMetrics();

    /**
     * @return the element this section's heading is written with, {@code h1}..{@code h4}.
     *         Authorable; left on Auto the opener of the page takes the {@code h1}.
     */
    String getHeadingTag();

    boolean isEmpty();
}
