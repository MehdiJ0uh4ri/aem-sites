package com.mysite.core.models.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.Timeline;
import com.mysite.core.models.TimelineEntry;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class TimelineImplTest {

    private static final String CONTENT_ROOT = "/content/mysite/us/en/home";
    private static final String TEST_BASE = "/content/mysite/us/en/home/jcr:content/root";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/TimelineImplTest.json",
                CONTENT_ROOT + "/jcr:content");
    }

    private Timeline getModelAt(String name) {
        context.currentResource(TEST_BASE + "/" + name);
        return context.request().adaptTo(Timeline.class);
    }

    @Test
    void authoredHeadingAndElementAreExposed() {
        Timeline timeline = getModelAt("timeline-full");

        assertNotNull(timeline);
        assertEquals("Seventy years of flight", timeline.getHeading());
        assertEquals("h3", timeline.getHeadingElement());
        assertFalse(timeline.isEmpty());
    }

    @Test
    void headingElementFallsBackToH2() {
        assertEquals("h2", getModelAt("timeline-defaults").getHeadingElement());
    }

    @Test
    void entriesWithoutAPeriodAreDropped() {
        // Four are authored; the fourth has a title but no period, which is what places it.
        assertEquals(3, getModelAt("timeline-full").getEntries().size());
    }

    @Test
    void periodIsFreeTextSoSpansAndQuartersSurvive() {
        // A date picker would force a precision programme histories do not have.
        assertEquals("1956", getModelAt("timeline-full").getEntries().get(0).getPeriod());
        assertEquals("1994-97", getModelAt("timeline-full").getEntries().get(1).getPeriod());
        assertEquals("Q3 2021", getModelAt("timeline-full").getEntries().get(2).getPeriod());
    }

    @Test
    void milestoneDetailsAreExposed() {
        TimelineEntry entry = getModelAt("timeline-full").getEntries().get(0);

        assertEquals("Founded in Toulouse", entry.getTitle());
        assertEquals("Four engineers and a leased hangar.", entry.getDescription());
        assertEquals("/content/dam/mysite/history/1956.jpg", entry.getFileReference());
        assertEquals("The original Toulouse hangar", entry.getAlt());
        assertFalse(entry.isHighlighted());
    }

    @Test
    void turningPointIsFlagged() {
        assertTrue(getModelAt("timeline-full").getEntries().get(1).isHighlighted());
    }

    @Test
    void altDefaultsToEmptySoTheImageIsDecorative() {
        // The milestone text already carries the meaning, so an unauthored alt is empty
        // rather than null — which would make HTL drop the attribute entirely.
        assertEquals("", getModelAt("timeline-full").getEntries().get(2).getAlt());
    }

    @Test
    void entryWithoutADescriptionIsStillRendered() {
        TimelineEntry entry = getModelAt("timeline-full").getEntries().get(1);

        assertEquals("AV-200 development", entry.getTitle());
        assertNull(entry.getDescription());
        assertNull(entry.getFileReference());
    }

    @Test
    void timelineWithoutEntriesIsEmpty() {
        Timeline timeline = getModelAt("timeline-empty");

        assertNotNull(timeline);
        assertTrue(timeline.isEmpty());
        assertTrue(timeline.getEntries().isEmpty());
    }

    @Test
    void exportedTypeIsTheComponentResourceType() {
        Timeline timeline = getModelAt("timeline-full");

        assertEquals(TimelineImpl.RESOURCE_TYPE, timeline.getExportedType());
        assertNotNull(timeline.getId());
    }
}
