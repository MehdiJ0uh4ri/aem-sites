package com.mysite.core.services.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.SerializedName;
import com.mysite.core.services.JobPosting;
import com.mysite.core.services.JobQuery;
import com.mysite.core.services.JobSearchResult;
import com.mysite.core.services.JobSearchService;

/**
 * Reads open roles from a Workday-style ATS REST endpoint.
 *
 * <p>Results are cached per query for a short interval. Careers pages are crawled heavily and
 * a single job family can be linked to from dozens of programme pages, so without a cache the
 * ATS receives one call per view — and ATS vendors rate-limit aggressively. The interval is
 * short because a posting that closes should disappear within minutes, not hours.</p>
 */
@Component(service = JobSearchService.class, immediate = true)
@Designate(ocd = WorkdayJobSearchService.Config.class)
public class WorkdayJobSearchService implements JobSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(WorkdayJobSearchService.class);

    @ObjectClassDefinition(name = "AeroVance — Job Search Service",
                           description = "Reads open roles from the applicant tracking system for the careers pages.")
    public @interface Config {

        @AttributeDefinition(name = "Endpoint",
                description = "Job search endpoint of the ATS, without a query string.")
        String endpoint() default "https://aerovance.wd3.myworkdayjobs.com/wday/cxs/aerovance/careers/jobs";

        @AttributeDefinition(name = "API token",
                description = "Sent as a bearer token. Set this through a Cloud Manager secret environment variable, never in a committed OSGi config.",
                type = AttributeType.PASSWORD)
        String apiToken() default "";

        @AttributeDefinition(name = "Cache duration (seconds)",
                description = "How long a result is reused before the ATS is asked again. Keep this short — a closed requisition must stop being advertised quickly.")
        int cacheSeconds() default 300;

        @AttributeDefinition(name = "Maximum cached queries",
                description = "Distinct filter combinations held in memory. The cache is bounded because the filter values come from visitors, and an unbounded map keyed on visitor input is a memory-exhaustion vector.")
        int cacheSize() default 200;
    }

    /** Shape of the ATS response. Only the fields the careers pages render are mapped. */
    private static final class SearchPayload {
        @SerializedName("total")
        Integer total;
        @SerializedName("jobPostings")
        List<JobPostingImpl> jobPostings;
        @SerializedName("facets")
        FacetsPayload facets;
    }

    private static final class FacetsPayload {
        @SerializedName("locations")
        List<String> locations;
        @SerializedName("jobFamilies")
        List<String> jobFamilies;
    }

    private static final class CacheEntry {
        final JobSearchResult result;
        final Instant fetchedAt;

        CacheEntry(JobSearchResult result, Instant fetchedAt) {
            this.result = result;
            this.fetchedAt = fetchedAt;
        }
    }

    /**
     * Bounded LRU. Access-ordered so the eviction that runs on insert drops the query nobody
     * has asked for in the longest time, rather than the oldest one written.
     */
    private Map<JobQuery, CacheEntry> cache;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        int capacity = Math.max(config.cacheSize(), 1);
        this.cache = Collections.synchronizedMap(
                new LinkedHashMap<JobQuery, CacheEntry>(16, 0.75f, true) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<JobQuery, CacheEntry> eldest) {
                        return size() > capacity;
                    }
                });
    }

    @Override
    public JobSearchResult search(JobQuery query) {
        if (StringUtils.isBlank(config.apiToken())) {
            // Expected on a local author instance with no ATS credentials.
            LOG.debug("No ATS token configured; the careers page will report the system as unavailable.");
            return JobSearchResultImpl.unavailable(query.getPage());
        }

        CacheEntry cached = cache.get(query);
        if (cached != null && !isExpired(cached)) {
            return cached.result;
        }

        JobSearchResult result = fetch(query);
        if (!result.isUnavailable()) {
            cache.put(query, new CacheEntry(result, Instant.now()));
            return result;
        }

        // A failed refresh falls back to the stale page rather than telling a visitor there
        // are no openings — which would be actively misleading, not merely unhelpful.
        if (cached != null) {
            LOG.warn("ATS search failed for {}; serving results from {}", query, cached.fetchedAt);
            return cached.result;
        }
        return result;
    }

    private boolean isExpired(CacheEntry entry) {
        return Duration.between(entry.fetchedAt, Instant.now())
                .compareTo(Duration.ofSeconds(config.cacheSeconds())) >= 0;
    }

    private JobSearchResult fetch(JobQuery query) {
        Map<String, String> headers = Map.of("Authorization", "Bearer " + config.apiToken());

        Optional<SearchPayload> payload = JsonHttp.get(buildUrl(query), headers, SearchPayload.class);
        if (payload.isEmpty()) {
            return JobSearchResultImpl.unavailable(query.getPage());
        }

        SearchPayload body = payload.get();
        List<JobPosting> postings = body.jobPostings == null
                ? Collections.emptyList()
                : body.jobPostings.stream()
                        .filter(JobPostingImpl::isRenderable)
                        .collect(Collectors.<JobPosting>toList());

        return JobSearchResultImpl.of(
                Collections.unmodifiableList(postings),
                facet(body.facets == null ? null : body.facets.locations),
                facet(body.facets == null ? null : body.facets.jobFamilies),
                body.total == null ? postings.size() : body.total,
                query.getPage(),
                query.getPageSize());
    }

    /** Sorted and de-duplicated, so the filter controls read predictably whatever order the ATS used. */
    private List<String> facet(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toUnmodifiableList());
    }

    private String buildUrl(JobQuery query) {
        List<String> params = new ArrayList<>();
        params.add("limit=" + query.getPageSize());
        params.add("offset=" + (query.getPage() - 1) * query.getPageSize());
        addParam(params, "searchText", query.getKeyword());
        addParam(params, "locations", query.getLocation());
        addParam(params, "jobFamily", query.getFunction());

        return config.endpoint() + "?" + String.join("&", params);
    }

    private void addParam(List<String> params, String name, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            // Filter values reach us from the query string, so they are encoded rather than
            // concatenated: an unencoded '&' would otherwise let a visitor inject parameters
            // into the ATS request.
            params.add(name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is guaranteed present; the filter is dropped rather than the search failing.
            LOG.error("UTF-8 is unavailable, which should be impossible", e);
        }
    }
}
