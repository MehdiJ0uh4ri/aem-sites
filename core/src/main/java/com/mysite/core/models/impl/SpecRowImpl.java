package com.mysite.core.models.impl;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.mysite.core.models.SpecRow;

@Model(adaptables = Resource.class,
       adapters = SpecRow.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SpecRowImpl implements SpecRow {

    @ValueMapValue
    private String label;

    @ValueMapValue
    private String category;

    @ValueMapValue
    private String metricValue;

    @ValueMapValue
    private String metricUnit;

    @ValueMapValue
    private String imperialValue;

    @ValueMapValue
    private String imperialUnit;

    @ValueMapValue
    private String note;

    private Double sortKey;

    @PostConstruct
    private void init() {
        sortKey = ValueParts.of(metricValue).getNumber();
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getMetricValue() {
        return metricValue;
    }

    @Override
    public String getMetricUnit() {
        return metricUnit;
    }

    @Override
    public String getImperialValue() {
        return imperialValue;
    }

    @Override
    public String getImperialUnit() {
        return imperialUnit;
    }

    @Override
    public boolean isDualUnit() {
        return StringUtils.isNotBlank(imperialValue);
    }

    @Override
    public String getNote() {
        return note;
    }

    @Override
    public Double getSortKey() {
        return sortKey;
    }
}
