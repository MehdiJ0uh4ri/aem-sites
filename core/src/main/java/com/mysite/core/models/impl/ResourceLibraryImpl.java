package com.mysite.core.models.impl;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.util.AbstractComponentImpl;
import com.mysite.core.models.ResourceEntry;
import com.mysite.core.models.ResourceLibrary;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { ResourceLibrary.class, ComponentExporter.class },
       resourceType = ResourceLibraryImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class ResourceLibraryImpl extends AbstractComponentImpl implements ResourceLibrary {

    public static final String RESOURCE_TYPE = "mysite/components/resourcelibrary";

    /** Name of the composite multifield node holding the documents. */
    private static final String NN_ENTRIES = "entries";

    private static final String DEFAULT_HEADING_ELEMENT = "h2";

    private static final String DEFAULT_CONSENT_TEXT =
            "I would like to receive occasional updates from AeroVance Systems about products, "
                    + "programmes and events. You can withdraw consent at any time.";

    @SlingObject
    private Resource resource;

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String headingElement;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String consentText;

    @ChildResource(name = NN_ENTRIES)
    private Resource entriesResource;

    private List<ResourceEntry> entries;

    @PostConstruct
    private void init() {
        // An entry with no asset behind it is a dead row: the visitor either sees a broken
        // link or fills in a form for nothing.
        entries = MultifieldUtil.readItems(entriesResource, ResourceEntry.class,
                entry -> StringUtils.isNotBlank(entry.getTitle())
                        && StringUtils.isNotBlank(entry.getAssetPath()));
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
    public String getDescription() {
        return description;
    }

    @Override
    public List<ResourceEntry> getEntries() {
        return entries;
    }

    @Override
    public boolean isAnyGated() {
        return entries.stream().anyMatch(ResourceEntry::isGated);
    }

    @Override
    public String getFormAction() {
        // The component's own path with the download selector, which GatedDownloadServlet is
        // bound to. Posting to the component rather than to a /bin path keeps the handler
        // scoped to this resource type and gives it the authored entries for free.
        return resource.getPath() + ".download.html";
    }

    @Override
    public String getConsentText() {
        return StringUtils.defaultIfBlank(consentText, DEFAULT_CONSENT_TEXT);
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
