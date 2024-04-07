package software.coley.recaf.ui.pane;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.ResourceSummaryService;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Pane to display summary data about the loaded {@link Workspace} when opened.
 *
 * @author Matt Coley
 * @see WorkspaceRootPane Parent display.
 * @see ResourceSummaryService Manages content to display here via discovered {@link ResourceSummarizer} types.
 */
@Dependent
public class WorkspaceInformationPane extends BorderPane implements Navigable {
	private final WorkspacePathNode path;

	@Inject
	public WorkspaceInformationPane(@Nonnull TextProviderService textService,
	                                @Nonnull IconProviderService iconService,
	                                @Nonnull ResourceSummaryService summaryService,
	                                @Nonnull Workspace workspace) {
		path = PathNodes.workspacePath(workspace);

		// Adding content
		Grid content = new Grid();
		content.setPadding(new Insets(10));
		content.prefWidthProperty().bind(widthProperty().subtract(10));
		ScrollPane scroll = new ScrollPane(content);
		setCenter(scroll);
		getStyleClass().add("background");

		// Populate summary data for each resource.
		List<WorkspaceResource> resources = workspace.getAllResources(false);
		for (WorkspaceResource resource : resources) {
			// Create header.
			Node graphic = iconService.getResourceIconProvider(workspace, resource).makeIcon();
			Label title = new Label(textService.getResourceTextProvider(workspace, resource).makeText());
			Label subtitle = new Label(String.format("%d classes, %d files",
					resource.classBundleStream().mapToInt(Map::size).sum(),
					resource.fileBundleStream().mapToInt(Map::size).sum()
			));
			title.getStyleClass().add(Styles.TITLE_4);
			title.setGraphic(graphic);
			subtitle.getStyleClass().add(Styles.TEXT_SUBTLE);

			if (resources.size() > 1) {
				// Add summaries for this resource into a collapsible panel.
				Grid section = content.newSection();
				TitledPane resourcePane = new TitledPane();
				resourcePane.setContent(section);
				resourcePane.setGraphic(new VBox(title, subtitle));
				content.add(resourcePane, 0, content.getRowCount(), 2, 1);
				summaryService.summarizeTo(workspace, resource, section);
			} else {
				// Single resource, no need to box it.
				VBox wrapper = new VBox(title, subtitle);
				content.add(wrapper, 0, content.getRowCount(), 2, 1);
				summaryService.summarizeTo(workspace, resource, content.newSection());
			}
		}
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		setDisable(true);
	}

	private static class Grid extends GridPane implements SummaryConsumer {
		private Grid() {
			setVgap(5);
			setHgap(5);
			ColumnConstraints column1 = new ColumnConstraints();
			ColumnConstraints column2 = new ColumnConstraints();
			column1.setPercentWidth(25);
			column2.setPercentWidth(75);
			getColumnConstraints().addAll(column1, column2);
		}

		@Nonnull
		public Grid newSection() {
			Grid section = new Grid();
			add(section, 0, getRowCount(), 2, 1);
			return section;
		}

		@Override
		public void appendSummary(Node node) {
			FxThreadUtil.run(() -> add(node, 0, getRowCount(), 2, 1));
		}

		@Override
		public void appendSummary(Node left, Node right) {
			FxThreadUtil.run(() -> {
				int row = getRowCount();
				add(left, 0, row);
				add(right, 1, row);
			});
		}
	}
}
