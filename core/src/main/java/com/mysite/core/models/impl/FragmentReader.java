package com.mysite.core.models.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

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
        return resource == null ? null : resource.adaptTo(ContentFragment.class);
    }

    /**
     * @return the content of the first of {@code names} that exists and is not blank,
     *         or {@code null}. Passing several names lets one component read fragment
     *         models that call the same idea by different names.
     */
    static String text(ContentFragment fragment, String... names) {
        if (fragment == null) {
            return null;
        }
        for (String name : names) {
            if (StringUtils.isBlank(name) || !fragment.hasElement(name)) {
                continue;
            }
            ContentElement element = fragment.getElement(name);
            String content = element == null ? null : element.getContent();
            if (StringUtils.isNotBlank(content)) {
                return content;
            }
        }
        return null;
    }

    /**
     * @return the named date element formatted for display, the raw value when it is not
     *         an ISO-8601 timestamp, or {@code null}
     */
    static String date(ContentFragment fragment, String... names) {
        String raw = text(fragment, names);
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
