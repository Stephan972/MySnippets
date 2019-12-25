package my.web.fetcher;

import org.jsoup.select.Elements;

/**
 * 
 * A collection of search results organized into pages.
 * 
 * One page contains only a part of the whole search results.
 * 
 * @author stephan
 *
 */
public interface SearchResultsBook {
	
	boolean hasResultsOnCurrentPage();

	Elements getResultsOnCurrentPage();

	boolean hasNextPage();

	/**
	 * 
	 * Fetch the search results following the current collection search results.
	 * 
	 * @return true => next fetch success
	 * false => next fetch failed
	 * 
	 * @throws Exception
	 *             If the fetch failed, no next page can be found in this search
	 *             results page or the next-page css path doesn't point to an
	 *             anchor.
	 */
	boolean fetchNextPage();
}