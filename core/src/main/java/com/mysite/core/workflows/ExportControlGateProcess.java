package com.mysite.core.workflows;

import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.wcm.api.NameConstants;

/**
 * Blocks publication of a page whose technical content has not passed export-control review.
 *
 * <p>The site publishes aircraft performance figures, and some of those figures are
 * export-controlled under ITAR or the EAR. Publishing one that has not been cleared is a
 * regulatory breach, not a content mistake — so the check is a workflow step that fails the
 * workflow, rather than a note in a dialog that an author can read past.</p>
 *
 * <p>The step is deliberately not able to grant clearance itself. It only verifies that a
 * named reviewer recorded a decision on the page, and refuses to let the workflow continue
 * otherwise. Clearance is a human act; this process makes it a required one.</p>
 */
@Component(service = WorkflowProcess.class,
           property = { "process.label=AeroVance — Export Control Gate" })
@ServiceDescription("Fails the workflow unless the page carries a recorded export-control clearance.")
public class ExportControlGateProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(ExportControlGateProcess.class);

    /** Set by the reviewer on the page's Export Control properties tab. */
    static final String PN_CLEARED = "exportControlCleared";

    /** The classification the reviewer assigned, for example {@code EAR99}. */
    static final String PN_CLASSIFICATION = "exportControlClassification";

    /** Written by this step so the audit trail lives on the page, not only in the workflow history. */
    static final String PN_GATE_PASSED_AT = "exportControlGatePassedAt";
    static final String PN_GATE_PASSED_BY = "exportControlGatePassedBy";

    @Override
    public void execute(WorkItem item, WorkflowSession session, MetaDataMap args)
            throws WorkflowException {

        String payloadPath = item.getWorkflowData().getPayload().toString();
        ResourceResolver resolver = session.adaptTo(ResourceResolver.class);
        if (resolver == null) {
            throw new WorkflowException("Cannot resolve the workflow session; refusing to clear "
                    + payloadPath + " for publication.");
        }

        Resource content = resolver.getResource(payloadPath + "/" + NameConstants.NN_CONTENT);
        if (content == null) {
            // The payload is not a page. Failing closed is the only safe direction here: an
            // unrecognised payload must not be treated as cleared.
            throw new WorkflowException("Export control gate: " + payloadPath
                    + " has no page content to review. The workflow cannot clear it.");
        }

        ModifiableValueMap properties = content.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            throw new WorkflowException("Export control gate: cannot write the audit record on "
                    + payloadPath + ". The workflow user needs write access to the page.");
        }

        if (!properties.get(PN_CLEARED, false)) {
            // The message surfaces in the workflow inbox, so it says what to do next rather
            // than only what went wrong.
            throw new WorkflowException("Export control review has not been recorded for "
                    + payloadPath + ". Open the page properties, complete the Export Control tab,"
                    + " and restart this workflow. Publication is blocked until then.");
        }

        String classification = properties.get(PN_CLASSIFICATION, String.class);
        if (StringUtils.isBlank(classification)) {
            // A clearance with no classification is not a reviewed decision; it is a ticked box.
            throw new WorkflowException("Page " + payloadPath + " is marked as cleared but carries"
                    + " no export-control classification. Record the classification (for example"
                    + " EAR99, or the USML category) before publication.");
        }

        record(properties, resolver, payloadPath, classification);
    }

    private void record(ModifiableValueMap properties, ResourceResolver resolver,
                        String payloadPath, String classification) throws WorkflowException {
        // The resolver carries the identity the workflow step is running as, which is the
        // approver whose decision this record is evidence of.
        String approver = StringUtils.defaultIfBlank(resolver.getUserID(), "unknown");
        properties.put(PN_GATE_PASSED_AT, Calendar.getInstance());
        properties.put(PN_GATE_PASSED_BY, approver);

        try {
            resolver.commit();
        } catch (PersistenceException e) {
            // If the audit record cannot be written, the page must not proceed: an approval
            // nobody can evidence later is worth less than no approval at all.
            throw new WorkflowException("Could not write the export-control audit record on "
                    + payloadPath, e);
        }

        LOG.info("Export control gate passed for {} (classification {}, approved by {})",
                payloadPath, classification, approver);
    }
}
