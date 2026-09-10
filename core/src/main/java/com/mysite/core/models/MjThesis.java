package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Thesis / Point of View section: a numbered set of beliefs about markets
 * and investing, set as a reading column with the ordinals hung in the margin.
 */
public interface MjThesis extends Component {

    String getEyebrow();

    String getHeading();

    List<MjThesisItem> getItems();

    boolean isEmpty();
}
