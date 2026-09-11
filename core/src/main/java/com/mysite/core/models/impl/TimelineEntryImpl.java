package com.mysite.core.models.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.mysite.core.models.TimelineEntry;

@Model(adaptables = Resource.class,
       adapters = TimelineEntry.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TimelineEntryImpl implements TimelineEntry {

    @ValueMapValue
    private String period;

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String fileReference;

    @ValueMapValue
    private String alt;

    @ValueMapValue
    private boolean highlighted;

    @Override
    public String getPeriod() {
        return period;
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
        // Decorative by default: the milestone text already carries the meaning.
        return StringUtils.defaultString(alt);
    }

    @Override
    public boolean isHighlighted() {
        return highlighted;
    }
}
