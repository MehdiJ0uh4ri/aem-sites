package com.mysite.core.models.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.wcm.core.components.util.AbstractComponentImpl;

/**
 * Shared base for every MJ section that owns a heading.
 *
 * <p>The heading element is decided here, once, rather than being hard-coded in seven
 * HTL files. A page needs exactly one {@code h1} and it belongs to whatever opens the
 * page — which differs per page and is an authoring decision, not a component decision.
 * So the level is authorable, and when it is left on Auto the component works it out:
 * the first section in its container opens the page and takes the {@code h1}; everything
 * after it is an {@code h2}.</p>
 *
 * <p>Subclasses expose {@link #getHeadingTag()} through their own model interface; the
 * markup then writes its heading with {@code data-sly-element}, so the visual weight of
 * the heading never moves when its level changes.</p>
 */
public abstract class MjComponentImpl extends AbstractComponentImpl {

    /** Levels an author may choose. Anything else falls back to the derived value. */
    private static final List<String> LEVELS = Arrays.asList("h1", "h2", "h3", "h4");

    @ValueMapValue
    private String headingLevel;

    private String headingTag;

    @PostConstruct
    private void initHeading() {
        String authored = StringUtils.lowerCase(StringUtils.trimToNull(headingLevel), Locale.ROOT);
        if (authored != null && LEVELS.contains(authored)) {
            headingTag = authored;
            return;
        }
        headingTag = isFirstInContainer() ? "h1" : "h2";
    }

    /**
     * @return {@code true} when this component is the first child of the container it
     *         sits in — which is what "opens the page" means in template content order
     */
    private boolean isFirstInContainer() {
        // `resource` is injected by AbstractComponentImpl; no second injection needed.
        if (resource == null) {
            return false;
        }
        Resource parent = resource.getParent();
        if (parent == null) {
            return false;
        }
        for (Resource sibling : parent.getChildren()) {
            // jcr:content and other metadata nodes are not authored components.
            if (sibling.getName().startsWith("jcr:") || sibling.getName().startsWith("cq:")) {
                continue;
            }
            return sibling.getPath().equals(resource.getPath());
        }
        return false;
    }

    /** @return the element name to render this section's heading with, {@code h1}..{@code h4} */
    public String getHeadingTag() {
        return headingTag;
    }
}
