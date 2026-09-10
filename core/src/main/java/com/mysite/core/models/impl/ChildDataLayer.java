package com.mysite.core.models.impl;

import org.apache.commons.lang3.StringUtils;

import com.adobe.cq.wcm.core.components.models.datalayer.builder.ComponentDataBuilder;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;

/**
 * Builds Adobe Client Data Layer entries for repeatable child items (slides, tiles).
 *
 * <p>Core Components give the section itself a data layer entry via
 * {@code AbstractComponentImpl}. Children need their own entry so that a click can be
 * attributed to the individual item; pairing the emitted JSON with
 * {@code data-cmp-clickable} in the markup is what makes the Core Components data layer
 * clientlib raise a click event for it.</p>
 */
final class ChildDataLayer {

    private ChildDataLayer() {
        // static helper
    }

    /**
     * @param enabled  whether the page has the data layer switched on; when {@code false}
     *                 nothing is emitted and the markup omits the attribute entirely
     * @param id       unique id for the child
     * @param type     resource-type-like discriminator, e.g. {@code mysite/components/slides/slide}
     * @param title    optional title
     * @param linkUrl  optional resolved link
     * @return the JSON to place in {@code data-cmp-data-layer}, or {@code null}
     */
    static String json(boolean enabled, String id, String type, String title, String linkUrl) {
        if (!enabled || StringUtils.isBlank(id)) {
            return null;
        }
        ComponentDataBuilder builder = DataLayerBuilder.forComponent()
                .withId(() -> id)
                .withType(() -> type);
        if (StringUtils.isNotBlank(title)) {
            builder = builder.withTitle(() -> title);
        }
        if (StringUtils.isNotBlank(linkUrl)) {
            builder = builder.withLinkUrl(() -> linkUrl);
        }
        return builder.build().getJson();
    }
}
