package software.coley.recaf.ui.pane.mapping;

import atlantafx.base.controls.Spacer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingApplierService;
import software.coley.recaf.services.mapping.MappingHelper;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.UniqueKeyMappings;
import software.coley.recaf.services.mapping.format.EnigmaMappings;
import software.coley.recaf.services.mapping.format.MappingFileFormat;
import software.coley.recaf.services.mapping.format.MappingFormatManager;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.services.workspace.WorkspaceOpenListener;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.problem.Problem;
import software.coley.recaf.ui.control.richtext.problem.ProblemLevel;
import software.coley.recaf.ui.control.richtext.problem.ProblemPhase;
import software.coley.recaf.ui.control.richtext.problem.ProblemTracking;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.window.MappingPreviewWindow;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.FileChooserBundle;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.awt.Toolkit;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Pane for applying mappings to a workspace.
 *
 * @author Matt Coley
 */
@Dependent
public class MappingApplicationPane extends BorderPane {
	private static final Logger logger = Logging.get(MappingApplicationPane.class);
	private final ObjectProperty<IntermediateMappings> mappingsProperty = new SimpleObjectProperty<>();
	private final ObjectProperty<MappingFileFormat> mappingFormatProperty = new SimpleObjectProperty<>();
	private final BooleanProperty ignoreHierarchy = new SimpleBooleanProperty();
	private final ProblemTracking mappingProblems = new ProblemTracking();
	private final WorkspaceOpenListener workspaceOpenListener = workspace -> updateMissingClassProblems(mappingsProperty.get());
	private final WorkspaceCloseListener workspaceCloseListener = workspace -> mappingProblems.clear();
	private final MappingFormatManager formatManager;
	private final MappingHelper mappingHelper;
	private final MappingApplierService mappingApplierService;
	private final WindowManager windowManager;
	private final Instance<MappingPreviewWindow> previewWindowProvider;
	private final BooleanProperty previewRunning = new SimpleBooleanProperty();
	private final FileTypeSyntaxAssociationService languageAssociation;
	private final WorkspaceManager workspaceManager;
	private final RecentFilesConfig recentFilesConfig;
	private final Instance<SearchBar> searchBarProvider;
	private Runnable applyCallback;

	@Inject
	public MappingApplicationPane(@Nonnull MappingFormatManager formatManager,
	                              @Nonnull MappingHelper mappingHelper,
	                              @Nonnull MappingApplierService mappingApplierService,
	                              @Nonnull WindowManager windowManager,
	                              @Nonnull Instance<MappingPreviewWindow> previewWindowProvider,
	                              @Nonnull FileTypeSyntaxAssociationService languageAssociation,
	                              @Nonnull WorkspaceManager workspaceManager,
	                              @Nonnull RecentFilesConfig recentFilesConfig,
	                              @Nonnull Instance<SearchBar> searchBarProvider) {
		this.formatManager = formatManager;
		this.mappingHelper = mappingHelper;
		this.mappingApplierService = mappingApplierService;
		this.windowManager = windowManager;
		this.previewWindowProvider = previewWindowProvider;
		this.languageAssociation = languageAssociation;
		this.workspaceManager = workspaceManager;
		this.recentFilesConfig = recentFilesConfig;
		this.searchBarProvider = searchBarProvider;

		setBottom(createButtonBar());
		setCenter(createDisplay());
	}

	@PostConstruct
	private void installWorkspaceListeners() {
		workspaceManager.addWorkspaceOpenListener(workspaceOpenListener);
		workspaceManager.addWorkspaceCloseListener(workspaceCloseListener);
		updateMissingClassProblems(mappingsProperty.get());
	}

	@PreDestroy
	private void removeWorkspaceListeners() {
		workspaceManager.removeWorkspaceOpenListener(workspaceOpenListener);
		workspaceManager.removeWorkspaceCloseListener(workspaceCloseListener);
	}

	@Nonnull
	private Node createDisplay() {
		Editor editor = new Editor();
		editor.setProblemTracking(mappingProblems);
		editor.getRootLineGraphicFactory().addDefaultCodeGraphicFactories();
		editor.getCodeArea().setEditable(false);
		editor.setText("No mappings loaded");
		languageAssociation.configureEditorSyntax("enigma", editor);

		SearchBar searchBar = searchBarProvider.get();
		searchBar.install(editor);

		mappingsProperty.addListener((ob, old, cur) -> {
			if (cur == null) {
				editor.setText("No mappings loaded");
			} else {
				try {
					String mappingsText = mappingFormatProperty.get().exportText(cur);
					editor.setText(mappingsText);
				} catch (Throwable t) {
					editor.setText("Failed previewing mappings\n" + StringUtil.traceToString(t));
				}
			}

			updateMissingClassProblems(cur);
		});
		ignoreHierarchy.addListener((ob, old, cur) -> updateMissingClassProblems(mappingsProperty.get()));

		return editor;
	}

	private void updateMissingClassProblems(@Nullable IntermediateMappings mappings) {
		// Skip if 'ignore hierarchy' is enabled, as that will remove the need for class hierarchy information.
		mappingProblems.clear();
		if (mappings == null || ignoreHierarchy.get() || !workspaceManager.hasCurrentWorkspace())
			return;

		// Otherwise mappings rely on figuring out class hierarchies so that a mapping applied to a method
		// in one class of a family will propagate to the other classes in the family. This means you don't
		// need the mappings to cover every explicit case of that method for all overrides, but in exchange
		// require the class hierarchy to be present in the workspace.
		Workspace workspace = workspaceManager.getCurrent();
		List<String> missingClasses = mappings.getClassesWithMappings().stream()
				.filter(className -> workspace.findClass(className) == null)
				.toList();
		if (missingClasses.isEmpty())
			return;

		// Format all missing classes into a single message for the user to see.
		String classList = String.join("\n", missingClasses.stream().map(className -> " - " + className).toList());
		String message = Lang.get("mapapply.problem.missing-classes").formatted(missingClasses.size(), classList);
		mappingProblems.addItem(new Problem(-1, -1, 0, ProblemLevel.WARN, ProblemPhase.LINT, message));
	}

	@Nonnull
	private HBox createButtonBar() {
		// Dropdown of available formats
		List<MappingFileFormat> formats = formatManager.getMappingFileFormats().stream()
				.map(formatManager::createFormatInstance)
				.toList();
		ComboBox<MappingFileFormat> formatBox = new ComboBox<>(FXCollections.observableList(formats));
		formatBox.setConverter(new StringConverter<>() {
			@Override
			public String toString(MappingFileFormat format) {
				return format.implementationName();
			}

			@Override
			public MappingFileFormat fromString(String s) {
				return null;
			}
		});
		formatBox.setValue(formats.getFirst());
		mappingFormatProperty.bind(formatBox.valueProperty());
		BooleanProperty disableDirImport = new SimpleBooleanProperty();
		disableDirImport.bind(formatBox.valueProperty().map(f -> !(f instanceof EnigmaMappings)));

		// Settings menu
		//  - Allows tweaking of mapping application process parameters
		CheckMenuItem uniqueKeyed = new CheckMenuItem();
		ignoreHierarchy.bind(uniqueKeyed.selectedProperty());
		uniqueKeyed.textProperty().bind(Lang.getBinding("mapapply.settings.unique"));
		MenuButton settingsMenu = new MenuButton(null, new FontIconView(CarbonIcons.SETTINGS), uniqueKeyed);

		// Action buttons
		FileChooserBundle choosers = FileChooserBundle.fromRecent(recentFilesConfig);
		Button setMappingFile = new ActionButton(CarbonIcons.DOCUMENT, Lang.getBinding("mapapply.pick.file"), () -> {
			// Show the prompt, load the mappings text ant attempt to load them.
			File file = choosers.showFileOpen(getScene().getWindow());
			if (file != null) {
				try {
					IntermediateMappings mappings = mappingHelper.parse(formatBox.getValue(), file.toPath());
					mappingsProperty.set(mappings);
				} catch (Throwable t) {
					logger.error("Failed importing mappings from {}", file.getName(), t);
				}
			}
		});
		Button setMappingDir = new ActionButton(CarbonIcons.FOLDER, Lang.getBinding("mapapply.pick.dir"), () -> {
			// Handle implementation specific directory loading
			if (formatBox.getValue() instanceof EnigmaMappings enigmaMappings) {
				File file = choosers.showDirOpen(getScene().getWindow());
				if (file != null) {
					try {
						IntermediateMappings mappings = enigmaMappings.parse(file.toPath());
						logger.info("Loaded enigma directory mappings from {}", file.getName());
						mappingsProperty.set(mappings);
					} catch (Throwable t) {
						logger.error("Failed importing mappings from {}", file.getName(), t);
					}
				}
			} else {
				Toolkit.getDefaultToolkit().beep();
			}
		});
		setMappingFile.disableProperty().bind(mappingFormatProperty.isNull());
		setMappingDir.disableProperty().bind(mappingFormatProperty.isNull().or(disableDirImport));
		Button preview = new ActionButton(CarbonIcons.VIEW, Lang.getBinding("mapapply.preview"), () -> {
			Mappings mappings = getEffectiveMappings();
			if (mappings != null)
				previewMappings(formatBox.getValue(), mappings);
		});
		preview.disableProperty().bind(mappingsProperty.isNull().or(previewRunning));
		Button apply = new ActionButton(CarbonIcons.CAFE, Lang.getBinding("mapapply"), () -> {
			try {
				Mappings mappings = getEffectiveMappings();
				if (mappings == null)
					return;

				mappingHelper.applyMappings(formatBox.getValue(), mappings);
				if (applyCallback != null) applyCallback.run();
			} catch (Exception ex) {
				logger.error("Failed to read {} mappings", formatBox.getValue().implementationName(), ex);
			}
		});
		apply.disableProperty().bind(mappingsProperty.isNull());

		HBox buttonBar = new HBox(formatBox, setMappingFile, setMappingDir, new Spacer(), preview, apply, settingsMenu);
		buttonBar.setSpacing(5);
		buttonBar.setPadding(new Insets(5));
		return buttonBar;
	}

	@Nullable
	private Mappings getEffectiveMappings() {
		IntermediateMappings mappings = mappingsProperty.get();
		if (mappings == null)
			return null;
		return ignoreHierarchy.get() ? new UniqueKeyMappings(mappings) : mappings;
	}

	private void previewMappings(@Nonnull MappingFileFormat format, @Nonnull Mappings mappings) {
		if (!workspaceManager.hasCurrentWorkspace())
			return;

		Workspace workspace = workspaceManager.getCurrent();
		previewRunning.set(true);

		// Run the mapping application in a background thread, and then show the results in a new window on the FX thread.
		CompletableFuture.supplyAsync(() -> mappingApplierService.inWorkspace(workspace).applyToPrimaryResource(mappings), ThreadUtil.executor())
				.whenCompleteAsync((results, error) -> {
					previewRunning.set(false);

					if (error != null) {
						logger.error("Failed previewing {} mappings", format.implementationName(), error);
						ErrorDialogs.show(Lang.get("mapapply.preview.error.title"),
								Lang.get("mapapply.preview.error.header"),
								Lang.get("mapapply.preview.error.content"), error);
						return;
					}

					MappingPreviewWindow window = previewWindowProvider.get();
					try {
						window.setMappingResults(workspace, results);
						window.setOnCloseRequest(e -> previewWindowProvider.destroy(window));
						window.show();
						window.requestFocus();
						windowManager.registerAnonymous(window);
					} catch (Throwable t) {
						previewWindowProvider.destroy(window);
						logger.error("Failed opening mapping preview for {} mappings", format.implementationName(), t);
						ErrorDialogs.show(Lang.get("mapapply.preview.error.title"),
								Lang.get("mapapply.preview.error.header"),
								Lang.get("mapapply.preview.error.content"), t);
					}
				}, FxThreadUtil.executor());
	}

	/**
	 * @param applyCallback
	 * 		Callback to run after mappings are applied.
	 */
	public void setApplyCallback(@Nullable Runnable applyCallback) {
		this.applyCallback = applyCallback;
	}
}
