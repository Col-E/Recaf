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
import software.coley.recaf.services.analysis.entry.EntryPoint;
import software.coley.recaf.services.analysis.entry.EntryAnalysisService;
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
	private final EntryAnalysisService entryAnalysisService;
	private final CellConfigurationService cellConfigurationService;
	private final Actions actions;

	@Inject
	public EntryPointSummarizer(@Nonnull EntryAnalysisService entryAnalysisService,
	                            @Nonnull CellConfigurationService cellConfigurationService,
	                            @Nonnull Actions actions) {
		this.entryAnalysisService = entryAnalysisService;
		this.cellConfigurationService = cellConfigurationService;
		this.actions = actions;
	}

	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		var entryPoints = entryAnalysisService.findEntryPoints(workspace, resource);
		Batch batch = FxThreadUtil.batch();
		batch.add(() -> {
			Label title = new BoundLabel(Lang.getBinding("service.analysis.entry-points"));
			title.getStyleClass().addAll(Styles.TITLE_4);
			consumer.appendSummary(title);
		});

		// TODO: We now have a more flexible entry-point system, so we should look into being more descriptive
		//  with what kind of entry-point we are showing. For basic 'main' methods what we have is fine, but
		//  we should be able to differentiate between a JVM main method, an Android activity, or a Minecraft mod init method.
		for (EntryPoint entry : entryPoints) {
			batch.add(() -> consumer.appendSummary(pathLabel(entry.classPath(), 0)));
			ClassMemberPathNode memberPath = entry.memberPath();
			if (memberPath != null)
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
