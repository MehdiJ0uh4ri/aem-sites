package com.mysite.core.models.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.Marquee;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class MarqueeImplTest {

    private static final String PAGE = "/content/mysite/us/en/home";
    private static final String BASE = PAGE + "/jcr:content/root";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/MarqueeImplTest.json", PAGE + "/jcr:content");
    }

    private Marquee modelAt(String name) {
        context.currentResource(BASE + "/" + name);
        return context.request().adaptTo(Marquee.class);
    }

    @Test
    void blankPhrasesAreDroppedAndTheRestTrimmed() {
        List<String> items = modelAt("marquee-full").getItems();

        assertEquals(List.of("Accessible", "Composable", "Fast"), items);
    }

    @Test
    void authoredDurationIsUsed() {
        assertEquals(45, modelAt("marquee-full").getDuration());
    }

    @Test
    void durationStoredAsStringIsCoerced() {
        // Granite's numberfield writes a String, so the model must survive that.
        assertEquals(18, modelAt("marquee-string-duration").getDuration());
    }

    @Test
    void durationIsClampedAtBothEnds() {
        assertEquals(MarqueeImpl.MIN_DURATION, modelAt("marquee-too-fast").getDuration());
        assertEquals(MarqueeImpl.MAX_DURATION, modelAt("marquee-too-slow").getDuration());
    }

    @Test
    void missingDurationFallsBackToTheDefault() {
        assertEquals(MarqueeImpl.DEFAULT_DURATION, modelAt("marquee-defaults").getDuration());
    }

    @Test
    void marqueeWithoutPhrasesIsEmpty() {
        Marquee marquee = modelAt("marquee-empty");

        assertNotNull(marquee);
        assertTrue(marquee.isEmpty());
        assertTrue(marquee.getItems().isEmpty());
    }
}
