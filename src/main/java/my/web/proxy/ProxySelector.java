package my.web.proxy;

import java.net.URL;

import my.exceptions.ApplicationRuntimeException;

public interface ProxySelector {

	/**
	 *
	 * Returns a proxy, that can handle given url(s), chosen at this selector
	 * own discretion.
	 *
	 * The proxy instance is reserved to the calling thread. No other thread can
	 * receive the proxy insstance returned by this method.
	 *
	 * This method MUST NOT return a null value.
	 * 
	 * If there is no selectable proxy, this ProxySelector returns
	 * {@link NoProxy#INSTANCE} or raises {@link ApplicationRuntimeException}. The
	 * exception is raised if {@code raiseExceptionIfNoProxyFound} is TRUE.
	 *
	 * The provided proxy MUST be returned to Selector in order to un-reserve it
	 * (the proxy). This return is also used for providing a feedback on proxy.
	 * This feedback helps Selector keep in its internal pool only working
	 * proxies.
	 *
	 * @param url
	 * @param raiseExceptionIfNoProxyFound
	 * @param otherUrls
	 *
	 * @return A proxy instance or {@link NoProxy#INSTANCE}.
	 * @see #unbookProxy(Proxy, boolean)
	 *
	 */
	Proxy bookProxy(URL url, boolean raiseExceptionIfNoProxyFound, URL... otherUrls);

	/**
	 * 
	 * Method used to un-reserve previously reserved proxy.
	 * 
	 * @param proxy
	 * @param like
	 */
	void unbookProxy(Proxy proxy, boolean like);

}
