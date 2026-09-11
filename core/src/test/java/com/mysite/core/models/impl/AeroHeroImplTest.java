package com.mysite.core.models.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.AeroHero;
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
class AeroHeroImplTest {

    private static final String CONTENT_ROOT = "/content/mysite/us/en/home";
    private static final String TEST_BASE = "/content/mysite/us/en/home/jcr:content/root";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/AeroHeroImplTest.json",
                CONTENT_ROOT + "/jcr:content");
    }

    private AeroHero getModelAt(String name) {
        context.currentResource(TEST_BASE + "/" + name);
        return context.request().adaptTo(AeroHero.class);
    }

    @Test
    void authoredCopyAndComplianceNoticeAreExposed() {
        AeroHero hero = getModelAt("aerohero-video");

        assertNotNull(hero);
        assertEquals("Commercial Aviation", hero.getEyebrow());
        assertEquals("Engineered for the next 100,000 hours", hero.getTitle());
        assertEquals("The AV-380X family in service across 64 countries.", hero.getDescription());
        assertEquals("Technical data on this page is classified EAR99 and is not subject to ITAR.",
                hero.getComplianceNotice());
        assertFalse(hero.isEmpty());
    }

    @Test
    void videoBackdropUsesTheStillAsItsPoster() {
        AeroHero hero = getModelAt("aerohero-video");

        assertTrue(hero.isVideo());
        assertEquals("/content/dam/mysite/programs/av-380x/hero.mp4", hero.getVideoReference());
        assertEquals("/content/dam/mysite/programs/av-380x/hero.jpg", hero.getPosterReference());
    }

    @Test
    void imageBackdropIsNotTreatedAsVideo() {
        AeroHero hero = getModelAt("aerohero-image");

        assertFalse(hero.isVideo());
        assertNull(hero.getVideoReference());
        assertEquals("/content/dam/mysite/units/defence/hero.jpg", hero.getFileReference());
    }

    @Test
    void heroWithoutAnyArtworkHasNoPoster() {
        assertNull(getModelAt("aerohero-empty").getPosterReference());
    }

    @Test
    void altDefaultsToEmptySoTheBackdropIsDecorative() {
        assertEquals("AV-380X on final approach", getModelAt("aerohero-video").getAlt());
        assertEquals("", getModelAt("aerohero-image").getAlt());
    }

    @Test
    void internalCtaGetsHtmlExtensionWhileAssetPathDoesNot() {
        AeroHero hero = getModelAt("aerohero-video");

        assertEquals("/content/mysite/us/en/programs/av-380x.html", hero.getPrimaryCtaUrl());
        assertEquals("/content/dam/mysite/programs/av-380x/datasheet.pdf", hero.getSecondaryCtaUrl());
    }

    @Test
    void overlayPanelIsCappedAtThreeFigures() {
        // Five entries are authored: one is blank and one exceeds the three-up grid.
        assertEquals(3, getModelAt("aerohero-video").getStats().size());
    }

    @Test
    void figureValuesAreSplitForTheCountUpAnimation() {
        StatsItem first = getModelAt("aerohero-video").getStats().get(0);

        assertEquals("12,400+", first.getValue());
        assertEquals(12400d, first.getNumber());
        assertEquals("", first.getPrefix());
        assertEquals("+", first.getSuffix());
        assertEquals("Aircraft delivered", first.getLabel());
    }

    @Test
    void heroWithoutFiguresRendersNoPanel() {
        assertTrue(getModelAt("aerohero-image").getStats().isEmpty());
    }

    @Test
    void heroWithoutAHeadlineIsEmpty() {
        AeroHero hero = getModelAt("aerohero-empty");

        assertNotNull(hero);
        assertTrue(hero.isEmpty());
    }

    @Test
    void exportedTypeIsTheComponentResourceType() {
        AeroHero hero = getModelAt("aerohero-video");

        assertEquals(AeroHeroImpl.RESOURCE_TYPE, hero.getExportedType());
        assertNotNull(hero.getId());
    }
}
