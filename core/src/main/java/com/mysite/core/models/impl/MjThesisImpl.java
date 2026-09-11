package com.mysite.core.models.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.mysite.core.models.MjThesis;
import com.mysite.core.models.MjThesisItem;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { MjThesis.class, ComponentExporter.class },
       resourceType = MjThesisImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MjThesisImpl extends MjComponentImpl implements MjThesis {

    private static final Logger LOG = LoggerFactory.getLogger(MjThesisImpl.class);

    public static final String RESOURCE_TYPE = "mysite/components/mjthesis";

    private static final String NN_ITEMS = "items";

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String heading;

    @ChildResource(name = NN_ITEMS)
    private Resource itemsResource;

    /** Optional DAM folder of Thesis fragments. Set, it wins over the dialog multifield. */
    @ValueMapValue
    private String parentPath;

    @SlingObject
    private ResourceResolver resourceResolver;

    private List<MjThesisItem> items;

    @PostConstruct
    private void init() {
        items = Collections.emptyList();
        List<MjThesisItem> collected = new ArrayList<>();

        Resource folder = StringUtils.isBlank(parentPath) ? null : resourceResolver.getResource(parentPath);
        if (folder != null) {
            List<MjThesisItemImpl> fromFragments = new ArrayList<>();
            for (Resource child : folder.getChildren()) {
                if (!FragmentReader.isFragment(child)) {
                    continue;
                }
                MjThesisItemImpl item = MjThesisItemImpl.fromFragment(child, fromFragments.size());
                if (item.hasContent()) {
                    fromFragments.add(item);
                }
            }
            // A numbered list has to read 01, 02, 03. JCR child order is whatever order the
            // fragments happened to be created in, so the authored ordinal decides.
            fromFragments.sort(Comparator.comparing(MjThesisItem::getOrdinal));
            collected.addAll(fromFragments);
            if (collected.isEmpty()) {
                LOG.warn("Thesis '{}' points at {} but found no thesis fragments there.",
                        getId(), parentPath);
            }
            items = Collections.unmodifiableList(collected);
            return;
        }

        if (itemsResource == null) {
            return;
        }
        int index = 0;
        for (Resource child : itemsResource.getChildren()) {
            MjThesisItemImpl item = new MjThesisItemImpl(child, index);
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
    public String getHeading() {
        return heading;
    }

    @Override
    public List<MjThesisItem> getItems() {
        return items;
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    protected ComponentData getComponentData() {
        return DataLayerBuilder.extending(super.getComponentData()).asComponent()
                .withTitle(() -> StringUtils.defaultString(heading))
                .build();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
