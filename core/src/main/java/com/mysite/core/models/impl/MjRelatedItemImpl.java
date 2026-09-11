package com.mysite.core.models.impl;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.dam.cfm.ContentFragment;
import com.mysite.core.models.MjRelatedItem;

/**
 * One referenced fragment, flattened for the related list.
 */
class MjRelatedItemImpl implements MjRelatedItem {

    static final String TYPE = "mysite/components/mjrelated/item";

    /**
     * Fragment element names that can carry a human title, tried in order.
     *
     * <p>Every model in this site names its title differently — {@code name} on a Sector,
     * {@code companyName} on a Portfolio Company, {@code metric} on a Market Metric. One
     * ordered list here is what lets a single component render all of them.</p>
     */
    private static final String[] TITLES =
            { "title", "name", "companyName", "metric", "company" };

    private static final String[] SUMMARIES =
            { "summary", "description", "statement", "commentary", "challenge" };

    private final String id;
    private final String title;
    private final String kind;
    private final String summary;
    private final String url;
    private final String dataLayerJson;

    MjRelatedItemImpl(Resource resource, ContentFragment fragment, String modelName, String url,
                      String parentId, boolean dataLayerEnabled) {
        this.title = FragmentReader.text(fragment, resource, TITLES);
        this.summary = FragmentReader.text(fragment, resource, SUMMARIES);
        this.kind = label(modelName);
        this.url = url;
        this.id = StringUtils.isBlank(parentId) ? null
                : parentId + "-rel-" + resource.getName().replaceAll("[^a-zA-Z0-9_-]", "-");
        this.dataLayerJson = ChildDataLayer.json(dataLayerEnabled, this.id, TYPE, this.title, url);
    }

    /**
     * Turns a model node name into something readable: {@code portfolio-company} becomes
     * "Portfolio company". Derived rather than mapped, so a model added later labels
     * itself without this class being touched.
     */
    private static String label(String modelName) {
        if (StringUtils.isBlank(modelName)) {
            return null;
        }
        String words = modelName.replace('-', ' ').trim();
        return words.substring(0, 1).toUpperCase(Locale.ROOT) + words.substring(1);
    }

    boolean hasContent() {
        return StringUtils.isNotBlank(title);
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
    public String getKind() {
        return kind;
    }

    @Override
    public String getSummary() {
        return summary;
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
