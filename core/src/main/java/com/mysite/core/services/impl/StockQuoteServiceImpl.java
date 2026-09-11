package com.mysite.core.services.impl;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.SerializedName;
import com.mysite.core.services.StockQuote;
import com.mysite.core.services.StockQuoteService;

/**
 * Reads the share price from a market-data vendor and holds it in memory for a configured
 * interval.
 *
 * <p>The cache is the point of this class. The ticker sits in the investor-relations header,
 * so without it every uncached page view would become a vendor call — and a dispatcher flush
 * would turn a traffic spike into a rate-limit ban. One entry is enough: the site quotes a
 * single symbol.</p>
 *
 * <p>A stale quote is preferred to no quote. When a refresh fails the previous value is kept
 * and served past its expiry rather than blanked, so a brief vendor outage shows a price a few
 * minutes old instead of an empty header.</p>
 */
@Component(service = StockQuoteService.class, immediate = true)
@Designate(ocd = StockQuoteServiceImpl.Config.class)
public class StockQuoteServiceImpl implements StockQuoteService {

    private static final Logger LOG = LoggerFactory.getLogger(StockQuoteServiceImpl.class);

    @ObjectClassDefinition(name = "AeroVance — Stock Quote Service",
                           description = "Reads the share price from a market-data vendor for the investor-relations ticker.")
    public @interface Config {

        @AttributeDefinition(name = "Endpoint",
                description = "Quote endpoint. {symbol} is replaced with the ticker symbol.")
        String endpoint() default "https://cloud.iexapis.com/stable/stock/{symbol}/quote";

        @AttributeDefinition(name = "Ticker symbol")
        String symbol() default "AVS";

        @AttributeDefinition(name = "Exchange",
                description = "Displayed alongside the symbol. Not read from the feed, because vendors disagree on how they name it.")
        String exchange() default "NYSE";

        @AttributeDefinition(name = "Currency",
                description = "ISO 4217 code the vendor quotes in.")
        String currency() default "USD";

        @AttributeDefinition(name = "API token",
                description = "Sent as a bearer token. Set this through a Cloud Manager secret environment variable, never in a committed OSGi config.",
                type = AttributeType.PASSWORD)
        String apiToken() default "";

        @AttributeDefinition(name = "Cache duration (seconds)",
                description = "How long a quote is served before a refresh is attempted. Delayed-quote licences are typically 15 minutes; keep this at or above what the vendor agreement requires.")
        int cacheSeconds() default 900;
    }

    /** Shape of the vendor's response. Only the fields the ticker renders are mapped. */
    private static final class QuotePayload {
        @SerializedName("latestPrice")
        BigDecimal latestPrice;
        @SerializedName("change")
        BigDecimal change;
        @SerializedName("changePercent")
        BigDecimal changePercent;
    }

    /** A quote and the moment it was fetched, replaced as one so a reader never sees a half-update. */
    private static final class CacheEntry {
        final StockQuote quote;
        final Instant fetchedAt;

        CacheEntry(StockQuote quote, Instant fetchedAt) {
            this.quote = quote;
            this.fetchedAt = fetchedAt;
        }
    }

    private final AtomicReference<CacheEntry> cache = new AtomicReference<>();

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        // A configuration change may have altered the symbol or the endpoint, so anything
        // held from the previous configuration is no longer the right answer.
        cache.set(null);
    }

    @Override
    public Optional<StockQuote> getQuote() {
        if (StringUtils.isBlank(config.apiToken())) {
            // Expected on a local author instance, where no market-data licence is configured.
            LOG.debug("No market-data token configured; the ticker will render its fallback.");
            return Optional.empty();
        }

        CacheEntry current = cache.get();
        if (current != null && !isExpired(current)) {
            return Optional.of(current.quote);
        }

        Optional<StockQuote> fetched = fetch();
        if (fetched.isPresent()) {
            cache.set(new CacheEntry(fetched.get(), Instant.now()));
            return fetched;
        }

        // Refresh failed. Serving the last known price beats blanking the header, so the
        // stale entry is kept and returned; the next request will try again.
        if (current != null) {
            LOG.warn("Stock quote refresh failed; serving the quote from {}", current.fetchedAt);
            return Optional.of(current.quote);
        }
        return Optional.empty();
    }

    private boolean isExpired(CacheEntry entry) {
        return Duration.between(entry.fetchedAt, Instant.now())
                .compareTo(Duration.ofSeconds(config.cacheSeconds())) >= 0;
    }

    private Optional<StockQuote> fetch() {
        String url = config.endpoint().replace("{symbol}", config.symbol());
        Map<String, String> headers = Map.of("Authorization", "Bearer " + config.apiToken());

        return JsonHttp.get(url, headers, QuotePayload.class)
                .filter(payload -> payload.latestPrice != null)
                .map(this::toQuote);
    }

    private StockQuote toQuote(QuotePayload payload) {
        BigDecimal change = payload.change == null ? BigDecimal.ZERO : payload.change;
        // Vendors differ on whether the percentage arrives as 0.0123 or 1.23. IEX sends a
        // fraction, so it is scaled here rather than in the template.
        BigDecimal percent = payload.changePercent == null
                ? BigDecimal.ZERO
                : payload.changePercent.multiply(BigDecimal.valueOf(100));

        return new StockQuoteImpl(
                config.symbol(),
                config.exchange(),
                payload.latestPrice.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                config.currency(),
                change.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                percent.abs().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                change.signum(),
                Instant.now());
    }
}
