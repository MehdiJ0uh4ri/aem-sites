package com.mysite.core.workflows;

import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.mysite.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class ExportControlGateProcessTest {

    private static final String CLEARED_PAGE = "/content/mysite/us/en/programs/av-380x";
    private static final String UNCLEARED_PAGE = "/content/mysite/us/en/programs/av-120";
    private static final String NO_CLASSIFICATION_PAGE = "/content/mysite/us/en/programs/av-90";

    private final AemContext context = AppAemContext.newAemContext();

    private final ExportControlGateProcess process = new ExportControlGateProcess();

    private MetaDataMap args;

    @BeforeEach
    void setUp() {
        context.create().page(CLEARED_PAGE, "", Map.of(
                ExportControlGateProcess.PN_CLEARED, true,
                ExportControlGateProcess.PN_CLASSIFICATION, "EAR99"));
        context.create().page(UNCLEARED_PAGE, "", Map.of("jcr:title", "AV-120"));
        context.create().page(NO_CLASSIFICATION_PAGE, "", Map.of(
                ExportControlGateProcess.PN_CLEARED, true));

        args = Mockito.mock(MetaDataMap.class);
    }

    /** A work item whose payload is the given path, backed by the test resource resolver. */
    private WorkItem workItemFor(String path) {
        WorkflowData data = Mockito.mock(WorkflowData.class);
        Mockito.when(data.getPayload()).thenReturn(path);

        WorkItem item = Mockito.mock(WorkItem.class);
        Mockito.when(item.getWorkflowData()).thenReturn(data);
        return item;
    }

    private WorkflowSession session() {
        WorkflowSession session = Mockito.mock(WorkflowSession.class);
        Mockito.when(session.adaptTo(ResourceResolver.class)).thenReturn(context.resourceResolver());
        return session;
    }

    @Test
    void clearedPageWithAClassificationPasses() {
        assertDoesNotThrow(() -> process.execute(workItemFor(CLEARED_PAGE), session(), args));
    }

    @Test
    void passingTheGateWritesAnAuditRecordOnThePage() throws WorkflowException {
        process.execute(workItemFor(CLEARED_PAGE), session(), args);

        ValueMap properties = context.resourceResolver()
                .getResource(CLEARED_PAGE + "/jcr:content").getValueMap();

        // An approval nobody can evidence later is worth less than no approval at all, so
        // both who and when are recorded on the page itself, not only in workflow history.
        assertNotNull(properties.get(ExportControlGateProcess.PN_GATE_PASSED_AT));
        assertNotNull(properties.get(ExportControlGateProcess.PN_GATE_PASSED_BY, String.class));
    }

    @Test
    void pageWithoutARecordedReviewIsBlocked() {
        WorkflowException thrown = assertThrows(WorkflowException.class,
                () -> process.execute(workItemFor(UNCLEARED_PAGE), session(), args));

        // The message lands in the workflow inbox, so it must say what to do next.
        assertTrue(thrown.getMessage().contains("Export control review has not been recorded"));
        assertTrue(thrown.getMessage().contains(UNCLEARED_PAGE));
    }

    @Test
    void tickedBoxWithoutAClassificationIsNotAReview() {
        WorkflowException thrown = assertThrows(WorkflowException.class,
                () -> process.execute(workItemFor(NO_CLASSIFICATION_PAGE), session(), args));

        assertTrue(thrown.getMessage().contains("no export-control classification"));
    }

    @Test
    void unrecognisedPayloadFailsClosed() {
        // A payload that is not a page must never be treated as cleared by default.
        assertThrows(WorkflowException.class,
                () -> process.execute(workItemFor("/content/dam/mysite/not-a-page"), session(), args));
    }

    @Test
    void unresolvableSessionFailsClosed() {
        WorkflowSession broken = Mockito.mock(WorkflowSession.class);
        Mockito.when(broken.adaptTo(ResourceResolver.class)).thenReturn(null);

        assertThrows(WorkflowException.class,
                () -> process.execute(workItemFor(CLEARED_PAGE), broken, args));
    }
}
