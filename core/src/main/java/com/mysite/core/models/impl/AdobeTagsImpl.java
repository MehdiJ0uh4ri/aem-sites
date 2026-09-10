package com.mysite.core.models.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

import com.mysite.core.models.AdobeTags;
import com.mysite.core.services.AdobeTagsService;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = AdobeTags.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class AdobeTagsImpl implements AdobeTags {

    @OSGiService
    private AdobeTagsService tagsService;

    @Override
    public boolean isEnabled() {
        return tagsService != null && tagsService.isEnabled();
    }

    @Override
    public String getEmbedUrl() {
        return tagsService == null ? null : tagsService.getEmbedUrl();
    }

    @Override
    public boolean isAsync() {
        return tagsService == null || tagsService.isAsync();
    }
}
