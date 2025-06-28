package software.coley.recaf.ui.control;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.util.Icons;

import java.util.List;

/**
 * Common base for text-targeting search bar controls.
 * After constructing, call {@link #setup()}.
 *
 * @author Matt Coley
 */
public abstract class AbstractSearchBar extends VBox {
	protected static final int MAX_HISTORY = 10;
	protected final ObservableList<String> pastSearches = FXCollections.observableArrayList();
	protected final SimpleBooleanProperty hasResults = new SimpleBooleanProperty();
	protected final SimpleBooleanProperty caseSensitivity = new SimpleBooleanProperty();
	protected final SimpleBooleanProperty regex = new SimpleBooleanProperty();
	protected final CustomTextField searchInput = new CustomTextField();
	protected final Button oldSearches = new Button();
	protected final Label resultCount = new Label();

	/**
	 * <b>Note</b>: The base {@link AbstractSearchBar} does not tie into any search systems,
	 * so its up to implementors to ensure values are added to this list.
	 *
	 * @return Property holding list of prior searches.
	 */
	@Nonnull
	public ObservableList<String> getPastSearches() {
		return pastSearches;
	}

	/**
	 * @return Property of the current search text.
	 */
	@Nonnull
	public StringProperty getSearchTextProperty() {
		return searchInput.textProperty();
	}

	/**
	 * <b>Note</b>: The base {@link AbstractSearchBar} does not tie into any search systems,
	 * so its up to implementors to ensure this is wired up properly.
	 *
	 * @return Property indicating if search results are found.
	 */
	@Nonnull
	public SimpleBooleanProperty hasResultsProperty() {
		return hasResults;
	}

	/**
	 * @return Property indicating if searching is case-sensitive.
	 */
	@Nonnull
	public SimpleBooleanProperty caseSensitivityProperty() {
		return caseSensitivity;
	}

	/**
	 * @return Property indicating if searching is regex-based.
	 */
	@Nonnull
	public SimpleBooleanProperty regexProperty() {
		return regex;
	}

	/**
	 * Initializes controls for the search bar.
	 */
	public void setup() {
		getStyleClass().add("search-bar");

		// Remove border from search/replace text fields.
		getInputFields().forEach(field -> field.getStyleClass().addAll(Styles.ACCENT));

		// Create menu for search input left graphic (like IntelliJ) to display prior searches when clicked.
		searchInput.setLeft(oldSearches);
		getInputButtons().forEach(button -> {
			button.setDisable(true); // re-enabled when searches are populated.
			button.setGraphic(new FontIconView(CarbonIcons.SEARCH));
			button.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT, Styles.SMALL);
		});

		// Create toggles for search input query modes.
		BoundToggleIcon toggleSensitivity = new BoundToggleIcon(new FontIconView(CarbonIcons.LETTER_CC), caseSensitivity).withTooltip("misc.casesensitive");
		BoundToggleIcon toggleRegex = new BoundToggleIcon(Icons.REGEX, regex).withTooltip("misc.regex");
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
		resultCount.setMinWidth(70);
		resultCount.setAlignment(Pos.CENTER);
		resultCount.setTextAlignment(TextAlignment.CENTER);
		bindResultCountDisplay(resultCount.textProperty());
		resultCount.styleProperty().bind(hasResults.map(has -> {
			if (has) {
				return "-fx-text-fill: -color-fg-default;";
			} else {
				return "-fx-text-fill: red;";
			}
		}));

		// Add to past searches when enter is pressed.
		searchInput.setOnKeyPressed(this::onSearchInputKeyPress);
		searchInput.setOnKeyReleased(e -> refreshResults());

		// Ensure typed keys are only handled when the input is focused.
		// If the user is tab-navigating to toggle the input buttons then we want to cancel the event.
		searchInput.addEventFilter(KeyEvent.KEY_TYPED, e -> {
			if (!searchInput.isFocused() && e.getCode() != KeyCode.TAB)
				e.consume();
		});

		// When past searches list is modified, update old search menu.
		updatePastListing(oldSearches, pastSearches, searchInput);

		// Layout
		setupLayout();
	}

	/**
	 * Lays out search bar controls.
	 * <p/>
	 * Called at the end of {@link #setup()}.
	 */
	protected void setupLayout() {
		HBox.setHgrow(searchInput, Priority.ALWAYS);
		HBox searchLine = new HBox(searchInput, resultCount);
		searchLine.setAlignment(Pos.CENTER_LEFT);
		searchLine.setSpacing(10);
		searchLine.setPadding(new Insets(0, 5, 0, 0));
		getChildren().addAll(searchLine);
	}

	/**
	 * Focus the search input field, and select the text so users can quickly retype something new if desired.
	 */
	public void requestSearchFocus() {
		searchInput.requestFocus();
		searchInput.selectAll();
	}

	/**
	 * Called when {@link #searchInput} keys are pressed.
	 *
	 * @param e
	 * 		Input key press event.
	 */
	protected void onSearchInputKeyPress(@Nonnull KeyEvent e) {
		// Clear old searches if there are too many
		while (pastSearches.size() > MAX_HISTORY)
			pastSearches.removeLast();
	}

	/**
	 * Implementations should configure how the result count is displayed here.
	 *
	 * @param resultTextProperty
	 * 		Property of the display count text.
	 */
	protected abstract void bindResultCountDisplay(@Nonnull StringProperty resultTextProperty);

	/**
	 * Handle searching.
	 */
	protected abstract void refreshResults();

	/**
	 * @return List of input fields
	 */
	@Nonnull
	protected List<TextField> getInputFields() {
		return List.of(searchInput);
	}

	/**
	 * @return List of input buttons for filling in old search inputs.
	 */
	@Nonnull
	protected List<Button> getInputButtons() {
		return List.of(oldSearches);
	}

	/**
	 * When the past items list is updated, update the menu that fills in the input with the selected old entry.
	 *
	 * @param button
	 * 		Button to show the list.
	 * @param list
	 * 		Source list to pull values from.
	 * @param input
	 * 		Input to apply text to.
	 */
	protected void updatePastListing(@Nonnull Button button, @Nonnull ObservableList<String> list, @Nonnull CustomTextField input) {
		list.addListener((ListChangeListener<String>) c -> {
			List<ActionMenuItem> items = list.stream()
					.map(text -> new ActionMenuItem(text, () -> {
						input.setText(text);
						requestSearchFocus();
					}))
					.toList();
			if (items.isEmpty()) {
				button.setDisable(true);
			} else {
				button.setDisable(false);
				ContextMenu contextMenu = new ContextMenu();
				contextMenu.getItems().setAll(items);
				button.setOnMousePressed(e -> contextMenu.show(button, e.getScreenX(), e.getScreenY()));
			}
		});
	}
}
