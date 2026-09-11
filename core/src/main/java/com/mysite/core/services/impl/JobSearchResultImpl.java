package com.mysite.core.services.impl;

import java.util.Collections;
import java.util.List;

import com.mysite.core.services.JobPosting;
import com.mysite.core.services.JobSearchResult;

/**
 * Immutable {@link JobSearchResult} value.
 */
final class JobSearchResultImpl implements JobSearchResult {

    private final List<JobPosting> postings;
    private final List<String> locations;
    private final List<String> functions;
    private final int total;
    private final int page;
    private final int pageSize;
    private final boolean unavailable;

    private JobSearchResultImpl(List<JobPosting> postings, List<String> locations,
                                List<String> functions, int total, int page, int pageSize,
                                boolean unavailable) {
        this.postings = postings;
        this.locations = locations;
        this.functions = functions;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.unavailable = unavailable;
    }

    static JobSearchResult of(List<JobPosting> postings, List<String> locations,
                              List<String> functions, int total, int page, int pageSize) {
        return new JobSearchResultImpl(postings, locations, functions, total, page, pageSize, false);
    }

    /**
     * @return a result marking the ATS as unreachable. The careers page renders an apology
     *         and keeps its filter controls, rather than claiming there are no open roles.
     */
    static JobSearchResult unavailable(int page) {
        return new JobSearchResultImpl(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), 0, page, 1, true);
    }

    @Override
    public List<JobPosting> getPostings() {
        return postings;
    }

    @Override
    public List<String> getLocations() {
        return locations;
    }

    @Override
    public List<String> getFunctions() {
        return functions;
    }

    @Override
    public int getTotal() {
        return total;
    }

    @Override
    public int getPage() {
        return page;
    }

    @Override
    public int getTotalPages() {
        // Always at least one page, so a template can render "page 1 of 1" for an empty
        // result without special-casing zero.
        return Math.max(1, (int) Math.ceil((double) total / pageSize));
    }

    @Override
    public boolean isUnavailable() {
        return unavailable;
    }
}
