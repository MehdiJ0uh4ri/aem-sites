package com.mysite.core.testcontext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mysite.core.services.JobPosting;
import com.mysite.core.services.JobQuery;
import com.mysite.core.services.JobSearchResult;
import com.mysite.core.services.JobSearchService;

/**
 * An in-memory {@link JobSearchService} for tests.
 *
 * <p>Records the query it was given, so a test can assert on what the model asked the ATS for
 * — which is where the filtering and pinning decisions actually live — rather than only on
 * what came back.</p>
 */
public final class StubJobSearchService implements JobSearchService {

    private final int total;
    private final boolean unavailable;

    private JobQuery lastQuery;

    private StubJobSearchService(int total, boolean unavailable) {
        this.total = total;
        this.unavailable = unavailable;
    }

    /** @param total how many matches to report across all pages */
    public static StubJobSearchService withResults(int total) {
        return new StubJobSearchService(total, false);
    }

    /** @return a service that behaves as an unreachable ATS */
    public static StubJobSearchService unavailable() {
        return new StubJobSearchService(0, true);
    }

    /** @return the query the model last submitted, or {@code null} */
    public JobQuery getLastQuery() {
        return lastQuery;
    }

    @Override
    public JobSearchResult search(JobQuery query) {
        lastQuery = query;
        if (unavailable) {
            return new StubResult(Collections.emptyList(), 0, query, true);
        }

        int from = (query.getPage() - 1) * query.getPageSize();
        List<JobPosting> page = new ArrayList<>();
        for (int i = from; i < Math.min(from + query.getPageSize(), total); i++) {
            page.add(new StubPosting(i));
        }
        return new StubResult(page, total, query, false);
    }

    private static final class StubResult implements JobSearchResult {
        private final List<JobPosting> postings;
        private final int total;
        private final JobQuery query;
        private final boolean unavailable;

        StubResult(List<JobPosting> postings, int total, JobQuery query, boolean unavailable) {
            this.postings = postings;
            this.total = total;
            this.query = query;
            this.unavailable = unavailable;
        }

        @Override
        public List<JobPosting> getPostings() {
            return postings;
        }

        @Override
        public List<String> getLocations() {
            return List.of("Palmdale, United States", "Toulouse, France");
        }

        @Override
        public List<String> getFunctions() {
            return List.of("Avionics", "Structures Engineering");
        }

        @Override
        public int getTotal() {
            return total;
        }

        @Override
        public int getPage() {
            return query.getPage();
        }

        @Override
        public int getTotalPages() {
            return Math.max(1, (int) Math.ceil((double) total / query.getPageSize()));
        }

        @Override
        public boolean isUnavailable() {
            return unavailable;
        }
    }

    private static final class StubPosting implements JobPosting {
        private final int index;

        StubPosting(int index) {
            this.index = index;
        }

        @Override
        public String getId() {
            return "REQ-" + index;
        }

        @Override
        public String getTitle() {
            return "Stress Engineer " + index;
        }

        @Override
        public String getFunction() {
            return "Structures Engineering";
        }

        @Override
        public String getLocation() {
            return "Toulouse, France";
        }

        @Override
        public String getEmploymentType() {
            return "Full time";
        }

        @Override
        public java.time.LocalDate getPostedOn() {
            return java.time.LocalDate.of(2026, 3, 1);
        }

        @Override
        public String getSummary() {
            return "Airframe stress analysis for the AV-380X programme.";
        }

        @Override
        public String getClearance() {
            // Only the first posting requires one, so a test can tell the two cases apart.
            return index == 0 ? "Secret" : null;
        }

        @Override
        public String getApplyUrl() {
            return "https://aerovance.wd3.myworkdayjobs.com/careers/job/REQ-" + index;
        }

        @Override
        public boolean isRemote() {
            return index == 1;
        }

        @Override
        public List<String> getTags() {
            return Collections.emptyList();
        }
    }
}
