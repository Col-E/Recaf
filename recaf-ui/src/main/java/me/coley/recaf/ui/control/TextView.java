package me.coley.recaf.ui.control;

import javafx.beans.property.IntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.FileRepresentation;
import me.coley.recaf.ui.behavior.FontSizeChangeable;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.code.Language;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.SyntaxArea;
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.ui.control.menu.ActionMenuItem;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.util.LanguageAssociationListener;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Basic scrollable text display with syntax highlighting support.
 *
 * @author Matt Coley
 */
public class TextView extends BorderPane implements FileRepresentation, Cleanable, LanguageAssociationListener, FontSizeChangeable {
	private final SyntaxArea area;
	private final VirtualizedScrollPane<SyntaxArea> scroll;
	private boolean ignoreNextUpdate;
	private FileInfo info;

	/**
	 * @param language
	 * 		Language to use for syntax highlighting.
	 * @param problemTracking
	 * 		Problem tracker.
	 */
	public TextView(Language language, ProblemTracking problemTracking) {
		if (language == Languages.JAVA) {
			this.area = new JavaArea(problemTracking);
		} else if (language == Languages.MANIFEST) {
			this.area = new ManifestArea(problemTracking);
		} else {
			this.area = new SyntaxArea(language, problemTracking);
			if (language == Languages.NONE)
				addUnknownExtensionWarning();
		}
		scroll = new VirtualizedScrollPane<>(area);
		setCenter(scroll);
		SearchBar.install(this, area);
		// When the user changes what language is associated with this type of file
		Languages.addAssociationListener(this);
	}

	private void addUnknownExtensionWarning() {
		GridPane warning = new GridPane();
		warning.setPadding(new Insets(0, 0, 0, 4));
		Label label = new BoundLabel(Lang.getBinding("dialog.unknownextension"));
		warning.getChildren().add(label);

		// Align the configure/dismiss buttons to the right of the bar
		ColumnConstraints constraint = new ColumnConstraints();
		constraint.setHgrow(Priority.ALWAYS);

		warning.getColumnConstraints().add(constraint);

		Hyperlink configure = new Hyperlink();
		configure.textProperty().bind(Lang.getBinding("dialog.configure"));
		Hyperlink dismiss = new Hyperlink();
		dismiss.textProperty().bind(Lang.getBinding("dialog.dismiss"));

		warning.getChildren().addAll(configure, dismiss);

		GridPane.setColumnIndex(configure, 1);
		GridPane.setColumnIndex(dismiss, 2);

		configure.setOnMouseClicked(e -> onSelectLanguageAssociation(configure));
		dismiss.setOnMouseClicked(e -> setBottom(null));

		setBottom(warning);
	}

	private void onSelectLanguageAssociation(Node anchor) {
		Consumer<Language> onItemSelect = (language) -> {
			Languages.setExtensionAssociation(info.getExtension(), language);
			// Dismiss the bar
			setBottom(null);
		};

		ContextMenu selection = new ContextMenu();
		List<ActionMenuItem> items = Languages.allLanguages().stream()
				.sorted(Comparator.comparing(Language::getName))
				.map(language -> new ActionMenuItem(language.getName(), () -> onItemSelect.accept(language)))
				.collect(Collectors.toList());
		selection.getItems().addAll(items);
		selection.show(anchor, Side.BOTTOM, 0, 4);
	}

	/**
	 * @return Wrapped code display.
	 */
	public SyntaxArea getTextArea() {
		return area;
	}

	/**
	 * @return Scroll wrapper around {@link #getTextArea()}.
	 */
	public VirtualizedScrollPane<SyntaxArea> getScroll() {
		return scroll;
	}

	@Override
	public void onAssociationChanged(String extension, Language newLanguage) {
		// This new association doesn't apply to us.
		if (!extension.equals(info.getExtension()))
			return;

		// Dismiss the warning bar just in case another tab
		// was the one that applied the association.
		setBottom(null);

		// Apply the new language (triggers re-style of document)
		area.applyLanguage(newLanguage);
	}

	@Override
	public void onUpdate(FileInfo info) {
		this.info = info;
		if (ignoreNextUpdate) {
			ignoreNextUpdate = false;
			return;
		}
		area.setText(new String(info.getValue(), StandardCharsets.UTF_8));
	}

	@Override
	public FileInfo getCurrentFileInfo() {
		return info;
	}

	@Override
	public SaveResult save() {
		Workspace workspace = RecafUI.getController().getWorkspace();
		Resource primary = workspace.getResources().getPrimary();
		// Update in primary resource
		ignoreNextUpdate = true;
		FileInfo newInfo = new FileInfo(info.getName(), area.getText().getBytes(StandardCharsets.UTF_8));
		primary.getFiles().put(newInfo);
		return SaveResult.SUCCESS;
	}

	@Override
	public boolean supportsEditing() {
		return true;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public void cleanup() {
		Languages.removeAssociationListener(this);
		area.cleanup();
	}

	@Override
	public void bindFontSize(IntegerProperty property) {
		area.bindFontSize(property);
	}

	@Override
	public void applyEventsForFontSizeChange(Consumer<Node> consumer) {
		area.applyEventsForFontSizeChange(consumer);
	}
}
