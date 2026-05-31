package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.services.analysis.antitamper.AntiReversalAnalysisService;
import software.coley.recaf.services.analysis.antitamper.IllegalNameAnalysis;
import software.coley.recaf.services.analysis.antitamper.IllegalNameAntiReversalAnalyzer;
import software.coley.recaf.services.analysis.antitamper.TransformerImpactAnalysis;
import software.coley.recaf.services.analysis.antitamper.TransformerImpactAntiReversalAnalyzer;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.pane.mapping.MappingGeneratorPane;
import software.coley.recaf.ui.window.MappingGeneratorWindow;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Summarizer that allows patching of common anti-decompilation tricks.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AntiDecompilationSummarizer implements ResourceSummarizer {
	private static final int BUTTON_WIDTH = 210;
	private static final Logger logger = Logging.get(AntiDecompilationSummarizer.class);
	private final CellConfigurationService cellConfigurationService;
	private final Actions actions;
	private final AntiReversalAnalysisService antiReversalAnalysisService;
	private final Instance<MappingGeneratorWindow> generatorWindowProvider;

	@Inject
	public AntiDecompilationSummarizer(@Nonnull CellConfigurationService cellConfigurationService,
									   @Nonnull Actions actions,
	                                   @Nonnull AntiReversalAnalysisService antiReversalAnalysisService,
	                                   @Nonnull Instance<MappingGeneratorWindow> generatorWindowProvider) {
		this.cellConfigurationService = cellConfigurationService;
		this.actions = actions;
		this.antiReversalAnalysisService = antiReversalAnalysisService;
		this.generatorWindowProvider = generatorWindowProvider;
	}

	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		TransformerImpactAnalysis transformAnalysis =
				antiReversalAnalysisService.analyze(workspace, resource, TransformerImpactAntiReversalAnalyzer.class);
		IllegalNameAnalysis illegalNameAnalysis =
				antiReversalAnalysisService.analyze(workspace, resource, IllegalNameAntiReversalAnalyzer.class);

		// TODO: We let the AntiReversalAnalysisService register custom analyzers but we don't have a way to really
		//  handle them here in a way that lets us produce a nice UI for them unless we know about them ahead of time
		//  like the existing core analyzers. For now, plugins will need to provide their own summarizer rather than
		//  be included here even if they offer the same intent.

		// Skip if there is no anti-decompilation work to be done.
		JvmTransformResult transformResult = transformAnalysis.jvm().result();
		if (transformResult == null)
			return false;
		int illegalNameCount = illegalNameAnalysis.classesWithIllegalNames().size();
		int transformCount = transformResult.getTransformedClasses().size() + transformResult.getClassesToRemove().size();
		boolean hasIllegalNames = illegalNameCount > 0;
		boolean hasTransformations = transformCount > 0;
		if (!hasIllegalNames && !hasTransformations)
			return false;

		// We have actions to take, create UI to apply patches.
		FxThreadUtil.run(() -> {
			ExecutorService service = ThreadPoolFactory.newSingleThreadExecutor("anti-decompile-patching");
			Label title = new BoundLabel(Lang.getBinding("service.analysis.anti-decompile"));
			title.getStyleClass().addAll(Styles.TITLE_4);
			consumer.appendSummary(title);
			if (hasTransformations) {
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
					showAffectedClassesPopover(label, affectedClasses);
				});

				Button action = new ActionButton(CarbonIcons.CLEAN, Lang.format("service.analysis.anti-decompile.illegal-attr", transformCount), transformResult::apply)
						.width(BUTTON_WIDTH).once().async(service);
				consumer.appendSummary(box(action, label));
			}
			if (hasIllegalNames) {
				List<ClassPathNode> affectedClasses = illegalNameAnalysis.classesWithIllegalNames().stream()
						.sorted(Comparator.comparing((ClassPathNode path) -> path.getValue().getName()))
						.collect(Collectors.toList());

				Hyperlink label = new Hyperlink();
				label.textProperty().bind(Lang.format("service.analysis.anti-decompile.label-patch", illegalNameCount));
				label.setOnAction(e -> {
					label.setVisited(false);
					showAffectedClassesPopover(label, affectedClasses);
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

				consumer.appendSummary(box(action, label));
			}
		});
		return true;
	}

	@Nonnull
	private static Node box(@Nonnull Node left, @Nonnull Node right) {
		HBox box = new HBox(left, right);
		box.setSpacing(10);
		box.setAlignment(Pos.CENTER_LEFT);
		return box;
	}

	private void showAffectedClassesPopover(@Nonnull Hyperlink owner, @Nonnull List<ClassPathNode> affectedClasses) {
		Popover popover = new Popover(createAffectedClassesList(affectedClasses));
		popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
		popover.setAutoHide(true);
		popover.show(owner);
	}

	@Nonnull
	private Node createAffectedClassesList(@Nonnull List<ClassPathNode> affectedClasses) {
		ListView<ClassPathNode> list = new ListView<>(FXCollections.observableArrayList(affectedClasses));
		list.setCellFactory(param -> new ListCell<>() {
			@Override
			protected void updateItem(ClassPathNode item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					setOnMouseClicked(null);
				} else {
					setText(cellConfigurationService.textOf(item));
					setGraphic(cellConfigurationService.graphicOf(item));
					setOnMouseClicked(_ -> {
						try {
							actions.gotoDeclaration(item);
						} catch (IncompletePathException ex) {
							// Should realistically never happen
							logger.warn("Cannot goto location, path incomplete", ex);
						}
					});
				}
			}
		});
		list.setFocusTraversable(false);
		list.setPrefWidth(400);
		list.setPrefHeight(Math.min(affectedClasses.size(), 8) * 30 + 2);
		return list;
	}
}
