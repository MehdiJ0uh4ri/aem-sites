package com.mysite.core.models.impl;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import com.mysite.core.models.StatsBar;
import com.mysite.core.models.StatsItem;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { StatsBar.class, ComponentExporter.class },
       resourceType = StatsBarImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class StatsBarImpl extends AbstractComponentImpl implements StatsBar {

    public static final String RESOURCE_TYPE = "mysite/components/statsbar";

    /**
     * Name of the composite multifield node holding the individual statistics.
     */
    private static final String NN_ITEMS = "items";

    private static final String DEFAULT_HEADING_ELEMENT = "h2";

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String headingElement;

    @ChildResource(name = NN_ITEMS)
    private Resource itemsResource;

    private List<StatsItem> items;

    @PostConstruct
    private void init() {
        if (itemsResource == null) {
            items = Collections.emptyList();
            return;
        }
        items = StreamSupport.stream(itemsResource.getChildren().spliterator(), false)
                .map(child -> child.adaptTo(StatsItem.class))
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.isNotBlank(item.getValue()))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String getHeading() {
        return heading;
    }

    @Override
    public String getHeadingElement() {
        // The dialog already restricts the choice to the supported elements, so only the
        // unauthored case is handled here. HTL's data-sly-element enforces the element-name
        // context on output.
        return StringUtils.defaultIfBlank(headingElement, DEFAULT_HEADING_ELEMENT);
    }

    @Override
    public List<StatsItem> getItems() {
        return items;
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
