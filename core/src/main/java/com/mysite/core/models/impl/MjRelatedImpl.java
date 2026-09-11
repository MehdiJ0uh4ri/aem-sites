package com.mysite.core.models.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.adobe.cq.wcm.core.components.util.AbstractComponentImpl;
import com.mysite.core.models.MjRelated;
import com.mysite.core.models.MjRelatedItem;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { MjRelated.class, ComponentExporter.class },
       resourceType = MjRelatedImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MjRelatedImpl extends AbstractComponentImpl implements MjRelated {

    private static final Logger LOG = LoggerFactory.getLogger(MjRelatedImpl.class);

    public static final String RESOURCE_TYPE = "mysite/components/mjrelated";

    private static final int DEFAULT_LIMIT = 6;

    private static final int MAX_LIMIT = 24;

    /**
     * Where each fragment model's detail pages live, relative to the site section root.
     *
     * <p>This mirrors the site's information architecture, which is an authoring fact, not
     * a content-model fact — so it is a lookup here rather than a field on every fragment.
     * A model with no entry simply renders without a link, which is correct for Thesis and
     * Sector: they have no detail pages yet. Every candidate link is resolved before it is
     * rendered, so a missing page produces plain text rather than a dead anchor.</p>
     */
    private static final Map<String, String> DETAIL_SEGMENTS = new HashMap<>();

    static {
        DETAIL_SEGMENTS.put("insight", "insights");
        DETAIL_SEGMENTS.put("deal", "deals");
        DETAIL_SEGMENTS.put("case-study", "case-studies");
        DETAIL_SEGMENTS.put("portfolio-company", "portfolio");
    }

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String heading;

    /** The fragment whose relationships are being followed. */
    @ValueMapValue
    private String fragmentPath;

    /** Site section root under which the detail-page segments above are resolved. */
    @ValueMapValue
    private String basePath;

    @ValueMapValue
    private Integer limit;

    @SlingObject
    private ResourceResolver resourceResolver;

    private List<MjRelatedItem> items;

    @PostConstruct
    private void init() {
        items = Collections.emptyList();
        if (StringUtils.isBlank(fragmentPath)) {
            return;
        }
        Resource source = resourceResolver.getResource(fragmentPath);
        if (source == null || !FragmentReader.isFragment(source)) {
            LOG.warn("Related content at '{}' points at {}, which is not a content fragment.",
                    getId(), fragmentPath);
            return;
        }

        boolean dataLayerEnabled = getData() != null;
        String parentId = getId();
        int max = Math.min(limit == null || limit <= 0 ? DEFAULT_LIMIT : limit, MAX_LIMIT);

        List<MjRelatedItem> collected = new ArrayList<>();
        for (String path : references(source)) {
            Resource target = resourceResolver.getResource(path);
            if (target == null || !FragmentReader.isFragment(target)) {
                continue;
            }
            ContentFragment fragment = FragmentReader.of(target);
            String modelName = modelOf(target);
            MjRelatedItemImpl item = new MjRelatedItemImpl(target, fragment, modelName,
                    detailUrl(modelName, target), parentId, dataLayerEnabled);
            if (item.hasContent()) {
                collected.add(item);
            }
            if (collected.size() == max) {
                break;
            }
        }
        if (collected.isEmpty()) {
            LOG.warn("Related content at '{}' followed {} but resolved nothing. The source "
                    + "fragment may carry no references, or they may point at content this "
                    + "session cannot read.", getId(), fragmentPath);
        }
        items = Collections.unmodifiableList(collected);
    }

    /**
     * Every reference the source fragment holds, in authored order and de-duplicated.
     *
     * <p>Read straight off the fragment's stored values rather than from a list of field
     * names. Any value under {@code /content/dam} is a candidate; the caller discards the
     * ones that turn out not to be fragments. This is what makes the component work for a
     * model nobody had written when it was built.</p>
     */
    private static Set<String> references(Resource source) {
        Set<String> paths = new LinkedHashSet<>();
        Resource master = source.getChild("jcr:content/data/master");
        if (master == null) {
            return paths;
        }
        ValueMap values = master.getValueMap();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String[]) {
                for (String candidate : (String[]) value) {
                    addIfReference(paths, candidate, source.getPath());
                }
            } else if (value instanceof String) {
                addIfReference(paths, (String) value, source.getPath());
            }
        }
        return paths;
    }

    private static void addIfReference(Set<String> paths, String candidate, String selfPath) {
        if (StringUtils.startsWith(candidate, "/content/dam/") && !candidate.equals(selfPath)) {
            paths.add(candidate);
        }
    }

    /** @return the node name of the fragment's model, e.g. {@code sector} */
    private static String modelOf(Resource fragment) {
        Resource data = fragment.getChild("jcr:content/data");
        String model = data == null ? null : data.getValueMap().get("cq:model", String.class);
        return StringUtils.substringAfterLast(StringUtils.defaultString(model), "/");
    }

    /** @return a verified detail-page href, or {@code null} when there is no page to link to */
    private String detailUrl(String modelName, Resource fragment) {
        String segment = DETAIL_SEGMENTS.get(modelName);
        if (StringUtils.isBlank(basePath) || segment == null) {
            return null;
        }
        String path = StringUtils.removeEnd(basePath, "/") + "/" + segment + "/" + fragment.getName();
        // Resolved before it is rendered: an unbuilt detail page becomes plain text rather
        // than an anchor the link checker would strip out from under us.
        return resourceResolver.getResource(path) == null ? null : LinkUtil.toUrl(path);
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
    public List<MjRelatedItem> getItems() {
        return items;
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
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
