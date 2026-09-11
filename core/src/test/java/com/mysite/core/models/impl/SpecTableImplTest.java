package com.mysite.core.models.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.SpecRow;
import com.mysite.core.models.SpecTable;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class SpecTableImplTest {

    private static final String CONTENT_ROOT = "/content/mysite/us/en/home";
    private static final String TEST_BASE = "/content/mysite/us/en/home/jcr:content/root";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/SpecTableImplTest.json",
                CONTENT_ROOT + "/jcr:content");
    }

    private SpecTable getModelAt(String name) {
        context.currentResource(TEST_BASE + "/" + name);
        return context.request().adaptTo(SpecTable.class);
    }

    @Test
    void authoredTableSettingsAreExposed() {
        SpecTable table = getModelAt("spectable-full");

        assertNotNull(table);
        assertEquals("AV-380X technical specification", table.getCaption());
        assertEquals("Figures quoted at ISA, sea level, baseline variant.", table.getDescription());
        assertEquals(SpecTable.UNITS_IMPERIAL, table.getDefaultUnits());
        assertEquals("EAR99", table.getClassification());
        assertTrue(table.isSortable());
        assertFalse(table.isEmpty());
    }

    @Test
    void unitSystemFallsBackToMetric() {
        SpecTable table = getModelAt("spectable-defaults");

        assertEquals(SpecTable.UNITS_METRIC, table.getDefaultUnits());
        assertFalse(table.isSortable());
        assertNull(table.getClassification());
    }

    @Test
    void rowsMissingALabelOrAFigureAreDropped() {
        SpecTable table = getModelAt("spectable-full");

        // Six rows are authored; row4 has no figure and row5 has no label.
        assertEquals(4, table.getRows().size());
    }

    @Test
    void dualUnitRowExposesBothSystems() {
        SpecRow row = getModelAt("spectable-full").getRows().get(0);

        assertEquals("Maximum take-off weight", row.getLabel());
        assertEquals("Performance", row.getCategory());
        assertEquals("12,400", row.getMetricValue());
        assertEquals("kg", row.getMetricUnit());
        assertEquals("27,337", row.getImperialValue());
        assertEquals("lb", row.getImperialUnit());
        assertEquals("at ISA +15 °C", row.getNote());
        assertTrue(row.isDualUnit());
    }

    @Test
    void unitlessRowIsNotDualUnit() {
        SpecRow row = getModelAt("spectable-full").getRows().get(2);

        assertEquals("Engines", row.getLabel());
        assertFalse(row.isDualUnit());
        assertNull(row.getImperialValue());
    }

    @Test
    void sortKeyIgnoresThousandsSeparators() {
        // 980 cm must sort below 12,400 kg; a string comparison would put "980" last.
        assertEquals(12400d, getModelAt("spectable-full").getRows().get(0).getSortKey());
        assertEquals(980d, getModelAt("spectable-full").getRows().get(1).getSortKey());
    }

    @Test
    void nonNumericFigureHasNoSortKey() {
        SpecRow row = getModelAt("spectable-full").getRows().get(3);

        assertEquals("Classified", row.getMetricValue());
        assertNull(row.getSortKey());
    }

    @Test
    void unitToggleIsOfferedOnlyWhenARowCanSwitch() {
        assertTrue(getModelAt("spectable-full").isUnitToggle());
        // The minimal table has a single unitless row, so a switch would change nothing.
        assertFalse(getModelAt("spectable-defaults").isUnitToggle());
    }

    @Test
    void tableWithoutRowsIsEmpty() {
        SpecTable table = getModelAt("spectable-empty");

        assertNotNull(table);
        assertTrue(table.isEmpty());
        assertTrue(table.getRows().isEmpty());
        assertFalse(table.isUnitToggle());
    }

    @Test
    void rowsAreImmutable() {
        SpecTable table = getModelAt("spectable-full");

        assertThrows(UnsupportedOperationException.class, () -> table.getRows().clear());
    }

    @Test
    void exportedTypeIsTheComponentResourceType() {
        SpecTable table = getModelAt("spectable-full");

        assertEquals(SpecTableImpl.RESOURCE_TYPE, table.getExportedType());
        assertNotNull(table.getId());
    }
}
