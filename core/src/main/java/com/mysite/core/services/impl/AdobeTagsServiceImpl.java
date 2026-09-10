package com.mysite.core.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.mysite.core.services.AdobeTagsService;

@Component(service = AdobeTagsService.class)
@Designate(ocd = AdobeTagsServiceImpl.Config.class)
public class AdobeTagsServiceImpl implements AdobeTagsService {

    @ObjectClassDefinition(name = "My Site - Adobe Experience Platform Tags",
                           description = "Embed configuration for Adobe Tags (formerly Launch).")
    public @interface Config {

        @AttributeDefinition(name = "Enabled",
                             description = "Render the Tags embed script in the page head.")
        boolean enabled() default false;

        @AttributeDefinition(name = "Embed URL",
                             description = "Full URL of the Tags embed script for this environment, "
                                     + "e.g. https://assets.adobedtm.com/<property>/<env>/launch-<hash>.min.js")
        String embedUrl() default "";

        @AttributeDefinition(name = "Load asynchronously",
                             description = "Uncheck only if a Tags extension needs to run before page render.")
        boolean async() default true;
    }

    private boolean enabled;
    private String embedUrl;
    private boolean async;

    @Activate
    protected void activate(Config config) {
        this.embedUrl = StringUtils.trimToNull(config.embedUrl());
        // An enabled flag with no URL would emit <script src> with no source.
        this.enabled = config.enabled() && this.embedUrl != null;
        this.async = config.async();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getEmbedUrl() {
        return embedUrl;
    }

    @Override
    public boolean isAsync() {
        return async;
    }
}
