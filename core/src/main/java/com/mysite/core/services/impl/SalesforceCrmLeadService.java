package com.mysite.core.services.impl;

import java.util.LinkedHashMap;
import java.util.Map;

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

import com.mysite.core.services.CrmLead;
import com.mysite.core.services.CrmLeadService;

/**
 * Posts gated-download leads to Salesforce's Web-to-Lead style REST endpoint.
 */
@Component(service = CrmLeadService.class, immediate = true)
@Designate(ocd = SalesforceCrmLeadService.Config.class)
public class SalesforceCrmLeadService implements CrmLeadService {

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceCrmLeadService.class);

    /**
     * Leads that could not be delivered are written here rather than to error.log, so an
     * outage produces one replayable file per incident instead of personal data scattered
     * through the general log. The appender is configured in ui.config.
     */
    private static final Logger LEAD_REPLAY_LOG = LoggerFactory.getLogger("com.mysite.leads.replay");

    @ObjectClassDefinition(name = "AeroVance — CRM Lead Service",
                           description = "Sends leads captured by gated download forms to Salesforce.")
    public @interface Config {

        @AttributeDefinition(name = "Endpoint",
                description = "Lead capture endpoint of the Salesforce org.")
        String endpoint() default "https://aerovance.my.salesforce.com/services/data/v59.0/sobjects/Lead";

        @AttributeDefinition(name = "Access token",
                description = "Sent as a bearer token. Set this through a Cloud Manager secret environment variable, never in a committed OSGi config.",
                type = AttributeType.PASSWORD)
        String accessToken() default "";

        @AttributeDefinition(name = "Lead source",
                description = "Written to the LeadSource field so campaign reporting can tell gated downloads apart from other channels.")
        String leadSource() default "Website — Gated Download";
    }

    /** Shape Salesforce expects. Field names are the API names on the Lead object. */
    private static final class LeadPayload extends LinkedHashMap<String, Object> {
        private static final long serialVersionUID = 1L;
    }

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    @Override
    public boolean submit(CrmLead lead) {
        if (!lead.isValid()) {
            // The servlet validates before calling, so reaching here means a caller skipped
            // that step rather than a visitor mistyping.
            LOG.warn("Refusing to submit an incomplete lead: {}", lead);
            return false;
        }

        if (StringUtils.isBlank(config.accessToken())) {
            // Expected on a local author instance with no Salesforce credentials.
            LOG.debug("No CRM token configured; {} was not submitted.", lead);
            return false;
        }

        Map<String, String> headers = Map.of("Authorization", "Bearer " + config.accessToken());
        boolean accepted = JsonHttp.post(config.endpoint(), headers, toPayload(lead), Map.class).isPresent();

        if (!accepted) {
            // The visitor still gets their download — see CrmLeadService#submit — so this is
            // the only record that the lead existed. It must be replayable.
            LEAD_REPLAY_LOG.error("UNDELIVERED_LEAD email={} company={} country={} consent={} resource={} title={}",
                    lead.getEmail(), lead.getCompany(), lead.getCountry(),
                    lead.hasMarketingConsent(), lead.getResourcePath(), lead.getResourceTitle());
        }
        return accepted;
    }

    private LeadPayload toPayload(CrmLead lead) {
        LeadPayload payload = new LeadPayload();
        payload.put("Email", lead.getEmail());
        // Salesforce requires LastName on a Lead. A form that only asked for an email still
        // has to produce a record, so the local part stands in rather than the call failing.
        payload.put("LastName", StringUtils.defaultIfBlank(lead.getLastName(),
                StringUtils.substringBefore(lead.getEmail(), "@")));
        putIfPresent(payload, "FirstName", lead.getFirstName());
        // Company is also mandatory; individuals downloading a whitepaper often have none.
        payload.put("Company", StringUtils.defaultIfBlank(lead.getCompany(), "Unknown"));
        putIfPresent(payload, "Country", lead.getCountry());
        payload.put("LeadSource", config.leadSource());
        payload.put("HasOptedOutOfEmail", !lead.hasMarketingConsent());
        putIfPresent(payload, "Requested_Document__c", lead.getResourceTitle());
        putIfPresent(payload, "Requested_Document_Path__c", lead.getResourcePath());
        return payload;
    }

    private void putIfPresent(LeadPayload payload, String field, String value) {
        if (StringUtils.isNotBlank(value)) {
            payload.put(field, value);
        }
    }
}
