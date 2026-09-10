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
import com.mysite.core.models.Bento;
import com.mysite.core.models.BentoCard;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { Bento.class, ComponentExporter.class },
       resourceType = BentoImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class BentoImpl extends AbstractComponentImpl implements Bento {

    public static final String RESOURCE_TYPE = "mysite/components/bento";

    private static final String NN_CARDS = "cards";

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String description;

    @ChildResource(name = NN_CARDS)
    private Resource cardsResource;

    private List<BentoCard> cards;

    @PostConstruct
    private void init() {
        if (cardsResource == null) {
            cards = Collections.emptyList();
            return;
        }
        // getData() is null when the page has the data layer switched off, so this is
        // also the switch for the per-tile entries.
        boolean dataLayerEnabled = getData() != null;
        String parentId = getId();

        List<BentoCard> collected = new ArrayList<>();
        int index = 0;
        for (Resource child : cardsResource.getChildren()) {
            BentoCardImpl card = new BentoCardImpl(child, parentId, index, dataLayerEnabled);
            // A tile with neither a title nor artwork would render as an empty box.
            if (card.hasContent()) {
                collected.add(card);
                index++;
            }
        }
        cards = Collections.unmodifiableList(collected);
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
    public String getDescription() {
        return description;
    }

    @Override
    public List<BentoCard> getCards() {
        return cards;
    }

    @Override
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    @Override
    protected ComponentData getComponentData() {
        // Enrich the inherited entry so Analytics reports a readable name instead of
        // just the resource type.
        return DataLayerBuilder.extending(super.getComponentData()).asComponent()
                .withTitle(() -> StringUtils.defaultString(heading))
                .withDescription(() -> StringUtils.defaultString(description))
                .build();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
