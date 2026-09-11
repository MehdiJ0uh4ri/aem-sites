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
import com.mysite.core.models.SpecRow;
import com.mysite.core.models.SpecTable;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { SpecTable.class, ComponentExporter.class },
       resourceType = SpecTableImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class SpecTableImpl extends AbstractComponentImpl implements SpecTable {

    public static final String RESOURCE_TYPE = "mysite/components/spectable";

    /** Name of the composite multifield node holding the rows. */
    private static final String NN_ROWS = "rows";

    @ValueMapValue
    private String caption;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String defaultUnits;

    @ValueMapValue
    private boolean sortable;

    @ValueMapValue
    private String classification;

    @ChildResource(name = NN_ROWS)
    private Resource rowsResource;

    private List<SpecRow> rows;

    @PostConstruct
    private void init() {
        // A row with a label but no figure is an authoring accident, not a deliberate blank:
        // rendering it would leave a dash in a table of measurements.
        rows = MultifieldUtil.readItems(rowsResource, SpecRow.class,
                row -> StringUtils.isNotBlank(row.getLabel())
                        && StringUtils.isNotBlank(row.getMetricValue()));
    }

    @Override
    public String getCaption() {
        return caption;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getDefaultUnits() {
        // Anything other than an explicit imperial choice reads as metric, which is how the
        // figures are authored and therefore the safest default.
        return UNITS_IMPERIAL.equals(defaultUnits) ? UNITS_IMPERIAL : UNITS_METRIC;
    }

    @Override
    public boolean isUnitToggle() {
        // Offering a switch that changes nothing is worse than not offering one, so the
        // control appears only once a row actually carries an imperial equivalent.
        return rows.stream().anyMatch(SpecRow::isDualUnit);
    }

    @Override
    public boolean isSortable() {
        return sortable;
    }

    @Override
    public String getClassification() {
        return classification;
    }

    @Override
    public List<SpecRow> getRows() {
        return rows;
    }

    @Override
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
