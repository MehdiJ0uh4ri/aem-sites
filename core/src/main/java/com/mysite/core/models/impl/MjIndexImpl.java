package com.mysite.core.models.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mysite.core.models.MjIndex;
import com.mysite.core.models.MjIndexItem;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { MjIndex.class, ComponentExporter.class },
       resourceType = MjIndexImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MjIndexImpl extends MjComponentImpl implements MjIndex {

    private static final Logger LOG = LoggerFactory.getLogger(MjIndexImpl.class);

    public static final String RESOURCE_TYPE = "mysite/components/mjindex";

    private static final List<String> VARIANTS =
            Arrays.asList(VARIANT_PORTFOLIO, VARIANT_DEALS, VARIANT_INSIGHTS, VARIANT_SECTORS);

    /** Guards the homepage against an author pointing the index at a large folder. */
    private static final int DEFAULT_LIMIT = 6;

    private static final int MAX_LIMIT = 48;

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String intro;

    @ValueMapValue
    private String variant;

    /** DAM folder holding the fragments to list. */
    @ValueMapValue
    private String parentPath;

    /** Content root under which one detail page exists per fragment. */
    @ValueMapValue
    private String detailRoot;

    @ValueMapValue
    private Integer limit;

    @ValueMapValue
    private String ctaText;

    @ValueMapValue
    private String fileReference;

    @ValueMapValue
    private String alt;

    @ValueMapValue
    private String ctaLink;

    @SlingObject
    private ResourceResolver resourceResolver;

    private List<MjIndexItem> items;

    @PostConstruct
    private void init() {
        items = Collections.emptyList();
        if (StringUtils.isBlank(parentPath)) {
            return;
        }
        Resource folder = resourceResolver.getResource(parentPath);
        if (folder == null) {
            LOG.warn("Index '{}' points at {}, which does not resolve.", getId(), parentPath);
            return;
        }
        boolean dataLayerEnabled = getData() != null;
        String parentId = getId();
        String resolvedVariant = getVariant();

        List<MjIndexItemImpl> collected = new ArrayList<>();

        // As with the market band: a single fragment is a legitimate, if small, index.
        boolean single = FragmentReader.isFragment(folder);
        if (single) {
            MjIndexItemImpl only = new MjIndexItemImpl(folder, FragmentReader.of(folder),
                    resolvedVariant, detailRoot, parentId, dataLayerEnabled);
            if (only.hasContent()) {
                collected.add(only);
            }
        }

        for (Resource child : single ? Collections.<Resource>emptyList() : folder.getChildren()) {
            if (!FragmentReader.isFragment(child)) {
                // Folders, images, renditions and anything else that is not a fragment.
                continue;
            }
            MjIndexItemImpl item = new MjIndexItemImpl(child, FragmentReader.of(child),
                    resolvedVariant, detailRoot, parentId, dataLayerEnabled);
            if (item.hasContent()) {
                collected.add(item);
            }
        }
        if (VARIANT_SECTORS.equals(resolvedVariant)) {
            // Sectors carry no date. Alphabetical is at least a reason; JCR child order is
            // just the order somebody happened to create the fragments in.
            collected.sort(Comparator.comparing(
                    item -> StringUtils.defaultString(item.getTitle())));
        } else {
            // Newest first. Fragments with no date sort last rather than jumping to the top.
            collected.sort(Comparator.comparing(MjIndexItemImpl::getSortKey).reversed());
        }

        int max = Math.min(limit == null || limit <= 0 ? DEFAULT_LIMIT : limit, MAX_LIMIT);
        items = Collections.unmodifiableList(
                new ArrayList<MjIndexItem>(collected.subList(0, Math.min(max, collected.size()))));

        if (items.isEmpty()) {
            LOG.warn("Index '{}' resolved {} but produced no items. Either the folder holds "
                    + "no content fragments, or none of them carry the field this variant "
                    + "reads as a title ({}).", getId(), parentPath, resolvedVariant);
        }
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
    public String getVariant() {
        return VARIANTS.contains(variant) ? variant : VARIANT_INSIGHTS;
    }

    @Override
    public List<MjIndexItem> getItems() {
        return items;
    }

    @Override
    public String getCtaText() {
        return ctaText;
    }

    @Override
    public String getFileReference() {
        return fileReference;
    }

    @Override
    public String getAlt() {
        // Composition, not information: the copy beside it already says what it is.
        return StringUtils.defaultString(alt);
    }

    @Override
    public String getCtaUrl() {
        return LinkUtil.toUrl(ctaLink);
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    protected ComponentData getComponentData() {
        return DataLayerBuilder.extending(super.getComponentData()).asComponent()
                .withTitle(() -> StringUtils.defaultString(heading))
                .withDescription(() -> StringUtils.defaultString(intro))
                .build();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
