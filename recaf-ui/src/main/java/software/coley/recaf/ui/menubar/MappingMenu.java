package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.Stage;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingHelper;
import software.coley.recaf.services.mapping.aggregate.AggregateMappingManager;
import software.coley.recaf.services.mapping.format.MappingFileFormat;
import software.coley.recaf.services.mapping.format.MappingFormatManager;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.window.MappingApplicationWindow;
import software.coley.recaf.ui.window.MappingGeneratorWindow;
import software.coley.recaf.util.FileChooserBundle;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;

import java.io.File;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import static software.coley.recaf.util.Lang.getBinding;
import static software.coley.recaf.util.Menus.*;

/**
 * Mapping menu component for {@link MainMenu}.
 *
 * @author Matt Coley
 */
@Dependent
public class MappingMenu extends WorkspaceAwareMenu {
	private static final Logger logger = Logging.get(MappingMenu.class);
	private final WindowManager windowManager;
	private final MappingFormatManager formatManager;
	private final MappingHelper mappingHelper;

	@Inject
	public MappingMenu(@Nonnull WindowManager windowManager,
	                   @Nonnull WorkspaceManager workspaceManager,
	                   @Nonnull AggregateMappingManager aggregateMappingManager,
	                   @Nonnull MappingFormatManager formatManager,
	                   @Nonnull MappingHelper mappingHelper,
	                   @Nonnull Instance<MappingGeneratorWindow> generatorWindowProvider,
	                   @Nonnull Instance<MappingApplicationWindow> applyWindowProvider,
	                   @Nonnull RecentFilesConfig recentFiles) {
		super(workspaceManager);

		this.windowManager = windowManager;
		this.formatManager = formatManager;
		this.mappingHelper = mappingHelper;

		textProperty().bind(getBinding("menu.mappings"));
		setGraphic(new FontIconView(CarbonIcons.MAP_BOUNDARY));

		FileChooserBundle choosers = FileChooserBundle.fromRecent(recentFiles);

		Menu apply = menu("menu.mappings.apply", CarbonIcons.DOCUMENT_IMPORT);
		Menu export = menu("menu.mappings.export", CarbonIcons.DOCUMENT_EXPORT);
		Map<MappingFileFormat, MenuItem> formatToExportAsItems = new IdentityHashMap<>();
		for (String formatName : formatManager.getMappingFileFormats()) {
			// Temp instance to check for special cases.
			MappingFileFormat tmpFormat = formatManager.createFormatInstance(formatName);
			if (tmpFormat == null) continue;

			MappingMenuItems mappingItems = createMappingItems(formatName, choosers);
			apply.getItems().add(mappingItems.importAsItem());
			if (tmpFormat.supportsExportText()) {
				export.getItems().add(mappingItems.exportAsItem());
				formatToExportAsItems.put(tmpFormat, mappingItems.exportAsItem());
			} else {
				MenuItem item = new MenuItem();
				item.textProperty().bind(Lang.format("menu.mappings.export.unsupported", formatName));
				item.setGraphic(new FontIconView(CarbonIcons.CLOSE));
				item.setDisable(true);
				export.getItems().add(item);
			}
		}

		getItems().addAll(apply, export,
				action("menu.mappings.generate", CarbonIcons.LICENSE_MAINTENANCE, () -> openGenerate(generatorWindowProvider)),
				action("menu.mappings.view", CarbonIcons.VIEW, this::openView),
				new SeparatorMenuItem(),
				action("menu.mappings.apply-advanced", CarbonIcons.LICENSE_GLOBAL, () -> openApply(applyWindowProvider))
		);

		// Disable if attached via agent, or there is no workspace
		disableProperty().bind(hasAgentWorkspace.or(hasWorkspace.not()));

		// Disable formats that require field type differentiation if we have aggregate data that does not have differentiation.
		aggregateMappingManager.addAggregatedMappingsListener(mappings -> {
			FxThreadUtil.run(() -> {
				for (var formatItemEntry : formatToExportAsItems.entrySet()) {
					if (formatItemEntry.getKey().doesSupportFieldTypeDifferentiation())
						formatItemEntry.getValue().setDisable(mappings.isMissingFieldDescriptors());
				}
			});
		});
		workspaceManager.addWorkspaceCloseListener(closedWorkspace -> {
			// Re-enable when closing so the next workspace has a clean slate
			formatToExportAsItems.values().forEach(i -> i.setDisable(false));
		});
	}

	@Nonnull
	private MappingMenuItems createMappingItems(@Nonnull String formatName,
	                                            @Nonnull FileChooserBundle choosers) {
		MappingFileFormat format = Objects.requireNonNull(formatManager.createFormatInstance(formatName),
				"Failed creating mapping format instance for: " + formatName);
		MenuItem importAsItem = actionLiteral(formatName, CarbonIcons.LICENSE, () -> {
			// Show the prompt, load the mappings text ant attempt to load them.
			File file = choosers.showFileOpen(windowManager.getMainWindow());
			if (file != null) {
				try {
					IntermediateMappings mappings = mappingHelper.parse(format, file.toPath());
					mappingHelper.applyMappings(format, mappings);
				} catch (Throwable t) {
					logger.error("Failed importing mappings from {}", file.getName(), t);
				}
			}
		});
		MenuItem exportAsItem = actionLiteral(formatName, CarbonIcons.LICENSE, () -> {
			// Show the prompt, write current mappings to the given path.
			File file = choosers.showFileExport(windowManager.getMainWindow());
			if (file != null) {
				mappingHelper.exportMappingsFile(format, file.toPath());
			}
		});
		return new MappingMenuItems(importAsItem, exportAsItem);
	}

	private void openGenerate(@Nonnull Instance<MappingGeneratorWindow> generatorWindowProvider) {
		MappingGeneratorWindow window = generatorWindowProvider.get();
		window.setOnCloseRequest(e -> generatorWindowProvider.destroy(window));
		window.show();
		window.requestFocus();
		windowManager.registerAnonymous(window);
	}

	private void openView() {
		Stage window = windowManager.getMappingPreviewWindow();
		window.show();
		window.requestFocus();
	}

	private void openApply(@Nonnull Instance<MappingApplicationWindow> applyWindowProvider) {
		MappingApplicationWindow window = applyWindowProvider.get();
		window.setOnCloseRequest(e -> applyWindowProvider.destroy(window));
		window.show();
		window.requestFocus();
		windowManager.registerAnonymous(window);
	}

	private record MappingMenuItems(@Nonnull MenuItem importAsItem, @Nullable MenuItem exportAsItem) {}
}
