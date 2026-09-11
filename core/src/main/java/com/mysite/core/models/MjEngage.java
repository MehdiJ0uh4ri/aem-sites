package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the closing section: one restrained invitation, split into lanes by audience
 * so a founder, an LP and a portfolio operator each find their own door.
 */
public interface MjEngage extends Component {

    String getEyebrow();

    String getStatement();

    List<MjEngageItem> getItems();

    /** @return DAM path of the full-bleed backdrop, or {@code null} */
    String getFileReference();

    String getAlt();

    boolean isEmpty();
}
