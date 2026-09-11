package com.mysite.core.models.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mysite.core.models.MjMarket;
import com.mysite.core.models.MjMetric;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { MjMarket.class, ComponentExporter.class },
       resourceType = MjMarketImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MjMarketImpl extends AbstractComponentImpl implements MjMarket {

    private static final Logger LOG = LoggerFactory.getLogger(MjMarketImpl.class);

    public static final String RESOURCE_TYPE = "mysite/components/mjmarket";

    /** Four figures is the most that stays readable at editorial size on a desktop row. */
    private static final int DEFAULT_LIMIT = 4;

    private static final int MAX_LIMIT = 12;

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String intro;

    @ValueMapValue
    private String parentPath;

    @ValueMapValue
    private Integer limit;

    @SlingObject
    private ResourceResolver resourceResolver;

    private List<MjMetric> metrics;

    @PostConstruct
    private void init() {
        metrics = Collections.emptyList();
        if (StringUtils.isBlank(parentPath)) {
            // Unconfigured is not an error: a freshly dropped component has no folder yet.
            return;
        }
        Resource folder = resourceResolver.getResource(parentPath);
        if (folder == null) {
            LOG.warn("Market band at {} points at {}, which does not resolve. Check the path "
                    + "and that the fragments are readable by this session.",
                    getResourcePath(), parentPath);
            return;
        }
        int max = Math.min(limit == null || limit <= 0 ? DEFAULT_LIMIT : limit, MAX_LIMIT);

        List<MjMetric> collected = new ArrayList<>();

        // An author pointing at one fragment rather than the folder around it is a
        // reasonable reading of "Metric folder", and the band renders perfectly well with
        // a single figure. Accepting it costs three lines and removes a dead end that
        // otherwise renders as nothing at all.
        if (FragmentReader.isFragment(folder)) {
            MjMetricImpl metric = new MjMetricImpl(FragmentReader.of(folder), folder);
            if (metric.hasContent()) {
                collected.add(metric);
            }
            metrics = Collections.unmodifiableList(collected);
            return;
        }

        for (Resource child : folder.getChildren()) {
            if (!FragmentReader.isFragment(child)) {
                // Folders, images, renditions and anything else that is not a fragment.
                continue;
            }
            MjMetricImpl metric = new MjMetricImpl(FragmentReader.of(child), child);
            if (metric.hasContent()) {
                collected.add(metric);
            }
            if (collected.size() == max) {
                break;
            }
        }
        metrics = Collections.unmodifiableList(collected);

        if (collected.isEmpty()) {
            // The distinction that matters when debugging: a folder with no fragments in
            // it, versus fragments whose fields could not be read.
            LOG.warn("Market band at {} resolved {} but produced no metrics. Either the "
                    + "folder holds no content fragments, or the fragments carry no 'metric' "
                    + "and 'value' fields. Pointing this component at a single fragment "
                    + "instead of the folder that contains it has the same effect.",
                    getResourcePath(), parentPath);
        }
    }

    /** @return this component's own path, for log lines that have to be actionable */
    private String getResourcePath() {
        return getId();
    }

    @Override
    public String getEyebrow() {
        return eyebrow;
    }

    @Override
    public String getHeading() {
        return heading;
    }

    @Override
    public String getIntro() {
        return intro;
    }

    @Override
    public List<MjMetric> getMetrics() {
        return metrics;
    }

    @Override
    public boolean isEmpty() {
        return metrics.isEmpty();
    }

    @Override
    protected ComponentData getComponentData() {
        return DataLayerBuilder.extending(super.getComponentData()).asComponent()
                .withTitle(() -> StringUtils.defaultString(heading))
                .build();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
