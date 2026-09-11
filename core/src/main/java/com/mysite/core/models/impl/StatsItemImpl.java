package com.mysite.core.models.impl;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.mysite.core.models.StatsItem;

@Model(adaptables = Resource.class,
       adapters = StatsItem.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class StatsItemImpl implements StatsItem {

    @ValueMapValue
    private String value;

    @ValueMapValue
    private String label;

    @ValueMapValue
    private String description;

    private ValueParts parts;

    @PostConstruct
    private void init() {
        parts = ValueParts.of(value);
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public Double getNumber() {
        return parts.getNumber();
    }

    @Override
    public String getPrefix() {
        return parts.getPrefix();
    }

    @Override
    public String getSuffix() {
        return parts.getSuffix();
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
