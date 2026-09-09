package com.mysite.core.models.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.Bento;
import com.mysite.core.models.BentoCard;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class BentoImplTest {

    private static final String PAGE = "/content/mysite/us/en/home";
    private static final String BASE = PAGE + "/jcr:content/root";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/BentoImplTest.json", PAGE + "/jcr:content");
    }

    private Bento modelAt(String name) {
        context.currentResource(BASE + "/" + name);
        return context.request().adaptTo(Bento.class);
    }

    @Test
    void tilesWithoutTitleOrImageAreDropped() {
        List<BentoCard> cards = modelAt("bento-full").getCards();

        // item4 has only a description, so four of the five survive.
        assertEquals(4, cards.size());
    }

    @Test
    void tileKeepsItsAuthoredFootprint() {
        List<BentoCard> cards = modelAt("bento-full").getCards();

        assertEquals("wide", cards.get(0).getSpan());
        assertEquals("tall", cards.get(1).getSpan());
    }

    @Test
    void missingFootprintFallsBackToNormal() {
        assertEquals(BentoCardImpl.DEFAULT_SPAN, modelAt("bento-full").getCards().get(2).getSpan());
    }

    @Test
    void unknownFootprintFallsBackRatherThanEmittingAnUnknownClass() {
        assertEquals(BentoCardImpl.DEFAULT_SPAN, modelAt("bento-full").getCards().get(3).getSpan());
    }

    @Test
    void tileLinkIsResolved() {
        BentoCard card = modelAt("bento-full").getCards().get(0);

        assertEquals("/content/mysite/us/en/stats-demo.html", card.getUrl());
        assertEquals("Composable", card.getTitle());
    }

    @Test
    void tileWithoutLinkHasNoUrl() {
        assertNull(modelAt("bento-full").getCards().get(1).getUrl());
    }

    @Test
    void gridWithoutTilesIsEmpty() {
        Bento bento = modelAt("bento-empty");

        assertNotNull(bento);
        assertTrue(bento.isEmpty());
        assertTrue(bento.getCards().isEmpty());
        assertEquals("Nothing here", bento.getHeading());
    }
}
