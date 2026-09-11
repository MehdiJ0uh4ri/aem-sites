package com.mysite.core.models.impl;

import java.util.List;
import java.util.Map;

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
import com.mysite.core.models.ProgramTeaser;
import com.mysite.core.models.StatsItem;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { ProgramTeaser.class, ComponentExporter.class },
       resourceType = ProgramTeaserImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class ProgramTeaserImpl extends AbstractComponentImpl implements ProgramTeaser {

    public static final String RESOURCE_TYPE = "mysite/components/programteaser";

    /** Name of the composite multifield node holding the headline figures. */
    private static final String NN_SPECS = "specs";

    /**
     * The card lays the figures out in a single row. A fourth would wrap under the first
     * and read as a second, unrelated group.
     */
    private static final int MAX_SPECS = 3;

    /**
     * Status slugs the dialog offers, mapped to their English display text. The slug is what
     * lands in the repository, so relabelling a status later does not require a content
     * migration. HTL passes the label through i18n for translated sites.
     */
    private static final Map<String, String> STATUS_LABELS = Map.of(
            "in-development", "In development",
            "in-testing", "In flight test",
            "certified", "Certified",
            "in-service", "In service",
            "in-production", "In production",
            "legacy", "Legacy support");

    private static final String DEFAULT_LINK_TEXT = "View programme";

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String status;

    @ValueMapValue
    private String fileReference;

    @ValueMapValue
    private String alt;

    @ValueMapValue
    private String link;

    @ValueMapValue
    private String linkText;

    @ChildResource(name = NN_SPECS)
    private Resource specsResource;

    private List<StatsItem> specs;

    @PostConstruct
    private void init() {
        specs = MultifieldUtil.readItems(specsResource, StatsItem.class,
                spec -> StringUtils.isNotBlank(spec.getValue()), MAX_SPECS);
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
    public String getStatus() {
        // An unrecognised slug would style as an unknown state and read as a broken badge,
        // so only the statuses the dialog offers survive.
        return STATUS_LABELS.containsKey(status) ? status : null;
    }

    @Override
    public String getStatusLabel() {
        return STATUS_LABELS.get(status);
    }

    @Override
    public String getFileReference() {
        return fileReference;
    }

    @Override
    public String getAlt() {
        // Decorative by default: the card's headline already names the programme.
        return StringUtils.defaultString(alt);
    }

    @Override
    public List<StatsItem> getSpecs() {
        return specs;
    }

    @Override
    public String getLinkUrl() {
        return LinkUtil.toUrl(link);
    }

    @Override
    public String getLinkText() {
        return StringUtils.defaultIfBlank(linkText, DEFAULT_LINK_TEXT);
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
