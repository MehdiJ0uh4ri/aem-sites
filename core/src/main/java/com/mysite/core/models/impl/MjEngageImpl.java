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
import com.mysite.core.models.MjEngage;
import com.mysite.core.models.MjEngageItem;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { MjEngage.class, ComponentExporter.class },
       resourceType = MjEngageImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MjEngageImpl extends AbstractComponentImpl implements MjEngage {

    public static final String RESOURCE_TYPE = "mysite/components/mjengage";

    private static final String NN_ITEMS = "items";

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String statement;

    @ChildResource(name = NN_ITEMS)
    private Resource itemsResource;

    private List<MjEngageItem> items;

    @PostConstruct
    private void init() {
        items = Collections.emptyList();
        if (itemsResource == null) {
            return;
        }
        boolean dataLayerEnabled = getData() != null;
        String parentId = getId();

        List<MjEngageItem> collected = new ArrayList<>();
        int index = 0;
        for (Resource child : itemsResource.getChildren()) {
            MjEngageItemImpl item = new MjEngageItemImpl(child, parentId, index, dataLayerEnabled);
            if (item.hasContent()) {
                collected.add(item);
                index++;
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
    public List<MjEngageItem> getItems() {
        return items;
    }

    @Override
    public boolean isEmpty() {
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
