package com.mysite.core.models;

/**
 * One audience lane in the {@link MjEngage} closing section.
 */
public interface MjEngageItem {

    /** @return id unique within the page, derived from the parent component id */
    String getId();

    /** @return who this lane addresses, e.g. {@code Founders} */
    String getAudience();

    String getBody();

    String getCtaText();

    /** @return resolved href, or {@code null} when the lane has no call to action */
    String getCtaUrl();

    /** @return the Adobe Client Data Layer entry for this lane, or {@code null} */
    String getDataLayerJson();
}
