package com.mysite.core.models.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.StatsBar;
import com.mysite.core.models.StatsItem;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class StatsBarImplTest {

    private static final String CONTENT_ROOT = "/content/mysite/us/en/home";
    private static final String TEST_BASE = "/content/mysite/us/en/home/jcr:content/root";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/StatsBarImplTest.json",
                CONTENT_ROOT + "/jcr:content");
    }

    private StatsBar getModelAt(String name) {
        context.currentResource(TEST_BASE + "/" + name);
        return context.request().adaptTo(StatsBar.class);
    }

    @Test
    void authoredHeadingAndElementAreExposed() {
        StatsBar stats = getModelAt("statsbar-full");

        assertNotNull(stats);
        assertEquals("By the numbers", stats.getHeading());
        assertEquals("h3", stats.getHeadingElement());
        assertFalse(stats.isEmpty());
    }

    @Test
    void headingElementFallsBackToH2() {
        StatsBar stats = getModelAt("statsbar-defaults");

        assertNotNull(stats);
        assertNull(stats.getHeading());
        assertEquals("h2", stats.getHeadingElement());
    }

    @Test
    void itemsWithoutAValueAreDropped() {
        StatsBar stats = getModelAt("statsbar-full");

        // item4 has a label but no value, so four of the five authored items survive.
        assertEquals(4, stats.getItems().size());
    }

    @Test
    void percentageValueIsSplitIntoNumberAndSuffix() {
        StatsItem item = getModelAt("statsbar-full").getItems().get(0);

        assertEquals("98%", item.getValue());
        assertEquals(98d, item.getNumber());
        assertEquals("", item.getPrefix());
        assertEquals("%", item.getSuffix());
        assertEquals("Uptime", item.getLabel());
        assertEquals("Measured over the last twelve months.", item.getDescription());
    }

    @Test
    void currencyValueIsSplitIntoPrefixNumberAndSuffix() {
        StatsItem item = getModelAt("statsbar-full").getItems().get(1);

        assertEquals(1.2d, item.getNumber());
        assertEquals("$", item.getPrefix());
        assertEquals("M", item.getSuffix());
        assertNull(item.getDescription());
    }

    @Test
    void thousandsSeparatorIsStrippedFromTheNumber() {
        StatsItem item = getModelAt("statsbar-full").getItems().get(2);

        assertEquals(1200d, item.getNumber());
        assertEquals("", item.getPrefix());
        assertEquals("+", item.getSuffix());
        // The authored formatting is preserved for rendering.
        assertEquals("1,200+", item.getValue());
    }

    @Test
    void valueWithoutDigitsIsNotAnimatable() {
        StatsItem item = getModelAt("statsbar-full").getItems().get(3);

        assertEquals("N/A", item.getValue());
        assertNull(item.getNumber());
        assertEquals("", item.getPrefix());
        assertEquals("", item.getSuffix());
    }

    @Test
    void componentWithoutItemsIsEmpty() {
        StatsBar stats = getModelAt("statsbar-empty");

        assertNotNull(stats);
        assertTrue(stats.isEmpty());
        List<StatsItem> items = stats.getItems();
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    void exportedTypeIsTheComponentResourceType() {
        StatsBar stats = getModelAt("statsbar-full");

        assertEquals(StatsBarImpl.RESOURCE_TYPE, stats.getExportedType());
        assertNotNull(stats.getId());
    }
}
