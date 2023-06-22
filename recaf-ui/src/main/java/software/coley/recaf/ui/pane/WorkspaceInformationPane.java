package software.coley.recaf.ui.pane;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import software.coley.recaf.services.cell.IconProviderService;
import software.coley.recaf.services.cell.TextProviderService;
import software.coley.recaf.services.info.ResourceSummarizer;
import software.coley.recaf.services.info.ResourceSummaryService;
import software.coley.recaf.services.info.SummaryConsumer;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Map;

/**
 * Pane to display summary data about the loaded {@link Workspace} when opened.
 *
 * @author Matt Coley
 * @see WorkspaceRootPane Parent display.
 * @see ResourceSummaryService Manages content to display here via discovered {@link ResourceSummarizer} types.
 */
@Dependent
public class WorkspaceInformationPane extends BorderPane {
	@Inject
	public WorkspaceInformationPane(@Nonnull TextProviderService textService,
									@Nonnull IconProviderService iconService,
									@Nonnull ResourceSummaryService summaryService,
									@Nonnull Workspace workspace) {
		// Adding content
		Grid content = new Grid();
		content.setPadding(new Insets(5));
		content.prefWidthProperty().bind(widthProperty());
		ScrollPane scroll = new ScrollPane(content);
		setCenter(scroll);
		getStyleClass().add("background");

		// Populate summary data for each resource.
		for (WorkspaceResource resource : workspace.getAllResources(false)) {
			// Add header
			Node graphic = iconService.getResourceIconProvider(workspace, resource).makeIcon();
			Label title = new Label(textService.getResourceTextProvider(workspace, resource).makeText());
			Label subtitle = new Label(String.format("%d classes, %d files",
					resource.classBundleStream().mapToInt(Map::size).sum(),
					resource.fileBundleStream().mapToInt(Map::size).sum()
			));
			title.getStyleClass().add(Styles.TITLE_4);
			title.setGraphic(graphic);
			subtitle.getStyleClass().add(Styles.TEXT_SUBTLE);
			VBox wrapper = new VBox(title, subtitle);
			content.add(wrapper, 0, content.getRowCount(), 2, 2);

			// Add summaries
			summaryService.summarizeTo(workspace, resource, content);

			// Break each summary by newline
			content.add(new Separator(), 0, content.getRowCount(), 2, 1);
		}
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

		@Override
		public void appendSummary(Node node) {
			add(node, 0, getRowCount(), 2, 1);
		}

		@Override
		public void appendSummary(Node left, Node right) {
			int row = getRowCount();
			add(left, 0, row);
			add(right, 1, row);
		}
	}
}
