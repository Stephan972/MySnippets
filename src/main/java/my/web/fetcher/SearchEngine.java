package my.web.fetcher;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import lombok.Getter;
import lombok.Setter;
import my.exceptions.ApplicationRuntimeException;
import my.web.WebClient;
import my.web.WebClientConfiguration;

import org.jsoup.nodes.Document;

public class SearchEngine {

	@XmlAttribute
	@Getter
	@Setter
	private String name;

	@XmlElement(name = "next-page")
	@Getter
	@Setter
	private String nextPage;

	@XmlElement(name = "search-page")
	@Getter
	@Setter
	private List<SearchPage> searchPages;

	@XmlElement(name = "no-result")
	@Getter
	@Setter
	private String noResult;

	private int currentSearchPageCursorPosition;

	private WebClient webClient;

	@Getter
	private SearchPage currentSearchPage;

	public SearchEngine() {
		this(getDefaultWebClient());
	}

	public SearchEngine(WebClient webClient) {
		resetSearchPageCursor();
		this.webClient = webClient;
	}

	private static WebClient getDefaultWebClient() {
		WebClientConfiguration configuration = new WebClientConfiguration();

		return new WebClient(configuration);
	}

	/**
	 * 
	 * Fetch results on the current search page and select next search page for
	 * the next call of this method.
	 * 
	 * @param query
	 * @return a SearchResultsPage
	 * 
	 * @throws RuntimeException
	 *             If the query cannot be encoded in UTF-8.
	 */
	public SearchResultsBook findOnNextSearchPage(String query) {
		int len = searchPages.size();
		if ((len == 0) || (currentSearchPageCursorPosition >= len)) {
			return EmptySearchResultsBook.INSTANCE;
		}

		currentSearchPage = searchPages.get(currentSearchPageCursorPosition);
		// TODO: Avoid internal Pattern compilation of String#replace...
		String url;
		try {
			url = currentSearchPage.getUrl().replace("${query}", URLEncoder.encode(query, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new ApplicationRuntimeException(e);
		}
		currentSearchPageCursorPosition++;

		return new DefaultSearchResultsBook(this, currentSearchPage, fetch(url));
	}

	/* package */Document fetch(String url) {
		return webClient.fetchAsDocument(url);
	}

	public boolean hasMoreSearchPage() {
		return currentSearchPageCursorPosition < searchPages.size();
	}

	public void resetSearchPageCursor() {
		currentSearchPageCursorPosition = 0;
	}
}
