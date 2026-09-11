package com.mysite.core.models.impl;

import java.util.ArrayList;
import java.util.Collections;
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
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.adobe.cq.wcm.core.components.util.AbstractComponentImpl;
import com.mysite.core.models.MjSignal;
import com.mysite.core.models.MjSignalItem;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { MjSignal.class, ComponentExporter.class },
       resourceType = MjSignalImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MjSignalImpl extends AbstractComponentImpl implements MjSignal {

    public static final String RESOURCE_TYPE = "mysite/components/mjsignal";

    private static final String NN_ITEMS = "items";

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String statement;

    @ValueMapValue
    private String footnote;

    @ValueMapValue
    private String fileReference;

    @ValueMapValue
    private String alt;

    @ChildResource(name = NN_ITEMS)
    private Resource itemsResource;

    private List<MjSignalItem> items;

    @PostConstruct
    private void init() {
        items = Collections.emptyList();
        if (itemsResource == null) {
            return;
        }
        List<MjSignalItem> collected = new ArrayList<>();
        for (Resource child : itemsResource.getChildren()) {
            MjSignalItemImpl item = new MjSignalItemImpl(child);
            if (item.hasContent()) {
                collected.add(item);
            }
        }
        items = Collections.unmodifiableList(collected);
    }

    @Override
    public String getEyebrow() {
        return eyebrow;
    }

    @Override
    public String getStatement() {
        return statement;
    }

    @Override
    public List<MjSignalItem> getItems() {
        return items;
    }

    @Override
    public String getFootnote() {
        return footnote;
    }

    @Override
    public String getFileReference() {
        return fileReference;
    }

    @Override
    public String getAlt() {
        // Composition, not information: the copy beside it already says what it is.
        return StringUtils.defaultString(alt);
    }

    @Override
    public boolean isEmpty() {
        // The statement carries the section on its own; the ledger is supporting evidence.
        return StringUtils.isBlank(statement) && items.isEmpty();
    }

    @Override
    protected ComponentData getComponentData() {
        return DataLayerBuilder.extending(super.getComponentData()).asComponent()
                .withTitle(() -> StringUtils.defaultString(statement))
                .build();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
