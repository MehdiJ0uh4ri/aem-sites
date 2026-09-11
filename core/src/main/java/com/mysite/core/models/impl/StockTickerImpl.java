package com.mysite.core.models.impl;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.util.AbstractComponentImpl;
import com.mysite.core.models.StockTicker;
import com.mysite.core.services.StockQuote;
import com.mysite.core.services.StockQuoteService;

@Model(adaptables = SlingHttpServletRequest.class,
       adapters = { StockTicker.class, ComponentExporter.class },
       resourceType = StockTickerImpl.RESOURCE_TYPE,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class StockTickerImpl extends AbstractComponentImpl implements StockTicker {

    public static final String RESOURCE_TYPE = "mysite/components/stockticker";

    private static final String DEFAULT_FALLBACK = "Share price temporarily unavailable";

    @OSGiService
    private StockQuoteService stockQuoteService;

    @ValueMapValue
    private String fallbackText;

    @ValueMapValue
    private String disclaimer;

    @ValueMapValue
    private String link;

    private StockQuote quote;

    @PostConstruct
    private void init() {
        // The service never throws for an unreachable vendor; it returns empty, and the
        // template renders the fallback. See StockQuoteServiceImpl.
        quote = stockQuoteService == null ? null : stockQuoteService.getQuote().orElse(null);
    }

    @Override
    public StockQuote getQuote() {
        return quote;
    }

    @Override
    public boolean isAvailable() {
        return quote != null;
    }

    @Override
    public String getFallbackText() {
        return StringUtils.defaultIfBlank(fallbackText, DEFAULT_FALLBACK);
    }

    @Override
    public String getDisclaimer() {
        return disclaimer;
    }

    @Override
    public String getLinkUrl() {
        return LinkUtil.toUrl(link);
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
