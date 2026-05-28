package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.analysis.entry.EntryPoint;
import software.coley.recaf.services.analysis.entry.EntryPointKind;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

		if (entryPoints.isEmpty()) {
			batch.add(() -> consumer.appendSummary(new BoundLabel(Lang.getBinding("service.analysis.entry-points.none"))));
		} else {
			// Group entry-points by:
			//  - kind -> class -> members
			for (var kindEntry : groupEntries(entryPoints).entrySet()) {
				EntryPointKind kind = kindEntry.getKey();
				batch.add(() -> consumer.appendSummary(kindLabel(kind)));
				for (var classEntry : kindEntry.getValue().entrySet()) {
					batch.add(() -> consumer.appendSummary(pathLabel(classEntry.getKey(), 15)));
					for (ClassMemberPathNode memberPath : classEntry.getValue())
						batch.add(() -> consumer.appendSummary(pathLabel(memberPath, 30)));
				}
			}
		}

		batch.execute();

		return true;
	}

	@Nonnull
	private static Map<EntryPointKind, Map<ClassPathNode, List<ClassMemberPathNode>>> groupEntries(@Nonnull List<EntryPoint> entryPoints) {
		Map<EntryPointKind, Map<ClassPathNode, List<ClassMemberPathNode>>> grouped = new TreeMap<>(Comparator.comparing(EntryPointKind::id));
		for (EntryPoint entry : entryPoints) {
			Map<ClassPathNode, List<ClassMemberPathNode>> classes = grouped.computeIfAbsent(entry.kind(), ignored -> new TreeMap<>());
			List<ClassMemberPathNode> members = classes.computeIfAbsent(entry.classPath(), ignored -> new ArrayList<>());
			ClassMemberPathNode memberPath = entry.memberPath();
			if (memberPath != null && !members.contains(memberPath))
				members.add(memberPath);
		}
		return grouped;
	}

	@Nonnull
	private Label kindLabel(@Nonnull EntryPointKind kind) {
		String translationKey = "service.analysis.entry-points.kind." + kind.id();
		Label label = Lang.has(translationKey) ?
				new BoundLabel(Lang.getBinding(translationKey)) :
				new Label(kind.displayName());
		label.getStyleClass().add(Styles.TEXT_BOLD);
		return label;
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
