package com.mysite.core.models.impl;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.mysite.core.models.BentoCard;

/**
 * Plain value object rather than a Sling Model: a tile's id has to be derived from its
 * parent component's id, which an independently-adapted model cannot see.
 */
class BentoCardImpl implements BentoCard {

    static final String DEFAULT_SPAN = "normal";

    static final String TYPE = "mysite/components/bento/card";

    /**
     * The dialog offers exactly these; anything else (hand-edited content, or a value
     * left over from an older dialog) falls back rather than emitting an unknown class.
     */
    private static final List<String> SPANS = Arrays.asList("normal", "wide", "tall");

    private final String id;
    private final String title;
    private final String description;
    private final String fileReference;
    private final String alt;
    private final String url;
    private final String span;
    private final String dataLayerJson;

    BentoCardImpl(Resource resource, String parentId, int index, boolean dataLayerEnabled) {
        ValueMap properties = resource.getValueMap();
        this.title = properties.get("title", String.class);
        this.description = properties.get("description", String.class);
        this.fileReference = properties.get("fileReference", String.class);
        this.alt = StringUtils.defaultString(properties.get("alt", String.class));
        this.url = LinkUtil.toUrl(properties.get("link", String.class));
        String authoredSpan = properties.get("span", String.class);
        this.span = SPANS.contains(authoredSpan) ? authoredSpan : DEFAULT_SPAN;
        this.id = StringUtils.isBlank(parentId) ? null : parentId + "-card-" + index;
        this.dataLayerJson = ChildDataLayer.json(dataLayerEnabled, this.id, TYPE, this.title, this.url);
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
    public String getDataLayerJson() {
        return dataLayerJson;
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
    public String getUrl() {
        return url;
    }

    @Override
    public String getSpan() {
        return span;
    }
}
