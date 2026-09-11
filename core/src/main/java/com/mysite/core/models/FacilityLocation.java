package com.mysite.core.models;

/**
 * One facility pinned on a {@link LocationsMap}.
 */
public interface FacilityLocation {

    String getName();

    /** @return the city and country, for display under the name */
    String getCity();

    String getCountry();

    /** @return the postal address, or {@code null} */
    String getAddress();

    /**
     * @return the business unit this facility belongs to, as a slug. Used both as the filter
     *         value and as a CSS hook, so relabelling a unit does not require re-tagging
     *         every facility.
     */
    String getBusinessUnit();

    /** @return the display text for {@link #getBusinessUnit()}, or {@code null} */
    String getBusinessUnitLabel();

    /** @return what the site does, for example {@code Final assembly}, or {@code null} */
    String getFacilityType();

    /**
     * @return horizontal position of the pin as a percentage of the base map's width, or
     *         {@code null} when the coordinates are missing or out of range
     */
    Double getX();

    /** @return vertical position of the pin as a percentage of the base map's height */
    Double getY();

    /** @return {@code true} when the facility can be pinned on the map as well as listed */
    boolean isMappable();

    /**
     * @return the CSS declarations positioning the pin, for example
     *         {@code left:78.9%;top:44.2%}, or {@code null} when the facility is not
     *         mappable. Built here rather than interpolated in the template because HTL
     *         blanks any expression in a style attribute that has no explicit context, and
     *         because the numbers must be formatted in a fixed locale: a French default
     *         locale would render 12.3 as "12,3" and produce CSS the browser discards.
     */
    String getPinStyle();

    /** @return resolved href of the facility's page, or {@code null} */
    String getLinkUrl();
}
