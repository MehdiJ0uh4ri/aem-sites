package com.mysite.core.models.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.dam.cfm.ContentFragment;
import com.mysite.core.models.MjIndex;
import com.mysite.core.models.MjIndexItem;

/**
 * One Content Fragment flattened for the index.
 *
 * <p>All three variants read the same shape — title, kicker, meta, summary, image — from
 * different element names. Keeping that mapping here rather than in the markup is what
 * lets Portfolio, Deals and Insights share one component and one stylesheet.</p>
 */
class MjIndexItemImpl implements MjIndexItem {

    static final String TYPE = "mysite/components/mjindex/item";

    private final String id;
    private final String title;
    private final String kicker;
    private final String meta;
    private final String summary;
    private final String fileReference;
    private final String alt;
    private final String url;
    private final String status;
    private final String dataLayerJson;

    /** Raw, unformatted date used only for ordering — ISO-8601 sorts lexicographically. */
    private final String sortKey;

    MjIndexItemImpl(Resource resource, ContentFragment fragment, String variant, String detailRoot,
                    String parentId, boolean dataLayerEnabled) {
        if (MjIndex.VARIANT_PORTFOLIO.equals(variant)) {
            this.title = FragmentReader.text(fragment, "companyName", "title");
            this.kicker = FragmentReader.text(fragment, "sector");
            this.meta = FragmentReader.text(fragment, "location");
            this.summary = FragmentReader.text(fragment, "description", "summary");
            this.status = FragmentReader.text(fragment, "status");
            this.fileReference = FragmentReader.text(fragment, "featuredImage", "logo");
            this.sortKey = FragmentReader.text(fragment, "investmentYear");
        } else if (MjIndex.VARIANT_DEALS.equals(variant)) {
            this.title = FragmentReader.text(fragment, "company", "title");
            this.kicker = FragmentReader.text(fragment, "sector");
            this.meta = FragmentReader.date(fragment, "date");
            this.summary = FragmentReader.text(fragment, "description", "summary");
            this.status = FragmentReader.text(fragment, "investmentType");
            this.fileReference = null;
            this.sortKey = FragmentReader.text(fragment, "date");
        } else {
            this.title = FragmentReader.text(fragment, "title");
            this.kicker = FragmentReader.text(fragment, "category");
            this.meta = FragmentReader.date(fragment, "publicationDate");
            this.summary = FragmentReader.text(fragment, "summary");
            this.status = null;
            this.fileReference = FragmentReader.text(fragment, "heroImage", "featuredImage");
            this.sortKey = FragmentReader.text(fragment, "publicationDate");
        }
        // Fragment images are art direction for a card the title already names.
        this.alt = "";
        this.url = detailUrl(resource, fragment, detailRoot);
        // Derived from the fragment name rather than a loop index: the list is sorted
        // after it is built, and an id that survives reordering keeps analytics comparable.
        this.id = itemId(parentId, resource);
        this.dataLayerJson = ChildDataLayer.json(dataLayerEnabled, this.id, TYPE, this.title, this.url);
    }

    private static String itemId(String parentId, Resource resource) {
        if (StringUtils.isBlank(parentId) || resource == null) {
            return null;
        }
        return parentId + "-item-" + resource.getName().replaceAll("[^a-zA-Z0-9_-]", "-");
    }

    /**
     * Detail pages are one page per fragment under a single root, named after the
     * fragment. A {@code slug} element overrides the fragment name when the two differ.
     */
    private static String detailUrl(Resource resource, ContentFragment fragment, String detailRoot) {
        if (StringUtils.isBlank(detailRoot) || resource == null) {
            return null;
        }
        String slug = StringUtils.defaultIfBlank(FragmentReader.text(fragment, "slug"), resource.getName());
        return LinkUtil.toUrl(StringUtils.removeEnd(detailRoot, "/") + "/" + slug);
    }

    boolean hasContent() {
        return StringUtils.isNotBlank(title);
    }

    /** @return the ordering key, never {@code null}, so items without a date sort last */
    String getSortKey() {
        return StringUtils.defaultString(sortKey);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getKicker() {
        return kicker;
    }

    @Override
    public String getMeta() {
        return meta;
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
    public String getAlt() {
        return alt;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getDataLayerJson() {
        return dataLayerJson;
    }
}
