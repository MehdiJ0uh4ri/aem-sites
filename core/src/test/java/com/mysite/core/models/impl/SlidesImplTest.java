package com.mysite.core.models.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.Slide;
import com.mysite.core.models.Slides;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class SlidesImplTest {

    private static final String PAGE = "/content/mysite/us/en/home";
    private static final String BASE = PAGE + "/jcr:content/root";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/SlidesImplTest.json", PAGE + "/jcr:content");
    }

    private Slides modelAt(String name) {
        context.currentResource(BASE + "/" + name);
        return context.request().adaptTo(Slides.class);
    }

    @Test
    void authoredLayoutIsUsed() {
        assertEquals(Slides.LAYOUT_SPOTLIGHT, modelAt("slides-full").getLayout());
    }

    @Test
    void unknownLayoutFallsBackToSplit() {
        // Guards against a hand-edited value emitting a modifier class with no styles.
        assertEquals(Slides.LAYOUT_SPLIT, modelAt("slides-bad-layout").getLayout());
    }

    @Test
    void slidesWithoutTitleOrImageAreDropped() {
        assertEquals(2, modelAt("slides-full").getSlides().size());
    }

    @Test
    void slideIdsAreDerivedFromTheParentAndAreUnique() {
        Slides slides = modelAt("slides-full");
        List<Slide> list = slides.getSlides();

        assertEquals(slides.getId() + "-slide-0", list.get(0).getId());
        assertEquals(slides.getId() + "-slide-1", list.get(1).getId());
    }

    @Test
    void slideCtaLinkIsResolved() {
        assertEquals("/content/mysite/us/en/stats-demo.html",
                modelAt("slides-full").getSlides().get(0).getCtaUrl());
    }

    @Test
    void slideWithoutCtaHasNoUrl() {
        assertNull(modelAt("slides-full").getSlides().get(1).getCtaUrl());
    }

    @Test
    void childDataLayerFollowsTheSectionsDataLayer() {
        Slides slides = modelAt("slides-full");
        Slide first = slides.getSlides().get(0);

        // The per-slide entry is gated on the section's own entry: when the page has the
        // data layer switched off, neither is emitted.
        if (slides.getData() == null) {
            assertNull(first.getDataLayerJson());
        } else {
            assertNotNull(first.getDataLayerJson());
            assertTrue(first.getDataLayerJson().contains(first.getId()));
        }
    }

    @Test
    void emptyShowcaseReportsEmpty() {
        Slides slides = modelAt("slides-empty");

        assertNotNull(slides);
        assertTrue(slides.isEmpty());
        assertTrue(slides.getSlides().isEmpty());
    }
}
