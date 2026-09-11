package com.mysite.core.models.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.util.AbstractComponentImpl;
import com.mysite.core.models.FacilityLocation;
import com.mysite.core.models.LocationsMap;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { LocationsMap.class, ComponentExporter.class },
       resourceType = LocationsMapImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class LocationsMapImpl extends AbstractComponentImpl implements LocationsMap {

    public static final String RESOURCE_TYPE = "mysite/components/locationsmap";

    /** Name of the composite multifield node holding the facilities. */
    private static final String NN_LOCATIONS = "locations";

    private static final String DEFAULT_HEADING_ELEMENT = "h2";

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String headingElement;

    @ValueMapValue
    private String baseMap;

    @ChildResource(name = NN_LOCATIONS)
    private Resource locationsResource;

    private List<FacilityLocation> locations;
    private Map<String, String> businessUnits;

    @PostConstruct
    private void init() {
        locations = MultifieldUtil.readItems(locationsResource, FacilityLocation.class,
                location -> StringUtils.isNotBlank(location.getName()));
        businessUnits = buildFilters();
    }

    /**
     * Only the units that actually have a facility here, in the company's standard order. A
     * filter offering a unit with no sites behind it produces an empty list and reads as a
     * bug.
     */
    private Map<String, String> buildFilters() {
        Set<String> present = locations.stream()
                .map(FacilityLocation::getBusinessUnit)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        Map<String, String> filters = new LinkedHashMap<>();
        BusinessUnits.all().forEach((slug, label) -> {
            if (present.contains(slug)) {
                filters.put(slug, label);
            }
        });
        return java.util.Collections.unmodifiableMap(filters);
    }

    @Override
    public String getHeading() {
        return heading;
    }

    @Override
    public String getHeadingElement() {
        // The dialog already restricts the choice, so only the unauthored case is handled here.
        return StringUtils.defaultIfBlank(headingElement, DEFAULT_HEADING_ELEMENT);
    }

    @Override
    public String getBaseMap() {
        return baseMap;
    }

    @Override
    public boolean isMapped() {
        // Rendering the map frame with no pins in it would look broken rather than empty.
        return StringUtils.isNotBlank(baseMap)
                && locations.stream().anyMatch(FacilityLocation::isMappable);
    }

    @Override
    public List<FacilityLocation> getLocations() {
        return locations;
    }

    @Override
    public Map<String, String> getBusinessUnits() {
        return businessUnits;
    }

    @Override
    public boolean isEmpty() {
        return locations.isEmpty();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
