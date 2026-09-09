package com.mysite.core.models.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.Hero;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class HeroImplTest {

    private static final String PAGE = "/content/mysite/us/en/home";
    private static final String BASE = PAGE + "/jcr:content/root";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/HeroImplTest.json", PAGE + "/jcr:content");
    }

    private Hero modelAt(String name) {
        context.currentResource(BASE + "/" + name);
        return context.request().adaptTo(Hero.class);
    }

    @Test
    void authoredTextIsExposed() {
        Hero hero = modelAt("hero-full");

        assertNotNull(hero);
        assertEquals("Design system", hero.getEyebrow());
        assertEquals("Build interfaces that feel inevitable", hero.getTitle());
        assertEquals("/content/dam/mysite/aurora.svg", hero.getFileReference());
        assertFalse(hero.isEmpty());
    }

    @Test
    void internalPageLinkGetsHtmlExtension() {
        assertEquals("/content/mysite/us/en/stats-demo.html", modelAt("hero-full").getPrimaryCtaUrl());
    }

    @Test
    void externalLinkIsLeftAlone() {
        assertEquals("https://example.com/docs", modelAt("hero-full").getSecondaryCtaUrl());
    }

    @Test
    void assetLinkKeepsItsOwnExtension() {
        assertEquals("/content/dam/mysite/brochure.pdf", modelAt("hero-assetlink").getPrimaryCtaUrl());
    }

    @Test
    void unauthoredLinkIsNull() {
        org.junit.jupiter.api.Assertions.assertNull(modelAt("hero-assetlink").getSecondaryCtaUrl());
    }

    @Test
    void altDefaultsToEmptyForDecorativeArtwork() {
        // An empty alt keeps decorative artwork out of the accessibility tree;
        // a null would render no attribute at all.
        assertEquals("", modelAt("hero-full").getAlt());
    }

    @Test
    void heroWithoutHeadlineIsEmpty() {
        Hero hero = modelAt("hero-empty");

        assertNotNull(hero);
        assertTrue(hero.isEmpty());
    }

    @Test
    void exportedTypeIsTheComponentResourceType() {
        assertEquals(HeroImpl.RESOURCE_TYPE, modelAt("hero-full").getExportedType());
    }
}
