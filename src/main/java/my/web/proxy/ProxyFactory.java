package my.web.proxy;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static my.web.proxy.AnonimityType.ELITE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import lombok.extern.slf4j.Slf4j;
import my.exceptions.ApplicationRuntimeException;
import my.web.WebClient;
import my.web.WebClientConfiguration;
import my.web.proxy.ProxyProvider.InvalidJsonValueException;
import my.web.utils.WebUtils;
import my.web.utils.WebUtils.DumpExtension;

import org.apache.commons.io.IOUtils;
import org.hjson.JsonArray;
import org.hjson.JsonValue;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Slf4j
// TODO: Save proxies in a list for later reuse AND faster restart... (be aware
// of dead proxies. our list MUST be fresh not a proxy graveyard !)
// TODO: Implement an evaluation method for detecting proxy anonimity level:
// ELITE, TRANSPARENT etc
// TODO: Ensure that a given proxy provided by two or more distinct providers
// has same anonimity evaluation on from each provider. (ie if proxy is reported
// as ELITE, it must be reported as ELITE by ALL providers providing this proxy)
public enum ProxyFactory {
	INSTANCE;

	private static final String PROXY_PROVIDERS_CONFIGURATION_FILE = "proxy_providers.hjson";
	private static final String BANNER_TEMPLATE = "-------------------------%n#%n# %s%n#%n# %s%n#";
	private static final Pattern PROXY_HOST_EXTRACTOR = Pattern.compile("(?<info>\\d+\\.\\d+\\.\\d+\\.\\d+)");
	private static final Pattern PROXY_PORT_EXTRACTOR = Pattern.compile(":?(?!\\.)(?<info>\\d+)$");
	private static final String JAVASCRIPT_ENGINE_NAME = "javascript";

	private static WebClient webClient;

	private transient Invocable decodingScriptEngine;

	private ProxyFactory() {
		WebClientConfiguration wcc = new WebClientConfiguration();
		wcc.makePauseBetweenFetches(false);
		wcc.ignoreHttpErrors(true);
		wcc.validateTLSCertificates(false);
		wcc.setTimeoutInMs(5 * 60 * 1000); // 5 mn

		setWebClient(new WebClient(wcc));
	}

	/**
	 * 
	 * Method used for test only.
	 * 
	 * @param newWebClient
	 */
	public static void setWebClient(WebClient newWebClient) {
		webClient = newWebClient;
	}

	public static int addNewProxiesInto(Set<Proxy> proxiesSet) {
		return INSTANCE.putProxiesInto0(proxiesSet);
	}

	private int putProxiesInto0(Set<Proxy> proxiesSet) {
		Set<Proxy> newDistinctProxies = new HashSet<>();

		for (ProxyProvider proxyProvider : loadProxyProviders()) {
			loadProxies(proxyProvider, newDistinctProxies);
		}

		proxiesSet.addAll(newDistinctProxies);

		return newDistinctProxies.size();
	}

	private void loadProxies(ProxyProvider proxyProvider, Set<Proxy> proxiesSet) {
		log.info(banner(proxyProvider));

		DecodingContext dc = createDecodingContext(proxyProvider);
		String proxyRowsCssQuery = proxyProvider.getProxyRowsCssQuery();
		String nextPageUrlCssQuery = proxyProvider.getNextPageUrlCssQuery();
		String spamWarningCssQuery = proxyProvider.getSpamWarningCssQuery();

		String nextUrl = proxyProvider.getStartUrl();
		do {
			Document proxiesPage = webClient.fetchAsDocument(nextUrl);
			if (spamWarningDetected(proxiesPage, spamWarningCssQuery)) {
				log.warn("Our activity has been detected (spam warning found).");
				log.warn("Skipping proxy provider [ {} ]", proxyProvider.getName());
				break;
			}

			if (dc.decodingIsNeeded()) {
				dc.setProxiesPage(proxiesPage);
				runDecodingScript(dc);
			}

			handleRows(proxiesPage.select(proxyRowsCssQuery), proxyProvider, proxiesSet);

			if (nextPageUrlCssQuery.isEmpty()) {
				nextUrl = null;
			} else {
				nextUrl = determineNextUrl(proxiesPage, nextPageUrlCssQuery);
			}
		} while (nextUrl != null);
	}

	private boolean spamWarningDetected(Document proxiesPage, String spamWarningCssQuery) {
		return !spamWarningCssQuery.isEmpty() && !proxiesPage.select(spamWarningCssQuery).isEmpty();
	}

	private DecodingContext createDecodingContext(ProxyProvider proxyProvider) {
		String decodingScript = proxyProvider.getDecodingScript();

		boolean proxiesPagesNeedDecoding = !decodingScript.isEmpty();
		if (proxiesPagesNeedDecoding) {
			if (decodingScriptEngine == null) {
				log.info("Loading script engine: {}", JAVASCRIPT_ENGINE_NAME);
				ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE_NAME);
				decodingScriptEngine = (Invocable) engine;
			}

			loadDecodingScript(decodingScript);
		}

		return new DecodingContext(proxiesPagesNeedDecoding);
	}

	private String determineNextUrl(Document proxiesPage, String nextPageUrlCssQuery) {
		Elements anchors = proxiesPage.select(nextPageUrlCssQuery);
		switch (anchors.size()) {
		case 0:
			return null;

		case 1:
			return anchors.first().absUrl("href");

		default:
			WebUtils.dumpDocument(proxiesPage, DumpExtension.HTML);
			throw new ApplicationRuntimeException("Unexpected number of anchors.");
		}
	}

	private void handleRows(Elements rows, ProxyProvider proxyProvider, Set<Proxy> proxiesSet) {
		String proxyProviderName = proxyProvider.getName();
		String proxyHostCellCssQuery = proxyProvider.getProxyHostCellCssQuery();
		String proxyPortCellCssQuery = proxyProvider.getProxyPortCellCssQuery();
		String proxyLatencyValueCssQuery = proxyProvider.getProxyLatencyValueCssQuery();
		Pattern proxyLatencyValuePattern = proxyProvider.getProxyLatencyValueRegexPattern();
		String proxyAnonimityTypeValueCssQuery = proxyProvider.getProxyAnonimityTypeValueCssQuery();
		Map<String, String> proxyAnonimityTypeMap = proxyProvider.getProxyAnonimityTypeMap();

		for (Element row : rows) {
			AnonimityType anonimityType = anonimityType(row, proxyAnonimityTypeValueCssQuery, proxyAnonimityTypeMap);
			if (anonimityType == ELITE) {
				Proxy p = DefaultProxy.from( //
						host(row, proxyHostCellCssQuery), //
						port(row, proxyPortCellCssQuery) //
						);

				if (!proxyLatencyValueCssQuery.isEmpty()) {
					p.setLatency(latency(row, proxyLatencyValueCssQuery, proxyLatencyValuePattern));
				}

				p.setAnonimityType(anonimityType);
				p.setProxyProviderName(proxyProviderName);

				if (proxiesSet.add(p)) {
					log.debug("Added: {}", p);
				} else {
					log.debug("Already added: {}", p);
				}
			}
		}
	}

	private void loadDecodingScript(String decodingScriptFilename) {
		try (Reader sr = getResourceAsReader(decodingScriptFilename)) {
			log.info("Compiling decoding script: {}", decodingScriptFilename);
			((ScriptEngine) decodingScriptEngine).eval(sr);
		} catch (IOException | ScriptException e) {
			throw new ApplicationRuntimeException(e);
		}
	}

	private void runDecodingScript(DecodingContext dc) {
		try {
			log.info("Decoding document: {}", dc.getProxiesPage().baseUri());
			decodingScriptEngine.invokeFunction("uncipherContext", dc);
		} catch (ScriptException | NoSuchMethodException e) {
			throw new ApplicationRuntimeException(e);
		}
	}

	private AnonimityType anonimityType(Element row, String proxyAnonimityTypeValueCssQuery, Map<String, String> proxyAnonimityTypeMap) {
		Element anonimytyTypeElement = row.selectFirst(proxyAnonimityTypeValueCssQuery);
		if (anonimytyTypeElement == null) {
			throw new ApplicationRuntimeException("Unable to detect anonimity type.\nCSS query: " + proxyAnonimityTypeValueCssQuery + "\n\n"
					+ row.outerHtml());
		}

		return AnonimityType.valueOf(proxyAnonimityTypeMap.get(anonimytyTypeElement.text()));
	}

	private long latency(Element row, String proxyLatencyValueCssQuery, Pattern proxyLatencyValuePattern) {
		Element latencyElement = row.selectFirst(proxyLatencyValueCssQuery);
		if (latencyElement == null) {
			throw new ApplicationRuntimeException("Unable to detect latency information.\nCSS query: " + proxyLatencyValueCssQuery + "\n\n"
					+ row.outerHtml());
		}

		Matcher m = proxyLatencyValuePattern.matcher(latencyElement.text());
		if (!m.find()) {
			throw new ApplicationRuntimeException("Unable to detect latency information.\nCSS query: " + proxyLatencyValueCssQuery + "\n\n"
					+ row.outerHtml());
		}

		return Duration.parse(format("PT%.9fS", seconds(m))).toMillis();
	}

	/**
	 * 
	 * This method assumes the Matcher contains data to be extracted.
	 * 
	 * @param m
	 * @return
	 */
	private double seconds(Matcher m) {
		String ms = extract(m, "ms");
		String s = extract(m, "s");

		return Double.parseDouble(s) + Double.parseDouble(ms) / 1000;
	}

	private String extract(Matcher m, String groupName) {
		String res;

		if (m.pattern().toString().contains("?<" + groupName + ">")) {
			res = m.group(groupName);
		} else {
			res = "0";
		}

		return res;
	}

	private String port(Element row, String proxyPortCellCssQuery) {
		return proxyInfo(row, proxyPortCellCssQuery, PROXY_PORT_EXTRACTOR, "proxy port");
	}

	private String host(Element row, String proxyHostCellCssQuery) {
		return proxyInfo(row, proxyHostCellCssQuery, PROXY_HOST_EXTRACTOR, "proxy host");
	}

	private String proxyInfo(Element row, String proxyCellCssQuery, Pattern pattern, String infoName) {
		Element infoCell = row.selectFirst(proxyCellCssQuery);
		if (infoCell == null) {
			throw newProxyInfoNotFoundException(infoName, proxyCellCssQuery, row);
		}

		Matcher m = pattern.matcher(infoCell.text());
		if (!m.find()) {
			throw newProxyInfoNotFoundException(infoName, proxyCellCssQuery, row);
		}

		String proxyInfo = m.group("info");
		if (proxyInfo == null) {
			throw newProxyInfoNotFoundException(infoName, proxyCellCssQuery, row);
		}

		return proxyInfo;
	}

	private ProxyInfoNotFoundException newProxyInfoNotFoundException(String infoName, String cssQuery, Element row) {
		return new ProxyInfoNotFoundException("Unable to detect " + infoName + ".\nCSS query: " + cssQuery + "\nHTML code:\n" + row.outerHtml());
	}

	private String banner(ProxyProvider pp) {
		return String.format(BANNER_TEMPLATE, pp.getName(), pp.getDescription());
	}

	private List<ProxyProvider> loadProxyProviders() {
		List<ProxyProvider> proxyProviders = new ArrayList<>();
		Path currentPath = Paths.get(".").toAbsolutePath().normalize();
		log.info("Looking for proxy providers configuration on: {}", currentPath);

		Reader r = null;
		try {
			// Locate source of configuration
			Path candidateConfigurationPath = currentPath.resolve(PROXY_PROVIDERS_CONFIGURATION_FILE);
			if (Files.exists(candidateConfigurationPath)) {
				r = Files.newBufferedReader(candidateConfigurationPath, UTF_8);
			} else {
				log.info("Configuration not found. Using built-in configuration.");
				r = getResourceAsReader(PROXY_PROVIDERS_CONFIGURATION_FILE);
			}

			// Load configuration
			JsonValue candidate = JsonValue.readHjson(r);
			if (candidate.isArray()) {
				JsonArray providersArray = candidate.asArray();

				for (JsonValue jsonValue : providersArray) {
					proxyProviders.add(ProxyProvider.createFrom(jsonValue));
				}
			} else {
				log.warn("Unreadable configuration.");
			}
		} catch (InvalidJsonValueException | IOException e) {
			log.warn("", e);
		} finally {
			IOUtils.closeQuietly(r);
		}

		return proxyProviders;
	}

	private Reader getResourceAsReader(String resource) {
		return new BufferedReader(new InputStreamReader(getResourceAsStream(resource), UTF_8));
	}

	private InputStream getResourceAsStream(String resource) {
		return ProxyFactory.class.getResourceAsStream("/" + resource);
	}

	public static class ProxyInfoNotFoundException extends ApplicationRuntimeException {

		private static final long serialVersionUID = 6383192575938286053L;

		public ProxyInfoNotFoundException(String msg) {
			super(msg);
		}
	}
}
