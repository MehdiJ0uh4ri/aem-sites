package com.mysite.core.models.impl;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.util.AbstractComponentImpl;
import com.mysite.core.models.AeroHero;
import com.mysite.core.models.StatsItem;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { AeroHero.class, ComponentExporter.class },
       resourceType = AeroHeroImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class AeroHeroImpl extends AbstractComponentImpl implements AeroHero {

    public static final String RESOURCE_TYPE = "mysite/components/aerohero";

    /** Name of the composite multifield node holding the overlay figures. */
    private static final String NN_STATS = "stats";

    /**
     * The overlay panel is a three-up grid. Authoring a fourth figure would break the
     * layout rather than wrap, so the extra entries are dropped on render.
     */
    private static final int MAX_STATS = 3;

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
    private String videoReference;

    @ValueMapValue
    private String primaryCtaText;

    @ValueMapValue
    private String primaryCtaLink;

    @ValueMapValue
    private String secondaryCtaText;

    @ValueMapValue
    private String secondaryCtaLink;

    @ValueMapValue
    private String complianceNotice;

    @ChildResource(name = NN_STATS)
    private Resource statsResource;

    private List<StatsItem> stats;

    @PostConstruct
    private void init() {
        stats = MultifieldUtil.readItems(statsResource, StatsItem.class,
                item -> StringUtils.isNotBlank(item.getValue()), MAX_STATS);
    }

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
        // Decorative by default: an empty alt keeps the backdrop out of the accessibility tree.
        return StringUtils.defaultString(alt);
    }

    @Override
    public String getVideoReference() {
        return videoReference;
    }

    @Override
    public boolean isVideo() {
        return StringUtils.isNotBlank(videoReference);
    }

    @Override
    public String getPosterReference() {
        // Without a poster the browser paints the hero black until the first frame decodes,
        // which on a slow connection is most of the visit.
        return StringUtils.defaultIfBlank(fileReference, null);
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
    public List<StatsItem> getStats() {
        return stats;
    }

    @Override
    public String getComplianceNotice() {
        return complianceNotice;
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
