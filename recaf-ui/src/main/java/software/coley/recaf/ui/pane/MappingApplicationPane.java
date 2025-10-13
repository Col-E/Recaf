package software.coley.recaf.ui.pane;

import atlantafx.base.controls.Spacer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import software.coley.recaf.services.mapping.MappingHelper;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.UniqueKeyMappings;
import software.coley.recaf.services.mapping.format.EnigmaMappings;
import software.coley.recaf.services.mapping.format.MappingFileFormat;
import software.coley.recaf.services.mapping.format.MappingFormatManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.util.FileChooserBundle;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;

import java.awt.Toolkit;
import java.io.File;
import java.util.List;

@Dependent
public class MappingApplicationPane extends BorderPane {
	private static final Logger logger = Logging.get(MappingApplicationPane.class);
	private final ObjectProperty<IntermediateMappings> mappingsProperty = new SimpleObjectProperty<>();
	private final ObjectProperty<MappingFileFormat> mappingFormatProperty = new SimpleObjectProperty<>();
	private final BooleanProperty ignoreHierarchy = new SimpleBooleanProperty();
	private final MappingFormatManager formatManager;
	private final MappingHelper mappingHelper;
	private final FileTypeSyntaxAssociationService languageAssociation;
	private final WorkspaceManager workspaceManager;
	private final RecentFilesConfig recentFilesConfig;
	private final Instance<SearchBar> searchBarProvider;
	private Runnable applyCallback;

	@Inject
	public MappingApplicationPane(@Nonnull MappingFormatManager formatManager,
	                              @Nonnull MappingHelper mappingHelper,
	                              @Nonnull FileTypeSyntaxAssociationService languageAssociation,
	                              @Nonnull WorkspaceManager workspaceManager,
	                              @Nonnull RecentFilesConfig recentFilesConfig,
	                              @Nonnull Instance<SearchBar> searchBarProvider) {
		this.formatManager = formatManager;
		this.mappingHelper = mappingHelper;
		this.languageAssociation = languageAssociation;
		this.workspaceManager = workspaceManager;
		this.recentFilesConfig = recentFilesConfig;
		this.searchBarProvider = searchBarProvider;

		setBottom(createButtonBar());
		setCenter(createDisplay());
	}

	@Nonnull
	private Node createDisplay() {
		// TODO: Put more in this display
		//  - Warning of classes in mappings that are not found in workspace
		//    - Unless the config for 'ignoreHierarchy' is true
		//  - Preview of class before/after similar to deobfuscation window

		Editor editor = new Editor();
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
		});

		return editor;
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
		Button apply = new ActionButton(CarbonIcons.CAFE, Lang.getBinding("mapapply"), () -> {
			try {
				Mappings mappings;
				if (ignoreHierarchy.get()) {
					mappings = new UniqueKeyMappings(mappingsProperty.get());
				} else {
					mappings = mappingsProperty.get();
				}

				mappingHelper.applyMappings(formatBox.getValue(), mappings);
				if (applyCallback != null) applyCallback.run();
			} catch (Exception ex) {
				logger.error("Failed to read {} mappings", formatBox.getValue().implementationName(), ex);
			}
		});
		apply.disableProperty().bind(mappingsProperty.isNull());

		HBox buttonBar = new HBox(formatBox, setMappingFile, setMappingDir, new Spacer(), apply, settingsMenu);
		buttonBar.setSpacing(5);
		buttonBar.setPadding(new Insets(5));
		return buttonBar;
	}

	public void setApplyCallback(@Nullable Runnable applyCallback) {
		this.applyCallback = applyCallback;
	}
}
