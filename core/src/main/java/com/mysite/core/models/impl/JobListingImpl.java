package com.mysite.core.models.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.util.AbstractComponentImpl;
import com.mysite.core.models.JobListing;
import com.mysite.core.services.JobPosting;
import com.mysite.core.services.JobQuery;
import com.mysite.core.services.JobSearchResult;
import com.mysite.core.services.JobSearchService;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { JobListing.class, ComponentExporter.class },
       resourceType = JobListingImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class JobListingImpl extends AbstractComponentImpl implements JobListing {

    public static final String RESOURCE_TYPE = "mysite/components/joblisting";

    /** Query parameter names. Also the form field names, so a submit reproduces them exactly. */
    static final String PARAM_KEYWORD = "q";
    static final String PARAM_LOCATION = "location";
    static final String PARAM_FUNCTION = "function";
    static final String PARAM_PAGE = "page";

    private static final String DEFAULT_HEADING_ELEMENT = "h2";
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private JobSearchService jobSearchService;

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String headingElement;

    @ValueMapValue
    private Integer pageSize;

    /** Pre-set by the author; a visitor cannot widen the search past its business unit. */
    @ValueMapValue
    private String fixedFunction;

    private JobQuery query;
    private JobSearchResult result;

    @PostConstruct
    private void init() {
        query = new JobQuery(
                param(PARAM_KEYWORD),
                param(PARAM_LOCATION),
                // An authored function pins the component to one job family — a careers page
                // for Defence & Space should not list commercial cabin-crew roles just because
                // someone edited the URL.
                StringUtils.defaultIfBlank(fixedFunction, param(PARAM_FUNCTION)),
                parsePage(),
                pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);

        // The service never throws; an unreachable ATS comes back marked unavailable.
        result = jobSearchService == null
                ? null
                : jobSearchService.search(query);
    }

    private String param(String name) {
        return request == null ? null : StringUtils.trimToNull(request.getParameter(name));
    }

    private int parsePage() {
        String raw = param(PARAM_PAGE);
        if (raw == null) {
            return 1;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            // A hand-edited or truncated URL lands on the first page rather than an error.
            return 1;
        }
    }

    @Override
    public String getHeading() {
        return heading;
    }

    @Override
    public String getHeadingElement() {
        // The dialog already restricts the choice, so only the unauthored case is handled here.
        return StringUtils.defaultIfBlank(headingElement, DEFAULT_HEADING_ELEMENT);
    }

    @Override
    public List<JobPosting> getPostings() {
        return result == null ? List.of() : result.getPostings();
    }

    @Override
    public List<String> getLocations() {
        return result == null ? List.of() : result.getLocations();
    }

    @Override
    public List<String> getFunctions() {
        // The filter is meaningless when the author has pinned the component to one family.
        return result == null || StringUtils.isNotBlank(fixedFunction)
                ? List.of()
                : result.getFunctions();
    }

    @Override
    public String getKeyword() {
        return query.getKeyword();
    }

    @Override
    public String getSelectedLocation() {
        return query.getLocation();
    }

    @Override
    public String getSelectedFunction() {
        return StringUtils.isNotBlank(fixedFunction) ? null : query.getFunction();
    }

    @Override
    public int getTotal() {
        return result == null ? 0 : result.getTotal();
    }

    @Override
    public int getPage() {
        return query.getPage();
    }

    @Override
    public int getTotalPages() {
        return result == null ? 1 : result.getTotalPages();
    }

    @Override
    public String getPreviousPageQuery() {
        return getPage() > 1 ? pageQuery(getPage() - 1) : null;
    }

    @Override
    public String getNextPageQuery() {
        return getPage() < getTotalPages() ? pageQuery(getPage() + 1) : null;
    }

    /**
     * Builds the query string for another page of the same search, so paging never silently
     * drops the filters the visitor applied.
     */
    private String pageQuery(int targetPage) {
        List<String> params = new ArrayList<>();
        append(params, PARAM_KEYWORD, query.getKeyword());
        append(params, PARAM_LOCATION, query.getLocation());
        append(params, PARAM_FUNCTION, getSelectedFunction());
        append(params, PARAM_PAGE, String.valueOf(targetPage));
        return "?" + String.join("&", params);
    }

    private void append(List<String> params, String name, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            params.add(name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is guaranteed present; the parameter is dropped rather than the link breaking.
            params.add(name + "=");
        }
    }

    @Override
    public boolean isFiltered() {
        return !query.isUnfiltered();
    }

    @Override
    public boolean isUnavailable() {
        return result == null || result.isUnavailable();
    }

    @Override
    public boolean isEmpty() {
        return !isUnavailable() && getPostings().isEmpty();
    }

    @Override
    public String getStructuredData() {
        return JobPostingSchema.toJsonLd(getPostings());
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
