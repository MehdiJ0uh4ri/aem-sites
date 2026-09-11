package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Timeline: company history, or the milestone sequence of a programme.
 */
public interface Timeline extends Component {

    /** @return optional heading rendered above the sequence */
    String getHeading();

    /** @return the HTML element used for the heading, defaulting to {@code h2} */
    String getHeadingElement();

    /** @return the milestones, oldest first, never {@code null} */
    List<TimelineEntry> getEntries();

    /** @return {@code true} when there are no milestones to render */
    boolean isEmpty();
}
