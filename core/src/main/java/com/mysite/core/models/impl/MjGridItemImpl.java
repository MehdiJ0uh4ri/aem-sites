package com.mysite.core.models.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.dam.cfm.ContentFragment;
import com.mysite.core.models.MjGridItem;

/**
 * One portfolio company flattened for the index.
 */
class MjGridItemImpl implements MjGridItem {

    static final String TYPE = "mysite/components/mjgrid/company";

    private final String id;
    private final String name;
    private final String sector;
    private final String stage;
    private final String region;
    private final String year;
    private final String status;
    private final String summary;
    private final String fileReference;
    private final String url;
    private final String dataLayerJson;

    MjGridItemImpl(Resource resource, ContentFragment fragment, String detailRoot,
                   String parentId, boolean dataLayerEnabled) {
        this.name = FragmentReader.text(fragment, resource, "companyName", "title");
        this.sector = FragmentReader.text(fragment, resource, "sector");
        this.stage = FragmentReader.text(fragment, resource, "stage");
        this.region = FragmentReader.text(fragment, resource, "geography");
        this.year = FragmentReader.text(fragment, resource, "investmentYear");
        this.status = FragmentReader.text(fragment, resource, "status");
        this.summary = FragmentReader.text(fragment, resource, "description", "summary");
        this.fileReference = FragmentReader.text(fragment, resource, "featuredImage", "logo");
        this.url = detailUrl(resource, fragment, detailRoot);
        this.id = StringUtils.isBlank(parentId) ? null
                : parentId + "-co-" + resource.getName().replaceAll("[^a-zA-Z0-9_-]", "-");
        this.dataLayerJson = ChildDataLayer.json(dataLayerEnabled, this.id, TYPE, this.name, this.url);
    }

    /**
     * The detail route, built whether or not the page exists yet.
     *
     * <p>Unlike Related Content this does not check for the page first. The index is the
     * canonical list of companies and every one of them will have a detail page; emitting
     * the agreed URL now is what lets the detail build land without the index being
     * touched. The markup marks these links so AEM's link checker leaves them alone —
     * otherwise an unbuilt page has its anchor silently stripped and the contract is
     * invisible until the day it works.</p>
     */
    private static String detailUrl(Resource resource, ContentFragment fragment, String detailRoot) {
        if (StringUtils.isBlank(detailRoot) || resource == null) {
            return null;
        }
        String slug = StringUtils.defaultIfBlank(
                FragmentReader.text(fragment, resource, "slug"), resource.getName());
        return StringUtils.removeEnd(detailRoot, "/") + "/" + slug + ".html";
    }

    boolean hasContent() {
        return StringUtils.isNotBlank(name);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSector() {
        return sector;
    }

    @Override
    public String getStage() {
        return stage;
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getYear() {
        return year;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public String getFileReference() {
        return fileReference;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getDataLayerJson() {
        return dataLayerJson;
    }
}
