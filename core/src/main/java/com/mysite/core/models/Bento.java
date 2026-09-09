package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Bento grid: an asymmetric image-led feature grid.
 */
public interface Bento extends Component {

    String getEyebrow();

    String getHeading();

    String getDescription();

    List<BentoCard> getCards();

    boolean isEmpty();
}
