package com.mysite.core.services;

/**
 * Supplies the Adobe Experience Platform Tags (formerly Launch) embed for the site.
 *
 * <p>Tags is the usual delivery vehicle for Adobe Analytics, Target and the Web SDK on an
 * AEM site: the embed script is loaded in the page head and reads the Adobe Client Data
 * Layer that the Core Components populate. Keeping the embed in OSGi configuration means
 * each environment (author, publish, stage, prod) can point at its own Tags library
 * through run-mode-specific config folders, with no code change.</p>
 */
public interface AdobeTagsService {

    /** @return {@code true} when an embed is configured and should be rendered */
    boolean isEnabled();

    /** @return the Tags embed script URL, or {@code null} when not configured */
    String getEmbedUrl();

    /** @return whether to load the embed asynchronously */
    boolean isAsync();
}
