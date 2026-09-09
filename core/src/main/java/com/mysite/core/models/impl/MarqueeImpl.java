package com.mysite.core.models.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.util.AbstractComponentImpl;
import com.mysite.core.models.Marquee;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { Marquee.class, ComponentExporter.class },
       resourceType = MarqueeImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MarqueeImpl extends AbstractComponentImpl implements Marquee {

    public static final String RESOURCE_TYPE = "mysite/components/marquee";

    static final int DEFAULT_DURATION = 30;
    static final int MIN_DURATION = 5;
    static final int MAX_DURATION = 120;

    /** Simple (non-composite) multifield: Granite stores the values as a String[]. */
    @ValueMapValue
    private String[] items;

    @ValueMapValue
    private Integer duration;

    private List<String> phrases;

    @PostConstruct
    private void init() {
        phrases = items == null
                ? Collections.emptyList()
                : Arrays.stream(items)
                        .filter(StringUtils::isNotBlank)
                        .map(String::trim)
                        .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<String> getItems() {
        return phrases;
    }

    @Override
    public int getDuration() {
        if (duration == null) {
            return DEFAULT_DURATION;
        }
        // The dialog is a number field, so an author can type anything; a zero or negative
        // duration would make the CSS animation degenerate.
        return Math.min(MAX_DURATION, Math.max(MIN_DURATION, duration));
    }

    @Override
    public boolean isEmpty() {
        return phrases.isEmpty();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
