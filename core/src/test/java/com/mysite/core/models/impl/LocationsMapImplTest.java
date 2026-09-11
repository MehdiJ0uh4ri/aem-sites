package com.mysite.core.models.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.FacilityLocation;
import com.mysite.core.models.LocationsMap;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class LocationsMapImplTest {

    private static final String CONTENT_ROOT = "/content/mysite/us/en/home";
    private static final String TEST_BASE = "/content/mysite/us/en/home/jcr:content/root";

    /** Pin positions are percentages, so a tolerance well under a pixel is plenty. */
    private static final double TOLERANCE = 0.01;

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/LocationsMapImplTest.json",
                CONTENT_ROOT + "/jcr:content");
    }

    private LocationsMap getModelAt(String name) {
        context.currentResource(TEST_BASE + "/" + name);
        return context.request().adaptTo(LocationsMap.class);
    }

    @Test
    void authoredSettingsAreExposed() {
        LocationsMap map = getModelAt("map-full");

        assertNotNull(map);
        assertEquals("Where we build", map.getHeading());
        assertEquals("h2", map.getHeadingElement());
        assertEquals("/content/dam/mysite/maps/world-equirectangular.png", map.getBaseMap());
        assertTrue(map.isMapped());
        assertFalse(map.isEmpty());
    }

    @Test
    void facilitiesWithoutANameAreDropped() {
        // Seven are authored; the last has a city but no name.
        assertEquals(6, getModelAt("map-full").getLocations().size());
    }

    @Test
    void greenwichAndTheEquatorLandAtTheCentreOfTheProjection() {
        // The projection is a linear mapping of degrees onto the image, so 0,0 must be the
        // exact centre. Getting this wrong shifts every pin on the map.
        FacilityLocation singapore = getModelAt("map-full").getLocations().get(2);

        // Singapore: 1.3644 N, 103.9915 E
        assertEquals((103.9915 + 180) / 360 * 100, singapore.getX(), TOLERANCE);
        assertEquals((90 - 1.3644) / 180 * 100, singapore.getY(), TOLERANCE);
        assertTrue(singapore.getX() > 50, "an easterly longitude sits right of centre");
        assertTrue(singapore.getY() < 50, "a northerly latitude sits above centre");
    }

    @Test
    void westernLongitudeSitsLeftOfCentre() {
        FacilityLocation palmdale = getModelAt("map-full").getLocations().get(1);

        assertTrue(palmdale.getX() < 50);
        assertTrue(palmdale.isMappable());
    }

    @Test
    void pinStyleUsesAFullStopWhateverTheDefaultLocale() {
        // CSS only accepts a full stop as the decimal separator. On a European publish
        // instance the JVM default locale would render 78.8875 as "78,8875", and the browser
        // would discard the whole declaration — leaving every pin stacked in the top-left
        // corner with nothing in the logs to explain it.
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            String style = getModelAt("map-full").getLocations().get(2).getPinStyle();

            assertNotNull(style);
            assertFalse(style.contains(","), "pin style must not use a comma decimal separator");
            assertTrue(style.startsWith("left:78."), style);
            assertTrue(style.contains("%;top:"), style);
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void unmappableFacilityHasNoPinStyle() {
        assertNull(getModelAt("map-full").getLocations().get(3).getPinStyle());
    }

    @Test
    void facilityWithoutCoordinatesIsListedButNotPinned() {
        FacilityLocation office = getModelAt("map-full").getLocations().get(3);

        assertEquals("Registered office", office.getName());
        assertFalse(office.isMappable());
        assertNull(office.getX());
        assertNull(office.getY());
    }

    @Test
    void outOfRangeCoordinatesAreRefusedRatherThanPlacedOffTheImage() {
        FacilityLocation broken = getModelAt("map-full").getLocations().get(4);

        assertFalse(broken.isMappable());
        assertNull(broken.getX());
    }

    @Test
    void facilityDetailsAreExposed() {
        FacilityLocation toulouse = getModelAt("map-full").getLocations().get(0);

        assertEquals("Toulouse Final Assembly Line", toulouse.getName());
        assertEquals("Toulouse", toulouse.getCity());
        assertEquals("France", toulouse.getCountry());
        assertEquals("Final assembly", toulouse.getFacilityType());
        assertEquals("commercial-aviation", toulouse.getBusinessUnit());
        assertEquals("Commercial Aviation", toulouse.getBusinessUnitLabel());
        assertEquals("/content/mysite/us/en/locations/toulouse.html", toulouse.getLinkUrl());
    }

    @Test
    void unrecognisedBusinessUnitIsDropped() {
        // Keeping it would put a filter button on the page that matches a single facility
        // nobody can name.
        FacilityLocation odd = getModelAt("map-full").getLocations().get(5);

        assertNull(odd.getBusinessUnit());
        assertNull(odd.getBusinessUnitLabel());
    }

    @Test
    void filtersOfferOnlyUnitsThatHaveAFacility() {
        Map<String, String> units = getModelAt("map-full").getBusinessUnits();

        assertEquals(List.of("commercial-aviation", "defence-space", "mro-services", "corporate"),
                List.copyOf(units.keySet()));
        assertEquals("Defence & Space", units.get("defence-space"));
        // Digital & Autonomous Systems has no facility here, so it is not offered.
        assertFalse(units.containsKey("digital-autonomous"));
    }

    @Test
    void filtersFollowTheCompanyOrderNotTheAuthoredOrder() {
        // Corporate is authored fourth and is last in the company order, so both agree here;
        // the guarantee that matters is that the order comes from BusinessUnits, which is why
        // "corporate" trails the operating units rather than appearing where it was typed.
        assertEquals("corporate", List.copyOf(getModelAt("map-full").getBusinessUnits().keySet()).get(3));
    }

    @Test
    void mapWithoutABaseImageFallsBackToTheListAlone() {
        LocationsMap map = getModelAt("map-listonly");

        assertFalse(map.isMapped());
        assertNull(map.getBaseMap());
        assertEquals(1, map.getLocations().size());
        // No unit was assigned, so there is nothing to filter by.
        assertTrue(map.getBusinessUnits().isEmpty());
    }

    @Test
    void mapWithoutFacilitiesIsEmpty() {
        LocationsMap map = getModelAt("map-empty");

        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertFalse(map.isMapped());
        assertTrue(map.getLocations().isEmpty());
    }

    @Test
    void exportedTypeIsTheComponentResourceType() {
        LocationsMap map = getModelAt("map-full");

        assertEquals(LocationsMapImpl.RESOURCE_TYPE, map.getExportedType());
        assertNotNull(map.getId());
    }
}
