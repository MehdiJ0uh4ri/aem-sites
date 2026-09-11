package com.mysite.core.models.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.ResourceEntry;
import com.mysite.core.models.ResourceLibrary;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class ResourceLibraryImplTest {

    private static final String CONTENT_ROOT = "/content/mysite/us/en/programs/av-380x";
    private static final String TEST_BASE = CONTENT_ROOT + "/jcr:content/root";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/ResourceLibraryImplTest.json",
                CONTENT_ROOT + "/jcr:content");
    }

    private ResourceLibrary getModelAt(String name) {
        context.currentResource(TEST_BASE + "/" + name);
        return context.request().adaptTo(ResourceLibrary.class);
    }

    @Test
    void authoredIntroductionIsExposed() {
        ResourceLibrary library = getModelAt("library-full");

        assertNotNull(library);
        assertEquals("Documentation", library.getHeading());
        assertEquals("h3", library.getHeadingElement());
        assertEquals("Published specifications and airport planning data.", library.getDescription());
        assertFalse(library.isEmpty());
    }

    @Test
    void entriesMissingATitleOrAnAssetAreDropped() {
        // Four are authored; one has no asset behind it and one has no title. Both would
        // render as a dead row — a broken link, or a form filled in for nothing.
        assertEquals(2, getModelAt("library-full").getEntries().size());
    }

    @Test
    void ungatedEntryExposesADirectDownloadUrl() {
        ResourceEntry entry = getModelAt("library-full").getEntries().get(0);

        assertFalse(entry.isGated());
        assertEquals("/content/dam/mysite/programs/av-380x/av-380x-datasheet.pdf", entry.getDownloadUrl());
        assertEquals("Data sheet", entry.getCategory());
        assertEquals("EAR99", entry.getClassification());
    }

    @Test
    void gatedEntryExposesNoDirectUrlAnywhereInTheMarkup() {
        ResourceEntry entry = getModelAt("library-full").getEntries().get(1);

        assertTrue(entry.isGated());
        // The whole point of the gate is that the path is not reachable from the page. The
        // template has the asset path only in the hidden field the servlet re-checks.
        assertNull(entry.getDownloadUrl());
        assertEquals("/content/dam/mysite/gated/av-380x-performance-manual.pdf", entry.getAssetPath());
    }

    @Test
    void formatIsDerivedFromTheAssetAndUpperCased() {
        assertEquals("PDF", getModelAt("library-full").getEntries().get(0).getFormat());
        // A lower- or mixed-case extension must still read as PDF beside the download.
        assertEquals("PDF", getModelAt("library-ungated").getEntries().get(0).getFormat());
    }

    @Test
    void missingAssetCostsTheSizeButNotTheEntry() {
        // The asset is not in the test repository. A broken link is more visible to an author
        // than a row that silently disappeared, so the entry survives without its size.
        ResourceEntry entry = getModelAt("library-full").getEntries().get(0);

        assertNotNull(entry.getTitle());
        assertNull(entry.getSize());
    }

    @Test
    void gatingIsReportedForTheWholeComponent() {
        assertTrue(getModelAt("library-full").isAnyGated());
        assertFalse(getModelAt("library-ungated").isAnyGated());
    }

    @Test
    void formActionTargetsTheComponentsOwnDownloadSelector() {
        // Posting to the component rather than a /bin path keeps the handler scoped to this
        // resource type and gives it the authored entries to validate against.
        assertEquals(TEST_BASE + "/library-full.download.html",
                getModelAt("library-full").getFormAction());
    }

    @Test
    void consentWordingFallsBackToTheApprovedDefault() {
        assertEquals("Tell me about AeroVance events.", getModelAt("library-ungated").getConsentText());
        // An opt-in with no explanation of what is being consented to is not consent, so the
        // default is never blank.
        assertTrue(getModelAt("library-full").getConsentText().contains("withdraw consent"));
    }

    @Test
    void headingElementFallsBackToH2() {
        assertEquals("h2", getModelAt("library-ungated").getHeadingElement());
    }

    @Test
    void libraryWithoutEntriesIsEmpty() {
        ResourceLibrary library = getModelAt("library-empty");

        assertNotNull(library);
        assertTrue(library.isEmpty());
        assertFalse(library.isAnyGated());
        assertTrue(library.getEntries().isEmpty());
    }

    @Test
    void exportedTypeIsTheComponentResourceType() {
        ResourceLibrary library = getModelAt("library-full");

        assertEquals(ResourceLibraryImpl.RESOURCE_TYPE, library.getExportedType());
        assertNotNull(library.getId());
    }
}
