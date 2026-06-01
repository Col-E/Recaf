package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.services.analysis.structure.AreaAnalysisResult;
import software.coley.recaf.services.analysis.structure.AreaAnalysisService;
import software.coley.recaf.services.analysis.structure.AreaGroup;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Comparator;

/**
 * Summarizer that previews application areas.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AreaAnalysisSummarizer implements ResourceSummarizer {
	private final AreaAnalysisService areaAnalysisService;
	private final Actions actions;

	@Inject
	public AreaAnalysisSummarizer(@Nonnull AreaAnalysisService areaAnalysisService,
	                              @Nonnull Actions actions) {
		this.areaAnalysisService = areaAnalysisService;
		this.actions = actions;
	}

	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		AreaAnalysisResult result = areaAnalysisService.analyze(workspace, resource);
		if (result.analyzedClassCount() == 0 && result.groupCount() == 0)
			return false;

		FxThreadUtil.run(() -> {
			Label title = new BoundLabel(Lang.getBinding("service.analysis.areas"));
			title.getStyleClass().addAll(Styles.TITLE_4);
			consumer.appendSummary(title);

			Label counts = new Label(formatCounts(result));
			counts.getStyleClass().add(Styles.TEXT_SUBTLE);
			ActionButton open = new ActionButton(CarbonIcons.CHART_CUSTOM, Lang.getBinding("dialog.file.open"),
					() -> actions.openAreaAnalysis(resource, result));
			consumer.appendSummary(box(open, counts));

			if (!result.groups().isEmpty()) {
				Label previewTitle = new BoundLabel(Lang.getBinding("service.analysis.areas.preview.header"));
				previewTitle.getStyleClass().add(Styles.TEXT_BOLD);
				consumer.appendSummary(previewTitle);

				result.groups().stream()
						.sorted(Comparator
								.comparingInt((AreaGroup group) -> group.classes().size()).reversed()
								.thenComparingInt(AreaGroup::id))
						.limit(3)
						.map(AreaAnalysisSummarizer::formatGroupPreview)
						.map(Label::new)
						.forEach(consumer::appendSummary);
			}
		});
		return true;
	}

	@Override
	public int getPriority() {
		return PRIORITY_AREA_ANALYSIS;
	}

	@Nonnull
	private static String formatCounts(@Nonnull AreaAnalysisResult result) {
		String text = String.format(Lang.get("service.analysis.areas.status"),
				result.analyzedClassCount(),
				result.groupCount(),
				result.linkCount());
		if (result.spaghettiDetected())
			text += ", " + Lang.get("service.analysis.areas.warning.short");
		return text;
	}

	@Nonnull
	private static String formatGroupPreview(@Nonnull AreaGroup group) {
		return String.format("#%d - %d classes - %s - %s - %d%%",
				group.id(),
				group.classes().size(),
				group.purpose(),
				group.formationKind().name(),
				Math.round(group.confidence() * 100));
	}

	@Nonnull
	private static Node box(@Nonnull Node left, @Nonnull Node right) {
		HBox box = new HBox(left, right);
		box.setSpacing(10);
		box.setAlignment(Pos.CENTER_LEFT);
		return new VBox(box);
	}
}
