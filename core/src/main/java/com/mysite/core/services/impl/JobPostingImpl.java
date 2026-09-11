package com.mysite.core.services.impl;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.SerializedName;
import com.mysite.core.services.JobPosting;

/**
 * A posting as deserialized from the ATS response.
 *
 * <p>Gson populates the fields directly, so this class is both the wire shape and the model.
 * Keeping them together means there is one place to look when the vendor renames a field, and
 * the getters are where every defensive decision about the vendor's data lives.</p>
 */
final class JobPostingImpl implements JobPosting {

    private static final Logger LOG = LoggerFactory.getLogger(JobPostingImpl.class);

    @SerializedName("requisitionId")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("jobFamily")
    private String function;

    @SerializedName("primaryLocation")
    private String location;

    @SerializedName("timeType")
    private String employmentType;

    @SerializedName("postedDate")
    private String postedDate;

    @SerializedName("shortDescription")
    private String summary;

    @SerializedName("securityClearance")
    private String clearance;

    @SerializedName("externalUrl")
    private String applyUrl;

    @SerializedName("remoteType")
    private String remoteType;

    @SerializedName("tags")
    private List<String> tags;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getFunction() {
        return function;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getEmploymentType() {
        return employmentType;
    }

    @Override
    public LocalDate getPostedOn() {
        if (StringUtils.isBlank(postedDate)) {
            return null;
        }
        try {
            // The ATS sends ISO dates. A malformed one costs the card its "posted" line;
            // it must not cost the visitor the whole listing.
            return LocalDate.parse(StringUtils.substring(postedDate, 0, 10));
        } catch (DateTimeParseException e) {
            LOG.debug("Requisition {} has an unparsable posted date: {}", id, postedDate);
            return null;
        }
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public String getClearance() {
        // "None" is how the ATS says a role has no clearance requirement; rendering a badge
        // reading "None" would be noise on the majority of commercial roles.
        return StringUtils.equalsIgnoreCase(clearance, "none") ? null : StringUtils.trimToNull(clearance);
    }

    @Override
    public String getApplyUrl() {
        return applyUrl;
    }

    @Override
    public boolean isRemote() {
        return StringUtils.containsIgnoreCase(remoteType, "remote");
    }

    @Override
    public List<String> getTags() {
        return tags == null ? Collections.emptyList() : Collections.unmodifiableList(tags);
    }

    /**
     * @return {@code true} when the posting carries enough to render a usable card. A
     *         requisition with no title or no application URL is a data error at the vendor,
     *         and showing it would give the visitor something they cannot act on.
     */
    boolean isRenderable() {
        return StringUtils.isNotBlank(title) && StringUtils.isNotBlank(applyUrl);
    }
}
