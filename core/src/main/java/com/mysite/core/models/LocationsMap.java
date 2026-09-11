package com.mysite.core.models;

import java.util.List;
import java.util.Map;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Model for the Global Locations Map.
 *
 * <p>The list of facilities is the component; the map is an index into it. That ordering is
 * deliberate: a visitor looking for the Toulouse site needs an address, and a pin on an image
 * cannot give them one. The map is decorative in the accessibility sense — every pin's
 * information is present in the list beneath it, so nothing is only reachable by pointing.</p>
 */
public interface LocationsMap extends Component {

    /** @return optional heading rendered above the map */
    String getHeading();

    /** @return the HTML element used for the heading, defaulting to {@code h2} */
    String getHeadingElement();

    /** @return DAM path of the equirectangular base map, or {@code null} when only listing */
    String getBaseMap();

    /** @return {@code true} when a base map is set and at least one facility can be pinned */
    boolean isMapped();

    /** @return the facilities, never {@code null} */
    List<FacilityLocation> getLocations();

    /**
     * @return the business-unit filters to offer, as slug to display label, in a stable order.
     *         Built from the facilities actually authored, so a filter never offers a unit
     *         with no sites behind it.
     */
    Map<String, String> getBusinessUnits();

    /** @return {@code true} when there are no facilities to render */
    boolean isEmpty();
}
