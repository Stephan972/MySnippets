package my.web.proxy;

import static my.web.WebClient.OK;
import static my.web.proxy.ProxyClock.now;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonObject.Member;
import org.hjson.JsonValue;
import org.jsoup.Connection.Response;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import my.RandomUtils;
import my.exceptions.ApplicationRuntimeException;
import my.web.WebClient;
import my.web.WebClientConfiguration;
import my.web.proxy.Protocol.Support;

@Slf4j
// Not thread safe !
//
// FIXME: Make RotatingProxySelector thread safe for honoring ProxySelector
// interface contract.
// FIXME: Manage a database with known working proxies in order to speed up
// proxy finnding...
public enum RotatingProxySelector implements ProxySelector, Comparator<Proxy> {
	INSTANCE;

	private static final long FIVE_HOURS = 5;
	// 20% = 0.2; Valid values range: [0.0;1.0[
	private static final double LOW_PROXIES_COUNT_RATIO = 0.2;

	private int initialProxiesSetSize;
	private NavigableSet<Proxy> proxiesSet;
	@Setter
	private boolean raiseExceptionIfNoProxyFound;
	private ManualProxySelector manualProxySelector;
	private WebClient proxyChecker;

	/**
	 * 
	 * @see RotatingProxySelector#bookProxy(URL,raiseExceptionIfNoProxyFound,
	 *      URL...)
	 */
	private RotatingProxySelector() {
		initialProxiesSetSize = 0;
		proxiesSet = new TreeSet<>(this);

		manualProxySelector = new ManualProxySelector();

		WebClientConfiguration webClientConfiguration = new WebClientConfiguration();
		webClientConfiguration.setProxySelector(manualProxySelector);
		webClientConfiguration.makePauseBetweenFetches(false);
		webClientConfiguration.validateTLSCertificates(false);
		webClientConfiguration.setTimeoutInMs(3000);

		proxyChecker = new WebClient(webClientConfiguration);
	}

	/**
	 * 
	 * Returns a {@link Proxy} for the given url.
	 * 
	 * @param raiseExceptionIfNoProxyFound
	 *            <ul>
	 *            <li>TRUE => bookProxy method raises
	 *            ApplicationRuntimeException when there is no more proxy</li>
	 *            <li>FALSE => bookProxy method returns {@link NoProxy#INSTANCE}
	 *            when there is no more proxy</li>
	 *            </ul>
	 *
	 * @return If no proxy found, either returns {@link NoProxy#INSTANCE} or
	 *         throws {@link ApplicationRuntimeException} depending on
	 *         {@code raiseExceptionIfNoProxyFound} value.
	 * 
	 * @see {@link RotatingProxySelector(boolean)}
	 * 
	 */
	@Override
	public Proxy bookProxy(URL url, boolean raiseExceptionIfNoProxyFound, URL... otherUrls) {
		refreshProxiesIfNeeded();

		URL[] urls = new URL[otherUrls.length + 1];
		urls[0] = url;
		int len = urls.length;
		for (int i = 1; i < len; i++) {
			urls[i] = otherUrls[i - 1];
		}

		Proxy proxy = findAdequateProxyFor(urls);
		if (proxy == null) {
			if (raiseExceptionIfNoProxyFound) {
				throw new ApplicationRuntimeException("No more proxy selectable...");
			} else {
				proxy = NoProxy.INSTANCE;
			}
		}

		return proxy;
	}

	private Proxy findAdequateProxyFor(URL[] urls) {
		boolean keepUsing;
		Map<Proxy, Boolean> checkedProxies = new HashMap<>();
		Proxy proxy = null;
		Set<Protocol> detectedProtocols = detectProtocolsFrom(urls);

		boolean newProxyPollNeeded;
		int attemptsLeftCount = 10;
		do {
			proxy = proxiesSet.pollFirst();
			if (proxy == null) {
				attemptsLeftCount--;
				log.info(" ** Proxies list exhausted! Attempts left: {}", attemptsLeftCount);
				if (attemptsLeftCount > 0) {
					RandomUtils.pauseRandomly(1, 60, TimeUnit.MINUTES);
					refreshProxies("Previously exhausted while finding an adequate proxy for:\n" + Arrays.toString(urls));
					newProxyPollNeeded = true;
				} else {
					// No more proxies available...
					newProxyPollNeeded = false;
				}
			} else {
				Set<Protocol> protocolsToCheck = newProtocolCheckNeeded(proxy, detectedProtocols);
				keepUsing = true;
				if (!protocolsToCheck.isEmpty()) {
					keepUsing = checkProxyForProtocols(proxy, protocolsToCheck);
				}

				if (proxy.supportsAll(detectedProtocols)) {
					newProxyPollNeeded = false;
				} else {
					newProxyPollNeeded = true;
					checkedProxies.put(proxy, keepUsing);
				}
			}
		} while (newProxyPollNeeded);

		if (!checkedProxies.isEmpty()) {
			for (Map.Entry<Proxy, Boolean> e : checkedProxies.entrySet()) {
				unbookProxy(e.getKey(), e.getValue());
			}

			checkedProxies.clear();
		}

		return proxy;
	}

	private Set<Protocol> detectProtocolsFrom(URL[] urls) {
		Set<Protocol> detectedProtocols = EnumSet.noneOf(Protocol.class);
		int len = urls.length;

		for (int i = 0; i < len; i++) {
			String urlProtocol = urls[i].getProtocol().toLowerCase();
			if (!Protocol.recognize(urlProtocol)) {
				throw new ApplicationRuntimeException("Unsupported protocol: " + urlProtocol);
			}
			detectedProtocols.add(Protocol.from(urlProtocol));
		}

		return detectedProtocols;
	}

	/**
	 * 
	 * Return a set of protocols containing the protocols needing a new check
	 * for the given proxy.
	 * 
	 * @param proxy
	 * @param protocols
	 * @return
	 */
	private Set<Protocol> newProtocolCheckNeeded(Proxy proxy, Set<Protocol> protocols) {
		boolean ret;
		Set<Protocol> protocolsToCheck = EnumSet.noneOf(Protocol.class);

		for (Protocol protocolToCheck : protocols) {
			long lastProtocolCheck = proxy.getLastProtocolCheckFor(protocolToCheck);
			if (lastProtocolCheck == 0L) {
				ret = true;
			} else if (TimeUnit.MILLISECONDS.toHours(now() - lastProtocolCheck) >= FIVE_HOURS) {
				ret = RandomUtils.getSecureRandom().nextFloat() < 0.02;
			} else {
				ret = false;
			}

			if (ret) {
				protocolsToCheck.add(protocolToCheck);
			}
		}

		return protocolsToCheck;
	}

	/**
	 * 
	 * Return true if and only if proxy supports ALL given protocols (
	 * {@code protocolsToCheck}).
	 * 
	 * @param proxy
	 * @param protocolsToCheck
	 * @return
	 */
	private boolean checkProxyForProtocols(Proxy proxy, Set<Protocol> protocolsToCheck) {
		boolean ret = true;

		for (Protocol protocol : protocolsToCheck) {
			ret = checkProxyForProtocol(proxy, protocol);
			if (!ret) {
				break;
			}
		}

		return ret;
	}

	private boolean checkProxyForProtocol(Proxy proxy, Protocol protocolToCheck) {
		String url = protocolToCheck.toString() + "://posthere.io/" + UUID.randomUUID().toString();
		manualProxySelector.setCurrentProxy(proxy);
		boolean keepUsing = false;

		try {
			proxy.setProtocolSupport(protocolToCheck, Protocol.Support.UNKNOWN);
			int headerId = RandomUtils.randInt(1, 100000);
			String headerValue = Integer.toString(RandomUtils.randInt(1, 100000));
			Map<String, String> randomHeader = Collections.singletonMap("Foo" + headerId, headerValue);
			Response r = proxyChecker.post(url.toLowerCase(), randomHeader);
			keepUsing = manualProxySelector.getLastLike();

			if (r.statusCode() == OK) {
				verifyProtocolSupport(proxy, protocolToCheck, url, randomHeader);
			}
		} catch (Throwable t) {
			log.warn("Proxy check failed for protocol: " + protocolToCheck + " [" + proxy + "]", t);
		} finally {
			log.debug("Proxy {} / {} support: {}", proxy, protocolToCheck, proxy.supports(protocolToCheck));
		}

		return keepUsing;
	}

	private void verifyProtocolSupport(Proxy proxy, Protocol protocol, String url, Map<String, String> randomHeader) {
		manualProxySelector.setCurrentProxy(NoProxy.INSTANCE);

		Protocol.Support support = Protocol.Support.KO;
		JsonValue jsonValue = proxyChecker.fetchAsJson(url);
		if (jsonValue.isArray()) {
			JsonArray jsonArray = jsonValue.asArray();
			if (jsonArray.size() > 0) {
				JsonValue tmp = jsonArray.get(0);
				if (tmp.isObject()) {
					support = compareHeaders(tmp.asObject().get("headers"), randomHeader);
				}
			}
		}

		proxy.setProtocolSupport(protocol, support);

		proxyChecker.delete(url);
	}

	private Protocol.Support compareHeaders(JsonValue actualHeaders, Map<String, String> expectedHeaders) {
		Protocol.Support support = Protocol.Support.KO;

		if (actualHeaders.isObject()) {
			Map<String, String> actualHeadersSorted = new TreeMap<>();
			JsonObject headers = actualHeaders.asObject();
			for (Member m : headers) {
				actualHeadersSorted.put(m.getName().toLowerCase(), m.getValue().asString());
			}

			int actualHeadersCount = 0;
			for (Map.Entry<String, String> e : expectedHeaders.entrySet()) {
				String v = e.getValue();
				if (v == null) {
					v = "";
				}

				String tmp = actualHeadersSorted.get(e.getKey().toLowerCase());
				actualHeadersCount += v.equalsIgnoreCase(tmp) ? 1 : 0;
			}

			if (actualHeadersCount == expectedHeaders.size()) {
				support = Support.OK;
			}
		}

		return support;
	}

	private void refreshProxies(String reason) {
		if ((reason == null) || (reason.isEmpty())) {
			throw new ApplicationRuntimeException("No reason provided for refreshing proxies.");
		}

		log.info("Refreshing proxies list: \"{}\"", reason);
		log.warn(" *********************************\n\n *********************************\n\n *********************************\n\n",
				new Throwable());
		int newlyAddedProxiesCount = ProxyFactory.addNewProxiesInto(proxiesSet);

		if (newlyAddedProxiesCount == 0) {
			throw new ApplicationRuntimeException("Unable to load new proxies.");
		} else {
			// FIXME: Why calculate initialProxiesSetSize here and not earlier?
			initialProxiesSetSize = proxiesSet.size();
			log.info("SUCCESSFULLY added {} proxies ! (Total: {})", newlyAddedProxiesCount, initialProxiesSetSize);
		}
	}

	@Override
	public void unbookProxy(Proxy proxy, boolean keepUsingProxy) {
		if (keepUsingProxy) {
			proxy.incrementSuccessfulAttemptUse();
			proxiesSet.add(proxy);
		} else {
			log.trace("Removing following proxy from proxy list [ {} ]", proxy);
		}
	}

	private void refreshProxiesIfNeeded() {
		boolean refreshNeeded = true;
		String reason = "NO REASON SPECIFIED !!";

		if (proxiesSet.isEmpty()) {
			reason = "No proxies available.";
		} else if (proxiesSet.size() < LOW_PROXIES_COUNT_RATIO * initialProxiesSetSize) {
			reason = "Proxies start to running out.";
		} else {
			refreshNeeded = false;
		}

		if (refreshNeeded) {
			refreshProxies(reason);
		}
	}

	@Override
	public int compare(Proxy firstProxy, Proxy secondProxy) {
		// Compare anonimyty types (the highest the lower in order)
		AnonimityType firstProxyAT = firstProxy.getAnonimityType();
		AnonimityType secondProxyAT = secondProxy.getAnonimityType();
		if (firstProxyAT.isGreaterThan(secondProxyAT)) {
			return -1;
		}

		if (firstProxyAT.isLowerThan(secondProxyAT)) {
			return 1;
		}

		// Compare successful attempt use (the lowest the lower in order)
		int firstProxySuccessfulAttemptUse = firstProxy.getSuccessfulAttemptUse();
		int secondProxySuccessfulAttemptUse = secondProxy.getSuccessfulAttemptUse();
		if (firstProxySuccessfulAttemptUse > secondProxySuccessfulAttemptUse) {
			return 1;
		}

		if (firstProxySuccessfulAttemptUse < secondProxySuccessfulAttemptUse) {
			return -1;
		}

		// Compare median latency (the lowest the lower in order)
		double firstProxyMedianLatency = firstProxy.getMedianLatency();
		double secondProxyMedianLatency = secondProxy.getMedianLatency();
		if (firstProxyMedianLatency < secondProxyMedianLatency) {
			return -1;
		}

		if (firstProxyMedianLatency > secondProxyMedianLatency) {
			return 1;
		}

		return firstProxy.compareTo(secondProxy);
	}
}
