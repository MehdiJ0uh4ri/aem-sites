package com.mysite.core.services;

import java.time.LocalDate;
import java.util.List;

/**
 * One opening as the applicant tracking system holds it.
 *
 * <p>Jobs are never mirrored into the repository. The ATS is the record: a posting that closed
 * an hour ago must stop being applied to, and a nightly sync would leave the site advertising
 * roles that no longer exist. AEM renders what the ATS says right now.</p>
 */
public interface JobPosting {

    /** @return the requisition identifier, unique within the ATS */
    String getId();

    String getTitle();

    /** @return job family, for example {@code Structures Engineering} */
    String getFunction();

    /** @return the primary work location, for example {@code Toulouse, France} */
    String getLocation();

    /** @return employment type, for example {@code Full time} */
    String getEmploymentType();

    /** @return when the role was posted, or {@code null} when the ATS did not say */
    LocalDate getPostedOn();

    /** @return a short summary, plain text; the full description lives in the ATS */
    String getSummary();

    /**
     * @return the security clearance a candidate must already hold, for example
     *         {@code Secret}, or {@code null} when none is required. Surfaced on the card
     *         because it is the single largest filter on who can realistically apply to a
     *         defence programme role.
     */
    String getClearance();

    /** @return the absolute URL of the application form in the ATS */
    String getApplyUrl();

    /** @return {@code true} when the role can be performed remotely */
    boolean isRemote();

    /** @return arbitrary labels the ATS attached, never {@code null} */
    List<String> getTags();
}
