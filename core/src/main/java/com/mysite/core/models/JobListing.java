package com.mysite.core.models;

import java.util.List;

import com.adobe.cq.wcm.core.components.models.Component;
import com.mysite.core.services.JobPosting;

/**
 * Model for the Job Listing Grid.
 *
 * <p>Filtering happens through query parameters and a plain form submit rather than a
 * fetch-and-repaint. Three things follow from that, and all of them matter on a careers page:
 * the list works with JavaScript disabled, a filtered view has a URL a candidate can send to
 * someone, and search engines can crawl it.</p>
 */
public interface JobListing extends Component {

    /** @return optional heading rendered above the results */
    String getHeading();

    /** @return the HTML element used for the heading, defaulting to {@code h2} */
    String getHeadingElement();

    /** @return the openings on the current page, never {@code null} */
    List<JobPosting> getPostings();

    /** @return the location values the filter offers, never {@code null} */
    List<String> getLocations();

    /** @return the job-family values the filter offers, never {@code null} */
    List<String> getFunctions();

    /** @return the keyword the visitor searched for, or {@code null} */
    String getKeyword();

    /** @return the location currently filtered on, or {@code null} */
    String getSelectedLocation();

    /** @return the job family currently filtered on, or {@code null} */
    String getSelectedFunction();

    /** @return the total number of matching openings across all pages */
    int getTotal();

    /** @return the 1-based page being shown */
    int getPage();

    /** @return how many pages this query has, at least 1 */
    int getTotalPages();

    /** @return the query string for the previous page, or {@code null} when on the first */
    String getPreviousPageQuery();

    /** @return the query string for the next page, or {@code null} when on the last */
    String getNextPageQuery();

    /** @return {@code true} when a filter is applied and can be cleared */
    boolean isFiltered();

    /** @return {@code true} when the ATS could not be reached */
    boolean isUnavailable();

    /** @return {@code true} when the search ran but matched nothing */
    boolean isEmpty();

    /**
     * @return schema.org {@code JobPosting} JSON-LD describing the openings on this page, or
     *         {@code null} when there are none. Built from the same postings the page renders,
     *         so the structured data cannot drift from what a visitor sees.
     */
    String getStructuredData();
}
