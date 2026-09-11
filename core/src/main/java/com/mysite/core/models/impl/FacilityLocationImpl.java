package com.mysite.core.models.impl;

import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.mysite.core.models.FacilityLocation;

@Model(adaptables = Resource.class,
       adapters = FacilityLocation.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class FacilityLocationImpl implements FacilityLocation {

    @ValueMapValue
    private String name;

    @ValueMapValue
    private String city;

    @ValueMapValue
    private String country;

    @ValueMapValue
    private String address;

    @ValueMapValue
    private String businessUnit;

    @ValueMapValue
    private String facilityType;

    @ValueMapValue
    private Double latitude;

    @ValueMapValue
    private Double longitude;

    @ValueMapValue
    private String link;

    private Double x;
    private Double y;

    @PostConstruct
    private void init() {
        if (latitude == null || longitude == null
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            // Out-of-range coordinates would place a pin outside the image. The facility is
            // still listed — an address with no pin is far better than a pin in the margin.
            return;
        }
        // Equirectangular projection, which is what the base map must use for this to line up:
        // longitude maps linearly across the width, latitude linearly down the height.
        x = (longitude + 180d) / 360d * 100d;
        y = (90d - latitude) / 180d * 100d;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public String getCountry() {
        return country;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public String getBusinessUnit() {
        return BusinessUnits.slugOrNull(businessUnit);
    }

    @Override
    public String getBusinessUnitLabel() {
        return BusinessUnits.label(businessUnit);
    }

    @Override
    public String getFacilityType() {
        return facilityType;
    }

    @Override
    public Double getX() {
        return x;
    }

    @Override
    public Double getY() {
        return y;
    }

    @Override
    public boolean isMappable() {
        return x != null && y != null;
    }

    @Override
    public String getPinStyle() {
        if (!isMappable()) {
            return null;
        }
        // Locale.ENGLISH pins the decimal separator: CSS only accepts a full stop, and the
        // JVM default locale on a European publish instance would otherwise emit a comma.
        return String.format(Locale.ENGLISH, "left:%.4f%%;top:%.4f%%", x, y);
    }

    @Override
    public String getLinkUrl() {
        return LinkUtil.toUrl(link);
    }

    /** @return {@code true} when there is enough here to render a usable list entry */
    boolean isRenderable() {
        return StringUtils.isNotBlank(name);
    }
}
