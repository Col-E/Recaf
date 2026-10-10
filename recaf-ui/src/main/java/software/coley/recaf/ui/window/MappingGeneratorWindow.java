package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.mapping.MappingApplierService;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.pane.mapping.MappingGeneratorPane;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.util.concurrent.CompletableFuture;

/**
 * Window wrapper for {@link MappingGeneratorPane}.
 *
 * @author Matt Coley
 * @see MappingGeneratorPane
 */
@Dependent
public class MappingGeneratorWindow extends RecafStage {
	private static final Logger logger = Logging.get(MappingGeneratorWindow.class);
	private final MappingGeneratorPane generatorPane;
	private final Instance<MappingPreviewWindow> previewWindowProvider;
	private final MappingApplierService mappingApplierService;
	private final WindowManager windowManager;

	@Inject
	public MappingGeneratorWindow(@Nonnull MappingGeneratorPane generatorPane,
	                              @Nonnull Instance<MappingPreviewWindow> previewWindowProvider,
	                              @Nonnull MappingApplierService mappingApplierService,
	                              @Nonnull WindowManager windowManager) {
		this.generatorPane = generatorPane;
		this.previewWindowProvider = previewWindowProvider;
		this.mappingApplierService = mappingApplierService;
		this.windowManager = windowManager;
		generatorPane.setApplyCallback(this::close);
		generatorPane.setPreviewCallback(this::previewMappings);

		// Layout
		titleProperty().bind(Lang.getBinding("mapgen"));
		setMinWidth(450);
		setMinHeight(300);
		setScene(new RecafScene(generatorPane, 800, 600));
	}

	private void previewMappings(@Nonnull Workspace workspace, @Nonnull Mappings mappings) {
		generatorPane.setPreviewRunning(true);
		CompletableFuture.supplyAsync(() -> mappingApplierService.inWorkspace(workspace).applyToPrimaryResource(mappings), ThreadUtil.executor())
				.whenCompleteAsync((results, error) -> {
					generatorPane.setPreviewRunning(false);
					if (error != null) {
						logger.error("Failed previewing generated mappings", error);
						ErrorDialogs.show(Lang.get("mapapply.preview.error.title"), Lang.get("mapapply.preview.error.header"),
								Lang.get("mapapply.preview.error.content"), error);
						return;
					}

					MappingPreviewWindow previewWindow = previewWindowProvider.get();
					try {
						previewWindow.setMappingResults(workspace, results);
						previewWindow.setOnCloseRequest(e -> previewWindowProvider.destroy(previewWindow));
						previewWindow.show();
						previewWindow.requestFocus();
						windowManager.registerAnonymous(previewWindow);
					} catch (Throwable t) {
						previewWindowProvider.destroy(previewWindow);
						logger.error("Failed opening mapping preview for generated mappings", t);
						ErrorDialogs.show(Lang.get("mapapply.preview.error.title"), Lang.get("mapapply.preview.error.header"),
								Lang.get("mapapply.preview.error.content"), t);
					}
				}, FxThreadUtil.executor());
	}

	@Nonnull
	public MappingGeneratorPane getGeneratorPane() {
		return generatorPane;
	}
}
