package my.web;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import my.web.cookie.BlackHoleCookieManager;
import my.web.cookie.CookieManager;
import my.web.proxy.NoProxySelector;
import my.web.proxy.ProxySelector;

@Data
@Accessors(fluent=true)
public class WebClientConfiguration {
	private boolean makePauseBetweenFetches = true;

	/**
	 * 
	 * Set the connect AND read timeout to {@code timeoutInMs}.
	 * 
	 * The timeout is expressed in ms. Its default value is 60000ms (1 mn).
	 * 
	 */
	@Getter
	@Setter
	@Accessors(fluent=false)	
	private int timeoutInMs = 60000; // in ms; default 60K ms (1 mn)

	private boolean ignoreHttpErrors = false;

	private boolean validateTLSCertificates = true;

	@Getter
	@Setter
	@Accessors(fluent=false)
	private ProxySelector proxySelector = NoProxySelector.INSTANCE;

	private boolean proxySelectorRaisesExceptionIfNoProxyFound = false;

	private boolean followRedirects = true;

	@Getter
	@Setter
	@Accessors(fluent=false)
	private CookieManager cookieManager = BlackHoleCookieManager.INSTANCE;
	
    @Getter
    @Setter
	private boolean showSentHeaders=false;
}
