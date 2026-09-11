package com.mysite.core.models.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;

/**
 * Reads elements out of a Content Fragment.
 *
 * <p>The fragment models are authored by the content team and their element names are the
 * contract between the model and the components. Every read here is tolerant: a fragment
 * whose model has been extended, renamed or only partly filled in must still render, so a
 * missing element yields {@code null} rather than an exception.</p>
 */
final class FragmentReader {

    private static final Logger LOG = LoggerFactory.getLogger(FragmentReader.class);

    /**
     * Where a structured fragment keeps its authored values, relative to the asset.
     *
     * <p>This is the documented storage contract for content fragments and it holds
     * whether or not the fragment's model can be read. The model defines the element
     * <em>schema</em>; the values live here regardless.</p>
     */
    private static final String MASTER = "jcr:content/data/master";

    /** US editorial date style, e.g. "March 4, 2025". */
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);

    private FragmentReader() {
        // static helper
    }

    /**
     * @param resolver the resolver to read with
     * @param path     the fragment's path under {@code /content/dam}
     * @return the fragment, or {@code null} when the path is blank, missing or not a fragment
     */
    static ContentFragment fragment(ResourceResolver resolver, String path) {
        if (resolver == null || StringUtils.isBlank(path)) {
            return null;
        }
        Resource resource = resolver.getResource(path);
        if (resource == null) {
            LOG.warn("Content fragment not found at {} - the path is wrong, the fragment has "
                    + "moved, or it is not readable by this session", path);
            return null;
        }
        ContentFragment fragment = of(resource);
        if (fragment == null && !isFragment(resource)) {
            LOG.warn("Resource at {} is not a content fragment (primary type {})",
                    path, resource.getValueMap().get("jcr:primaryType", String.class));
        }
        return fragment;
    }

    /**
     * @return {@code true} when the resource is a content fragment.
     *
     * <p>Tested by the {@code contentFragment} marker on the asset's {@code jcr:content},
     * which is the documented way a fragment identifies itself, rather than by whether
     * {@link #fragment} could adapt it. Adaptation additionally requires the fragment's
     * model to be resolvable, so gating iteration on it silently drops perfectly good
     * fragments whenever a model is missing or misconfigured — which is exactly the
     * failure this method exists to survive.</p>
     */
    /**
     * Adapts a resource to a fragment, defensively.
     *
     * <p>{@code adaptTo} runs third-party adapter factories that resolve the fragment's
     * model, and a broken or absent model can make one throw. An exception here would
     * propagate out of a {@code @PostConstruct} and make Sling Models hand back a null
     * model, which takes down the whole component — a section rendering nothing because
     * one fragment is malformed. The values are readable either way, so this swallows the
     * failure, records it, and lets the caller carry on.</p>
     *
     * @return the fragment, or {@code null} when it cannot be adapted
     */
    static ContentFragment of(Resource resource) {
        if (resource == null) {
            return null;
        }
        try {
            return resource.adaptTo(ContentFragment.class);
        } catch (RuntimeException adapterFailed) {
            LOG.warn("Could not adapt {} to a content fragment; reading its stored values "
                    + "instead. This usually means its model is missing or misconfigured.",
                    resource.getPath(), adapterFailed);
            return null;
        }
    }

    static boolean isFragment(Resource resource) {
        if (resource == null) {
            return false;
        }
        Resource content = resource.getChild("jcr:content");
        return content != null && content.getValueMap().get("contentFragment", Boolean.FALSE);
    }

    /**
     * Reads an element by name, falling back to the fragment's stored values.
     *
     * <p>The Content Fragment API is the supported way in, and it is tried first. It
     * resolves elements through the fragment's <em>model</em>, so a model that is missing,
     * misconfigured or not yet deployed makes every element invisible even though the
     * values are sitting in the repository — which is a failure mode worth surviving and,
     * more importantly, worth saying out loud. When that happens the values are read
     * straight from {@code jcr:content/data/master} and the mismatch is logged once per
     * element, rather than the section quietly rendering as though it had no content.</p>
     */
    private static String element(ContentFragment fragment, Resource assetResource, String name) {
        if (fragment != null && fragment.hasElement(name)) {
            ContentElement element = fragment.getElement(name);
            String content = element == null ? null : element.getContent();
            if (StringUtils.isNotBlank(content)) {
                return content;
            }
        }
        if (assetResource == null) {
            return null;
        }
        Resource master = assetResource.getChild(MASTER);
        if (master == null) {
            return null;
        }
        ValueMap values = master.getValueMap();
        String stored = values.get(name, String.class);
        if (StringUtils.isNotBlank(stored) && fragment != null && !fragment.hasElement(name)) {
            LOG.warn("Fragment {} stores '{}' but its model does not declare it - check the "
                    + "model at jcr:content/data/master's cq:model. Reading the stored value.",
                    assetResource.getPath(), name);
        }
        return StringUtils.isBlank(stored) ? null : stored;
    }

    /**
     * @return the content of the first of {@code names} that exists and is not blank,
     *         or {@code null}. Passing several names lets one component read fragment
     *         models that call the same idea by different names.
     */
    static String text(ContentFragment fragment, Resource assetResource, String... names) {
        for (String name : names) {
            if (StringUtils.isBlank(name)) {
                continue;
            }
            String value = element(fragment, assetResource, name);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * @return the named date element formatted for display, the raw value when it is not
     *         an ISO-8601 timestamp, or {@code null}
     */
    static String date(ContentFragment fragment, Resource assetResource, String... names) {
        String raw = text(fragment, assetResource, names);
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw).format(DISPLAY_DATE);
        } catch (DateTimeParseException notATimestamp) {
            // A year-only element ("2021") or a hand-typed value is already display-ready.
            return raw;
        }
    }
}
