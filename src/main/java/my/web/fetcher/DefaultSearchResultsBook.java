package my.web.fetcher;

import javax.xml.bind.annotation.XmlTransient;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.exceptions.ApplicationRuntimeException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@RequiredArgsConstructor
@Slf4j
public class DefaultSearchResultsBook implements SearchResultsBook {

	@XmlTransient
	@NonNull
	private SearchEngine parentSearchEngine;

	@XmlTransient
	@NonNull
	private SearchPage parentSearchPage;

	@XmlTransient
	@NonNull
	private Document html;

	/*
	 * (non-Javadoc)
	 * 
	 * @see my.SearchResultsPage#getResults()
	 */
	@Override
	public Elements getResultsOnCurrentPage() {
		return html.select(parentSearchPage.getResultCssPath());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see my.SearchResultsPage#hasNext()
	 */
	@Override
	public boolean hasNextPage() {
		return !html.select(getNextPage()).isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see my.SearchResultsPage#next()
	 */
	@Override
	public boolean fetchNextPage() {
		// * Find next page link
		String nextPage = getNextPage();
		Elements elements = html.select(nextPage);
		if (elements.isEmpty()) {
			log.warn("Unable to find an url matching: {}", nextPage);
			return false;
		}

		// * Checking next page url
		Element element = elements.get(0);
		String tagName = element.tagName().toLowerCase();
		if (!tagName.equals("a")) {
			log.warn("Invalid next-page css path. It must give access to an anchor (a). However it gives access to ( {} ) [ {} ]", tagName, nextPage);
			return false;
		}

		// * Fetching next page url
		html = parentSearchEngine.fetch(element.absUrl("href"));
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see my.SearchResultsPage#hasResults()
	 */
	@Override
	public boolean hasResultsOnCurrentPage() {
		return html.select(parentSearchEngine.getNoResult()).isEmpty();
	}

	private String getNextPage() {
		String nextPage = parentSearchPage.getNextPage();

		if (nextPage == null) {
			nextPage = parentSearchEngine.getNextPage();
		}

		if (nextPage == null) {
			throw new ApplicationRuntimeException("No next page defined for current search results page (" + parentSearchPage.getUrl() + ")");
		}

		return nextPage;
	}
}
