package software.coley.recaf.services.info.summary.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.analysis.antitamper.AntiReversalAnalysisResult;
import software.coley.recaf.services.analysis.antitamper.TransformerImpactAnalysis;
import software.coley.recaf.services.analysis.antitamper.TransformerImpactAntiReversalAnalyzer;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.info.summary.AntiReversalResultPresenter;
import software.coley.recaf.services.info.summary.PresenterUtils;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Presents transformer-impact anti-reversal results.
 *
 * @author Matt Coley
 * @see TransformerImpactAntiReversalAnalyzer
 */
@ApplicationScoped
public class TransformerImpactResultPresenter implements AntiReversalResultPresenter {
	public static final int PRIORITY = -100;
	private static final int BUTTON_WIDTH = 210;

	private final CellConfigurationService cellConfigurationService;
	private final Actions actions;

	@Inject
	public TransformerImpactResultPresenter(@Nonnull CellConfigurationService cellConfigurationService,
	                                        @Nonnull Actions actions) {
		this.cellConfigurationService = cellConfigurationService;
		this.actions = actions;
	}

	@Nonnull
	@Override
	public String getAnalyzerId() {
		return TransformerImpactAntiReversalAnalyzer.SERVICE_ID;
	}

	@Nonnull
	@Override
	public Class<? extends AntiReversalAnalysisResult> getResultType() {
		return TransformerImpactAnalysis.class;
	}

	@Override
	public int getPriority() {
		return PRIORITY;
	}

	@Override
	public boolean isApplicable(@Nonnull AntiReversalAnalysisResult result) {
		if (!(result instanceof TransformerImpactAnalysis analysis))
			return false;

		JvmTransformResult transformResult = analysis.jvm().result();
		if (transformResult == null)
			return false;

		return transformResult.getTransformedClasses().size() + transformResult.getClassesToRemove().size() > 0;
	}

	@Override
	public void appendSummary(@Nonnull Workspace workspace,
	                          @Nonnull WorkspaceResource resource,
	                          @Nonnull AntiReversalAnalysisResult result,
	                          @Nonnull SummaryConsumer consumer,
	                          @Nonnull Executor actionExecutor) {
		TransformerImpactAnalysis analysis = (TransformerImpactAnalysis) result;
		JvmTransformResult transformResult = analysis.jvm().result();
		if (transformResult == null)
			return;

		int transformCount = transformResult.getTransformedClasses().size() + transformResult.getClassesToRemove().size();
		List<ClassPathNode> affectedClasses = Stream.concat(
						transformResult.getTransformedClasses().keySet().stream(),
						transformResult.getClassesToRemove().stream())
				.distinct()
				.sorted(Comparator.comparing((ClassPathNode path) -> path.getValue().getName()))
				.collect(Collectors.toList());

		Hyperlink label = new Hyperlink();
		label.textProperty().bind(Lang.format("service.analysis.anti-decompile.label-patch", transformCount));
		label.setOnAction(e -> {
			label.setVisited(false);
			PresenterUtils.showClassListPopover(label, affectedClasses,
					cellConfigurationService, actions);
		});

		Button action = new ActionButton(CarbonIcons.CLEAN,
				Lang.format("service.analysis.anti-decompile.illegal-attr", transformCount),
				transformResult::apply)
				.width(BUTTON_WIDTH)
				.once()
				.async(actionExecutor);
		consumer.appendSummary(PresenterUtils.box(action, label));
	}
}
