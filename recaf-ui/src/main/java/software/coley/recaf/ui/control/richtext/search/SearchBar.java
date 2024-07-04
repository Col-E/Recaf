package software.coley.recaf.ui.control.richtext.search;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import regexodus.Matcher;
import software.coley.collections.Lists;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.AbstractSearchBar;
import software.coley.recaf.ui.control.ActionButton;
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
	private Editor editor;
	private FindAndReplaceSearchBar bar;

	@Inject
	public SearchBar(@Nonnull KeybindingConfig keys) {
		this.keys = keys;
	}

	@Override
	public void install(@Nonnull Editor editor) {
		this.editor = editor;
		bar = new FindAndReplaceSearchBar(editor);
		NodeEvents.addKeyPressHandler(editor, this);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		editor.setTop(null);
		NodeEvents.removeKeyPressHandler(editor, this);
		bar = null;
		this.editor = null;
	}

	@Override
	public void handle(KeyEvent event) {
		if (keys.getFind().match(event)) {
			// Show if not visible
			if (!bar.isVisible())
				bar.show();

			// Update input text based on current selection
			if (editor != null) {
				String selectedText = editor.getCodeArea().getSelectedText();
				if (!selectedText.isEmpty()) bar.getSearchTextProperty().setValue(selectedText);
			}

			// Grab focus
			bar.requestSearchFocus();
			bar.hideReplace();
		} else if (keys.getReplace().match(event)) {
			// Show if not visible
			if (!bar.isVisible())
				bar.show();

			// Update input text based on current selection
			if (editor != null) {
				String selectedText = editor.getCodeArea().getSelectedText();
				if (!selectedText.isEmpty()) bar.getSearchTextProperty().setValue(selectedText);
			}

			// Grab focus
			bar.requestSearchFocus();
			bar.showReplace();
		}
	}


	/**
	 * The actual search bar.
	 */
	private static class FindAndReplaceSearchBar extends AbstractSearchBar {
		private final SimpleIntegerProperty lastResultIndex = new SimpleIntegerProperty(-1);
		private final ObservableList<String> pastReplaces = FXCollections.observableArrayList();
		private final ObservableList<Match> resultRanges = FXCollections.observableArrayList();
		private final CustomTextField replaceInput = new CustomTextField();
		private final Button oldReplaces = new Button();
		private final HBox replaceLine = new HBox();
		private final Editor editor;
		private Button prev;
		private Button next;
		private Button close;
		private Button replace;
		private Button replaceAll;

		private FindAndReplaceSearchBar(@Nonnull Editor editor) {
			this.editor = editor;

			setup();
		}

		@Override
		public void setup() {
			// Refresh results when text changes.
			editor.getTextChangeEventStream()
					.successionEnds(Duration.ofMillis(150))
					.addObserver(changes -> refreshResults());

			// Create buttons to iterate through results.
			prev = new ActionButton(CarbonIcons.ARROW_UP, this::prev);
			next = new ActionButton(CarbonIcons.ARROW_DOWN, this::next);
			prev.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.SMALL);
			next.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.SMALL);
			prev.disableProperty().bind(hasResults.not());
			next.disableProperty().bind(hasResults.not());

			// Button to close the search bar.
			close = new ActionButton(CarbonIcons.CLOSE, this::hide);
			close.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.SMALL);

			// Replace buttons.
			replace = new ActionButton(Lang.getBinding("find.replace"), this::replace);
			replaceAll = new ActionButton(Lang.getBinding("find.replaceall"), this::replaceAll);
			replace.getStyleClass().addAll(Styles.SMALL);
			replaceAll.getStyleClass().addAll(Styles.SMALL);
			replace.disableProperty().bind(hasResults.not());
			replaceAll.disableProperty().bind(hasResults.not());

			// Add to past replaces when enter is pressed.
			replaceInput.setLeft(oldReplaces);
			replaceInput.setOnKeyPressed(this::onReplaceInputKeyPress);
			replaceInput.setOnKeyReleased(e -> updateReplacePreview());

			// Initially hidden.
			hide();

			// Parent setup done last since we initialize some controls it will depend on above.
			super.setup();
		}

		@Override
		protected void setupLayout() {
			replaceInput.prefWidthProperty().bind(searchInput.widthProperty());
			HBox prevAndNext = new HBox(prev, next);
			prevAndNext.setAlignment(Pos.CENTER);
			prevAndNext.setFillHeight(false);
			HBox.setHgrow(searchInput, Priority.ALWAYS);
			HBox searchLine = new HBox(searchInput, resultCount, prevAndNext, new Spacer(), close);
			searchLine.setAlignment(Pos.CENTER_LEFT);
			searchLine.setSpacing(10);
			searchLine.setPadding(new Insets(0, 5, 0, 0));
			HBox.setHgrow(replaceInput, Priority.ALWAYS);
			replaceLine.getChildren().addAll(replaceInput, new Spacer(0), replace, replaceAll);
			replaceLine.setAlignment(Pos.CENTER_LEFT);
			replaceLine.setSpacing(10);
			replaceLine.setPadding(new Insets(0, 5, 0, 0));
			replaceLine.setVisible(false); // invis + group = 0 height (group wrapping is required for this to work)
			getChildren().addAll(searchLine, new Group(replaceLine));
		}

		@Override
		protected void onSearchInputKeyPress(@Nonnull KeyEvent e) {
			// Handle navigating to next entry and closing
			KeyCode code = e.getCode();
			if (code == KeyCode.ENTER) {
				next();
			} else if (code == KeyCode.ESCAPE) {
				hide();
			}

			super.onSearchInputKeyPress(e);
		}

		protected void onReplaceInputKeyPress(@Nonnull KeyEvent e) {
			// Handle replacing the next entry and closing
			KeyCode code = e.getCode();
			if (code == KeyCode.ENTER) {
				replace();
			} else if (code == KeyCode.ESCAPE) {
				hide();
			}

			// Update past replacements
			while (pastReplaces.size() > MAX_HISTORY)
				pastReplaces.remove(pastReplaces.size() - 1);
		}

		@Override
		protected void bindResultCountDisplay(@Nonnull StringProperty resultTextProperty) {
			resultTextProperty.bind(lastResultIndex.map(n -> {
				int i = n.intValue();
				if (i < 0) {
					return Lang.get("menu.search.noresults");
				} else {
					return (i + 1) + "/" + resultRanges.size();
				}
			}));
		}

		@Override
		protected void refreshResults() {
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
				int rangeIndex = Lists.sortedInsertIndex(resultRanges, Match.match(searchStart, searchStart));
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

		private void updateReplacePreview() {
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
		}

		@Nonnull
		@Override
		protected List<TextField> getInputFields() {
			return List.of(searchInput, replaceInput);
		}

		@Nonnull
		@Override
		protected List<Button> getInputButtons() {
			return List.of(oldSearches, oldReplaces);
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
			int rangeIndex = Lists.sortedInsertIndex(resultRanges, Match.match(caret, caret));
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
			int rangeIndex = Lists.sortedInsertIndex(resultRanges, Match.match(caret - area.getSelectedText().length(), caret)) - 1;
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
		@Nonnull
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
			if (editor.isEditable())
				replaceLine.setVisible(true);
		}

		/**
		 * Hides the replace-bar segment.
		 */
		public void hideReplace() {
			replaceLine.setVisible(false);
		}
	}

	/**
	 * @param range
	 * 		Range of the match.
	 * @param groups
	 * 		Regex groups of the match, if the match is done with
	 *        {@link AbstractSearchBar#regexProperty()} enabled.
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
