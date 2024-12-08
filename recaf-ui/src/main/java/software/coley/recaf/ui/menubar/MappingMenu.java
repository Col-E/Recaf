package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingApplierService;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.services.mapping.aggregate.AggregateMappingManager;
import software.coley.recaf.services.mapping.aggregate.AggregatedMappings;
import software.coley.recaf.services.mapping.format.MappingFileFormat;
import software.coley.recaf.services.mapping.format.MappingFormatManager;
import software.coley.recaf.services.window.WindowFactory;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.pane.MappingGeneratorPane;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.util.FileChooserBuilder;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.ThreadPoolFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;

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
	private final ExecutorService exportPool = ThreadPoolFactory.newSingleThreadExecutor("mapping-export");
	private final ExecutorService importPool = ThreadPoolFactory.newSingleThreadExecutor("mapping-import");
	private final WindowManager windowManager;
	private final WindowFactory windowFactory;

	@Inject
	public MappingMenu(@Nonnull WindowManager windowManager,
	                   @Nonnull WindowFactory windowFactory,
	                   @Nonnull WorkspaceManager workspaceManager,
	                   @Nonnull AggregateMappingManager aggregateMappingManager,
	                   @Nonnull MappingFormatManager formatManager,
	                   @Nonnull MappingApplierService mappingApplierService,
	                   @Nonnull Instance<MappingGeneratorPane> generatorPaneInstance,
	                   @Nonnull RecentFilesConfig recentFiles) {
		super(workspaceManager);

		this.windowManager = windowManager;
		this.windowFactory = windowFactory;

		textProperty().bind(getBinding("menu.mappings"));
		setGraphic(new FontIconView(CarbonIcons.MAP_BOUNDARY));

		Menu apply = menu("menu.mappings.apply", CarbonIcons.DOCUMENT_IMPORT);
		Menu export = menu("menu.mappings.export", CarbonIcons.DOCUMENT_EXPORT);

		// Use a shared file-chooser for mapping menu actions.
		// That way there is some continuity when working with mappings.
		FileChooser chooser = new FileChooserBuilder()
				.setInitialDirectory(recentFiles.getLastWorkspaceOpenDirectory())
				.setTitle(Lang.get("dialog.file.open"))
				.build();

		for (String formatName : formatManager.getMappingFileFormats()) {
			apply.getItems().add(actionLiteral(formatName, CarbonIcons.LICENSE, () -> {
				// Show the prompt, load the mappings text ant attempt to load them.
				File file = chooser.showOpenDialog(windowManager.getMainWindow());
				if (file != null) {
					importPool.submit(() -> {
						try {
							MappingFileFormat format = formatManager.createFormatInstance(formatName);
							String mappingsText = Files.readString(file.toPath());
							IntermediateMappings parsedMappings = format.parse(mappingsText);
							logger.info("Loaded mappings from {} in {} format", file.getName(), formatName);

							MappingResults results = mappingApplierService.inCurrentWorkspace().applyToPrimaryResource(parsedMappings);
							results.apply();
							logger.info("Applied mappings from {} - Updated {} classes", file.getName(), results.getPostMappingPaths().size());
						} catch (Exception ex) {
							logger.error("Failed to read {} mappings from {}", formatName, file.getName(), ex);
						}
					});
				}
			}));

			// Temp instance to check for export support.
			MappingFileFormat tmp = formatManager.createFormatInstance(formatName);
			if (tmp == null) continue;
			if (tmp.supportsExportText()) {
				export.getItems().add(actionLiteral(formatName, CarbonIcons.LICENSE, () -> {
					// Show the prompt, write current mappings to the given path.
					File file = chooser.showSaveDialog(windowManager.getMainWindow());
					if (file != null) {
						exportPool.submit(() -> {
							try {
								AggregatedMappings mappings = aggregateMappingManager.getAggregatedMappings();
								MappingFileFormat format = formatManager.createFormatInstance(formatName);
								if (format != null) {
									String mappingsText = format.exportText(mappings);
									if (mappingsText != null) {
										Files.writeString(file.toPath(), mappingsText);
										logger.info("Exporting mappings to {} in {} format", file.getName(), formatName);
									} else {
										// We already checked for export support, so this should never happen
										throw new IllegalStateException("Mapping export shouldn't be null for format: " + formatName);
									}
								} else {
									throw new IllegalStateException("Format was unregistered: " + formatName);
								}
							} catch (Exception ex) {
								logger.error("Failed to write mappings in {} format to {}",
										formatName, file.getName(), ex);
							}
						});
					}
				}));
			} else {
				MenuItem item = new MenuItem();
				item.textProperty().bind(Lang.formatLiterals("menu.mappings.export.unsupported", formatName));
				item.setGraphic(new FontIconView(CarbonIcons.CLOSE));
				item.setDisable(true);
				export.getItems().add(item);
			}
		}

		getItems().add(apply);
		getItems().add(export);

		getItems().add(action("menu.mappings.generate", CarbonIcons.LICENSE_MAINTENANCE,
				() -> openGenerate(generatorPaneInstance)));
		getItems().add(action("menu.mappings.view", CarbonIcons.VIEW, this::openView));

		// Disable if attached via agent, or there is no workspace
		disableProperty().bind(hasAgentWorkspace.or(hasWorkspace.not()));
	}

	private void openGenerate(@Nonnull Instance<MappingGeneratorPane> generatorPaneInstance) {
		RecafScene scene = new RecafScene(generatorPaneInstance.get());
		Stage window = windowFactory.createAnonymousStage(scene, getBinding("mapgen"), 800, 400);
		window.show();
		window.requestFocus();
	}

	private void openView() {
		Stage window = windowManager.getMappingPreviewWindow();
		window.show();
		window.requestFocus();
	}
}
