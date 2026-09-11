package com.mysite.core.models.impl;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.adobe.cq.wcm.core.components.util.AbstractComponentImpl;
import com.mysite.core.models.MjFeature;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { MjFeature.class, ComponentExporter.class },
       resourceType = MjFeatureImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MjFeatureImpl extends AbstractComponentImpl implements MjFeature {

    public static final String RESOURCE_TYPE = "mysite/components/mjfeature";

    private static final String CTA_TYPE = RESOURCE_TYPE + "/cta";

    @ValueMapValue
    private String variant;

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String fragmentPath;

    /** Overrides — authored on the component, they win over the fragment's own fields. */
    @ValueMapValue
    private String titleOverride;

    @ValueMapValue
    private String standfirstOverride;

    @ValueMapValue
    private String fileReferenceOverride;

    @ValueMapValue
    private String alt;

    @ValueMapValue
    private String ctaText;

    @ValueMapValue
    private String ctaLink;

    @SlingObject
    private ResourceResolver resourceResolver;

    private String kicker;
    private String title;
    private String standfirst;
    private String outcome;
    private String meta;
    private String fileReference;

    @PostConstruct
    private void init() {
        ContentFragment fragment = FragmentReader.fragment(resourceResolver, fragmentPath);
        Resource fragmentResource = StringUtils.isBlank(fragmentPath)
                ? null : resourceResolver.getResource(fragmentPath);
        boolean caseStudy = VARIANT_CASESTUDY.equals(getVariant());

        if (caseStudy) {
            kicker = FragmentReader.text(fragment, fragmentResource, "company");
            title = FragmentReader.text(fragment, fragmentResource, "title");
            standfirst = FragmentReader.text(fragment, fragmentResource, "challenge", "summary");
            outcome = FragmentReader.text(fragment, fragmentResource, "outcome");
            meta = FragmentReader.date(fragment, fragmentResource, "investmentYear", "publicationDate");
        } else {
            kicker = FragmentReader.text(fragment, fragmentResource, "category");
            title = FragmentReader.text(fragment, fragmentResource, "title");
            standfirst = FragmentReader.text(fragment, fragmentResource, "summary");
            meta = FragmentReader.date(fragment, fragmentResource, "publicationDate");
        }
        fileReference = FragmentReader.text(fragment, fragmentResource, "heroImage", "featuredImage");

        // Overrides last, so an editor can retitle a feature on the homepage without
        // editing the fragment every other page reuses.
        title = StringUtils.defaultIfBlank(titleOverride, title);
        standfirst = StringUtils.defaultIfBlank(standfirstOverride, standfirst);
        fileReference = StringUtils.defaultIfBlank(fileReferenceOverride, fileReference);
    }

    @Override
    public String getVariant() {
        return VARIANT_CASESTUDY.equals(variant) ? VARIANT_CASESTUDY : VARIANT_INSIGHT;
    }

    @Override
    public String getEyebrow() {
        return eyebrow;
    }

    @Override
    public String getKicker() {
        return kicker;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getStandfirst() {
        return standfirst;
    }

    @Override
    public String getOutcome() {
        return outcome;
    }

    @Override
    public String getMeta() {
        return meta;
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
    public boolean isEmpty() {
        // A fragment that has been unpublished or moved leaves nothing to render; the
        // placeholder in edit mode tells the author, and publish renders nothing at all.
        return StringUtils.isBlank(title);
    }

    @Override
    protected ComponentData getComponentData() {
        return DataLayerBuilder.extending(super.getComponentData()).asComponent()
                .withTitle(() -> StringUtils.defaultString(title))
                .withDescription(() -> StringUtils.defaultString(standfirst))
                .withLinkUrl(() -> StringUtils.defaultString(getCtaUrl()))
                .build();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
