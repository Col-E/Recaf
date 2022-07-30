package me.coley.recaf.ui.control;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.InteractiveText;
import me.coley.recaf.ui.behavior.Searchable;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.StringUtil;

import java.util.EnumSet;
import java.util.Objects;

/**
 * Text search bar that can be installed on {@link Searchable} components
 * by pinning itself to a wrapper {@link BorderPane}'s top.
 *
 * @author Matt Coley
 * @see Searchable
 */
public class SearchBar extends GridPane {
	private static final int ICON_SIZE = 20;
	private static final double OFF_OPACITY = 0.4;
	private final EnumSet<Searchable.SearchModifier> modifiers = EnumSet.noneOf(Searchable.SearchModifier.class);
	private final TextField txtSearch = new TextField();
	private final Label lblIndex = new Label();
	private final BorderPane host;
	private final Searchable searchable;
	private Searchable.SearchResults results;
	private boolean isShowing;
	private boolean isForward = true;

	private SearchBar(BorderPane host, Searchable searchable) {
		this.host = host;
		this.searchable = searchable;
		// Button for toggling search going forwards or backwards
		Button btnDirToggle = new Button();
		btnDirToggle.setGraphic(Icons.getIconView(Icons.FORWARD, ICON_SIZE));
		btnDirToggle.setOnAction(e -> {
			isForward = !isForward;
			if (isForward) {
				btnDirToggle.setGraphic(Icons.getIconView(Icons.FORWARD, ICON_SIZE));
			} else {
				btnDirToggle.setGraphic(Icons.getIconView(Icons.BACKWARD, ICON_SIZE));
			}
		});
		// Buttons for toggling modifiers
		Button btnSensitivity = new Button();
		btnSensitivity.setGraphic(Icons.getIconView(Icons.CASE_SENSITIVITY, ICON_SIZE));
		btnSensitivity.setOnAction(e -> {
			if (modifiers.contains(Searchable.SearchModifier.CASE_SENSITIVE)) {
				modifiers.remove(Searchable.SearchModifier.CASE_SENSITIVE);
				btnSensitivity.getGraphic().setOpacity(1.0);
			} else {
				modifiers.add(Searchable.SearchModifier.CASE_SENSITIVE);
				btnSensitivity.getGraphic().setOpacity(OFF_OPACITY);
			}
		});
		Button btnWord = new Button();
		btnWord.setGraphic(Icons.getScaledIconView(Icons.WORD, ICON_SIZE));
		btnWord.setOnAction(e -> {
			if (modifiers.contains(Searchable.SearchModifier.WORD)) {
				modifiers.remove(Searchable.SearchModifier.WORD);
				btnWord.getGraphic().setOpacity(OFF_OPACITY);
			} else {
				modifiers.add(Searchable.SearchModifier.WORD);
				btnWord.getGraphic().setOpacity(1.0);
			}
		});
		Button btnRegex = new Button();
		btnRegex.setGraphic(Icons.getScaledIconView(Icons.REGEX, ICON_SIZE));
		btnRegex.setOnAction(e -> {
			if (modifiers.contains(Searchable.SearchModifier.REGEX)) {
				modifiers.remove(Searchable.SearchModifier.REGEX);
				btnRegex.getGraphic().setOpacity(OFF_OPACITY);
			} else {
				modifiers.add(Searchable.SearchModifier.REGEX);
				btnRegex.getGraphic().setOpacity(1.0);
			}
		});
		// Some modifier buttons off by default
		btnWord.getGraphic().setOpacity(OFF_OPACITY);
		btnRegex.getGraphic().setOpacity(OFF_OPACITY);
		// Putting it together
		int c = 0;
		add(txtSearch, c++, 0);
		add(btnDirToggle, c++, 0);
		add(btnSensitivity, c++, 0);
		add(btnWord, c++, 0);
		add(btnRegex, c++, 0);
		add(lblIndex, c++, 0);
		// Style
		lblIndex.setStyle("-fx-padding: 2 2 2 10;");
		getStyleClass().add("menu-bar");
		// Listeners
		txtSearch.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ESCAPE) {
				toggleVisibility();
			} else if (e.getCode() == KeyCode.ENTER) {
				runSearch();
				if (results.hasResults()) {
					results.result().select();
				}
			}
		});
		EventHandler<? super KeyEvent> oldPressHandler = host.getOnKeyPressed();
		host.setOnKeyPressed(e -> {
			if (Configs.keybinds().find.match(e)) {
				toggleVisibility();
			} else if (oldPressHandler != null) {
				oldPressHandler.handle(e);
			}
		});
	}

	private void toggleVisibility() {
		if (isShowing) {
			close();
		} else {
			open();
		}
		isShowing = !isShowing;
	}

	/**
	 * Open the search bar.
	 * <br>
	 * If there is a selection, we want to do a new search with the selected text.
	 * Otherwise, we are simply going to re-display the search bar.
	 */
	private void open() {
		if (searchable instanceof InteractiveText) {
			// Initialize new search with selected text if it is non-empty
			InteractiveText interactiveText = (InteractiveText) searchable;
			String selected = interactiveText.getSelectionText();
			if (!StringUtil.isNullOrEmpty(selected)) {
				txtSearch.setText(selected);
				// Run new search
				runSearch();
				// Update index based on current selection.
				adjustIndexToExistingSelection();
			}
		}
		host.setTop(this);
		txtSearch.requestFocus();
	}


	/**
	 * Close the search bar and re-focus the target content.
	 */
	private void close() {
		host.setTop(null);
		if (searchable instanceof Node) {
			((Node) searchable).requestFocus();
		} else {
			host.requestFocus();
		}
	}

	/**
	 * Runs the next search operation:
	 * <ul>
	 *     <li>{@link Searchable#next(EnumSet, String)}</li>
	 *     <li>{@link Searchable#previous(EnumSet, String)}</li>
	 * </ul>
	 * This populates the {@link #results} but does not operate on them
	 * <i>(IE: selecting the {@link Searchable.SearchResults#result()})</i>
	 */
	private void runSearch() {
		Searchable.SearchResults newResults = isForward ?
				searchable.next(modifiers, txtSearch.getText()) :
				searchable.previous(modifiers, txtSearch.getText());
		// Check if the results' wrapper is actually different.
		// If we're just moving to the next/prev items the wrapper instance should be the same.
		if (!Objects.equals(newResults, results)) {
			// New results wrapper, must be a new search or input.
			// Since this is new we want to update our reference.
			results = newResults;
			// We also want to adjust the results' index to whatever we currently have selected (if anything)
			adjustIndexToExistingSelection();
		}
		updateIndexDisplay();
	}

	/**
	 * In some cases, the user may have already selected some text that is a search result.
	 * We want the current search results index to match this selected text if possible.
	 * That way the <i>"next"</i> search doesn't slingshot the user all the way to the top of the text.
	 */
	private void adjustIndexToExistingSelection() {
		if (searchable instanceof InteractiveText) {
			InteractiveText interactiveText = (InteractiveText) searchable;
			int selectionStart = interactiveText.getSelectionStart();
			int i = 0;
			for (Searchable.SearchResult result : results.getAllResults()) {
				if (selectionStart >= result.getStart() && selectionStart <= result.getStop()) {
					results.setIndex(i);
					updateIndexDisplay();
					break;
				}
				i++;
			}
		}
	}

	private void updateIndexDisplay() {
		if (results == null || !results.hasResults()) {
			lblIndex.textProperty().bind(Lang.getBinding("menu.search.noresults"));
		} else {
			lblIndex.textProperty().unbind();
			lblIndex.setText((results.getIndex() + 1) + "/" + results.count());
		}
	}

	/**
	 * Adds a search bar to the given searchable control.
	 *
	 * @param host
	 * 		Host that has the {@link Searchable} element contained in any spot
	 * 		except the {@link BorderPane#getTop() top}.
	 * @param searchable
	 * 		Searchable control.
	 */
	public static void install(BorderPane host, Searchable searchable) {
		new SearchBar(host, searchable);
	}
}
