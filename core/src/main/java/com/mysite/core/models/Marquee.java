package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Marquee: a continuously scrolling strip of short phrases.
 */
public interface Marquee extends Component {

    /** @return the authored phrases, never {@code null} */
    List<String> getItems();

    /** @return seconds for one full loop; clamped to a sane range */
    int getDuration();

    boolean isEmpty();
}
