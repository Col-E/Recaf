package software.coley.recaf.ui.pane.search;

import software.coley.recaf.services.cell.context.BasicContextSource;

/**
 * Reference context source used by {@link AbstractSearchPane}.
 *
 * @author Matt Coley
 */
public class SearchContextSource extends BasicContextSource {
	/** Context singleton */
	public static final SearchContextSource SEARCH_INSTANCE = new SearchContextSource();

	private SearchContextSource() {
		super(false);
	}
}
