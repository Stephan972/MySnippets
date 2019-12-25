package my.web.proxy;

import java.net.URL;

import lombok.Setter;

// FIXME: Make compatible with thread safety by contract...
public class ManualProxySelector implements ProxySelector {

	@Setter
	private Proxy currentProxy = NoProxy.INSTANCE;

	private boolean lastLike;

	@Override
	public Proxy bookProxy(URL u, boolean raiseExceptionIfNoProxyFound, URL... otherUrls) {
		return currentProxy;
	}

	@Override
	public void unbookProxy(Proxy proxy, boolean like) {
		currentProxy = NoProxy.INSTANCE;
		lastLike = like;
	}

	public boolean getLastLike() {
		return lastLike;
	}
}
