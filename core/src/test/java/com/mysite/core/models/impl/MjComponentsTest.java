package com.mysite.core.models.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.MjEngage;
import com.mysite.core.models.MjHero;
import com.mysite.core.models.MjIndex;
import com.mysite.core.models.MjSignal;
import com.mysite.core.models.MjThesis;
import com.mysite.core.models.MjThesisItem;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The MJ &amp; Company sections.
 *
 * <p>What is worth testing here is the behaviour an author can trip over: a half-filled
 * multifield row, a call to action with a label but no link, a variant typed by hand, and
 * a fragment folder that has been moved or unpublished. Those are the states that reach
 * production, and each one has to degrade rather than break the page.</p>
 */
@ExtendWith(AemContextExtension.class)
class MjComponentsTest {

    private static final String PAGE = "/content/mysite/us/en/mj";
    private static final String BASE = PAGE + "/jcr:content/root";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/MjComponentsTest.json", PAGE + "/jcr:content");
    }

    private <T> T modelAt(String name, Class<T> type) {
        context.currentResource(BASE + "/" + name);
        return context.request().adaptTo(type);
    }

    // ---------------------------------------------------------------- hero

    @Test
    void heroCtaLinkIsResolvedToAPageUrl() {
        assertEquals("/content/mjco/us/en/approach.html",
                modelAt("hero-full", MjHero.class).getCtaUrl());
    }

    @Test
    void heroCtaWithoutALinkGetsNoIdAndNoDataLayerEntry() {
        // The markup only renders the CTA when it has both a label and a URL, so an id
        // for one that is never rendered would be a dangling data layer reference.
        MjHero hero = modelAt("hero-no-cta", MjHero.class);

        assertNull(hero.getCtaUrl());
        assertNull(hero.getCtaId());
        assertNull(hero.getCtaDataLayerJson());
    }

    @Test
    void heroAltDefaultsToEmptyRatherThanNull() {
        // An unauthored alt has to reach the markup as "", which is how the decorative
        // variant of the <img> is chosen.
        assertEquals("", modelAt("hero-full", MjHero.class).getAlt());
    }

    @Test
    void heroWithoutAHeadlineIsEmpty() {
        assertTrue(modelAt("hero-empty", MjHero.class).isEmpty());
    }

    @Test
    void heroWithOnlyAFirstLineIsNotEmpty() {
        assertFalse(modelAt("hero-no-cta", MjHero.class).isEmpty());
    }

    // -------------------------------------------------------------- thesis

    @Test
    void thesisOrdinalsAreDerivedAndZeroPadded() {
        List<MjThesisItem> items = modelAt("thesis-full", MjThesis.class).getItems();

        assertEquals("01", items.get(0).getOrdinal());
        assertEquals("02", items.get(1).getOrdinal());
    }

    @Test
    void anAuthoredOrdinalWins() {
        assertEquals("IV", modelAt("thesis-full", MjThesis.class).getItems().get(2).getOrdinal());
    }

    @Test
    void emptyThesisRowsAreDropped() {
        // The fourth row has neither a belief nor reasoning: rendering it would leave a
        // rule across the page with nothing under it.
        assertEquals(3, modelAt("thesis-full", MjThesis.class).getItems().size());
    }

    @Test
    void thesisWithNoBeliefsIsEmpty() {
        MjThesis thesis = modelAt("thesis-empty", MjThesis.class);

        assertTrue(thesis.isEmpty());
        assertTrue(thesis.getItems().isEmpty());
    }

    // -------------------------------------------------------------- signal

    @Test
    void signalKeepsAFigureThatHasOnlyALabel() {
        // A label with no figure yet is a normal intermediate state while an author is
        // still gathering numbers; only a row with neither is dropped.
        assertEquals(2, modelAt("signal-full", MjSignal.class).getItems().size());
    }

    @Test
    void signalWithAStatementAndNoLedgerStillRenders() {
        MjSignal signal = modelAt("signal-statement-only", MjSignal.class);

        assertFalse(signal.isEmpty());
        assertTrue(signal.getItems().isEmpty());
    }

    // --------------------------------------------------------------- index

    @Test
    void unknownIndexVariantFallsBackToInsights() {
        // Guards against a hand-edited value emitting a modifier class with no styles.
        assertEquals(MjIndex.VARIANT_INSIGHTS, modelAt("index-bad-variant", MjIndex.class).getVariant());
    }

    @Test
    void indexPointedAtAMissingFolderIsEmptyRatherThanBroken() {
        // Fragments get moved and unpublished. The section then renders nothing on
        // publish and shows its placeholder to the author, instead of failing the page.
        MjIndex index = modelAt("index-missing-folder", MjIndex.class);

        assertNotNull(index);
        assertTrue(index.isEmpty());
        assertTrue(index.getItems().isEmpty());
    }

    // -------------------------------------------------------------- engage

    @Test
    void engageLaneIdsAreDerivedFromTheParentAndAreUnique() {
        MjEngage engage = modelAt("engage-full", MjEngage.class);

        assertEquals(engage.getId() + "-lane-0", engage.getItems().get(0).getId());
        assertEquals(engage.getId() + "-lane-1", engage.getItems().get(1).getId());
    }

    @Test
    void externalLaneLinksAreLeftUntouched() {
        assertEquals("https://example.com/lp",
                modelAt("engage-full", MjEngage.class).getItems().get(1).getCtaUrl());
    }
}
