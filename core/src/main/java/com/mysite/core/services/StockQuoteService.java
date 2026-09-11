package com.mysite.core.services;

import java.util.Optional;

/**
 * Reads the company's share price from a market-data vendor.
 *
 * <p>Implementations are expected to cache: the ticker appears in the site header on every
 * investor-relations page, and a dispatcher flush would otherwise turn one page view into
 * one vendor call and exhaust the rate limit within minutes.</p>
 */
public interface StockQuoteService {

    /**
     * @return the latest quote, or empty when the vendor is unreachable, misconfigured, or
     *         returned something unusable. Callers render a fallback rather than an error:
     *         a missing price is a degraded header, not a broken page.
     */
    Optional<StockQuote> getQuote();
}
