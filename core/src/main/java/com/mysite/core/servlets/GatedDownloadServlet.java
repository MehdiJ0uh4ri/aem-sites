package com.mysite.core.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.mysite.core.models.ResourceEntry;
import com.mysite.core.models.ResourceLibrary;
import com.mysite.core.services.CrmLead;
import com.mysite.core.services.CrmLeadService;

/**
 * Serves a gated document once the lead form has been completed.
 *
 * <p>The gate is real, not cosmetic. Gated assets live under a DAM folder that {@code everyone}
 * cannot read on publish, so the only way to the file is through this servlet — guessing the
 * asset URL returns 404. The servlet reads the file with a service user that has read access
 * to that folder and nothing else.</p>
 *
 * <p>Two rules govern what it will serve. It only serves a path that the component being
 * posted to actually lists, so a crafted {@code asset} parameter cannot be used to read an
 * arbitrary node through the service user. And it serves the document even when the CRM
 * rejected the lead, because the visitor completed their side of the exchange and an internal
 * outage is not their problem.</p>
 */
@Component(service = { Servlet.class })
@SlingServletResourceTypes(
        resourceTypes = "mysite/components/resourcelibrary",
        selectors = "download",
        methods = HttpConstants.METHOD_POST)
@ServiceDescription("AeroVance Gated Download Servlet")
public class GatedDownloadServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(GatedDownloadServlet.class);

    /** Declared in ui.config; granted read on the gated DAM folder only. */
    private static final String SERVICE_USER = "aerovance-gated-download";

    private static final String PARAM_ASSET = "asset";
    private static final String PARAM_EMAIL = "email";
    private static final String PARAM_FIRST_NAME = "firstName";
    private static final String PARAM_LAST_NAME = "lastName";
    private static final String PARAM_COMPANY = "company";
    private static final String PARAM_COUNTRY = "country";
    private static final String PARAM_CONSENT = "marketingConsent";

    @Reference
    private transient ResourceResolverFactory resolverFactory;

    @Reference
    private transient CrmLeadService crmLeadService;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        ResourceLibrary library = request.adaptTo(ResourceLibrary.class);
        if (library == null) {
            response.sendError(SlingHttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String requestedPath = request.getParameter(PARAM_ASSET);
        Optional<ResourceEntry> entry = findGatedEntry(library, requestedPath);
        if (entry.isEmpty()) {
            // Either the path is not one this component offers, or it is offered ungated —
            // in which case it is already linked directly and there is nothing to gate.
            LOG.warn("Refused gated download of {}: not a gated entry of {}",
                    requestedPath, request.getResource().getPath());
            response.sendError(SlingHttpServletResponse.SC_FORBIDDEN);
            return;
        }

        CrmLead lead = readLead(request, entry.get());
        if (!lead.isValid()) {
            response.sendError(SlingHttpServletResponse.SC_BAD_REQUEST,
                    "A valid email address is required.");
            return;
        }

        // Fire and continue: the return value only decides whether a replay record is written.
        crmLeadService.submit(lead);

        stream(entry.get(), response);
    }

    /**
     * @return the entry only if the component actually lists this path as gated. This check is
     *         what stops the service user from becoming an arbitrary file reader.
     */
    private Optional<ResourceEntry> findGatedEntry(ResourceLibrary library, String path) {
        if (StringUtils.isBlank(path)) {
            return Optional.empty();
        }
        return library.getEntries().stream()
                .filter(ResourceEntry::isGated)
                .filter(candidate -> StringUtils.equals(candidate.getAssetPath(), path))
                .findFirst();
    }

    private CrmLead readLead(SlingHttpServletRequest request, ResourceEntry entry) {
        return new CrmLead(
                request.getParameter(PARAM_EMAIL),
                request.getParameter(PARAM_FIRST_NAME),
                request.getParameter(PARAM_LAST_NAME),
                request.getParameter(PARAM_COMPANY),
                request.getParameter(PARAM_COUNTRY),
                entry.getTitle(),
                entry.getAssetPath(),
                // An unchecked box submits nothing at all, so absence is a "no".
                StringUtils.isNotBlank(request.getParameter(PARAM_CONSENT)));
    }

    private void stream(ResourceEntry entry, SlingHttpServletResponse response) throws IOException {
        Map<String, Object> auth = new HashMap<>();
        auth.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(auth)) {
            Resource assetResource = resolver.getResource(entry.getAssetPath());
            Asset asset = assetResource == null ? null : assetResource.adaptTo(Asset.class);
            Rendition original = asset == null ? null : asset.getOriginal();

            if (original == null) {
                // The lead has already been recorded at this point, so the visitor has paid
                // for a document that is not there. That is an authoring error worth an alert.
                LOG.error("Gated asset {} is missing or has no original rendition", entry.getAssetPath());
                response.sendError(SlingHttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String fileName = StringUtils.substringAfterLast(entry.getAssetPath(), "/");
            response.setContentType(StringUtils.defaultIfBlank(original.getMimeType(),
                    "application/octet-stream"));
            response.setHeader("Content-Length", String.valueOf(original.getSize()));
            // RFC 6266 filename*: programme documents carry designations with non-ASCII
            // characters, and the plain filename parameter cannot represent them.
            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + fileName.replace("\"", "") + "\"; filename*=UTF-8''"
                    + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()));
            // The response is personal to this visitor's request and must never be cached by
            // the dispatcher, a CDN, or a shared proxy.
            response.setHeader("Cache-Control", "private, no-store, max-age=0");

            try (InputStream in = original.getStream();
                 OutputStream out = response.getOutputStream()) {
                IOUtils.copy(in, out);
            }

        } catch (LoginException e) {
            // The service user mapping is missing or was not deployed with the bundle.
            LOG.error("Cannot obtain the {} service user; gated downloads are unavailable", SERVICE_USER, e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
