package my.web.proxy.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.NavigableSet;
import java.util.TreeSet;

import lombok.extern.slf4j.Slf4j;
import my.web.WebClient;
import my.web.proxy.Proxy;
import my.web.proxy.ProxyFactory;
import my.web.utils.WebUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

@Slf4j
public class ProxyFactoryTest {

	@Test
	public void testProxyProviders() {
		ProxyFactory.setWebClient(new WebClient() {

			@Override
			public Document fetchAsDocument(String url) {
				return loadDocumentFromFile(url);
			}
		});

		NavigableSet<Proxy> proxiesSet = new TreeSet<>();
		ProxyFactory.addNewProxiesInto(proxiesSet);

		assertThat(proxiesSet.size(), is(14));
		assertThat(
				proxiesSet.toString(),
				is("[free-proxy-list.net / HTTP @ 1.2.3.4:80(ELITE, 0.000), hidemy.name / HTTP @ 1.2.3.4:1080(ELITE, 880.000), xRoxy.com / HTTP @ 1.2.3.4:3128(ELITE, 4136.000), ip-adress.com / HTTP @ 1.2.3.4:8080(ELITE, 0.000), hidemy.name / HTTP @ 1.2.3.4:53281(ELITE, 1060.000), xRoxy.com / HTTP @ 2.2.3.4:3128(ELITE, 4136.000), ProxyServers.pro / HTTP @ 2.2.3.4:8080(ELITE, 50.000), xRoxy.com / HTTP @ 3.2.3.4:3128(ELITE, 4136.000), ProxyServers.pro / HTTP @ 3.2.3.4:8080(ELITE, 50.000), xRoxy.com / HTTP @ 4.2.3.4:3128(ELITE, 4136.000), ProxyServers.pro / HTTP @ 4.2.3.4:8080(ELITE, 50.000), best-proxy.com / HTTP @ 45.249.8.14:80(ELITE, 0.000), Proxz.com / HTTP @ 119.28.50.37:82(ELITE, 0.000), ProxyNova / HTTP @ 150.161.21.203:8080(ELITE, 2934.000)]"));
	}

	@Test
	public void testProxyserversProAntispamWarningDetection() {
		ProxyFactory.setWebClient(new WebClient() {

			@Override
			public Document fetchAsDocument(String url) {
				Document doc = WebUtils.createEmptyDocument();

				if (url.contains("proxyservers.pro")) {
					doc = loadDocumentFromFile(url.replace("index.htm", "spam_detected.htm"));
				}

				return doc;
			}
		});

		NavigableSet<Proxy> proxiesSet = new TreeSet<>();
		ProxyFactory.addNewProxiesInto(proxiesSet);

		assertThat(proxiesSet.size(), is(0));
	}

	private Document loadDocumentFromFile(String filename) {
		try {
			log.info("Loading document from {}", filename);

			File f = new File(URI.create(filename));

			return Jsoup.parse(f, "UTF-8", f.getParentFile().toURI().toASCIIString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
