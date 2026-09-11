package com.mysite.core.models.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.mysite.core.services.JobPosting;

/**
 * Builds the schema.org {@code JobPosting} JSON-LD for a page of openings.
 *
 * <p>Google Jobs is normally the largest single referrer to a corporate careers page, and it
 * reads this markup rather than the rendered list. The graph is emitted from the same postings
 * the page renders, so the structured data cannot drift from what a visitor actually sees —
 * which is both an SEO requirement and a condition of staying in the index.</p>
 */
final class JobPostingSchema {

    /**
     * Default Gson, deliberately: its HTML escaping turns {@code <}, {@code >} and {@code &}
     * into unicode escapes, so a requisition title containing {@code </script>} cannot break
     * out of the script element it is embedded in.
     */
    private static final Gson GSON = new Gson();

    private static final String ORGANIZATION = "AeroVance Systems";

    private JobPostingSchema() {
        // static helper
    }

    /**
     * @param postings the openings rendered on the page
     * @return a JSON-LD {@code ItemList} of job postings, or {@code null} when there is
     *         nothing to describe — an empty graph is worse than none, as it advertises a
     *         careers page with no roles
     */
    static String toJsonLd(List<JobPosting> postings) {
        if (postings == null || postings.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> elements = new ArrayList<>();
        int position = 1;
        for (JobPosting posting : postings) {
            elements.add(listItem(position++, posting));
        }

        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("@context", "https://schema.org");
        graph.put("@type", "ItemList");
        graph.put("itemListElement", elements);
        return GSON.toJson(graph);
    }

    private static Map<String, Object> listItem(int position, JobPosting posting) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("@type", "ListItem");
        item.put("position", position);
        item.put("item", jobPosting(posting));
        return item;
    }

    private static Map<String, Object> jobPosting(JobPosting posting) {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("@type", "JobPosting");
        job.put("title", posting.getTitle());
        job.put("hiringOrganization", Map.of("@type", "Organization", "name", ORGANIZATION));
        job.put("url", posting.getApplyUrl());

        putIfPresent(job, "identifier", posting.getId());
        putIfPresent(job, "description", posting.getSummary());
        putIfPresent(job, "employmentType", posting.getEmploymentType());
        putIfPresent(job, "occupationalCategory", posting.getFunction());

        if (posting.getPostedOn() != null) {
            job.put("datePosted", posting.getPostedOn().toString());
        }
        if (StringUtils.isNotBlank(posting.getLocation())) {
            job.put("jobLocation", Map.of(
                    "@type", "Place",
                    "address", Map.of("@type", "PostalAddress", "addressLocality", posting.getLocation())));
        }
        if (posting.isRemote()) {
            // The property Google reads to mark a role as remote; the address above stays,
            // because a remote role still has a hiring region.
            job.put("jobLocationType", "TELECOMMUTE");
        }
        return job;
    }

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }
}
