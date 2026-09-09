package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Stats Bar component, which renders a heading and a row of key figures.
 *
 * <p>Extends the Core Components {@link Component} interface so the component participates in
 * the Adobe Client Data Layer and gets a stable, unique id in the same way the Core Components
 * do (both are supplied by {@code AbstractComponentImpl}).</p>
 */
public interface StatsBar extends Component {

    /**
     * @return the authored heading, or {@code null} when none was set
     */
    String getHeading();

    /**
     * @return the HTML element to use for the heading, defaulting to {@code h2}
     */
    String getHeadingElement();

    /**
     * @return the authored statistics, never {@code null} but possibly empty
     */
    List<StatsItem> getItems();

    /**
     * @return {@code true} when there is nothing to render, so the editor can show a placeholder
     */
    boolean isEmpty();
}
