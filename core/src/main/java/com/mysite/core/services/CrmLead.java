package com.mysite.core.services;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * A lead captured by a gated download form.
 *
 * <p>Only the fields the form asks for. Nothing is inferred, enriched or derived from the
 * request — an IP address or a user agent added quietly here would be personal data the
 * consent notice on the form never mentioned.</p>
 */
public final class CrmLead {

    private final String email;
    private final String firstName;
    private final String lastName;
    private final String company;
    private final String country;
    private final String resourceTitle;
    private final String resourcePath;
    private final boolean marketingConsent;

    public CrmLead(String email, String firstName, String lastName, String company,
                   String country, String resourceTitle, String resourcePath,
                   boolean marketingConsent) {
        this.email = StringUtils.trimToNull(email);
        this.firstName = StringUtils.trimToNull(firstName);
        this.lastName = StringUtils.trimToNull(lastName);
        this.company = StringUtils.trimToNull(company);
        this.country = StringUtils.trimToNull(country);
        this.resourceTitle = resourceTitle;
        this.resourcePath = resourcePath;
        this.marketingConsent = marketingConsent;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getCompany() {
        return company;
    }

    public String getCountry() {
        return country;
    }

    /** @return the title of the document that was requested */
    public String getResourceTitle() {
        return resourceTitle;
    }

    /** @return the DAM path of the document that was requested */
    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * @return whether the visitor opted in to marketing contact. Separate from submitting the
     *         form: downloading a whitepaper is not consent to be called about it, and in the
     *         EU treating it as such is unlawful.
     */
    public boolean hasMarketingConsent() {
        return marketingConsent;
    }

    /**
     * @return {@code true} when the lead carries the minimum the CRM will accept. An email
     *         address alone is enough to fulfil the download; the rest is nice to have.
     */
    public boolean isValid() {
        // Deliberately not a full RFC 5322 check. Anything stricter rejects addresses that
        // are in fact valid, and the CRM does its own verification downstream anyway.
        return email != null && email.matches("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CrmLead)) {
            return false;
        }
        CrmLead that = (CrmLead) other;
        return marketingConsent == that.marketingConsent
                && Objects.equals(email, that.email)
                && Objects.equals(resourcePath, that.resourcePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, resourcePath, marketingConsent);
    }

    @Override
    public String toString() {
        // The email address is deliberately absent: this object ends up in log lines, and a
        // captured lead's address does not belong in error.log.
        return "CrmLead[resourcePath=" + resourcePath + ", consent=" + marketingConsent + "]";
    }
}
