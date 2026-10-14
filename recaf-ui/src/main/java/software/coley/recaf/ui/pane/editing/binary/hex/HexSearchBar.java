package software.coley.recaf.ui.pane.editing.binary.hex;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.control.AbstractSearchBar;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.ThreadUtil;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Search bar for the hex editor.
 *
 * @author Matt Coley
 */
public final class HexSearchBar extends AbstractSearchBar {
	private static final int[] EMPTY = new int[0];

	private final HexEditor editor;
	private final ComboBox<QueryMode> queryMode = new ComboBox<>();
	private final Button previous = new ActionButton(CarbonIcons.ARROW_UP, this::previousMatch);
	private final Button next = new ActionButton(CarbonIcons.ARROW_DOWN, this::nextMatch);
	private final Button close = new ActionButton(CarbonIcons.CLOSE, this::hide);
	private final SimpleStringProperty countText = new SimpleStringProperty();
	private String noResultsMessage;
	private final PauseTransition debounce = new PauseTransition(Duration.millis(150));
	private int[] matches = new int[0];
	private long requestId;
	private Future<?> searchFuture;
	private int matchLength;
	private int currentMatchIndex = -1;

	public HexSearchBar(@Nonnull HexEditor editor) {
		this.editor = editor;
		queryMode.getItems().setAll(QueryMode.HEX, QueryMode.ASCII);
		queryMode.getStyleClass().addAll("borderless", "transparent-cell");
		queryMode.setValue(QueryMode.HEX);
		queryMode.setConverter(new StringConverter<>() {
			@Override
			public String toString(QueryMode mode) {
				return mode == null ? "" : Lang.get(mode == QueryMode.HEX ? "find.hex.search.mode.hex" : "find.hex.search.mode.ascii");
			}

			@Override
			public QueryMode fromString(String value) {
				return null;
			}
		});
		queryMode.setMaxWidth(105);
		queryMode.valueProperty().addListener((obs, old, value) -> refreshResults());
		searchInput.textProperty().addListener((obs, old, value) -> refreshResults());
		previous.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.SMALL);
		next.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.SMALL);
		close.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.SMALL);
		previous.disableProperty().bind(hasResults.not());
		next.disableProperty().bind(hasResults.not());
		countText.set(Lang.get("menu.search.noresults"));
		setup();
		setVisible(false);
		setManaged(false);
	}

	/**
	 * Open the bar, keeping the current query and focusing the input.
	 */
	public void show() {
		setVisible(true);
		setManaged(true);
		editor.setSearchBarNode(this);
		refreshResults();
		requestSearchFocus();
	}

	/**
	 * Hide the bar and return focus to the byte editor.
	 */
	public void hide() {
		setVisible(false);
		setManaged(false);
		requestId++;
		debounce.stop();
		cancelFuture();
		noResultsMessage = null;
		clearResults();
		editor.setSearchBarNode(null);
		editor.requestByteFocus();
	}

	/**
	 * Stop pending searches when the owning editor is disposed.
	 */
	public void dispose() {
		requestId++;
		debounce.stop();
		cancelFuture();
	}

	/**
	 * Checks whether a byte offset belongs to any current search result.
	 *
	 * @return {@code true} when the offset is inside any match.
	 */
	public boolean isMatch(int offset) {
		return findContaining(offset) >= 0;
	}

	/**
	 * Checks whether a byte offset belongs to the active search result.
	 *
	 * @return {@code true} when the offset is inside the active match.
	 */
	public boolean isActiveMatch(int offset) {
		if (currentMatchIndex < 0 || currentMatchIndex >= matches.length)
			return false;
		int start = matches[currentMatchIndex];
		return offset >= start && offset < start + matchLength;
	}

	@Override
	public void setup() {
		super.setup();
		searchInput.setOnKeyReleased(null);
	}

	@Nonnull
	@Override
	protected Node createSearchInputOptions() {
		HBox options = new HBox(queryMode);
		options.setAlignment(Pos.CENTER_RIGHT);
		return options;
	}

	@Override
	protected void setupLayout() {
		HBox navigation = new HBox(previous, next, close);
		navigation.setAlignment(Pos.CENTER);
		HBox searchLine = new HBox(searchInput, resultCount, navigation);
		searchLine.setAlignment(Pos.CENTER_LEFT);
		searchLine.setSpacing(8);
		searchLine.setPadding(new Insets(0, 5, 0, 0));
		HBox.setHgrow(searchInput, Priority.ALWAYS);
		getChildren().add(searchLine);
	}

	@Override
	protected void bindResultCountDisplay(@Nonnull StringProperty resultTextProperty) {
		resultTextProperty.bind(countText);
	}

	@Override
	protected void onSearchInputKeyPress(@Nonnull KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			if (event.isShiftDown())
				previousMatch();
			else
				nextMatch();
			event.consume();
		} else if (event.getCode() == KeyCode.ESCAPE) {
			hide();
			event.consume();
		}
		super.onSearchInputKeyPress(event);
	}

	@Override
	protected void refreshResults() {
		if (!isVisible())
			return;

		// Empty search input means no results, but also no error message.
		String query = searchInput.getText();
		if (query == null || query.isEmpty()) {
			noResultsMessage = null;
			invalidateSearch();
			return;
		}

		// Parse pattern from input, inform user of invalid input if necessary.
		HexSearch.Pattern pattern;
		try {
			pattern = queryMode.getValue() == QueryMode.HEX
					? HexSearch.parseHexPattern(query)
					: HexSearch.asciiPattern(query);
		} catch (IllegalArgumentException ex) {
			noResultsMessage = Lang.get("find.hex.search.invalid-pattern");
			invalidateSearch();
			return;
		}
		if (pattern.length() == 0) {
			noResultsMessage = null;
			invalidateSearch();
			return;
		}

		// Start a new search, canceling any previous search.
		noResultsMessage = null;
		matchLength = pattern.length();
		invalidateSearch();
		long id = requestId;
		debounce.setOnFinished(event -> launchSearch(id, query, pattern));
		debounce.playFromStart();
	}

	private void launchSearch(long id, @Nonnull String query, @Nonnull HexSearch.Pattern pattern) {
		// Skip search if the request is no longer valid.
		if (id != requestId || !isVisible())
			return;

		// Skip search if the content is empty.
		byte[] data = editor.getOperations().currentAccess().getData();
		if (data == null || data.length == 0)
			return;

		// Start search in background thread, and apply results on the FX thread.
		long revision = editor.getRevision();
		CompletableFuture<Void> submitted = ThreadUtil.supply(() -> {
			try {
				return HexSearch.findAll(data.clone(), pattern);
			} catch (CancellationException ignored) {
				// A newer query or content revision owns the result.
				return null;
			} catch (RuntimeException error) {
				// Search failed, but we can still apply the error to the UI.
				return new int[0];
			}
		}, EMPTY, editor.getExecutor()).thenAcceptAsync(found -> {
			if (found != null) applySearchResult(id, revision, query, pattern, found);
		}, FxThreadUtil.executor());
		if (id == requestId)
			searchFuture = submitted;
		else
			submitted.cancel(true);
	}

	private void applySearchResult(long id, long revision, @Nonnull String query, @Nonnull HexSearch.Pattern pattern,
	                               int[] found) {
		// Skip old searches, or results on outdated content.
		if (id != requestId
				|| !isVisible()
				|| revision != editor.getRevision()
				|| !query.equals(searchInput.getText()))
			return;

		noResultsMessage = null;
		searchFuture = null;
		matchLength = pattern.length();
		matches = found;
		currentMatchIndex = matches.length == 0 ? -1 : findAtOrAfter(editor.getOperations().navigation().selectionOffset());
		if (searchInput.isFocused() && currentMatchIndex >= 0)
			selectMatch(currentMatchIndex);
		updateResultState();
	}

	private void invalidateSearch() {
		requestId++;
		debounce.stop();
		cancelFuture();
		clearResults();
	}

	private void cancelFuture() {
		Future<?> previous = searchFuture;
		searchFuture = null;
		if (previous != null)
			previous.cancel(true);
	}

	private void nextMatch() {
		addToHistory();
		if (matches.length == 0)
			return;
		currentMatchIndex = currentMatchIndex < 0 ?
				findAtOrAfter(editor.getOperations().navigation().selectionOffset()) :
				(currentMatchIndex + 1) % matches.length;
		selectMatch(currentMatchIndex);
	}

	private void previousMatch() {
		addToHistory();
		if (matches.length == 0)
			return;
		if (currentMatchIndex < 0) {
			int candidate = findAtOrAfter(editor.getOperations().navigation().selectionOffset());
			currentMatchIndex = candidate <= 0 ? matches.length - 1 : candidate - 1;
		} else {
			currentMatchIndex = currentMatchIndex == 0 ? matches.length - 1 : currentMatchIndex - 1;
		}
		selectMatch(currentMatchIndex);
	}

	private void selectMatch(int index) {
		if (index < 0 || index >= matches.length)
			return;
		editor.getOperations().navigation().select(matches[index]);
		editor.refreshHexDisplay();
		updateResultState();
	}

	private int findAtOrAfter(int offset) {
		int index = Arrays.binarySearch(matches, offset);
		if (index < 0)
			index = -index - 1;
		return index >= matches.length ? 0 : index;
	}

	private int findContaining(int offset) {
		int index = Arrays.binarySearch(matches, offset);
		if (index < 0)
			index = -index - 2;
		return index >= 0 && offset < matches[index] + matchLength ? index : -1;
	}

	private void updateResultState() {
		boolean found = matches.length > 0;
		hasResults.set(found);
		if (!found) {
			countText.set(noResultsMessage != null ? noResultsMessage : Lang.get("menu.search.noresults"));
		} else if (currentMatchIndex >= 0) {
			countText.set(Lang.get("find.hex.search.current-count").formatted(currentMatchIndex + 1, matches.length));
		} else {
			countText.set(Lang.get("find.hex.search.result-count").formatted(matches.length));
		}
		editor.refreshHexDisplay();
	}

	private void clearResults() {
		matches = new int[0];
		matchLength = 0;
		currentMatchIndex = -1;
		updateResultState();
	}

	private void addToHistory() {
		String query = searchInput.getText();
		if (query != null && !query.isBlank() && !pastSearches.contains(query))
			pastSearches.addFirst(query);
		while (pastSearches.size() > MAX_HISTORY)
			pastSearches.removeLast();
	}

	private enum QueryMode {
		HEX,
		ASCII
	}
}
