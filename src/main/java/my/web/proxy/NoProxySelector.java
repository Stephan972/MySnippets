package my.web.proxy;

import java.net.URL;
import java.util.concurrent.Semaphore;

import my.exceptions.ApplicationRuntimeException;

/**
 * 
 * Selector that always returns no proxy.
 * 
 */
public enum NoProxySelector implements ProxySelector {
	INSTANCE;

	// For the maximum number of connections opened concurrently often used by
	// modern
	// browsers, see http://www.browserscope.org/?category=network&v=0
	private static final int MAX_PARALLEL_OPENED_CONNECTIONS = 30;

	private Semaphore noProxyUsageCount = new Semaphore(MAX_PARALLEL_OPENED_CONNECTIONS);

	@Override
	public Proxy bookProxy(URL u, boolean raiseExceptionIfNoProxyFound, URL... otherUrls) {
		try {
			noProxyUsageCount.acquire();
			return NoProxy.INSTANCE;
		} catch (InterruptedException e) {
			// TODO: Should we log exception (warn) and interrupt current thread
			// instead?
			throw new ApplicationRuntimeException(e);
		}
	}

	@Override
	public void unbookProxy(Proxy proxy, boolean like) {
		noProxyUsageCount.release();
	}
}
