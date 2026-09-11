package com.mysite.core.models.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.mysite.core.models.MjGrid;
import com.mysite.core.models.MjGridItem;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { MjGrid.class, ComponentExporter.class },
       resourceType = MjGridImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class MjGridImpl extends MjComponentImpl implements MjGrid {

    private static final Logger LOG = LoggerFactory.getLogger(MjGridImpl.class);

    public static final String RESOURCE_TYPE = "mysite/components/mjgrid";

    /**
     * Stage order. Alphabetical would put Growth before Seed, which is backwards for a
     * control an investor reads, so the sequence is declared rather than sorted.
     */
    private static final List<String> STAGE_ORDER =
            Arrays.asList("Seed", "Series A", "Series B", "Series C", "Growth");

    @ValueMapValue
    private String eyebrow;

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String intro;

    @ValueMapValue
    private String parentPath;

    @ValueMapValue
    private String detailRoot;

    @ValueMapValue
    private String emptyText;

    @ValueMapValue
    private boolean showSecondaryFilters;

    @SlingObject
    private ResourceResolver resourceResolver;

    private List<MjGridItem> items;
    private List<String> sectors;
    private List<String> stages;
    private List<String> years;

    @PostConstruct
    private void init() {
        items = Collections.emptyList();
        sectors = Collections.emptyList();
        stages = Collections.emptyList();
        years = Collections.emptyList();

        if (StringUtils.isBlank(parentPath)) {
            return;
        }
        Resource folder = resourceResolver.getResource(parentPath);
        if (folder == null) {
            LOG.warn("Portfolio grid '{}' points at {}, which does not resolve.", getId(), parentPath);
            return;
        }

        boolean dataLayerEnabled = getData() != null;
        String parentId = getId();

        List<MjGridItemImpl> collected = new ArrayList<>();
        for (Resource child : folder.getChildren()) {
            if (!FragmentReader.isFragment(child)) {
                continue;
            }
            MjGridItemImpl item = new MjGridItemImpl(child, FragmentReader.of(child),
                    detailRoot, parentId, dataLayerEnabled);
            if (item.hasContent()) {
                collected.add(item);
            }
        }
        // The index is a reference list, so it reads alphabetically. Anything else asks the
        // reader to work out the ordering before they can use it.
        collected.sort(Comparator.comparing(item -> StringUtils.defaultString(item.getName())));

        if (collected.isEmpty()) {
            LOG.warn("Portfolio grid '{}' resolved {} but found no company fragments.",
                    getId(), parentPath);
        }
        items = Collections.unmodifiableList(new ArrayList<MjGridItem>(collected));
        sectors = facet(collected, MjGridItem::getSector, true);
        years = facet(collected, MjGridItem::getYear, false);
        stages = orderedStages(facet(collected, MjGridItem::getStage, true));
    }

    /**
     * Distinct values of one field across the companies present.
     *
     * <p>Derived from the data rather than configured, so the filters can never offer a
     * sector that matches nothing — which is the usual way a faceted index goes stale.</p>
     */
    private static List<String> facet(List<MjGridItemImpl> source,
                                      java.util.function.Function<MjGridItem, String> field,
                                      boolean ascending) {
        Comparator<String> order = ascending
                ? Comparator.<String>naturalOrder()
                : Comparator.<String>reverseOrder();
        Set<String> values = new TreeSet<>(order);
        for (MjGridItem item : source) {
            String value = field.apply(item);
            if (StringUtils.isNotBlank(value)) {
                values.add(value.trim());
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<String> orderedStages(List<String> present) {
        Set<String> ordered = new LinkedHashSet<>();
        for (String stage : STAGE_ORDER) {
            if (present.contains(stage)) {
                ordered.add(stage);
            }
        }
        // Anything the firm uses that is not in the canonical sequence still gets offered.
        ordered.addAll(present);
        return Collections.unmodifiableList(new ArrayList<>(ordered));
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
    public List<MjGridItem> getItems() {
        return items;
    }

    @Override
    public List<String> getSectors() {
        return sectors;
    }

    @Override
    public List<String> getStages() {
        return stages;
    }

    @Override
    public List<String> getYears() {
        return years;
    }

    @Override
    public boolean isShowSecondaryFilters() {
        return showSecondaryFilters;
    }

    @Override
    public String getEmptyText() {
        return StringUtils.defaultIfBlank(emptyText,
                "No companies match that combination.");
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
