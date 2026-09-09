package com.mysite.core.models.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.util.AbstractComponentImpl;
import com.mysite.core.models.Hero;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { Hero.class, ComponentExporter.class },
       resourceType = HeroImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class HeroImpl extends AbstractComponentImpl implements Hero {

    public static final String RESOURCE_TYPE = "mysite/components/hero";

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String fileReference;

    @ValueMapValue
    private String alt;

    @ValueMapValue
    private String primaryCtaText;

    @ValueMapValue
    private String primaryCtaLink;

    @ValueMapValue
    private String secondaryCtaText;

    @ValueMapValue
    private String secondaryCtaLink;

    @Override
    public String getEyebrow() {
        return eyebrow;
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
        // Decorative artwork by default: an empty alt keeps it out of the accessibility tree.
        return StringUtils.defaultString(alt);
    }

    @Override
    public String getPrimaryCtaText() {
        return primaryCtaText;
    }

    @Override
    public String getPrimaryCtaUrl() {
        return LinkUtil.toUrl(primaryCtaLink);
    }

    @Override
    public String getSecondaryCtaText() {
        return secondaryCtaText;
    }

    @Override
    public String getSecondaryCtaUrl() {
        return LinkUtil.toUrl(secondaryCtaLink);
    }

    @Override
    public boolean isEmpty() {
        return StringUtils.isBlank(title);
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
