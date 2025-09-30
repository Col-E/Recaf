package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.deobfuscation.transform.generic.CycleClassRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.DuplicateAnnotationRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalAnnotationRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalSignatureRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.LongAnnotationRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.LongExceptionRemovingTransformer;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.services.mapping.gen.filter.IncludeKeywordNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeNonAsciiNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeNonJavaIdentifierNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeWhitespaceNameFilter;
import software.coley.recaf.services.mapping.gen.filter.NameGeneratorFilter;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.services.transform.TransformationApplierService;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.services.window.WindowFactory;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.pane.MappingGeneratorPane;
import software.coley.recaf.ui.window.MappingGeneratorWindow;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static software.coley.recaf.util.Lang.getBinding;

/**
 * Summarizer that allows patching of common anti-decompilation tricks.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AntiDecompilationSummarizer implements ResourceSummarizer {
	private static final int BUTTON_WIDTH = 210;
	private static final NameGeneratorFilter ILLEGAL_NAME_FILTER =
			new IncludeWhitespaceNameFilter(new IncludeNonAsciiNameFilter(new IncludeKeywordNameFilter(new IncludeNonJavaIdentifierNameFilter(null))));
	private static final Logger logger = Logging.get(AntiDecompilationSummarizer.class);
	private final TransformationApplierService transformationApplierService;
	private final Instance<MappingGeneratorWindow> generatorWindowProvider;
	private final WindowFactory windowFactory;

	@Inject
	public AntiDecompilationSummarizer(@Nonnull TransformationApplierService transformationApplierService,
	                                   @Nonnull Instance<MappingGeneratorWindow> generatorWindowProvider,
	                                   @Nonnull WindowFactory windowFactory) {
		this.generatorWindowProvider = generatorWindowProvider;
		this.transformationApplierService = transformationApplierService;
		this.windowFactory = windowFactory;
	}

	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		// Transform workspace with a number of strictly anti-crasher transformers.
		// Let the user determine if they want to apply the results.
		JvmTransformResult transformResult = computeTransformations(workspace);

		// Detect if we should show the prompt to show the pre-populated mapping generator panel for illegally named classes.
		int illegalNameCount = computeIllegalNames(resource);

		// Skip if there is no anti-decompilation work to be done.
		boolean hasIllegalNames = illegalNameCount > 0;
		boolean hasTransformations = (transformResult != null &&
				(!transformResult.getClassesToRemove().isEmpty() || !transformResult.getTransformedClasses().isEmpty()));
		if (!hasIllegalNames && !hasTransformations)
			return false;

		// We have actions to take, create UI to apply patches.
		FxThreadUtil.run(() -> {
			ExecutorService service = ThreadPoolFactory.newSingleThreadExecutor("anti-decompile-patching");
			Label title = new BoundLabel(Lang.getBinding("service.analysis.anti-decompile"));
			title.getStyleClass().addAll(Styles.TITLE_4);
			consumer.appendSummary(title);
			if (hasTransformations) {
				int transformCount = transformResult.getTransformedClasses().size() + transformResult.getClassesToRemove().size();
				Label label = new BoundLabel(Lang.format("service.analysis.anti-decompile.label-patch", transformCount));
				Button action = new ActionButton(CarbonIcons.CLEAN, Lang.format("service.analysis.anti-decompile.illegal-attr", transformCount), transformResult::apply)
						.width(BUTTON_WIDTH).once().async(service);
				consumer.appendSummary(box(action, label));
			}
			if (hasIllegalNames) {
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
				Label label = new BoundLabel(Lang.format("service.analysis.anti-decompile.label-patch", illegalNameCount));
				consumer.appendSummary(box(action, label));
			}
		});
		return true;
	}

	@Nullable
	private JvmTransformResult computeTransformations(@Nonnull Workspace workspace) {
		JvmTransformResult transformResult;
		try {
			return transformationApplierService.newApplier(workspace).transformJvm(List.of(
					// Remove classes with cycles in inheritance
					CycleClassRemovingTransformer.class,

					// Remove illegally formed annotations
					IllegalAnnotationRemovingTransformer.class,

					// Remove illegally formed generic signatures
					IllegalSignatureRemovingTransformer.class,

					// Remove bogus duplicate annotations
					DuplicateAnnotationRemovingTransformer.class,

					// Remove annoying long annotations
					LongAnnotationRemovingTransformer.class,

					// Remove annoying long exceptions
					LongExceptionRemovingTransformer.class
			));
		} catch (TransformationException ex) {
			logger.error("Failed applying anti-decompilation transformers", ex);
			return null;
		}
	}

	private int computeIllegalNames(@Nonnull WorkspaceResource resource) {
		Set<JvmClassInfo> classesWithIllegalNames = Collections.newSetFromMap(new IdentityHashMap<>());
		resource.jvmClassBundleStream().forEach(bundle -> {
			bundle.forEach(cls -> {
				if (ILLEGAL_NAME_FILTER.shouldMapClass(cls)) {
					classesWithIllegalNames.add(cls);
					return;
				}
				for (FieldMember field : cls.getFields()) {
					if (ILLEGAL_NAME_FILTER.shouldMapField(cls, field)) {
						classesWithIllegalNames.add(cls);
						return;
					}
				}
				for (MethodMember method : cls.getMethods()) {
					if (ILLEGAL_NAME_FILTER.shouldMapMethod(cls, method)) {
						classesWithIllegalNames.add(cls);
						return;
					}
				}
			});
		});
		return classesWithIllegalNames.size();
	}

	@Nonnull
	private static Node box(@Nonnull Node left, @Nonnull Node right) {
		HBox box = new HBox(left, right);
		box.setSpacing(10);
		box.setAlignment(Pos.CENTER_LEFT);
		return box;
	}
}
