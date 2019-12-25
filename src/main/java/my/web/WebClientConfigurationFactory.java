package my.web;

import my.web.proxy.RotatingProxySelector;

public enum WebClientConfigurationFactory {
	;

	/**
	 * <p>
	 * Returns a webClient configuration allowing fetching data against any
	 * server at will through a pool of ELITE proxies.
	 * </p>
	 * <p>
	 * A different proxy is used on each call made to the webClient build upon
	 * this webClient configuration.
	 * </p>
	 * <p>
	 * WARNING ! Caller must ensure data returned by the webClient matches its
	 * expected format (ie no empty string returned for example). If the data
	 * received doesn't look like the one expected, simply make a new call to
	 * the webClient.
	 * </p>
	 * 
	 * @return
	 * @see RotatingProxySelector
	 */
	public static WebClientConfiguration getConfigurationWithRotatingProxies() {
		WebClientConfiguration configurationWithProxy = new WebClientConfiguration();

		configurationWithProxy.makePauseBetweenFetches(false);
		configurationWithProxy.ignoreHttpErrors(true);
		configurationWithProxy.validateTLSCertificates(false);
		configurationWithProxy.setProxySelector(RotatingProxySelector.INSTANCE);
		configurationWithProxy.proxySelectorRaisesExceptionIfNoProxyFound(true);
		configurationWithProxy.setTimeoutInMs(2000); // We want speed...

		return configurationWithProxy;
	}

	public static WebClientConfiguration getConfigurationWithoutPauseBetweenFetches() {
		WebClientConfiguration configurationWithProxy = new WebClientConfiguration();

		configurationWithProxy.makePauseBetweenFetches(false);

		return configurationWithProxy;
	}
}
