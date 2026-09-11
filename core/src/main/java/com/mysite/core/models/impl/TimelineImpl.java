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
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.util.AbstractComponentImpl;
import com.mysite.core.models.Timeline;
import com.mysite.core.models.TimelineEntry;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { Timeline.class, ComponentExporter.class },
       resourceType = TimelineImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class TimelineImpl extends AbstractComponentImpl implements Timeline {

    public static final String RESOURCE_TYPE = "mysite/components/timeline";

    /** Name of the composite multifield node holding the milestones. */
    private static final String NN_ENTRIES = "entries";

    private static final String DEFAULT_HEADING_ELEMENT = "h2";

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String headingElement;

    @ChildResource(name = NN_ENTRIES)
    private Resource entriesResource;

    private List<TimelineEntry> entries;

    @PostConstruct
    private void init() {
        // The period is the spine of the layout; an entry without one has nowhere to sit.
        entries = MultifieldUtil.readItems(entriesResource, TimelineEntry.class,
                entry -> StringUtils.isNotBlank(entry.getPeriod()));
    }

    @Override
    public String getHeading() {
        return heading;
    }

    @Override
    public String getHeadingElement() {
        // The dialog already restricts the choice, so only the unauthored case is handled
        // here. HTL's data-sly-element enforces the element-name context on output.
        return StringUtils.defaultIfBlank(headingElement, DEFAULT_HEADING_ELEMENT);
    }

    @Override
    public List<TimelineEntry> getEntries() {
        return entries;
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
