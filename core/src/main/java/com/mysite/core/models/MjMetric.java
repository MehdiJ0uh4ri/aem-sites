package com.mysite.core.models;

import java.util.List;

/**
 * One market metric in the {@link MjMarket} band.
 */
public interface MjMetric {

    /** Direction of travel, used for both the glyph and the screen-reader text. */
    String TREND_UP = "up";

    String TREND_DOWN = "down";

    String TREND_FLAT = "flat";

    String getLabel();

    String getValue();

    String getUnit();

    /** @return one of {@link #TREND_UP}, {@link #TREND_DOWN}, {@link #TREND_FLAT} */
    String getTrend();

    /** @return the period-on-period change, e.g. {@code +12%} */
    String getChange();

    /** @return editorial annotation printed under the figure */
    String getCommentary();

    String getSource();

    /** @return as-of date, already formatted for display */
    String getAsOf();

    /** @return the sparkline points as an SVG {@code polyline} value, or {@code null} */
    String getSparkline();

    /** @return the raw series behind the sparkline, exposed as the accessible table */
    List<String> getSeries();
}
