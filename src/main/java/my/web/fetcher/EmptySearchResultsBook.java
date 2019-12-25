package my.web.fetcher;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import my.exceptions.ApplicationRuntimeException;

import org.jsoup.select.Elements;

@NoArgsConstructor(access=AccessLevel.PRIVATE)
public enum EmptySearchResultsBook implements SearchResultsBook {
	INSTANCE;

	private static final Elements ZERO_ELEMENTS = new Elements(0);

	@Override
	public final Elements getResultsOnCurrentPage() {
		return ZERO_ELEMENTS;
	}

	@Override
	public final boolean hasNextPage() {
		return false;
	}

	@Override
	public final boolean fetchNextPage() {
		throw new ApplicationRuntimeException("No more search results available.");
	}

	@Override
	public final boolean hasResultsOnCurrentPage() {
		return false;
	}
}
