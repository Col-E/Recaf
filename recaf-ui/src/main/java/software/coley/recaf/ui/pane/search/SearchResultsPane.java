package software.coley.recaf.ui.pane.search;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.result.Result;
import software.coley.recaf.services.search.result.Results;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.PathNodeTree;
import software.coley.recaf.ui.control.tree.TreeItems;
import software.coley.recaf.ui.control.tree.WorkspaceTreeNode;
import software.coley.recaf.util.ClipboardUtil;
import software.coley.recaf.util.FileChooserBuilder;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Pane presentation for completed search results.
 *
 * @author Matt Coley
 */
public class SearchResultsPane extends BorderPane {
	private static final Logger logger = Logging.get(SearchResultsPane.class);

	private final Workspace workspace;
	private final CellConfigurationService configurationService;
	private final SearchResultsModel model;
	private final PathNodeTree tree;

	/**
	 * @param workspace
	 * 		Workspace containing the results.
	 * @param configurationService
	 * 		Service to configure cell content.
	 * @param actions
	 * 		Navigation actions.
	 * @param model
	 * 		Result model to display.
	 */
	public SearchResultsPane(@Nonnull Workspace workspace,
	                         @Nonnull CellConfigurationService configurationService,
	                         @Nonnull Actions actions,
	                         @Nonnull SearchResultsModel model) {
		this.workspace = workspace;
		this.configurationService = configurationService;
		this.model = model;
		this.tree = new PathNodeTree(configurationService, actions);
		setTop(createToolbar());
		setCenter(tree);
	}

	/**
	 * @return Underlying search results model.
	 */
	@Nonnull
	public SearchResultsModel getModel() {
		return model;
	}

	/**
	 * Replace the displayed result snapshot.
	 *
	 * @param results
	 * 		Results to display.
	 */
	public void setResults(@Nonnull Results results) {
		model.setResults(results);
		WorkspaceTreeNode root = new WorkspaceTreeNode(PathNodes.workspacePath(workspace));
		root.setExpanded(true);
		for (Result<?> result : model.getResults()) {
			WorkspaceTreeNode node = WorkspaceTreeNode.getOrInsertIntoTree(root, result.getPath());
			TreeItems.expandParents(node);
		}
		tree.setRoot(root);
	}

	@Nonnull
	private HBox createToolbar() {
		Label counts = new Label();
		counts.textProperty().bind(Lang.format("search.results.count", model.matchCountProperty(), model.pathCountProperty()));

		Region spacer = new Region();
		ActionButton copy = new ActionButton(CarbonIcons.COPY, Lang.getBinding("search.results.copy"), this::copyResults);
		copy.withTooltip("search.results.copy");
		copy.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
		ActionButton export = new ActionButton(CarbonIcons.DOCUMENT_EXPORT, Lang.getBinding("search.results.export"), this::exportResults);
		export.withTooltip("search.results.export");
		export.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);

		HBox toolbar = new HBox(8, counts, spacer, copy, export);
		toolbar.setAlignment(Pos.CENTER_LEFT);
		toolbar.setPadding(new Insets(7, 10, 7, 10));
		toolbar.getStyleClass().add("config-toolbar");
		HBox.setHgrow(spacer, Priority.ALWAYS);
		return toolbar;
	}

	private void copyResults() {
		ClipboardUtil.copyString(formatResults());
	}

	private void exportResults() {
		File file = new FileChooserBuilder()
				.setTitle(Lang.get("search.results.export"))
				.setInitialFileName("search-results.txt")
				.setFileExtensionFilter("Text files", "*.txt")
				.save(getScene() == null ? null : getScene().getWindow());
		if (file == null)
			return;
		try {
			Files.writeString(file.toPath(), formatResults());
		} catch (IOException ex) {
			logger.error("Failed to export search results to '{}'", file, ex);
		}
	}

	@Nonnull
	private String formatResults() {
		return model.formatResults(this::formatResult);
	}

	@Nonnull
	private String formatResult(@Nonnull Result<?> result) {
		PathNode<?> path = result.getPath();
		return configurationService.textOf(path) + "\t" + result.getValue();
	}
}
