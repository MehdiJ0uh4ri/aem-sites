package com.mysite.core.services;

/**
 * Sends leads captured by gated download forms to the CRM.
 */
public interface CrmLeadService {

    /**
     * Records a lead.
     *
     * <p>Never throws. A CRM outage must not cost the visitor their download: they completed
     * their side of the exchange, and withholding the document because an internal system is
     * down punishes them for it. Failures are logged for replay instead.</p>
     *
     * @param lead the captured lead
     * @return {@code true} when the CRM accepted the lead
     */
    boolean submit(CrmLead lead);
}
