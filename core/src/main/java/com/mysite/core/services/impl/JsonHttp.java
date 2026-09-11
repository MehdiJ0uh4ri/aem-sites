package com.mysite.core.services.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Minimal JSON-over-HTTP client shared by the outbound integrations.
 *
 * <p>Everything here exists to keep a third party's bad day off the page. Every call is
 * bounded by a connect and a socket timeout, because an unbounded read on a request thread is
 * how one slow vendor takes an entire publish instance down with it. Failures are logged and
 * returned as an empty {@link Optional} rather than thrown: the components that call these
 * services all have a defined fallback, and none of them should turn a vendor outage into a
 * 500 on a page a visitor asked for.</p>
 */
final class JsonHttp {

    private static final Logger LOG = LoggerFactory.getLogger(JsonHttp.class);

    private static final Gson GSON = new Gson();

    /**
     * Deliberately short. These calls sit on a request thread behind the dispatcher, and a
     * cached-but-stale price beats a page that hangs for thirty seconds.
     */
    private static final int CONNECT_TIMEOUT_MS = 2_000;
    private static final int SOCKET_TIMEOUT_MS = 4_000;

    private JsonHttp() {
        // static helper
    }

    /**
     * @param url     the absolute endpoint URL, already carrying any query string
     * @param headers request headers, typically authorization
     * @param type    the class the response body is deserialized into
     * @return the parsed body, or empty on any transport, status or parse failure
     */
    static <T> Optional<T> get(String url, Map<String, String> headers, Class<T> type) {
        return execute(new HttpGet(url), headers, null, type);
    }

    /**
     * @param body the request body, serialized to JSON
     * @see #get(String, Map, Class)
     */
    static <T> Optional<T> post(String url, Map<String, String> headers, Object body, Class<T> type) {
        return execute(new HttpPost(url), headers, GSON.toJson(body), type);
    }

    private static <T> Optional<T> execute(HttpUriRequest request, Map<String, String> headers,
                                           String body, Class<T> type) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT_MS)
                .setSocketTimeout(SOCKET_TIMEOUT_MS)
                .build();

        headers.forEach(request::setHeader);
        request.setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());

        if (body != null && request instanceof HttpPost) {
            ((HttpPost) request).setEntity(
                    new StringEntity(body, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
        }

        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build();
             CloseableHttpResponse response = client.execute(request)) {

            int status = response.getStatusLine().getStatusCode();
            String payload = response.getEntity() == null
                    ? null
                    : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            if (status < 200 || status >= 300) {
                // The body of an error response often names the problem — an expired key, a
                // quota — so it is worth logging, but only at debug: it can carry the request
                // echoed back, credentials included.
                LOG.warn("{} {} returned HTTP {}", request.getMethod(), request.getURI(), status);
                LOG.debug("Error body: {}", payload);
                return Optional.empty();
            }
            return Optional.ofNullable(GSON.fromJson(payload, type));

        } catch (JsonSyntaxException e) {
            LOG.warn("{} returned a body that is not the expected JSON shape", request.getURI(), e);
        } catch (IOException | RuntimeException e) {
            LOG.warn("{} {} failed", request.getMethod(), request.getURI(), e);
        }
        return Optional.empty();
    }
}
