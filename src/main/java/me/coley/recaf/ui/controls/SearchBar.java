package me.coley.recaf.ui.controls;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import me.coley.recaf.util.struct.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * Basic search bar.
 *
 * @author Matt
 */
public class SearchBar extends GridPane {
	private final Label lblResults = new Label();
	private final TextField txtSearch = new TextField();
	private final Supplier<String> text;
	// actions
	private Runnable onEscape;
	private Consumer<Results> onSearch;
	// inputs
	private boolean dirty = true;
	private String lastSearchText;
	// last result
	private Results results;

	/**
	 * @param text
	 * 		Supplier of searchable text.
	 */
	public SearchBar(Supplier<String> text) {
		setAlignment(Pos.CENTER_LEFT);
		setHgap(7);
		ColumnConstraints column1 = new ColumnConstraints();
		column1.setPercentWidth(75);
		ColumnConstraints column2 = new ColumnConstraints();
		column2.setPercentWidth(25);
		getColumnConstraints().addAll(column1, column2);
		// TODO: Better search field:
		//  - Options
		//     - regex
		this.text = text;
		getStyleClass().add("context-menu");
		txtSearch.getStyleClass().add("search-field");
		txtSearch.setOnKeyPressed(e -> {
			// Check if we've updated the search query
			String searchText = e.getText();
			if(!searchText.equals(lastSearchText)) {
				dirty = true;
			}
			lastSearchText = searchText;
			// Handle operations
			if(e.getCode().getName().equals(KeyCode.ESCAPE.getName())) {
				// Escape the search bar
				if(onEscape != null)
					onEscape.run();
			} else if(e.getCode().getName().equals(KeyCode.ENTER.getName())) {
				// Empty check
				if (txtSearch.getText().isEmpty()) {
					results = null;
					return;
				}
				// Find next
				//  - Run search if necessary
				if(dirty) {
					results = search();
					dirty = false;
				}
				if(onSearch != null && results != null)
					onSearch.accept(results);
			}
		});
		add(txtSearch, 0, 0);
		add(lblResults, 1, 0);
	}

	/**
	 * @param onSearch
	 * 		Search result handler to run.
	 */
	public void setOnSearch(Consumer<Results> onSearch) {
		this.onSearch = onSearch;
	}

	/**
	 * @param onEscape
	 * 		Escape handler to run.
	 */
	public void setOnEscape(Runnable onEscape) {
		this.onEscape = onEscape;
	}

	/**
	 * Focus the search bar input text-field.
	 */
	public void focus() {
		txtSearch.requestFocus();
		txtSearch.selectAll();
	}

	/**
	 * Clear the search bar display.
	 */
	public void clear() {
		txtSearch.clear();
		lblResults.setText("");
	}

	/**
	 * @param text
	 * 		Text to set.
	 */
	public void setText(String text) {
		txtSearch.setText(text);
	}

	/**
	 * @return Search result ranges of the current search parameters.
	 */
	private Results search() {
		Results results = new Results();
		String searchText = txtSearch.getText();
		String targetText = text.get();
		int len = searchText.length();
		int index = targetText.indexOf(searchText);
		while(index >= 0) {
			// Add result
			results.add(index, index + len);
			// Find next
			index = targetText.indexOf(searchText, index + len);
		}
		return results;
	}

	/**
	 * Search results wrapper.
	 *
	 * @author Matt
	 */
	public class Results {
		private final List<Pair<Integer, Integer>> ranges = new ArrayList<>();

		private void add(int start, int end) {
			ranges.add(new Pair<>(start, end));
		}

		/**
		 * @param caret
		 * 		Caret position in text.
		 *
		 * @return Next range immediately after the caret position.
		 */
		public Pair<Integer, Integer> next(int caret) {
			// Check for no matches
			if(ranges.isEmpty()) {
				lblResults.setText(translate("ui.search.results.none"));
				return null;
			}
			// Find first result where the caret is before the result range
			Pair<Integer, Integer> match = null;
			int i = 1;
			for(Pair<Integer, Integer> range : ranges) {
				if(caret < range.getKey()) {
					match = range;
					break;
				}
				i++;
			}
			// No match after caret position, wrap around
			if(match == null) {
				i = 1;
				match = ranges.get(0);
			}
			lblResults.setText(translate("ui.search.results.indexpre") + i + "/" + ranges.size());
			return match;
		}
	}
}
