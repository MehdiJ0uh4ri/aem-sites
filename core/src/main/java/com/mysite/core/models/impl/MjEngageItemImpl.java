package com.mysite.core.models.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.mysite.core.models.MjEngageItem;

/**
 * Plain value object for one audience lane. Like a slide, its id has to come from the
 * parent component, so it is not an independently-adapted model.
 */
class MjEngageItemImpl implements MjEngageItem {

    static final String TYPE = "mysite/components/mjengage/lane";

    private final String id;
    private final String audience;
    private final String body;
    private final String ctaText;
    private final String ctaUrl;
    private final String dataLayerJson;

    MjEngageItemImpl(Resource resource, String parentId, int index, boolean dataLayerEnabled) {
        ValueMap properties = resource.getValueMap();
        this.audience = properties.get("audience", String.class);
        this.body = properties.get("body", String.class);
        this.ctaText = properties.get("ctaText", String.class);
        this.ctaUrl = LinkUtil.toUrl(properties.get("ctaLink", String.class));
        this.id = StringUtils.isBlank(parentId) ? null : parentId + "-lane-" + index;
        this.dataLayerJson = ChildDataLayer.json(dataLayerEnabled, this.id, TYPE, this.audience, this.ctaUrl);
    }

    boolean hasContent() {
        return StringUtils.isNotBlank(audience) || StringUtils.isNotBlank(body);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getAudience() {
        return audience;
    }

    @Override
    public String getBody() {
        return body;
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
