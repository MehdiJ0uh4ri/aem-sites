package com.mysite.core.models.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.ProgramTeaser;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class ProgramTeaserImplTest {

    private static final String CONTENT_ROOT = "/content/mysite/us/en/home";
    private static final String TEST_BASE = "/content/mysite/us/en/home/jcr:content/root";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/ProgramTeaserImplTest.json",
                CONTENT_ROOT + "/jcr:content");
    }

    private ProgramTeaser getModelAt(String name) {
        context.currentResource(TEST_BASE + "/" + name);
        return context.request().adaptTo(ProgramTeaser.class);
    }

    @Test
    void authoredContentIsExposed() {
        ProgramTeaser teaser = getModelAt("teaser-full");

        assertNotNull(teaser);
        assertEquals("Commercial Aviation", teaser.getEyebrow());
        assertEquals("AV-380X", teaser.getTitle());
        assertEquals("Twin-aisle, 320 seats, 6,400 km.", teaser.getDescription());
        assertEquals("AV-380X in AeroVance livery", teaser.getAlt());
        assertFalse(teaser.isEmpty());
    }

    @Test
    void knownStatusExposesBothSlugAndLabel() {
        ProgramTeaser teaser = getModelAt("teaser-full");

        assertEquals("in-service", teaser.getStatus());
        assertEquals("In service", teaser.getStatusLabel());
    }

    @Test
    void unrecognisedStatusIsDroppedRatherThanRendered() {
        // An unknown slug would style as an unknown state and read as a broken badge.
        ProgramTeaser teaser = getModelAt("teaser-bad-status");

        assertNull(teaser.getStatus());
        assertNull(teaser.getStatusLabel());
    }

    @Test
    void figuresAreCappedAtThree() {
        // Five are authored: one is blank and one would wrap under the row.
        assertEquals(3, getModelAt("teaser-full").getSpecs().size());
        assertEquals("6,400 km", getModelAt("teaser-full").getSpecs().get(0).getValue());
        assertEquals("Range", getModelAt("teaser-full").getSpecs().get(0).getLabel());
    }

    @Test
    void internalLinkGetsHtmlExtensionAndExternalOneDoesNot() {
        assertEquals("/content/mysite/us/en/programs/av-380x.html", getModelAt("teaser-full").getLinkUrl());
        assertEquals("https://partners.example.com/av-120", getModelAt("teaser-minimal").getLinkUrl());
    }

    @Test
    void callToActionFallsBackToAGenericLabel() {
        assertEquals("Programme detail", getModelAt("teaser-full").getLinkText());
        assertEquals("View programme", getModelAt("teaser-minimal").getLinkText());
    }

    @Test
    void altDefaultsToEmptySoTheImageIsDecorative() {
        // The card's headline already names the programme.
        assertEquals("", getModelAt("teaser-minimal").getAlt());
    }

    @Test
    void teaserWithoutFiguresRendersNone() {
        assertTrue(getModelAt("teaser-minimal").getSpecs().isEmpty());
    }

    @Test
    void teaserWithoutAHeadlineIsEmpty() {
        ProgramTeaser teaser = getModelAt("teaser-empty");

        assertNotNull(teaser);
        assertTrue(teaser.isEmpty());
        assertNull(teaser.getLinkUrl());
    }

    @Test
    void exportedTypeIsTheComponentResourceType() {
        ProgramTeaser teaser = getModelAt("teaser-full");

        assertEquals(ProgramTeaserImpl.RESOURCE_TYPE, teaser.getExportedType());
        assertNotNull(teaser.getId());
    }
}
