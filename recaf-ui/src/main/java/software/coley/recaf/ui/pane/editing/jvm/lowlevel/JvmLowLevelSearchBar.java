package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import software.coley.recaf.ui.control.AbstractSearchBar;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.tree.TreeFiltering;
import software.coley.recaf.ui.control.tree.TreeItems;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.RegexUtil;
import org.kordamp.ikonli.carbonicons.CarbonIcons;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import regexodus.Matcher;

/**
 * Search bar that cycles through matching {@link ClassElement} items in a {@link TreeView}.
 *
 * @author Matt Coley
 */
public final class JvmLowLevelSearchBar extends AbstractSearchBar {
	private final TreeView<ClassElement> tree;
	private final ObservableList<TreeItem<ClassElement>> results = FXCollections.observableArrayList();
	private final SimpleIntegerProperty currentResult = new SimpleIntegerProperty(-1);
	private final Runnable onClose;
	private Button previous;
	private Button next;
	private Button close;

	/**
	 * @param tree
	 * 		Tree to search and navigate.
	 * @param onClose
	 * 		Callback for when the user closes the search bar, via the close button or the {@code Escape} key.
	 */
	public JvmLowLevelSearchBar(@Nonnull TreeView<ClassElement> tree, @Nonnull Runnable onClose) {
		this.tree = tree;
		this.onClose = onClose;
		setup();
		searchInput.textProperty().addListener((observable, oldValue, newValue) -> refreshResults());
		TreeFiltering.install(searchInput, tree);
	}

	@Override
	public void setup() {
		previous = new ActionButton(CarbonIcons.ARROW_UP, this::previous);
		next = new ActionButton(CarbonIcons.ARROW_DOWN, this::next);
		close = new ActionButton(CarbonIcons.CLOSE, onClose);
		previous.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.SMALL);
		next.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.SMALL);
		close.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.SMALL);
		previous.disableProperty().bind(hasResults.not());
		next.disableProperty().bind(hasResults.not());
		super.setup();

		// Enter cycles instead of refreshing the result list back to its first item.
		searchInput.setOnKeyReleased(event -> {
			if (event.getCode() != KeyCode.ENTER)
				refreshResults();
		});
	}

	@Override
	protected void setupLayout() {
		HBox previousAndNext = new HBox(previous, next);
		previousAndNext.setAlignment(Pos.CENTER);
		previousAndNext.setFillHeight(false);
		HBox.setHgrow(searchInput, Priority.ALWAYS);
		HBox searchLine = new HBox(searchInput, resultCount, previousAndNext, close);
		searchLine.setAlignment(Pos.CENTER_LEFT);
		searchLine.setSpacing(10);
		searchLine.setPadding(new Insets(0, 5, 0, 0));
		getChildren().add(searchLine);
	}

	@Override
	protected void onSearchInputKeyPress(@Nonnull KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			next();
			recordSearch();
		} else if (event.getCode() == KeyCode.ESCAPE) {
			onClose.run();
		}
		super.onSearchInputKeyPress(event);
	}

	@Override
	protected void bindResultCountDisplay(@Nonnull StringProperty resultTextProperty) {
		resultTextProperty.bind(Bindings.createStringBinding(() -> {
			int index = currentResult.get();
			return index < 0 ? Lang.get("menu.search.noresults") : (index + 1) + "/" + results.size();
		}, currentResult, results));
	}

	@Override
	protected void refreshResults() {
		currentResult.set(-1);
		results.clear();
		hasResults.set(false);
		hideRegexPopover();

		String query = searchInput.getText();
		if (query == null || query.isEmpty())
			return;
		if (regex.get()) {
			RegexUtil.RegexValidation validation = RegexUtil.validate(query);
			if (!validation.valid()) {
				showRegexPopover(validation);
				return;
			}
		}

		List<TreeItem<ClassElement>> matches = new ArrayList<>();
		TreeItem<ClassElement> root = tree.getRoot();
		if (root == null)
			return;
		List<TreeItem<ClassElement>> sourceChildren = sourceChildren(root);
		for (TreeItem<ClassElement> child : sourceChildren)
			collectMatches(child, query, matches);
		if (matches.isEmpty())
			return;

		results.setAll(matches);
		hasResults.set(true);
		currentResult.set(0);
		selectCurrent();
	}

	private void collectMatches(@Nonnull TreeItem<ClassElement> item, @Nonnull String query,
	                            @Nonnull List<TreeItem<ClassElement>> matches) {
		ClassElement element = item.getValue();
		if (element != null && matches(element, query))
			matches.add(item);
		for (TreeItem<ClassElement> child : sourceChildren(item))
			collectMatches(child, query, matches);
	}

	private boolean matches(@Nonnull ClassElement element, @Nonnull String query) {
		String text = element.prefix() + ": " + element.content();
		if (regex.get()) {
			String expression = caseSensitivity.get() ? query : "(?i)" + query;
			Matcher matcher = RegexUtil.getMatcher(expression, text);
			return matcher.find();
		}
		if (caseSensitivity.get())
			return text.contains(query);
		return text.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
	}

	private void next() {
		if (results.isEmpty())
			return;
		int index = currentResult.get();
		currentResult.set(index < 0 ? 0 : (index + 1) % results.size());
		selectCurrent();
	}

	private void previous() {
		if (results.isEmpty())
			return;
		int index = currentResult.get();
		currentResult.set(index <= 0 ? results.size() - 1 : index - 1);
		selectCurrent();
	}

	private void selectCurrent() {
		int index = currentResult.get();
		if (index < 0 || index >= results.size())
			return;
		TreeItem<ClassElement> item = results.get(index);
		TreeItems.expandParents(item);
		tree.getSelectionModel().select(item);
		int row = tree.getRow(item);
		if (row >= 0) {
			tree.getFocusModel().focus(row);
			tree.scrollTo(row);
		}
	}

	@Nonnull
	private List<TreeItem<ClassElement>> sourceChildren(@Nonnull TreeItem<ClassElement> item) {
		if (item instanceof ClassItem classItem)
			return classItem.getSourceChildren();
		return item.getChildren();
	}

	private void recordSearch() {
		String query = searchInput.getText();
		if (query == null || query.isEmpty())
			return;
		pastSearches.remove(query);
		pastSearches.addFirst(query);
	}

	private void showRegexPopover(@Nonnull RegexUtil.RegexValidation validation) {
		Popover popover = new Popover(new Label(validation.message()));
		popover.setHeaderAlwaysVisible(true);
		popover.titleProperty().bind(Lang.getBinding("find.regexinvalid"));
		popover.show(searchInput);
		Object old = searchInput.getProperties().put("regex-popover", popover);
		if (old instanceof Popover oldPopover)
			oldPopover.hide();
	}

	private void hideRegexPopover() {
		Object old = searchInput.getProperties().put("regex-popover", null);
		if (old instanceof Popover oldPopover)
			oldPopover.hide();
	}
}
