package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.analysis.structure.EntryPointGroup;
import software.coley.recaf.services.analysis.structure.FlowAnalysisService;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.Batch;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Summarizer that shows entry-points.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class EntryPointSummarizer implements ResourceSummarizer {
	private final FlowAnalysisService flowAnalysisService;
	private final CellConfigurationService cellConfigurationService;
	private final Actions actions;

	@Inject
	public EntryPointSummarizer(@Nonnull FlowAnalysisService flowAnalysisService,
	                            @Nonnull CellConfigurationService cellConfigurationService,
	                            @Nonnull Actions actions) {
		this.flowAnalysisService = flowAnalysisService;
		this.cellConfigurationService = cellConfigurationService;
		this.actions = actions;
	}

	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		var entryPoints = flowAnalysisService.findEntryPointGroups(workspace, resource);
		Batch batch = FxThreadUtil.batch();
		batch.add(() -> {
			Label title = new BoundLabel(Lang.getBinding("service.analysis.entry-points"));
			title.getStyleClass().addAll(Styles.TITLE_4);
			consumer.appendSummary(title);
		});
		for (EntryPointGroup group : entryPoints) {
			batch.add(() -> consumer.appendSummary(pathLabel(group.classPath(), 0)));
			for (ClassMemberPathNode memberPath : group.memberEntryPoints())
				batch.add(() -> consumer.appendSummary(pathLabel(memberPath, 15)));
		}

		if (entryPoints.isEmpty())
			batch.add(() -> consumer.appendSummary(new BoundLabel(Lang.getBinding("service.analysis.entry-points.none"))));

		batch.execute();

		return true;
	}

	@Nonnull
	private Label pathLabel(@Nonnull PathNode<?> path, int leftPadding) {
		Label label = new Label(cellConfigurationService.textOf(path), cellConfigurationService.graphicOf(path));
		label.setCursor(Cursor.HAND);
		label.setPadding(new Insets(2, 2, 2, leftPadding));
		label.setOnMouseEntered(e -> label.getStyleClass().add(Styles.TEXT_UNDERLINED));
		label.setOnMouseExited(e -> label.getStyleClass().remove(Styles.TEXT_UNDERLINED));
		label.setOnMouseClicked(e -> {
			try {
				actions.gotoDeclaration(path);
			} catch (IncompletePathException ex) {
				throw new IllegalStateException("Cannot navigate incomplete entry-point path", ex);
			}
		});
		return label;
	}
}
