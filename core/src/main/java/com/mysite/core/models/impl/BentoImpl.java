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
        cards = StreamSupport.stream(cardsResource.getChildren().spliterator(), false)
                .map(child -> child.adaptTo(BentoCard.class))
                .filter(Objects::nonNull)
                // A tile with neither a title nor artwork would render as an empty box.
                .filter(card -> StringUtils.isNotBlank(card.getTitle())
                        || StringUtils.isNotBlank(card.getFileReference()))
                .collect(Collectors.toUnmodifiableList());
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
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
