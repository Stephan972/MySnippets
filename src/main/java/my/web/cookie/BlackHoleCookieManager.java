package my.web.cookie;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * 
 * A cookie manager forgetting consistently any cookie handled to it.
 *
 */
public enum BlackHoleCookieManager implements CookieManager {
    INSTANCE;

    @Override
    public Map<String, String> getCookies(String url) {
        return Collections.emptyMap();
    }

    @Override
    public void addCookies(URL url, Map<String, String> cookies) {
        // Do nothing
    }

    @Override
    public Map<String, Map<String, String>> getCookies() {
        return Collections.emptyMap();
    }

}
