package com.mysite.core.models.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.mysite.core.models.MjSignalItem;

/**
 * Plain value object for one figure in the Investment Signal ledger.
 */
class MjSignalItemImpl implements MjSignalItem {

    private final String value;
    private final String label;
    private final String note;

    MjSignalItemImpl(Resource resource) {
        ValueMap properties = resource.getValueMap();
        this.value = properties.get("value", String.class);
        this.label = properties.get("label", String.class);
        this.note = properties.get("note", String.class);
    }

    boolean hasContent() {
        return StringUtils.isNotBlank(value) || StringUtils.isNotBlank(label);
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getNote() {
        return note;
    }
}
