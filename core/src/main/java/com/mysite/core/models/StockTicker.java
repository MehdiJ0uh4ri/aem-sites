package com.mysite.core.models;

import com.adobe.cq.wcm.core.components.models.Component;
import com.mysite.core.services.StockQuote;

/**
 * Model for the Stock Ticker.
 *
 * <p>The component is included dynamically rather than cached with the page: a share price
 * and a page about the company's history have nothing in common in how often they change,
 * and caching them together would force the whole investor-relations section to expire every
 * fifteen minutes.</p>
 */
public interface StockTicker extends Component {

    /** @return the latest quote, or {@code null} when the feed is unavailable */
    StockQuote getQuote();

    /** @return {@code true} when a quote could be read */
    boolean isAvailable();

    /**
     * @return the sentence shown in place of the price when the feed is unavailable. Always
     *         populated, so the header never collapses to nothing.
     */
    String getFallbackText();

    /** @return the delayed-quote disclaimer required by the market-data licence, or {@code null} */
    String getDisclaimer();

    /** @return resolved href of the investor-relations landing page, or {@code null} */
    String getLinkUrl();
}
