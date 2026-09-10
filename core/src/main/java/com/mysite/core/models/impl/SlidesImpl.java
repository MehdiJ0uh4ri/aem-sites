package com.mysite.core.models.impl;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.mysite.core.models.Slide;
import com.mysite.core.models.Slides;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { Slides.class, ComponentExporter.class },
       resourceType = SlidesImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class SlidesImpl extends AbstractComponentImpl implements Slides {

    public static final String RESOURCE_TYPE = "mysite/components/slides";

    private static final String NN_SLIDES = "slides";

    private static final List<String> LAYOUTS =
            Arrays.asList(LAYOUT_SPLIT, LAYOUT_SPOTLIGHT, LAYOUT_CARDS);

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String layout;

    @ChildResource(name = NN_SLIDES)
    private Resource slidesResource;

    private List<Slide> slides;

    @PostConstruct
    private void init() {
        if (slidesResource == null) {
            slides = Collections.emptyList();
            return;
        }
        // getData() is null when the page has the data layer switched off, so this is
        // also the switch for the per-slide entries.
        boolean dataLayerEnabled = getData() != null;
        String parentId = getId();

        List<Slide> collected = new ArrayList<>();
        int index = 0;
        for (Resource child : slidesResource.getChildren()) {
            SlideImpl slide = new SlideImpl(child, parentId, index, dataLayerEnabled);
            if (slide.hasContent()) {
                collected.add(slide);
                index++;
            }
        }
        slides = Collections.unmodifiableList(collected);
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
    public String getLayout() {
        return LAYOUTS.contains(layout) ? layout : LAYOUT_SPLIT;
    }

    @Override
    public List<Slide> getSlides() {
        return slides;
    }

    @Override
    public boolean isEmpty() {
        return slides.isEmpty();
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
