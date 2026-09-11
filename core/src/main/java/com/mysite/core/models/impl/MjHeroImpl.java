package com.mysite.core.models.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.mysite.core.models.MjHero;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { MjHero.class, ComponentExporter.class },
       resourceType = MjHeroImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MjHeroImpl extends MjComponentImpl implements MjHero {

    public static final String RESOURCE_TYPE = "mysite/components/mjhero";

    private static final String CTA_TYPE = RESOURCE_TYPE + "/cta";

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String lineOne;

    @ValueMapValue
    private String lineTwo;

    @ValueMapValue
    private String standfirst;

    @ValueMapValue
    private String fileReference;

    @ValueMapValue
    private String alt;

    @ValueMapValue
    private String ctaText;

    @ValueMapValue
    private String ctaLink;

    @ValueMapValue
    private String scrollCue;

    @Override
    public String getEyebrow() {
        return eyebrow;
    }

    @Override
    public String getLineOne() {
        return lineOne;
    }

    @Override
    public String getLineTwo() {
        return lineTwo;
    }

    @Override
    public String getStandfirst() {
        return standfirst;
    }

    @Override
    public String getFileReference() {
        return fileReference;
    }

    @Override
    public String getAlt() {
        // The band is art direction, not information: an empty alt is the correct default.
        return StringUtils.defaultString(alt);
    }

    @Override
    public String getCtaText() {
        return ctaText;
    }

    @Override
    public String getCtaUrl() {
        return LinkUtil.toUrl(ctaLink);
    }

    @Override
    public String getCtaId() {
        return StringUtils.isBlank(getCtaUrl()) ? null : getId() + "-cta";
    }

    @Override
    public String getCtaDataLayerJson() {
        return ChildDataLayer.json(getData() != null, getCtaId(), CTA_TYPE, ctaText, getCtaUrl());
    }

    @Override
    public String getScrollCue() {
        return scrollCue;
    }

    @Override
    public boolean isEmpty() {
        return StringUtils.isBlank(lineOne) && StringUtils.isBlank(lineTwo);
    }

    @Override
    protected ComponentData getComponentData() {
        return DataLayerBuilder.extending(super.getComponentData()).asComponent()
                .withTitle(() -> StringUtils.trimToEmpty(
                        StringUtils.trimToEmpty(lineOne) + " " + StringUtils.trimToEmpty(lineTwo)))
                .withDescription(() -> StringUtils.defaultString(standfirst))
                .withLinkUrl(() -> StringUtils.defaultString(getCtaUrl()))
                .build();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
