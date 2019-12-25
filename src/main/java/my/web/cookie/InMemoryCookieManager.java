package my.web.cookie;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InMemoryCookieManager implements CookieManager {

    private Map<String, Map<String, String>> host2cookies = new HashMap<>();

    @Override
    public Map<String, String> getCookies(String url) {
        Map<String, String> cookies;
        String host = URI.create(url).getHost();

        Map<String, String> cookiesCache = host2cookies.get(host);
        if (cookiesCache == null) {
            cookies = Collections.emptyMap();
        } else {
            cookies = Collections.unmodifiableMap(cookiesCache);
        }

        return cookies;
    }

    @Override
    public void addCookies(URL url, Map<String, String> cookies) {
        String host = url.getHost();

        host2cookies.computeIfAbsent(host, h -> new HashMap<>()).putAll(cookies);
    }

    @Override
    public Map<String, Map<String, String>> getCookies() {
        return Collections.unmodifiableMap(host2cookies);
    }
}
