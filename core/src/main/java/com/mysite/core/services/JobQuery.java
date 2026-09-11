package com.mysite.core.services;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * The filters a visitor applied to the job search, as an immutable value.
 *
 * <p>Blank filters are normalised to {@code null} on construction so that
 * {@code ?location=} and no {@code location} parameter at all produce the same query — and
 * therefore the same cache key in {@link JobSearchService}.</p>
 */
public final class JobQuery {

    /** Bounded so a crafted {@code pageSize} cannot ask the ATS for its entire catalogue. */
    private static final int MAX_PAGE_SIZE = 100;

    private final String keyword;
    private final String location;
    private final String function;
    private final int page;
    private final int pageSize;

    /**
     * @param page     1-based; anything lower is treated as the first page
     * @param pageSize clamped to between 1 and 100
     */
    public JobQuery(String keyword, String location, String function, int page, int pageSize) {
        this.keyword = StringUtils.trimToNull(keyword);
        this.location = StringUtils.trimToNull(location);
        this.function = StringUtils.trimToNull(function);
        this.page = Math.max(page, 1);
        this.pageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    public String getKeyword() {
        return keyword;
    }

    public String getLocation() {
        return location;
    }

    public String getFunction() {
        return function;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    /** @return {@code true} when no filter is applied and the full list is being browsed */
    public boolean isUnfiltered() {
        return keyword == null && location == null && function == null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JobQuery)) {
            return false;
        }
        JobQuery that = (JobQuery) other;
        return page == that.page
                && pageSize == that.pageSize
                && Objects.equals(keyword, that.keyword)
                && Objects.equals(location, that.location)
                && Objects.equals(function, that.function);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyword, location, function, page, pageSize);
    }

    @Override
    public String toString() {
        return "JobQuery[keyword=" + keyword + ", location=" + location
                + ", function=" + function + ", page=" + page + ", pageSize=" + pageSize + "]";
    }
}
