package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for Related Content.
 *
 * <p>Deliberately generic. It is given one source fragment and follows whatever references
 * that fragment carries — a Thesis points at sectors, portfolio companies and insights; a
 * Sector points back at thesis points, companies and market metrics; an Insight points at
 * other insights. The component does not know or care which: it reads the source's stored
 * values, keeps the ones that resolve to another fragment, and asks each one what it is.</p>
 *
 * <p>That is the whole reason it is not a per-type component. The relationships already
 * exist in the content model, so the only thing left to decide is how to set them.</p>
 */
public interface MjRelated extends Component {

    String getEyebrow();

    String getHeading();

    List<MjRelatedItem> getItems();

    boolean isEmpty();
}
