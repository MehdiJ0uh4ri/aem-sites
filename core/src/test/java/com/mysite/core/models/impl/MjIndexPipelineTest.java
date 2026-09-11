package com.mysite.core.models.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.MjIndex;
import com.mysite.core.models.MjIndexItem;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The content-fragment pipeline, end to end: a fragment folder in the DAM, through the
 * authored path, to the values the markup binds to.
 *
 * <p>Written against a fragment fixture that carries only the documented storage shape —
 * {@code contentFragment} on the asset's content node and values under
 * {@code data/master} — with no model deployed. That is deliberate: it is the state the
 * repository was actually in when these sections rendered empty, and it is what the
 * component now has to survive.</p>
 */
@ExtendWith(AemContextExtension.class)
class MjIndexPipelineTest {

    private static final String PAGE = "/content/mysite/us/en/mjindex";
    private static final String DAM = "/content/dam/mjco/portfolio";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/MjIndexPipelineFragments.json", DAM);
        context.load().json("/com/mysite/core/models/impl/MjIndexPipelineTest.json", PAGE + "/jcr:content");
        context.currentResource(PAGE + "/jcr:content/root/portfolio");
    }

    private MjIndex index() {
        return context.request().adaptTo(MjIndex.class);
    }

    @Test
    void fragmentsAreFoundWithoutAModelBeingDeployed() {
        MjIndex index = index();

        assertNotNull(index);
        assertFalse(index.isEmpty(), "the folder holds one content fragment");
        assertEquals(1, index.getItems().size());
    }

    @Test
    void assetsThatAreNotFragmentsAreIgnored() {
        // The DAM folder also holds a plain image; it must not become a portfolio entry.
        assertEquals(1, index().getItems().size());
    }

    @Test
    void valuesAreReadFromTheFragment() {
        MjIndexItem item = index().getItems().get(0);

        assertEquals("Orlin Bio", item.getTitle());
        assertEquals("Health", item.getKicker());
        assertEquals("San Diego, CA", item.getMeta());
        assertEquals("/content/dam/mjco/images/co-orlin-bio.jpg", item.getFileReference());
    }

    @Test
    void detailUrlIsBuiltFromTheDetailRootAndTheFragmentName() {
        assertEquals("/content/mjco/us/en/portfolio/orlin-bio.html",
                index().getItems().get(0).getUrl());
    }
}
