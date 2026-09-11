package com.mysite.core.services;

import java.util.List;

/**
 * One page of job search results, together with the values the filter controls should offer.
 *
 * <p>The facets come from the ATS rather than from a hand-maintained list in a dialog. An
 * office that opens or closes changes the options a visitor sees without anyone editing a
 * page, and a filter can never offer a location that has no openings behind it.</p>
 */
public interface JobSearchResult {

    /** @return the postings on this page, never {@code null} */
    List<JobPosting> getPostings();

    /** @return every location with at least one matching opening, never {@code null} */
    List<String> getLocations();

    /** @return every job family with at least one matching opening, never {@code null} */
    List<String> getFunctions();

    /** @return the total number of matches across all pages */
    int getTotal();

    /** @return the 1-based page these postings came from */
    int getPage();

    /** @return the number of pages available for this query, at least 1 */
    int getTotalPages();

    /**
     * @return {@code true} when the ATS could not be reached. Distinct from an empty result:
     *         "no roles match your filters" and "we cannot reach the careers system right now"
     *         ask the visitor to do different things.
     */
    boolean isUnavailable();
}
