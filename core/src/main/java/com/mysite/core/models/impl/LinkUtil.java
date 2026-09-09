package com.mysite.core.models.impl;

import org.apache.commons.lang3.StringUtils;

/**
 * Turns an authored path into a usable href.
 */
final class LinkUtil {

    private LinkUtil() {
        // static helper
    }

    /**
     * Appends {@code .html} to internal content paths so an authored page path works as a link.
     * External URLs, anchors and paths that already carry an extension are returned untouched.
     *
     * @param link the authored value
     * @return the href to render, or {@code null} when nothing was authored
     */
    static String toUrl(String link) {
        if (StringUtils.isBlank(link)) {
            return null;
        }
        String trimmed = link.trim();
        if (!trimmed.startsWith("/content")) {
            // external URL, mailto:, or an in-page anchor
            return trimmed;
        }
        // /content/dam/foo.pdf already resolves; only extensionless page paths need .html
        String lastSegment = StringUtils.substringAfterLast(trimmed, "/");
        return lastSegment.contains(".") ? trimmed : trimmed + ".html";
    }
}
