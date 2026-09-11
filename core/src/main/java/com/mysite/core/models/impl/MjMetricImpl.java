package com.mysite.core.models.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.dam.cfm.ContentFragment;
import com.mysite.core.models.MjMetric;

/**
 * One market metric read from a fragment, including the geometry for its sparkline.
 */
class MjMetricImpl implements MjMetric {

    /** The sparkline's coordinate space; the SVG scales to whatever box the CSS gives it. */
    private static final double VIEW_WIDTH = 100d;

    private static final double VIEW_HEIGHT = 28d;

    private final String label;
    private final String value;
    private final String unit;
    private final String trend;
    private final String change;
    private final String commentary;
    private final String source;
    private final String asOf;
    private final List<String> series;
    private final String sparkline;
    private final String sparklineArea;

    MjMetricImpl(ContentFragment fragment, Resource resource) {
        this.label = FragmentReader.text(fragment, resource, "metric", "label", "title");
        this.value = FragmentReader.text(fragment, resource, "value");
        this.unit = FragmentReader.text(fragment, resource, "unit");
        this.trend = normaliseTrend(FragmentReader.text(fragment, resource, "trend"));
        this.change = FragmentReader.text(fragment, resource, "change");
        this.commentary = FragmentReader.text(fragment, resource, "commentary");
        this.source = FragmentReader.text(fragment, resource, "source");
        this.asOf = FragmentReader.date(fragment, resource, "date", "asOf");
        this.series = parseSeries(FragmentReader.text(fragment, resource, "series"));
        this.sparkline = plot(this.series);
        this.sparklineArea = close(this.sparkline);
    }

    boolean hasContent() {
        return StringUtils.isNotBlank(label) && StringUtils.isNotBlank(value);
    }

    private static String normaliseTrend(String raw) {
        if (raw == null) {
            return TREND_FLAT;
        }
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if (TREND_UP.equals(lower) || TREND_DOWN.equals(lower)) {
            return lower;
        }
        return TREND_FLAT;
    }

    private static List<String> parseSeries(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (String token : raw.split("[,\\s]+")) {
            if (StringUtils.isNotBlank(token)) {
                values.add(token.trim());
            }
        }
        return Collections.unmodifiableList(values);
    }

    /**
     * Turns the series into an SVG {@code polyline} points attribute.
     *
     * <p>Two points are the minimum that draws a line. The series is normalised to its own
     * min and max, so the sparkline shows shape rather than absolute level — which is why
     * the figure beside it, not the line, carries the value.</p>
     *
     * @return the points string, or {@code null} when there is nothing plottable
     */
    private static String plot(List<String> values) {
        if (values.size() < 2) {
            return null;
        }
        double[] numbers = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            try {
                numbers[i] = Double.parseDouble(values.get(i));
            } catch (NumberFormatException notANumber) {
                // A single unparseable point would distort the whole line: drop the chart
                // and let the figure and commentary stand alone.
                return null;
            }
        }
        double min = Arrays.stream(numbers).min().orElse(0d);
        double max = Arrays.stream(numbers).max().orElse(0d);
        double span = max - min;

        StringBuilder points = new StringBuilder();
        for (int i = 0; i < numbers.length; i++) {
            double x = VIEW_WIDTH * i / (numbers.length - 1d);
            // A flat series has no span to normalise against; draw it down the middle.
            double ratio = span == 0d ? 0.5d : (numbers[i] - min) / span;
            double y = VIEW_HEIGHT - (ratio * VIEW_HEIGHT);
            if (i > 0) {
                points.append(' ');
            }
            points.append(String.format(Locale.ROOT, "%.2f,%.2f", x, y));
        }
        return points.toString();
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public String getTrend() {
        return trend;
    }

    @Override
    public String getChange() {
        return change;
    }

    @Override
    public String getCommentary() {
        return commentary;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getAsOf() {
        return asOf;
    }

    /**
     * Closes the line down to the baseline so it can be filled.
     *
     * <p>Derived from the line rather than plotted again: two independent passes over the
     * same series would be two chances for the fill and the stroke to disagree.</p>
     *
     * @param points the polyline points, or {@code null}
     * @return polygon points, or {@code null}
     */
    private static String close(String points) {
        if (points == null) {
            return null;
        }
        return String.format(Locale.ROOT, "0.00,%.2f %s %.2f,%.2f",
                VIEW_HEIGHT, points, VIEW_WIDTH, VIEW_HEIGHT);
    }

    @Override
    public String getSparkline() {
        return sparkline;
    }

    @Override
    public String getSparklineArea() {
        return sparklineArea;
    }

    @Override
    public List<String> getSeries() {
        return series;
    }
}
