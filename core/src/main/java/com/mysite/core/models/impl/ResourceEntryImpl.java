package com.mysite.core.models.impl;

import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.mysite.core.models.ResourceEntry;

@Model(adaptables = Resource.class,
       adapters = ResourceEntry.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ResourceEntryImpl implements ResourceEntry {

    private static final long KILOBYTE = 1024L;

    @SlingObject
    private Resource resource;

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String category;

    @ValueMapValue
    private String assetPath;

    @ValueMapValue
    private boolean gated;

    @ValueMapValue
    private String classification;

    private String size;

    @PostConstruct
    private void init() {
        // Format and size are read from the asset rather than authored: a figure typed into a
        // dialog goes stale the moment the PDF is replaced, and nobody remembers to update it.
        size = readSize();
    }

    private String readSize() {
        if (StringUtils.isBlank(assetPath) || resource == null) {
            return null;
        }
        Resource assetResource = resource.getResourceResolver().getResource(assetPath);
        if (assetResource == null) {
            // The asset was moved or deleted after the entry was authored. The entry still
            // renders — the broken link is more visible to an author than a missing row.
            return null;
        }
        Asset asset = assetResource.adaptTo(Asset.class);
        if (asset == null) {
            return null;
        }
        // The original rendition's own size, rather than the dam:size metadata property:
        // metadata can lag behind a re-upload, the rendition cannot.
        Rendition original = asset.getOriginal();
        return original == null ? null : format(original.getSize());
    }

    /** Binary units, one decimal, which is how a file size is quoted next to a download. */
    private String format(long bytes) {
        if (bytes < KILOBYTE) {
            return bytes + " B";
        }
        double value = bytes / (double) KILOBYTE;
        String[] units = { "KB", "MB", "GB" };
        int unit = 0;
        while (value >= KILOBYTE && unit < units.length - 1) {
            value /= KILOBYTE;
            unit++;
        }
        return String.format(Locale.ENGLISH, "%.1f %s", value, units[unit]);
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
    public String getCategory() {
        return category;
    }

    @Override
    public String getAssetPath() {
        return assetPath;
    }

    @Override
    public boolean isGated() {
        return gated;
    }

    @Override
    public String getDownloadUrl() {
        // A gated entry must not carry a direct URL anywhere in the markup: the whole point
        // of the gate is that the path is not guessable from the page.
        return gated ? null : assetPath;
    }

    @Override
    public String getFormat() {
        String extension = StringUtils.substringAfterLast(StringUtils.defaultString(assetPath), ".");
        return StringUtils.isBlank(extension) ? null : extension.toUpperCase(Locale.ENGLISH);
    }

    @Override
    public String getSize() {
        return size;
    }

    @Override
    public String getClassification() {
        return classification;
    }
}
