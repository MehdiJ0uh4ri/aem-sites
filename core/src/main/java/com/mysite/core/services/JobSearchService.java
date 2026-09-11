package com.mysite.core.services;

/**
 * Searches the applicant tracking system for open roles.
 */
public interface JobSearchService {

    /**
     * @param query the filters to apply
     * @return the matching page of results. Never {@code null} and never throws: an
     *         unreachable ATS comes back as a result whose
     *         {@link JobSearchResult#isUnavailable()} is {@code true}, so the careers page
     *         degrades to an apology rather than a 500.
     */
    JobSearchResult search(JobQuery query);
}
