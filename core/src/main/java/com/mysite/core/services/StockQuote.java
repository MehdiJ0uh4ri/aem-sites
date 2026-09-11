package com.mysite.core.services;

import java.time.Instant;

/**
 * A point-in-time quote for the company's listed shares.
 *
 * <p>Every figure is carried as the string the feed returned rather than a rounded double.
 * Investor relations pages are read as a record: a price must be reproduced exactly as the
 * exchange published it, and re-formatting it in Java would introduce a rounding decision
 * nobody asked for.</p>
 */
public interface StockQuote {

    /** @return the ticker symbol, for example {@code AVS} */
    String getSymbol();

    /** @return the exchange the symbol trades on, for example {@code NYSE} */
    String getExchange();

    /** @return the last traded price, as published */
    String getPrice();

    /** @return the ISO 4217 currency code of {@link #getPrice()} */
    String getCurrency();

    /** @return the absolute change since the previous close, signed */
    String getChange();

    /** @return the percentage change since the previous close, signed, without the sign character */
    String getChangePercent();

    /**
     * @return {@code true} when the price rose, {@code false} when it fell or was unchanged.
     *         Derived once here so no template has to parse a signed string to pick a colour.
     */
    boolean isUp();

    /** @return {@code true} when the price is unchanged since the previous close */
    boolean isUnchanged();

    /** @return when the exchange published this quote */
    Instant getQuotedAt();
}
