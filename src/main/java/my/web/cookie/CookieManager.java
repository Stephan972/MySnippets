package my.web.cookie;

import java.net.URL;
import java.util.Map;

public interface CookieManager {

    Map<String, String> getCookies(String url);

    void addCookies(URL url, Map<String, String> cookies);

    /**
     *
     * Return a map containing all cookies in this cookieManager.
     *
     * <ul>
     * <li>Keys of this map are the domains for which cookies are set.
     * <li>Values ot this map are the cookies values.
     * </ul>
     * 
     * @return A map containing all cookies in this cookieManager.
     *
     */
    Map<String, Map<String, String>> getCookies();
}
