package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.Glow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.collections.Lists;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.assembler.Snippet;
import software.coley.recaf.services.assembler.SnippetListener;
import software.coley.recaf.services.assembler.SnippetManager;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.ui.LanguageStylesheets;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.GraphicActionButton;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;
import software.coley.recaf.ui.control.richtext.syntax.RegexSyntaxHighlighter;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.Lang;

import java.util.Collection;
import java.util.Collections;

/**
 * Pane for creating new snippets, editing existing ones, and listing all available snippets.
 *
 * @author Matt Coley
 */
@Dependent
public class SnippetsPane extends StackPane implements SnippetListener, Navigable {
	private final SnippetManager snippetManager;
	private final Editor editor = new Editor();
	private final GraphicActionButton btnSave;
	private final GraphicActionButton btnNew;
	private final GraphicActionButton btnDelete;
	private final GraphicActionButton btnLoad;
	private final ObservableList<Snippet> snippetList = FXCollections.observableArrayList();
	private final ObjectProperty<Snippet> currentSnippet = new SimpleObjectProperty<>();
	private final ComboBox<Snippet> snippetComboBox = new ComboBox<>(snippetList);

	@Inject
	public SnippetsPane(@Nonnull SnippetManager snippetManager) {
		this.snippetManager = snippetManager;

		// Initialize snippet list and add listener to ensure it gets updated when new snippets are made/removed.
		snippetList.addAll(snippetManager.getSnippets());
		snippetManager.addSnippetListener(this);

		// Configure for assembly content
		editor.disableProperty().bind(currentSnippet.isNull());
		editor.opacityProperty().bind(currentSnippet.isNull().map(nil -> nil ? 0.3 : 1.0));
		editor.getCodeArea().getStylesheets().add(LanguageStylesheets.getJasmStylesheet());
		editor.setSelectedBracketTracking(new SelectedBracketTracking());
		editor.setSyntaxHighlighter(new RegexSyntaxHighlighter(RegexLanguages.getJasmLanguage()));
		editor.getRootLineGraphicFactory().addDefaultCodeGraphicFactories();
		resetContent();

		// Setup actions
		btnSave = new GraphicActionButton(CarbonIcons.SAVE, this::saveSnippet);
		btnNew = new GraphicActionButton(CarbonIcons.DOCUMENT_ADD, this::newSnippet);
		btnDelete = new GraphicActionButton(CarbonIcons.TRASH_CAN, this::deleteSnippet);
		btnLoad = new GraphicActionButton(CarbonIcons.FOLDERS, this::loadSnippet);
		HBox tools = new HBox(btnSave, btnNew, btnDelete, btnLoad);
		tools.setSpacing(6);
		btnDelete.disableProperty().bind(currentSnippet.isNull());
		btnSave.disableProperty().bind(currentSnippet.isNull());

		// Bring attention to 'new' and 'load' initially, removed when a snippet is loaded.
		bringAttention(btnNew, btnLoad);
		currentSnippet.addListener(new ChangeListener<>() {
			@Override
			public void changed(ObservableValue<? extends Snippet> observableValue, Snippet snippet, Snippet t1) {
				removeAttention();
				currentSnippet.removeListener(this);
			}
		});

		// Layout
		Group toolsWrapper = new Group(tools);
		StackPane.setAlignment(toolsWrapper, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(toolsWrapper, new Insets(5));
		getChildren().addAll(editor, toolsWrapper);
	}

	/**
	 * Creates a new snippet to work off of.
	 */
	private void newSnippet() {
		Label nameLabel = new BoundLabel(Lang.getBinding("dialog.input.name"));
		Label descLabel = new BoundLabel(Lang.getBinding("dialog.input.desc")); // Close enough
		TextField nameText = new TextField();
		TextField descText = new TextField();
		Button commit = new ActionButton(CarbonIcons.DOCUMENT_ADD, Lang.getBinding("dialog.finish"), () -> {
			currentSnippet.set(new Snippet(nameText.getText(), descText.getText(), editor.getText()));
			editor.setText("// " + nameText.getText());
			bringAttention(btnSave); // Must happen after setting the snippet for the order of clearing/setting effects.
		});
		commit.disableProperty().bind(nameText.textProperty().isEmpty());
		GridPane grid = new GridPane(5, 5);
		grid.addRow(0, nameLabel, nameText);
		grid.addRow(1, descLabel, descText);
		grid.add(commit, 1, 2);

		// Show the new snippet prompt above the new-snippet button
		Popover popover = new Popover(grid);
		popover.setAutoHide(true);
		popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
		popover.show(btnNew);
	}

	/**
	 * Saves / updates the current snippet to the manager.
	 */
	private void saveSnippet() {
		removeAttention();

		// Should be not-null since the save button is disabled until the property is set.
		Snippet base = currentSnippet.get();
		snippetManager.putSnippet(base.withContent(editor.getText()));
		Animations.animateSuccess(editor, 1000);
	}

	/**
	 * Deletes the current snippet from the manager.
	 */
	private void deleteSnippet() {
		Button commit = new ActionButton(CarbonIcons.TRASH_CAN, Lang.getBinding("dialog.confirm"), () -> {
			// Should be not-null since the delete button is disabled until the property is set.
			Snippet snippet = currentSnippet.get();
			snippetManager.removeSnippet(snippet);

			// Clear the snippet selection and content.
			resetContent();
			currentSnippet.set(null);
		});

		// Show delete prompt above the delete button
		Popover popover = new Popover(commit);
		popover.setAutoHide(true);
		popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
		popover.show(btnDelete);
	}

	/**
	 * Shows the user a list of available snippets to load.
	 */
	private void loadSnippet() {
		ListView<Snippet> snippetsView = new ListView<>(snippetList);
		snippetsView.setCellFactory(v -> new ListCell<>() {
			@Override
			protected void updateItem(Snippet item, boolean empty) {
				super.updateItem(item, empty);

				if (empty || item == null) {
					setGraphic(null);
				} else {
					Label title = new Label(item.name());
					Label desc = new Label(item.description());
					title.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_BOLD);
					desc.getStyleClass().add(Styles.TEXT_SUBTLE);
					VBox box = new VBox(title, desc);
					setGraphic(box);
				}
			}
		});
		snippetsView.getSelectionModel().selectedItemProperty().addListener((ob, old, cur) -> {
			editor.setText(cur.content());
			currentSnippet.setValue(cur);
		});
		snippetsView.getStyleClass().addAll(Styles.BG_INSET, "borderless");
		snippetsView.setPrefHeight(50 * snippetList.size() + 20);

		// Show the load prompt above the load button
		Popover popover = new Popover(snippetsView);
		popover.setMaxWidth(400);
		popover.setPrefHeight(400);
		popover.setAutoHide(true);
		popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
		popover.show(btnLoad);
	}

	/**
	 * Resets the editor text to the default.
	 */
	private void resetContent() {
		editor.setText("// Select an existing snippet or make a new one\n" +
				"// from the buttons below.");
	}

	private void bringAttention(Node... nodes) {
		for (Node node : nodes) {
			node.setEffect(new Glow(0.7));
		}
	}

	/**
	 * Clears the special effects on buttons.
	 */
	private void removeAttention() {
		btnNew.setEffect(null);
		btnLoad.setEffect(null);
		btnSave.setEffect(null);
	}

	@Override
	public void disable() {
		snippetManager.removeSnippetListener(this);
		editor.close();
	}

	@Nullable
	@Override
	public PathNode<?> getPath() {
		return null;
	}

	@Override
	public boolean isTrackable() {
		return false;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void onSnippetAdded(@Nonnull Snippet snippet) {
		int index = Lists.sortedInsertIndex(Snippet.NAME_COMPARATOR, snippetList, snippet);
		snippetList.add(index, snippet);
	}

	@Override
	public void onSnippetModified(@Nonnull Snippet old, @Nonnull Snippet current) {
		int i = snippetList.indexOf(old);
		if (i >= 0) snippetList.set(i, current);
	}

	@Override
	public void onSnippetRemoved(@Nonnull Snippet snippet) {
		snippetList.remove(snippet);
	}
}
