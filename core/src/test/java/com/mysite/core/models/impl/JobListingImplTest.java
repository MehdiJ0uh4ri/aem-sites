package com.mysite.core.models.impl;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.mysite.core.models.JobListing;
import com.mysite.core.services.JobSearchService;
import com.mysite.core.testcontext.AppAemContext;
import com.mysite.core.testcontext.StubJobSearchService;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class JobListingImplTest {

    private static final String CONTENT_ROOT = "/content/mysite/us/en/careers";
    private static final String TEST_BASE = "/content/mysite/us/en/careers/jcr:content/root";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/com/mysite/core/models/impl/JobListingImplTest.json",
                CONTENT_ROOT + "/jcr:content");
    }

    /**
     * Registers the stub and adapts the component.
     *
     * <p>Call this at most once per test. Sling resolves an {@code @OSGiService} injection to
     * the first matching registration, so a second stub registered in the same test would be
     * ignored and the assertions would silently run against the first one.</p>
     */
    private JobListing model(String name, StubJobSearchService ats, Map<String, Object> params) {
        context.registerService(JobSearchService.class, ats);
        context.currentResource(TEST_BASE + "/" + name);
        context.request().setParameterMap(params);
        return context.request().adaptTo(JobListing.class);
    }

    private JobListing model(String name, StubJobSearchService ats) {
        return model(name, ats, Map.of());
    }

    @Test
    void authoredHeadingAndElementAreExposed() {
        JobListing jobs = model("joblisting-open", StubJobSearchService.withResults(5));

        assertNotNull(jobs);
        assertEquals("Open roles", jobs.getHeading());
        assertEquals("h3", jobs.getHeadingElement());
    }

    @Test
    void unfilteredFirstPageIsTheDefault() {
        StubJobSearchService ats = StubJobSearchService.withResults(5);
        JobListing jobs = model("joblisting-open", ats);

        assertEquals(1, jobs.getPage());
        assertEquals(5, jobs.getTotal());
        assertEquals(2, jobs.getPostings().size());
        assertFalse(jobs.isFiltered());
        assertFalse(jobs.isEmpty());
        assertTrue(ats.getLastQuery().isUnfiltered());
    }

    @Test
    void queryParametersReachTheAts() {
        StubJobSearchService ats = StubJobSearchService.withResults(5);
        JobListing jobs = model("joblisting-open", ats, Map.of(
                "q", "stress",
                "location", "Toulouse, France",
                "page", "2"));

        assertEquals("stress", ats.getLastQuery().getKeyword());
        assertEquals("Toulouse, France", ats.getLastQuery().getLocation());
        assertEquals(2, ats.getLastQuery().getPage());
        assertTrue(jobs.isFiltered());
        assertEquals("stress", jobs.getKeyword());
        assertEquals("Toulouse, France", jobs.getSelectedLocation());
    }

    @Test
    void pagingKeepsTheAppliedFilters() {
        JobListing jobs = model("joblisting-open", StubJobSearchService.withResults(5), Map.of(
                "q", "stress engineer",
                "location", "Toulouse, France",
                "page", "2"));

        // Losing the filters on page two is the classic bug here; both links must carry them,
        // and the space and comma must be encoded rather than pasted into the URL.
        assertEquals("?q=stress+engineer&location=Toulouse%2C+France&page=1", jobs.getPreviousPageQuery());
        assertEquals("?q=stress+engineer&location=Toulouse%2C+France&page=3", jobs.getNextPageQuery());
    }

    @Test
    void firstPageHasNoPreviousLink() {
        JobListing jobs = model("joblisting-open", StubJobSearchService.withResults(4));

        assertNull(jobs.getPreviousPageQuery());
        assertEquals("?page=2", jobs.getNextPageQuery());
    }

    @Test
    void lastPageHasNoNextLink() {
        JobListing jobs = model("joblisting-open", StubJobSearchService.withResults(4),
                Map.of("page", "2"));

        assertEquals("?page=1", jobs.getPreviousPageQuery());
        assertNull(jobs.getNextPageQuery());
    }

    @Test
    void unparsablePageFallsBackToTheFirst() {
        // A truncated or hand-edited URL should land somewhere useful, not on an error.
        JobListing jobs = model("joblisting-open", StubJobSearchService.withResults(5),
                Map.of("page", "not-a-number"));

        assertEquals(1, jobs.getPage());
    }

    @Test
    void pinnedJobFamilyCannotBeWidenedFromTheUrl() {
        StubJobSearchService ats = StubJobSearchService.withResults(3);
        JobListing jobs = model("joblisting-pinned", ats, Map.of("function", "Cabin Crew"));

        // The authored family wins: a business-unit careers page must not be turned into a
        // company-wide one by editing the query string.
        assertEquals("Structures Engineering", ats.getLastQuery().getFunction());
        assertNull(jobs.getSelectedFunction());
        // And the filter control is withdrawn, since there is nothing left to choose.
        assertTrue(jobs.getFunctions().isEmpty());
    }

    @Test
    void unpinnedListingOffersTheJobFamilyFilter() {
        JobListing jobs = model("joblisting-open", StubJobSearchService.withResults(3));

        assertEquals(2, jobs.getFunctions().size());
        assertEquals(2, jobs.getLocations().size());
    }

    @Test
    void unreachableAtsIsReportedAsUnavailableNotEmpty() {
        JobListing jobs = model("joblisting-open", StubJobSearchService.unavailable());

        assertTrue(jobs.isUnavailable());
        // Not "empty": telling a candidate there are no openings when the system is simply
        // down is actively misleading, and the two states ask them to do different things.
        assertFalse(jobs.isEmpty());
    }

    @Test
    void searchThatMatchesNothingIsEmptyNotUnavailable() {
        JobListing jobs = model("joblisting-open", StubJobSearchService.withResults(0));

        assertFalse(jobs.isUnavailable());
        assertTrue(jobs.isEmpty());
    }

    @Test
    void structuredDataDescribesTheRenderedPostings() {
        String jsonLd = model("joblisting-open", StubJobSearchService.withResults(5)).getStructuredData();

        assertNotNull(jsonLd);
        assertTrue(jsonLd.contains("\"@type\":\"ItemList\""));
        assertTrue(jsonLd.contains("\"@type\":\"JobPosting\""));
        assertTrue(jsonLd.contains("AeroVance Systems"));
        assertTrue(jsonLd.contains("Stress Engineer 0"));
        // The second posting is remote in the stub, so the property Google reads must be there.
        assertTrue(jsonLd.contains("TELECOMMUTE"));
        // Only the current page is described, not the full result set.
        assertFalse(jsonLd.contains("Stress Engineer 2"));
    }

    @Test
    void structuredDataIsOmittedWhenNothingMatched() {
        // An empty graph advertises a careers page with no roles, which is worse than none.
        assertNull(model("joblisting-open", StubJobSearchService.withResults(0)).getStructuredData());
    }

    @Test
    void structuredDataIsOmittedWhenTheAtsIsUnreachable() {
        assertNull(model("joblisting-open", StubJobSearchService.unavailable()).getStructuredData());
    }

    @Test
    void exportedTypeIsTheComponentResourceType() {
        JobListing jobs = model("joblisting-open", StubJobSearchService.withResults(1));

        assertEquals(JobListingImpl.RESOURCE_TYPE, jobs.getExportedType());
        assertNotNull(jobs.getId());
    }
}
