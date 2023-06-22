package software.coley.recaf.ui.control.richtext.search;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import regexodus.Matcher;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.BoundToggleIcon;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;
import software.coley.recaf.util.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Search bar component, with layout and functionality inspired from IntelliJ's.
 *
 * @author Matt Coley
 */
@Dependent
public class SearchBar implements EditorComponent, EventHandler<KeyEvent> {
	private final KeybindingConfig keys;
	private Bar bar;

	@Inject
	public SearchBar(@Nonnull KeybindingConfig keys) {
		this.keys = keys;
	}

	@Override
	public void install(@Nonnull Editor editor) {
		bar = new Bar(editor);
		NodeEvents.addKeyPressHandler(editor, this);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		editor.setTop(null);
		NodeEvents.removeKeyPressHandler(editor, this);
		bar = null;
	}

	@Override
	public void handle(KeyEvent event) {
		if (keys.getFind().match(event)) {
			// Show if not visible
			if (!bar.isVisible())
				bar.show();

			// Grab focus
			bar.requestSearchFocus();
			bar.hideReplace();
		} else if (keys.getReplace().match(event)) {
			// Show if not visible
			if (!bar.isVisible())
				bar.show();

			// Grab focus
			bar.requestSearchFocus();
			bar.showReplace();
		}
	}

	/**
	 * The actual search bar.
	 */
	private static class Bar extends VBox {
		private static final int MAX_HISTORY = 10;
		private final SimpleIntegerProperty lastResultIndex = new SimpleIntegerProperty(-1);
		private final SimpleBooleanProperty hasResults = new SimpleBooleanProperty();
		private final SimpleBooleanProperty caseSensitivity = new SimpleBooleanProperty();
		private final SimpleBooleanProperty regex = new SimpleBooleanProperty();
		private final CustomTextField searchInput = new CustomTextField();
		private final CustomTextField replaceInput = new CustomTextField();
		private final ObservableList<String> pastSearches = FXCollections.observableArrayList();
		private final ObservableList<String> pastReplaces = FXCollections.observableArrayList();
		private final ObservableList<Match> resultRanges = FXCollections.observableArrayList();
		private final HBox replaceLine;
		private final Editor editor;

		private Bar(Editor editor) {
			this.editor = editor;
			getStyleClass().add("search-bar");

			// Initially hidden.
			hide();

			// Refresh results when text changes.
			editor.getTextChangeEventStream()
					.successionEnds(Duration.ofMillis(150))
					.addObserver(changes -> refreshResults());

			// Remove border from search/replace text fields.
			searchInput.getStyleClass().addAll(Styles.ACCENT);
			replaceInput.getStyleClass().addAll(Styles.ACCENT);

			// Create menu for search input left graphic (like IntelliJ) to display prior searches/replaces when clicked.
			Button oldSearches = new Button();
			oldSearches.setFocusTraversable(false);
			oldSearches.setDisable(true); // re-enabled when searches are populated.
			oldSearches.setGraphic(new FontIconView(CarbonIcons.SEARCH));
			oldSearches.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT, Styles.SMALL);
			searchInput.setLeft(oldSearches);
			Button oldReplaces = new Button();
			oldReplaces.setFocusTraversable(false);
			oldReplaces.setDisable(true); // re-enabled when replaces are populated.
			oldReplaces.setGraphic(new FontIconView(CarbonIcons.SEARCH));
			oldReplaces.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT, Styles.SMALL);
			replaceInput.setLeft(oldReplaces);

			// Create toggles for search input query modes.
			BoundToggleIcon toggleSensitivity = new BoundToggleIcon(Icons.CASE_SENSITIVITY, caseSensitivity).withTooltip("misc.casesensitive");
			BoundToggleIcon toggleRegex = new BoundToggleIcon(Icons.REGEX, regex).withTooltip("misc.regex");
			toggleSensitivity.setFocusTraversable(false);
			toggleRegex.setFocusTraversable(false);
			toggleSensitivity.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT, Styles.SMALL);
			toggleRegex.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT, Styles.SMALL);
			HBox inputToggles = new HBox(
					toggleSensitivity,
					toggleRegex
			);
			inputToggles.setAlignment(Pos.CENTER_RIGHT);
			searchInput.setRight(inputToggles);
			caseSensitivity.addListener((ob, old, cur) -> refreshResults());
			regex.addListener((ob, old, cur) -> refreshResults());

			// Create label to display number of results.
			Label resultCount = new Label();
			resultCount.setMinWidth(70);
			resultCount.setAlignment(Pos.CENTER);
			resultCount.setTextAlignment(TextAlignment.CENTER);
			resultCount.textProperty().bind(lastResultIndex.map(n -> {
				int i = n.intValue();
				if (i < 0) {
					return Lang.get("menu.search.noresults");
				} else {
					return (i + 1) + "/" + resultRanges.size();
				}
			}));
			resultRanges.addListener((ListChangeListener<Match>) c -> {
				if (resultRanges.isEmpty()) {
					resultCount.setStyle("-fx-text-fill: red;");
				} else {
					resultCount.setStyle("-fx-text-fill: -color-fg-default;");
				}
			});

			// Create buttons to iterate through results.
			Button prev = new ActionButton(CarbonIcons.ARROW_UP, this::prev);
			Button next = new ActionButton(CarbonIcons.ARROW_DOWN, this::next);
			prev.setFocusTraversable(false);
			next.setFocusTraversable(false);
			prev.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.SMALL);
			next.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.SMALL);
			prev.disableProperty().bind(hasResults.not());
			next.disableProperty().bind(hasResults.not());

			// Button to close the search bar.
			Button close = new ActionButton(CarbonIcons.CLOSE, this::hide);
			close.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.SMALL);
			close.setFocusTraversable(false);

			// Replace buttons.
			Button replace = new ActionButton(Lang.getBinding("find.replace"), this::replace);
			Button replaceAll = new ActionButton(Lang.getBinding("find.replaceall"), this::replaceAll);
			replace.getStyleClass().addAll(Styles.SMALL);
			replaceAll.getStyleClass().addAll(Styles.SMALL);
			replace.setFocusTraversable(false);
			replaceAll.setFocusTraversable(false);
			replace.disableProperty().bind(hasResults.not());
			replaceAll.disableProperty().bind(hasResults.not());

			// Add to past searches/replaces when enter is pressed.
			// Close when escape is pressed.
			searchInput.setOnKeyPressed(e -> {
				KeyCode code = e.getCode();
				if (code == KeyCode.ENTER) {
					next();
				} else if (code == KeyCode.ESCAPE) {
					hide();
				}
				while (pastSearches.size() > MAX_HISTORY)
					pastSearches.remove(pastSearches.size() - 1);
			});
			searchInput.setOnKeyReleased(e -> refreshResults());
			replaceInput.setOnKeyPressed(e -> {
				KeyCode code = e.getCode();
				if (code == KeyCode.ENTER) {
					replace();
				} else if (code == KeyCode.ESCAPE) {
					hide();
				}
				while (pastReplaces.size() > MAX_HISTORY)
					pastReplaces.remove(pastReplaces.size() - 1);
			});
			replaceInput.setOnKeyReleased(e -> {
				// We do this in on-release so the replacement input text value is up-to-date in this event processing.
				// Show a preview of the replacement for regex matches when it has regex group replacement.
				// For non-regex or simple replacements, we don't need to show anything.
				if (isVisible() && regex.get()) {
					int resultIndex = lastResultIndex.get();
					Popover popoverPreview;
					if (resultIndex == -1) {
						popoverPreview = null;
					} else {
						String replacement = getReplacement(resultIndex);
						if (!replacement.equals(replaceInput.getText())) {
							popoverPreview = new Popover(new Label(replacement));
							popoverPreview.setHeaderAlwaysVisible(true);
							popoverPreview.titleProperty().bind(Lang.getBinding("find.regexreplace"));
							popoverPreview.show(replaceInput);
						} else {
							popoverPreview = null;
						}
					}

					// Hide the prior popover if any exists.
					Object old = searchInput.getProperties().put("regex-preview", popoverPreview);
					if (old instanceof Popover oldPopover)
						oldPopover.hide();
				}
			});

			// When past searches list is modified, update old search menu.
			pastSearches.addListener((ListChangeListener<String>) c -> {
				List<ActionMenuItem> items = pastSearches.stream()
						.map(text -> new ActionMenuItem(text, () -> {
							searchInput.setText(text);
							requestSearchFocus();
						}))
						.toList();
				if (items.isEmpty()) {
					oldSearches.setDisable(true);
				} else {
					oldSearches.setDisable(false);
					ContextMenu contextMenu = new ContextMenu();
					contextMenu.getItems().setAll(items);
					oldSearches.setOnMousePressed(e -> contextMenu.show(oldSearches, e.getScreenX(), e.getScreenY()));
				}
			});
			pastReplaces.addListener((ListChangeListener<String>) c -> {
				List<ActionMenuItem> items = pastReplaces.stream()
						.map(text -> new ActionMenuItem(text, () -> {
							replaceInput.setText(text);
							requestSearchFocus();
						}))
						.toList();
				if (items.isEmpty()) {
					oldReplaces.setDisable(true);
				} else {
					oldReplaces.setDisable(false);
					ContextMenu contextMenu = new ContextMenu();
					contextMenu.getItems().setAll(items);
					oldReplaces.setOnMousePressed(e -> contextMenu.show(oldReplaces, e.getScreenX(), e.getScreenY()));
				}
			});

			// Layout
			replaceInput.prefWidthProperty().bind(searchInput.widthProperty());
			HBox prevAndNext = new HBox(prev, next);
			prevAndNext.setAlignment(Pos.CENTER);
			prevAndNext.setFillHeight(false);
			HBox searchLine = new HBox(searchInput, resultCount, prevAndNext, new Spacer(), close);
			searchLine.setAlignment(Pos.CENTER_LEFT);
			searchLine.setSpacing(10);
			searchLine.setPadding(new Insets(0, 5, 0, 0));
			replaceLine = new HBox(replaceInput, new Spacer(0), replace, replaceAll);
			replaceLine.setAlignment(Pos.CENTER_LEFT);
			replaceLine.setSpacing(10);
			replaceLine.setPadding(new Insets(0, 5, 0, 0));
			replaceLine.setVisible(false); // invis + group = 0 height (group wrapping is required for this to work)
			getChildren().addAll(searchLine, new Group(replaceLine));
		}

		/**
		 * Refresh the results by scanning for what text ranges match
		 */
		private void refreshResults() {
			// Reset index before any results are found.
			lastResultIndex.set(-1);

			// Skip when there is nothing
			String search = searchInput.getText();
			if (search == null || search.isEmpty()) {
				resultRanges.clear();
				hasResults.set(false);
				return;
			}

			// Check for regex, then do a standard search if not regex.
			String text = editor.getText();
			List<Match> tempRanges = new ArrayList<>();
			if (regex.get()) {
				// Validate the regex.
				RegexUtil.RegexValidation validation = RegexUtil.validate(search);
				Popover popoverValidation = null;
				if (validation.valid()) {
					// It's valid, populate the ranges.
					Matcher matcher = RegexUtil.getMatcher(search, text);
					while (matcher.find())
						tempRanges.add(Match.match(matcher.start(), matcher.end(), matcher));
				} else {
					// It's not valid. Tell the user what went wrong.
					popoverValidation = new Popover(new Label(validation.message()));
					popoverValidation.setHeaderAlwaysVisible(true);
					popoverValidation.titleProperty().bind(Lang.getBinding("find.regexinvalid"));
					popoverValidation.show(searchInput);
				}

				// Hide the prior popover if any exists.
				Object old = searchInput.getProperties().put("regex-popover", popoverValidation);
				if (old instanceof Popover oldPopover)
					oldPopover.hide();
			} else {
				// Modify the text/search for case-insensitive searches.
				if (!caseSensitivity.get()) {
					text = text.toLowerCase();
					search = search.toLowerCase();
				}

				// Loop over text, finding each index of matches.
				int size = search.length();
				int i = 0;
				while (i < text.length()) {
					int start = text.indexOf(search, i);
					if (start > -1) {
						int end = start + size;
						tempRanges.add(Match.match(start, end));
						i = end;
					} else {
						break;
					}
				}
			}

			// Update properties.
			hasResults.set(!tempRanges.isEmpty());
			resultRanges.setAll(tempRanges);

			// Update selection to show the match closest to the user's current caret position.
			if (!tempRanges.isEmpty()) {
				CodeArea area = editor.getCodeArea();
				int lastMatchedTerm = area.getSelectedText().length();
				int caret = area.getCaretPosition();
				int searchStart = Math.max(0, caret - lastMatchedTerm - 1);
				int rangeIndex = CollectionUtil.sortedInsertIndex(resultRanges, Match.match(searchStart, searchStart));
				if (rangeIndex >= resultRanges.size())
					rangeIndex = 0;
				lastResultIndex.set(rangeIndex);

				// Only update selection when the search inputs are focused.
				// We can re-compute due to the editor's text changing. So if a user is typing in there, this would be annoying.
				if (inputsAreFocused()) {
					IntRange targetRange = resultRanges.get(rangeIndex).range();
					area.selectRange(targetRange.start(), targetRange.end());
					area.showParagraphAtCenter(area.getCurrentParagraph());
				}
			}
		}

		/**
		 * Select the next match.
		 */
		private void next() {
			recordSearch();

			// No ranges for current search query, so do nothing.
			lastResultIndex.set(-1);
			if (resultRanges.isEmpty())
				return;

			// Get the next range index by doing a search starting from the current caret position + 1.
			CodeArea area = editor.getCodeArea();
			int caret = area.getCaretPosition() + 1;
			int rangeIndex = CollectionUtil.sortedInsertIndex(resultRanges, Match.match(caret, caret));
			if (rangeIndex >= resultRanges.size())
				rangeIndex = 0;

			// Set index & select the range.
			lastResultIndex.set(rangeIndex);
			IntRange range = resultRanges.get(rangeIndex).range();
			area.selectRange(range.start(), range.end());
			area.showParagraphAtCenter(area.getCurrentParagraph());
		}

		/**
		 * Select the previous match.
		 */
		private void prev() {
			recordSearch();

			// No ranges for current search query, so do nothing.
			lastResultIndex.set(-1);
			if (resultRanges.isEmpty())
				return;

			// Get the previous range index by doing a search starting from the current selection position in the text,
			// then go back by one position, wrapping around if necessary.
			CodeArea area = editor.getCodeArea();
			int caret = area.getCaretPosition();
			int rangeIndex = CollectionUtil.sortedInsertIndex(resultRanges, Match.match(caret - area.getSelectedText().length(), caret)) - 1;
			if (rangeIndex < 0)
				rangeIndex = resultRanges.size() - 1;

			// Set index & select the range.
			lastResultIndex.set(rangeIndex);
			IntRange range = resultRanges.get(rangeIndex).range();
			area.selectRange(range.start(), range.end());
			area.showParagraphAtCenter(area.getCurrentParagraph());
		}

		/**
		 * Replaces the current selected range.
		 */
		private void replace() {
			recordReplace();

			// Replace the current index.
			int index = lastResultIndex.get();
			if (index >= 0)
				replaceResult(index);

			// Highlight next result.
			refreshResults();
		}

		/**
		 * Replaces all ranges.
		 */
		private void replaceAll() {
			recordReplace();

			// Iterate backwards, replacing all matches.
			// We record the start/end ranges so that we can do a re-style after for the affected range.
			List<Match> rangesCopy = new ArrayList<>(resultRanges);
			int max = rangesCopy.size() - 1;
			for (int i = max; i >= 0; i--)
				replaceResult(i);

			// Restyle range from first match through last match.
			int start = rangesCopy.get(0).range().start();
			int end = rangesCopy.get(max).range().end();
			editor.restyleAtPosition(start, end - start);
		}

		/**
		 * Replaces the range in the {@link #resultRanges} at the given {@code index} with the text of {@link #getReplacement(int)}.
		 *
		 * @param index
		 * 		Index in {@link #resultRanges} to replace.
		 */
		private void replaceResult(int index) {
			String replacement = getReplacement(index);
			CodeArea area = editor.getCodeArea();
			Match match = resultRanges.get(index);
			IntRange range = match.range();
			area.replaceText(range.start(), range.end(), replacement);
		}

		/**
		 * if {@link #regex} is {@code true}, then transformations are made on the replacement text from
		 * {@link #replaceInput} to support regex groups.
		 * <br>
		 * For example:
		 * <ul>
		 *     <li>Search: {@code public (class)}</li>
		 *     <li>Replace: {@code private $1}</li>
		 *     <li>Yields: {@code private class}</li>
		 * </ul>
		 *
		 * @param index
		 * 		Index in {@link #resultRanges} to replace.
		 *
		 * @return Replacement for the given range.
		 */
		private String getReplacement(int index) {
			String replacement = replaceInput.getText();
			Match match = resultRanges.get(index);
			String[] groups = match.groups();
			if (groups != null) {
				// Record '$N' patterns in the string
				//  - 0: whole matched text
				//  - N: group N's text
				Matcher matcher = RegexUtil.getMatcher("(?<!\\\\)(?:(\\\\\\\\)*)\\$\\d+", replacement);
				while (matcher.find()) {
					int groupId = Integer.parseInt(matcher.group(0).replaceAll("\\D+", ""));
					if (groupId < groups.length) {
						String groupText = groups[groupId];
						replacement = replacement.replace("$" + groupId, groupText);
					}
				}
			}
			return replacement;
		}

		/**
		 * Records the current {@link #searchInput} text to {@link #pastSearches}.
		 */
		private void recordSearch() {
			// Update search input history.
			String searchText = searchInput.getText();
			pastSearches.remove(searchText);
			pastSearches.add(0, searchText);
		}

		/**
		 * Records the current {@link #replaceInput} text to {@link #pastReplaces}.
		 */
		private void recordReplace() {
			// Also record the search term.
			recordSearch();

			// Update replace input history.
			String replacement = replaceInput.getText();
			pastReplaces.remove(replacement);
			pastReplaces.add(0, replacement);
		}

		/**
		 * @return {@code true} when either the search or replace inputs are focused.
		 */
		private boolean inputsAreFocused() {
			return searchInput.isFocused() || replaceInput.isFocused();
		}

		/**
		 * Show the search bar.
		 */
		public void show() {
			setVisible(true);
			setDisable(false);
			editor.setTop(this);

			// If the editor has selected text, we will copy it to the search input field.
			CodeArea area = editor.getCodeArea();
			String selectedText = area.getSelectedText();
			if (!selectedText.isBlank())
				searchInput.setText(selectedText);
		}

		/**
		 * Hide the search bar.
		 */
		private void hide() {
			setVisible(false);
			setDisable(true);
			editor.setTop(null);

			// Need to send focus back to the editor's code-area.
			// Doesn't work without the delay when handled from 'ESCAPE' key-event.
			FxThreadUtil.delayedRun(1, () -> editor.getCodeArea().requestFocus());
		}

		/**
		 * Shows the replace-bar segment.
		 */
		public void showReplace() {
			replaceLine.setVisible(true);
		}

		/**
		 * Hides the replace-bar segment.
		 */
		public void hideReplace() {
			replaceLine.setVisible(false);
		}

		/**
		 * Focus the search input field, and select the text so users can quickly retype something new if desired.
		 */
		private void requestSearchFocus() {
			searchInput.requestFocus();
			searchInput.selectAll();
		}
	}

	/**
	 * @param range
	 * 		Range of the match.
	 * @param groups
	 * 		Regex groups of the match, if the match is done with {@link Bar#regex} enabled.
	 */
	private record Match(@Nonnull IntRange range, @Nullable String[] groups) implements Comparable<Match> {
		@Nonnull
		static Match match(int start, int end) {
			return new Match(new IntRange(start, end), null);
		}

		@Nonnull
		static Match match(int start, int end, @Nonnull Matcher matcher) {
			return new Match(new IntRange(start, end), matcher.groups());
		}

		@Override
		public int compareTo(Match o) {
			return range.compareTo(o.range);
		}
	}
}
