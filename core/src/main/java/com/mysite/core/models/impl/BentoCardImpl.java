package com.mysite.core.models.impl;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.mysite.core.models.BentoCard;

@Model(adaptables = Resource.class,
       adapters = BentoCard.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class BentoCardImpl implements BentoCard {

    static final String DEFAULT_SPAN = "normal";

    /**
     * The dialog offers exactly these; anything else (hand-edited content, or a value
     * left over from an older dialog) falls back rather than emitting an unknown class.
     */
    private static final List<String> SPANS = Arrays.asList("normal", "wide", "tall");

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String fileReference;

    @ValueMapValue
    private String alt;

    @ValueMapValue
    private String link;

    @ValueMapValue
    private String span;

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
        return StringUtils.defaultString(alt);
    }

    @Override
    public String getUrl() {
        return LinkUtil.toUrl(link);
    }

    @Override
    public String getSpan() {
        return SPANS.contains(span) ? span : DEFAULT_SPAN;
    }
}
