package software.coley.recaf.services.info.summary.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.analysis.antitamper.AntiReversalAnalysisResult;
import software.coley.recaf.services.analysis.antitamper.IllegalNameAnalysis;
import software.coley.recaf.services.analysis.antitamper.IllegalNameAntiReversalAnalyzer;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.info.summary.AntiReversalResultPresenter;
import software.coley.recaf.services.info.summary.PresenterUtils;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.pane.mapping.MappingGeneratorPane;
import software.coley.recaf.ui.window.MappingGeneratorWindow;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Presents illegal-name anti-reversal results.
 *
 * @author Matt Coley
 * @see IllegalNameAntiReversalAnalyzer
 */
@ApplicationScoped
public class IllegalNameResultPresenter implements AntiReversalResultPresenter {
	public static final int PRIORITY = -90;
	private static final int BUTTON_WIDTH = 210;

	private static final Logger logger = Logging.get(IllegalNameResultPresenter.class);

	private final CellConfigurationService cellConfigurationService;
	private final Actions actions;
	private final Instance<MappingGeneratorWindow> generatorWindowProvider;

	@Inject
	public IllegalNameResultPresenter(@Nonnull CellConfigurationService cellConfigurationService,
	                                  @Nonnull Actions actions,
	                                  @Nonnull Instance<MappingGeneratorWindow> generatorWindowProvider) {
		this.cellConfigurationService = cellConfigurationService;
		this.actions = actions;
		this.generatorWindowProvider = generatorWindowProvider;
	}

	@Nonnull
	@Override
	public String getAnalyzerId() {
		return IllegalNameAntiReversalAnalyzer.SERVICE_ID;
	}

	@Nonnull
	@Override
	public Class<? extends AntiReversalAnalysisResult> getResultType() {
		return IllegalNameAnalysis.class;
	}

	@Override
	public int getPriority() {
		return PRIORITY;
	}

	@Override
	public boolean isApplicable(@Nonnull AntiReversalAnalysisResult result) {
		return result instanceof IllegalNameAnalysis(List<ClassPathNode> classesWithIllegalNames)
				&& !classesWithIllegalNames.isEmpty();
	}

	@Override
	public void appendSummary(@Nonnull Workspace workspace,
	                          @Nonnull WorkspaceResource resource,
	                          @Nonnull AntiReversalAnalysisResult result,
	                          @Nonnull SummaryConsumer consumer,
	                          @Nonnull Executor actionExecutor) {
		IllegalNameAnalysis analysis = (IllegalNameAnalysis) result;
		List<ClassPathNode> affectedClasses = analysis.classesWithIllegalNames().stream()
				.sorted(Comparator.comparing((ClassPathNode path) -> path.getValue().getName()))
				.toList();

		Hyperlink label = new Hyperlink();
		label.textProperty().bind(Lang.format("service.analysis.anti-decompile.label-patch", affectedClasses.size()));
		label.setOnAction(e -> {
			label.setVisited(false);
			PresenterUtils.showClassListPopover(label, affectedClasses, cellConfigurationService, actions);
		});

		Button action = new ActionButton(CarbonIcons.LICENSE_MAINTENANCE, Lang.getBinding("service.analysis.anti-decompile.illegal-name"), () -> {
			CompletableFuture.runAsync(() -> {
				MappingGeneratorWindow window = generatorWindowProvider.get();

				MappingGeneratorPane mappingGeneratorPane = window.getGeneratorPane();
				mappingGeneratorPane.addConfiguredFilter(new MappingGeneratorPane.IncludeNonAsciiNames());
				mappingGeneratorPane.addConfiguredFilter(new MappingGeneratorPane.IncludeKeywordNames());
				mappingGeneratorPane.addConfiguredFilter(new MappingGeneratorPane.IncludeWhitespaceNames());
				mappingGeneratorPane.addConfiguredFilter(new MappingGeneratorPane.IncludeNonJavaIdentifierNames());
				mappingGeneratorPane.addConfiguredFilter(new MappingGeneratorPane.IncludeLongName(400));
				mappingGeneratorPane.generate();

				window.setOnCloseRequest(e -> generatorWindowProvider.destroy(window));
				window.show();
				window.requestFocus();
			}, FxThreadUtil.executor()).exceptionally(t -> {
				logger.error("Failed to open mapping viewer", t);
				return null;
			});
		}).width(BUTTON_WIDTH);

		consumer.appendSummary(PresenterUtils.box(action, label));
	}
}
