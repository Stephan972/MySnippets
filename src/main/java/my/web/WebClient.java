package my.web;

import static java.util.Collections.singletonMap;
import static my.RandomUtils.randInt;
import static my.web.utils.WebUtils.createEmptyDocument;
import static my.web.utils.WebUtils.getUserAgent;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hjson.JsonValue;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UncheckedIOException;
import org.jsoup.nodes.Document;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import my.web.cookie.CookieManager;
import my.web.proxy.NoProxy;
import my.web.proxy.Proxy;
import my.web.proxy.ProxySelector;
import my.web.utils.WebUtils;

@Slf4j
// Not thread safe
// TODO: Make fetchAsDocument and download methods use performMethod or an
// enhanced version of performMethod...
public class WebClient {
    private static final long FOUR_SECONDS_IN_NS = TimeUnit.NANOSECONDS.convert(4, TimeUnit.SECONDS);
    private static final int INFINITE_SIZE = 0;

    private static long lastFetchDurationInNs = -1;

    private static final Map<String, String> APPLICATION_JSON_HEADER = singletonMap("Accept", "application/json");

    public static final int OK = 200;
    public static final int NOT_MODIFIED = 304;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int FAILED_REQUEST = -1;

    private WebClientConfiguration configuration;

    public WebClient() {
        this(new WebClientConfiguration());
    }

    public WebClient(WebClientConfiguration configuration) {
        this.configuration = configuration;
    }

    public Document fetchAsDocument(String url) {
        return fetchAsDocument(url, Collections.emptyMap());
    }

    /**
     * 
     * @param url
     * @param headers
     * @return The document corresponding to the passed url or an empty document if
     *         an exception is raised.
     */
    public Document fetchAsDocument(String url, Map<String, String> headers) {
        Document ret = null;

        try {
            ret = fetch0(Method.GET, url, false, headers).parse();
        } catch (HttpStatusException hse) {
            handleHttpStatusException(hse);
        } catch (UncheckedIOException | InterruptedException | IOException e) {
            log.error("Fail to fetch " + url, e);
        }

        if (ret == null) {
            ret = createEmptyDocument();
        }

        return ret;
    }

    public byte[] download(String url) {
        return download(url, Collections.emptyMap());
    }

    public byte[] download(String url, Map<String, String> headers) {
        byte[] ret = null;

        try {
            ret = fetch0(Method.GET, url, true, headers).bodyAsBytes();
        } catch (HttpStatusException hse) {
            handleHttpStatusException(hse);
        } catch (UncheckedIOException | InterruptedException | IOException e) {
            log.error("Fail to download " + url, e);
        }

        if (ret == null) {
            ret = new byte[0];
        }

        return ret;
    }

    public Response head(String url) {
        return performMethod(Method.HEAD, url);
    }

    private Response performMethod(Method m, String url) {
        return performMethod(m, url, Collections.emptyMap(), Collections.emptyMap());
    }

    private Response performMethod(Method m, String url, Map<String, String> headers, Map<String, String> data) {
        return performMethod(m, url, true, headers, data);
    }

    private Response performMethod(Method m, String url, boolean ignoreContentType, Map<String, String> headers, Map<String, String> data) {
        Response ret;

        try {
            ret = fetch0(m, url, ignoreContentType, headers, data);
        } catch (HttpStatusException hse) {
            handleHttpStatusException(hse);
            ret = new EmptyResponse(m, url, hse.getStatusCode(), hse.getMessage());
        } catch (IOException e) {
            log.warn("Unable to perform " + m + " request on " + url, e);
            ret = new EmptyResponse(m, url, e.getMessage());
        } catch (InterruptedException e) {
            log.warn("Interruption raised while heading to " + url, e);
            ret = new EmptyResponse(m, url, e.getMessage());
            Thread.currentThread().interrupt();
        }

        return ret;
    }

    private void handleHttpStatusException(HttpStatusException hse) {
        log.warn("HTTP Error: [" + hse.getStatusCode() + "] " + hse.getUrl(), hse);
    }

    // private Response fetch0(Method method, String url, boolean
    // ignoreContentType) throws IOException, InterruptedException {
    // return fetch0(method, url, ignoreContentType, Collections.emptyMap());
    // }

    private <T> T fetch(FetchHelper<T> fetchHelper) {
        T ret = null;
        Method m = fetchHelper.method();
        String url = fetchHelper.url();

        try {
            Response response = fetch0(m, url, fetchHelper.ignoreContentType(), fetchHelper.headers(), fetchHelper.data());
            ret = fetchHelper.buildFinalResponse0(response);
        } catch (HttpStatusException hse) {
            handleHttpStatusException(hse);
            ret = fetchHelper.createEmptyFinalResponse(m, url, hse);
        } catch (/* UncheckedIOException | */ IOException e) {
            log.warn(fetchHelper.getWarnMessage(m, url, e), e);
            ret = fetchHelper.createEmptyFinalResponse(m, url, e);
        } catch (InterruptedException e) {
            log.warn(fetchHelper.getWarnMessage(m, url, e), e);
            ret = fetchHelper.createEmptyFinalResponse(m, url, e);
            Thread.currentThread().interrupt();
        }

        return ret;
    }

    private Response fetch0(Method method, String url, boolean ignoreContentType, Map<String, String> headers) throws IOException, InterruptedException {
        return fetch0(method, url, ignoreContentType, headers, Collections.emptyMap());
    }

    /**
     * 
     * The download cannot exceed 1MB (Jsoup default value) (see maxBodySize).
     * 
     * @param url
     * @param ignoreContentType
     * @param headers
     * @param data
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private Response fetch0(Method method, String url0, boolean ignoreContentType, Map<String, String> headers, Map<String, String> data) throws IOException, InterruptedException {
        if (configuration.makePauseBetweenFetches()) {
            haveBreakNowIfNecessary();
        }

        String url = WebUtils.format(url0);
        URL u = new URL(url);
        ProxySelector proxySelector = configuration.getProxySelector();
        Proxy proxy = proxySelector.bookProxy(u, configuration.proxySelectorRaisesExceptionIfNoProxyFound());
        if (proxy != NoProxy.INSTANCE) {
            log.info("Using proxy: {}", proxy.toString());
        }

        log.info("{} {}", method, url);
        long t0 = nanoTime();
        Response r = null;
        boolean keepUsingProxy = true;
        boolean responseReturnedSuccessfully = false;
        try {
            // Prepare headers to send...
            Map<String, String> effectiveHeaders = new LinkedHashMap<>(headers);
            effectiveHeaders.put("User-Agent", getUserAgent());
            effectiveHeaders.put("Accept-Language", "en-US,en;q=0.8,fr-FR;q=0.6,fr;q=0.4");

            if (configuration.showSentHeaders()) {
                StringBuilder sb = new StringBuilder();

                for (Map.Entry<String, String> e : effectiveHeaders.entrySet()) {
                    sb.append(e.getKey());
                    sb.append(": ");
                    sb.append(e.getValue());
                    sb.append("\n");
                }

                log.info("\n{}", sb.toString());
            }

            // Execute HTTP method
            try {
                CookieManager cookieManager = configuration.getCookieManager();

                r = Jsoup.connect(url) //
                        .proxy(proxy.toJavaProxy()) //
                        .validateTLSCertificates(configuration.validateTLSCertificates()) //
                        .method(method) //
                        .data(data) //
                        .followRedirects(configuration.followRedirects()) //
                        .timeout(configuration.getTimeoutInMs()) //
                        .maxBodySize(INFINITE_SIZE) //
                        .ignoreContentType(ignoreContentType) //
                        .ignoreHttpErrors(configuration.ignoreHttpErrors()) //
                        .headers(effectiveHeaders) //
                        .cookies(cookieManager.getCookies(url)) //
                        .execute();

                // We use r.url() instead of url because we may have received
                // redirections
                cookieManager.addCookies(r.url(), r.cookies());
            } catch (org.jsoup.UncheckedIOException uioe) {
                throw uioe.ioException();
            }

            responseReturnedSuccessfully = true;
            return r;
        } catch (HttpStatusException | SocketTimeoutException e) {
            keepUsingProxy = true;
            throw e;
        } catch (IOException e) {
            keepUsingProxy = false;
            throw e;
        } finally {
            if (responseReturnedSuccessfully) {
                lastFetchDurationInNs = nanoTime() - t0;
                proxy.setLatency(lastFetchDurationInNs, TimeUnit.NANOSECONDS);
            }

            proxySelector.unbookProxy(proxy, keepUsingProxy);
        }
    }

    private void haveBreakNowIfNecessary() throws InterruptedException {
        // TODO: differentiate lastFetch calculation by host...
        if (lastFetchDurationInNs != -1) {
            if (lastFetchDurationInNs < FOUR_SECONDS_IN_NS) {
                long suspensionTimeInNs = FOUR_SECONDS_IN_NS - lastFetchDurationInNs;
                log.info("Suspending execution to give server some rest during {} ms", toMs(suspensionTimeInNs));

                TimeUnit.NANOSECONDS.sleep(suspensionTimeInNs);
            } else {
                int suspensionTimeInMs = randInt(15000, 120000);
                log.warn("Last fetch was long (actually {} ms). Pausing execution randomly for {} ms.", toMs(lastFetchDurationInNs), suspensionTimeInMs);
                TimeUnit.MILLISECONDS.sleep(suspensionTimeInMs);
            }
        }
    }

    private long nanoTime() {
        return StrictMath.abs(System.nanoTime());
    }

    private long toMs(long nsValue) {
        return TimeUnit.MILLISECONDS.convert(nsValue, TimeUnit.NANOSECONDS);
    }

    public JsonValue fetchAsJson(String url) {
        return fetchAsJson(url, APPLICATION_JSON_HEADER);
    }

    public JsonValue fetchAsJson(String url, Map<String, String> headers) {
        byte[] rawData = download(url, headers);
        String rawJson = new String(rawData, StandardCharsets.UTF_8);

        return JsonValue.readJSON(rawJson);
    }

    public Response post(String url) {
        return post(url, Collections.emptyMap());
    }

    public Response post(String url, Map<String, String> headers) {
        return post(url, headers, Collections.emptyMap());
    }

    public Response post(String url, Map<String, String> headers, Map<String, String> data) {
        return performMethod(Method.POST, url, headers, data);
    }

    public Response delete(String url) {
        return performMethod(Method.DELETE, url);
    }

    public Response get(String url) {
        return get(url, Collections.emptyMap());
    }

    public Response get(String url, Map<String, String> headers) {
        return performMethod(Method.GET, url, headers, Collections.emptyMap());
    }

    @Accessors(chain = false, fluent = true)
    @Getter
    abstract class FetchHelper<T> {
        private Method method;
        private String url;
        private boolean ignoreContentType;
        private Map<String, String> headers;
        private Map<String, String> data;

        public final T buildFinalResponse0(Response response) throws IOException {
            try {
                return buildFinalResponse(response);
            } catch (UncheckedIOException e) {
                throw e.ioException();
            }
        }

        public abstract String getWarnMessage(Method m, String url2, IOException e);

        public abstract String getWarnMessage(Method m, String url, InterruptedException e);

        public abstract T createEmptyFinalResponse(Method m, String url, HttpStatusException hse);

        public abstract T createEmptyFinalResponse(Method m, String url, IOException ioe);

        public abstract T createEmptyFinalResponse(Method m, String url, InterruptedException ie);

        public abstract T buildFinalResponse(Response response);
    }
}
