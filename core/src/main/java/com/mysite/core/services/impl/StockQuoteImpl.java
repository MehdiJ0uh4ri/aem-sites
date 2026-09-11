package com.mysite.core.services.impl;

import java.time.Instant;

import com.mysite.core.services.StockQuote;

/**
 * Immutable {@link StockQuote} value.
 */
final class StockQuoteImpl implements StockQuote {

    private final String symbol;
    private final String exchange;
    private final String price;
    private final String currency;
    private final String change;
    private final String changePercent;
    private final int direction;
    private final Instant quotedAt;

    /**
     * @param direction the sign of the change: negative, zero or positive, as returned by
     *                  {@link java.math.BigDecimal#signum()}
     */
    StockQuoteImpl(String symbol, String exchange, String price, String currency,
                   String change, String changePercent, int direction, Instant quotedAt) {
        this.symbol = symbol;
        this.exchange = exchange;
        this.price = price;
        this.currency = currency;
        this.change = change;
        this.changePercent = changePercent;
        this.direction = direction;
        this.quotedAt = quotedAt;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public String getExchange() {
        return exchange;
    }

    @Override
    public String getPrice() {
        return price;
    }

    @Override
    public String getCurrency() {
        return currency;
    }

    @Override
    public String getChange() {
        return change;
    }

    @Override
    public String getChangePercent() {
        return changePercent;
    }

    @Override
    public boolean isUp() {
        return direction > 0;
    }

    @Override
    public boolean isUnchanged() {
        return direction == 0;
    }

    @Override
    public Instant getQuotedAt() {
        return quotedAt;
    }
}
