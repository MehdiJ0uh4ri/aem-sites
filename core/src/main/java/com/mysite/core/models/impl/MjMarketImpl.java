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
import com.mysite.core.models.MjMarket;
import com.mysite.core.models.MjMetric;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { MjMarket.class, ComponentExporter.class },
       resourceType = MjMarketImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MjMarketImpl extends AbstractComponentImpl implements MjMarket {

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
        Resource folder = StringUtils.isBlank(parentPath) ? null : resourceResolver.getResource(parentPath);
        if (folder == null) {
            return;
        }
        int max = Math.min(limit == null || limit <= 0 ? DEFAULT_LIMIT : limit, MAX_LIMIT);

        List<MjMetric> collected = new ArrayList<>();
        for (Resource child : folder.getChildren()) {
            ContentFragment fragment = child.adaptTo(ContentFragment.class);
            if (fragment == null) {
                continue;
            }
            MjMetricImpl metric = new MjMetricImpl(fragment);
            if (metric.hasContent()) {
                collected.add(metric);
            }
            if (collected.size() == max) {
                break;
            }
        }
        metrics = Collections.unmodifiableList(collected);
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
