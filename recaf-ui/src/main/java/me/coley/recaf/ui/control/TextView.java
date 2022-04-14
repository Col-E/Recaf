package me.coley.recaf.ui.control;

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
public class TextView extends BorderPane implements FileRepresentation, Cleanable, LanguageAssociationListener {
	private final SyntaxArea area;
	private boolean ignoreNextDecompile;
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
			this.area = new ManifestArea(language, problemTracking);
		} else {
			this.area = new SyntaxArea(language, problemTracking);
			if (language == Languages.NONE)
				addUnknownExtensionWarning();
		}

		setCenter(new VirtualizedScrollPane<>(area));
		SearchBar.install(this, area);
		// When the user changes what language is associated with this type of file
		Languages.addAssociationListener(this);
	}

	private void addUnknownExtensionWarning() {
		GridPane warning = new GridPane();
		warning.setPadding(new Insets(0, 0, 0, 4));
		warning.getChildren().add(new Label(Lang.get("dialog.unknownextension")));

		// Align the configure/dismiss buttons to the right of the bar
		ColumnConstraints constraint = new ColumnConstraints();
		constraint.setHgrow(Priority.ALWAYS);

		warning.getColumnConstraints().add(constraint);

		Hyperlink configure = new Hyperlink(Lang.get("dialog.configure"));
		Hyperlink dismiss = new Hyperlink(Lang.get("dialog.dismiss"));

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
		if (ignoreNextDecompile) {
			ignoreNextDecompile = false;
			return;
		}
		area.setText(new String(info.getValue()));
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
		ignoreNextDecompile = true;
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
}
