package com.mysite.core.models.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.mysite.core.models.Slide;

/**
 * Plain value object rather than a Sling Model: a slide's id has to be derived from its
 * parent component's id, which an independently-adapted model cannot see.
 */
class SlideImpl implements Slide {

    static final String TYPE = "mysite/components/slides/slide";

    private final String id;
    private final String title;
    private final String description;
    private final String fileReference;
    private final String alt;
    private final String ctaText;
    private final String ctaUrl;
    private final String dataLayerJson;

    SlideImpl(Resource resource, String parentId, int index, boolean dataLayerEnabled) {
        ValueMap properties = resource.getValueMap();
        this.title = properties.get("title", String.class);
        this.description = properties.get("description", String.class);
        this.fileReference = properties.get("fileReference", String.class);
        this.alt = StringUtils.defaultString(properties.get("alt", String.class));
        this.ctaText = properties.get("ctaText", String.class);
        this.ctaUrl = LinkUtil.toUrl(properties.get("ctaLink", String.class));
        this.id = StringUtils.isBlank(parentId) ? null : parentId + "-slide-" + index;
        this.dataLayerJson = ChildDataLayer.json(dataLayerEnabled, this.id, TYPE, this.title, this.ctaUrl);
    }

    /** @return {@code true} when there is something worth rendering */
    boolean hasContent() {
        return StringUtils.isNotBlank(title) || StringUtils.isNotBlank(fileReference);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getFileReference() {
        return fileReference;
    }

    @Override
    public String getAlt() {
        return alt;
    }

    @Override
    public String getCtaText() {
        return ctaText;
    }

    @Override
    public String getCtaUrl() {
        return ctaUrl;
    }

    @Override
    public String getDataLayerJson() {
        return dataLayerJson;
    }
}
