package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Slides showcase: a scroll-snapping set of slides rendered in one of
 * several layouts.
 */
public interface Slides extends Component {

    /** Side-by-side artwork and copy, alternating sides down the list. */
    String LAYOUT_SPLIT = "split";

    /** Full-bleed artwork with the copy overlaid. */
    String LAYOUT_SPOTLIGHT = "spotlight";

    /** A horizontally scrolling row of cards. */
    String LAYOUT_CARDS = "cards";

    String getEyebrow();

    String getHeading();

    String getDescription();

    /** @return one of {@link #LAYOUT_SPLIT}, {@link #LAYOUT_SPOTLIGHT}, {@link #LAYOUT_CARDS} */
    String getLayout();

    List<Slide> getSlides();

    boolean isEmpty();
}
