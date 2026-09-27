package software.coley.recaf.ui.pane;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.file.RecafDirectoriesConfig;
import software.coley.recaf.services.plugin.PluginException;
import software.coley.recaf.services.plugin.PluginInfo;
import software.coley.recaf.services.plugin.PluginManager;
import software.coley.recaf.services.plugin.PluginUnloader;
import software.coley.recaf.services.plugin.PreparedPlugin;
import software.coley.recaf.services.plugin.discovery.PathPluginDiscoverer;
import software.coley.recaf.services.plugin.discovery.PluginDiscoverer;
import software.coley.recaf.services.plugin.zip.ZipPluginLoader;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.DesktopUtil;
import software.coley.recaf.util.FileChooserBuilder;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.io.ByteSources;
import software.coley.recaf.util.threading.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static software.coley.recaf.util.Lang.getBinding;

/**
 * Pane to display and manage installed plugins.
 * <p/>
 * Plugins are jar files residing in the {@link RecafDirectoriesConfig#getPluginDirectory() plugin directory}.
 * Disabled plugins are moved into the {@link RecafDirectoriesConfig#getDisabledPluginDirectory() disabled sub-directory}
 * which is not scanned at startup.
 *
 * @author Canrad
 * @see PluginManager Source of loaded plugins.
 */
@Dependent
public class PluginManagerPane extends BorderPane {
	/** Link to the online plugin development guide. */
	public static final String URL_DEV_GUIDE = "https://recaf.coley.software/dev/plugins-and-scripts/plugins.html";
	/** Link to the template workspace for starting a new plugin project. */
	public static final String URL_TEMPLATE_WORKSPACE = "https://github.com/Recaf-Plugins/Recaf-4x-plugin-workspace";
	private static final Logger logger = Logging.get(PluginManagerPane.class);
	private final VBox pluginList = new VBox();
	private final PluginManager pluginManager;
	private final RecafDirectoriesConfig directories;
	private final ZipPluginLoader infoLoader = new ZipPluginLoader();

	@Inject
	public PluginManagerPane(@Nonnull PluginManager pluginManager,
	                         @Nonnull RecafDirectoriesConfig directories) {
		this(pluginManager, directories, false);
	}

	PluginManagerPane(@Nonnull PluginManager pluginManager,
	                  @Nonnull RecafDirectoriesConfig directories,
	                  boolean forTesting) {
		this.pluginManager = pluginManager;
		this.directories = directories;
		// UI initialization is skipped when constructed for testing.
		if (forTesting) return;

		pluginList.setFillWidth(true);
		pluginList.setSpacing(10);
		pluginList.setPadding(new Insets(10));

		ScrollPane scroll = new ScrollPane(pluginList);
		scroll.getStyleClass().add("dark-scroll-pane");
		scroll.setFitToWidth(true);
		setCenter(scroll);

		HBox controls = new HBox();
		controls.setStyle("""
				-fx-background-color: -color-bg-default;
				-fx-border-color: -color-border-default;
				-fx-border-width: 1 0 0 0;
				""");
		controls.setPadding(new Insets(10));
		controls.setSpacing(10);
		controls.setAlignment(Pos.CENTER_LEFT);
		// The development guide & template workspace links live in the Plugins menu, so they aren't duplicated here.
		controls.getChildren().addAll(
				new ActionButton(CarbonIcons.DOCUMENT_ADD, getBinding("menu.plugin.install"), this::installPlugin),
				new ActionButton(CarbonIcons.FOLDER, getBinding("menu.plugin.browse"), this::browse),
				new ActionButton(CarbonIcons.RENEW, getBinding("menu.plugin.refresh"), this::refresh)
		);
		controls.getChildren().forEach(b -> b.getStyleClass().add("muted"));
		setBottom(controls);

		refresh();
	}

	/**
	 * Repopulate the plugin list from the contents of the plugin directories.
	 */
	public void refresh() {
		ThreadUtil.run(() -> {
			List<LocalPluginFile> files = scanPluginFiles();
			FxThreadUtil.run(() -> {
				pluginList.getChildren().clear();
				if (files.isEmpty()) {
					Label noPlugins = new Label();
					noPlugins.textProperty().bind(getBinding("menu.plugin.none-found"));
					noPlugins.setGraphic(new FontIconView(CarbonIcons.SEARCH));
					pluginList.getChildren().add(noPlugins);
				} else {
					for (LocalPluginFile file : files)
						pluginList.getChildren().add(new PluginEntry(file));
				}
			});
		});
	}

	/**
	 * @return Plugin files found in the enabled and disabled plugin directories.
	 */
	@Nonnull
	List<LocalPluginFile> scanPluginFiles() {
		List<LocalPluginFile> files = new ArrayList<>();
		collectPluginFiles(files, directories.getPluginDirectory(), true);
		collectPluginFiles(files, directories.getDisabledPluginDirectory(), false);
		files.sort(Comparator.comparing(f -> f.displayName().toLowerCase()));
		return files;
	}

	private void collectPluginFiles(@Nonnull List<LocalPluginFile> out, @Nonnull Path directory, boolean enabled) {
		if (!Files.isDirectory(directory))
			return;
		try (Stream<Path> stream = Files.list(directory)) {
			stream.filter(path -> Files.isRegularFile(path) && path.toString().toLowerCase().endsWith(".jar"))
					.forEach(path -> {
						PluginInfo info = readPluginInfo(path);
						if (info != null)
							out.add(new LocalPluginFile(path, info, enabled));
					});
		} catch (IOException ex) {
			logger.error("Failed to scan plugin directory: {}", directory, ex);
		}
	}

	/**
	 * @param path
	 * 		Path to a candidate plugin jar.
	 *
	 * @return Parsed plugin information, or {@code null} if the file is not a valid plugin.
	 */
	@Nullable
	private PluginInfo readPluginInfo(@Nonnull Path path) {
		try {
			PreparedPlugin prepared = infoLoader.prepare(ByteSources.forPath(path));
			if (prepared == null)
				return null;
			PluginInfo info = prepared.info();
			// Release the file handle, we only wanted the plugin information.
			prepared.reject();
			return info;
		} catch (PluginException ex) {
			logger.warn("Skipping invalid plugin file: {}", path, ex);
			return null;
		}
	}

	/**
	 * Prompts the user for a plugin jar, then copies it into the plugin directory and loads it.
	 */
	private void installPlugin() {
		FileChooser chooser = new FileChooserBuilder()
				.setTitle(Lang.get("menu.plugin.install"))
				.setFileExtensionFilter("Java archives", "*.jar")
				.build();
		File selected = chooser.showOpenDialog(getScene().getWindow());
		if (selected == null)
			return;
		Path source = selected.toPath();
		ThreadUtil.run(() -> {
			// Validate before copying: reject non-plugins and duplicates with a targeted message.
			PluginInfo info = readPluginInfo(source);
			if (info == null) {
				FxThreadUtil.run(() -> ErrorDialogs.show(getBinding("menu.plugin.error.install"),
						getBinding("menu.plugin.install"),
						getBinding("menu.plugin.install.invalid"),
						new PluginException("Not a valid plugin: " + source.getFileName())));
				return;
			}
			if (pluginManager.isPluginLoaded(info.id())) {
				FxThreadUtil.run(() -> ErrorDialogs.show(getBinding("menu.plugin.error.install"),
						getBinding("menu.plugin.install"),
						getBinding("menu.plugin.install.duplicate"),
						new PluginException("Duplicate plugin id: " + info.id())));
				return;
			}
			try {
				installFrom(source);
			} catch (IOException | PluginException ex) {
				logger.error("Failed to install plugin: {}", source, ex);
				FxThreadUtil.run(() -> ErrorDialogs.show(getBinding("menu.plugin.error.install"),
						getBinding("menu.plugin.install"),
						getBinding("menu.plugin.error.load"), ex));
			}
			refresh();
		});
	}

	/**
	 * Copies a plugin jar into the plugin directory and loads it. Synchronous, no UI.
	 * On load failure the copied file is removed and the error is rethrown.
	 *
	 * @param source
	 * 		Path to the plugin jar to install.
	 *
	 * @throws IOException
	 * 		If the file could not be copied.
	 * @throws PluginException
	 * 		If the copied plugin could not be loaded.
	 */
	void installFrom(@Nonnull Path source) throws IOException, PluginException {
		Path destination = directories.getPluginDirectory().resolve(source.getFileName().toString());
		try {
			Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
			pluginManager.loadPlugins(singleFileDiscoverer(destination));
		} catch (IOException | PluginException ex) {
			try {
				Files.deleteIfExists(destination);
			} catch (IOException ignored) {
				// Keeping the file is not harmful, it failed to load anyway.
			}
			throw ex;
		}
	}

	/**
	 * Enables or disables the given plugin file.
	 * Enabling moves the file into the plugin directory and loads it.
	 * Disabling unloads the plugin <i>(plus any dependants)</i> and moves the file into the disabled directory.
	 *
	 * @param file
	 * 		Plugin file to update.
	 * @param enable
	 *        {@code true} to enable, {@code false} to disable.
	 */
	private void setPluginEnabled(@Nonnull LocalPluginFile file, boolean enable) {
		if (enable) {
			ThreadUtil.run(() -> {
				try {
					applyEnabled(file, true);
				} catch (IOException | PluginException ex) {
					logger.error("Failed to enable plugin: {}", file.info().id(), ex);
					FxThreadUtil.run(() -> ErrorDialogs.show(getBinding("menu.plugin.error"),
							literalBinding(file.displayName()),
							getBinding("menu.plugin.error.load"), ex));
				}
				refresh();
			});
		} else {
			// Warn about dependant plugins that will be unloaded alongside this one.
			List<String> dependants = dependantNames(file.info().id());
			if (!dependants.isEmpty() && !confirm(Lang.get("menu.plugin.enabled"),
					Lang.get("menu.plugin.uninstall.dependants") + "\n - " + String.join("\n - ", dependants))) {
				return;
			}
			ThreadUtil.run(() -> {
				try {
					applyEnabled(file, false);
				} catch (IOException | PluginException ex) {
					logger.error("Failed to disable plugin: {}", file.info().id(), ex);
					FxThreadUtil.run(() -> ErrorDialogs.show(getBinding("menu.plugin.error"),
							literalBinding(file.displayName()),
							getBinding("menu.plugin.error.unload"), ex));
				}
				refresh();
			});
		}
	}

	/**
	 * Applies an enable/disable state change to a plugin file. Synchronous, no UI.
	 * <ul>
	 *     <li>Enabling moves the file into the plugin directory and loads it if not already loaded.</li>
	 *     <li>Disabling unloads the plugin then moves the file into the disabled directory.</li>
	 * </ul>
	 * On enable failure the file is moved back to its original location before the error is rethrown.
	 *
	 * @param file
	 * 		Plugin file to update.
	 * @param enable
	 *        {@code true} to enable, {@code false} to disable.
	 *
	 * @throws IOException
	 * 		If the file could not be moved.
	 * @throws PluginException
	 * 		If the plugin could not be loaded or unloaded.
	 */
	void applyEnabled(@Nonnull LocalPluginFile file, boolean enable) throws IOException, PluginException {
		if (enable) {
			Path destination = directories.getPluginDirectory().resolve(file.path().getFileName().toString());
			try {
				Files.move(file.path(), destination, StandardCopyOption.REPLACE_EXISTING);
				if (!pluginManager.isPluginLoaded(file.info().id()))
					pluginManager.loadPlugins(singleFileDiscoverer(destination));
			} catch (IOException | PluginException ex) {
				// Move the file back so the on-disk state matches the failed load.
				try {
					Files.move(destination, file.path(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException ignored) {}
				throw ex;
			}
		} else {
			unloadIfLoaded(file.info().id());
			Path disabledDirectory = directories.getDisabledPluginDirectory();
			Files.createDirectories(disabledDirectory);
			Files.move(file.path(), disabledDirectory.resolve(file.path().getFileName().toString()),
					StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * Uninstalls the given plugin after user confirmation, unloading it first if necessary.
	 *
	 * @param file
	 * 		Plugin file to remove.
	 */
	private void uninstallPlugin(@Nonnull LocalPluginFile file) {
		List<String> dependants = file.enabled() ? dependantNames(file.info().id()) : List.of();
		String content = Lang.get("menu.plugin.uninstall.warning");
		if (!dependants.isEmpty())
			content += "\n" + Lang.get("menu.plugin.uninstall.dependants") + "\n - " + String.join("\n - ", dependants);
		if (!confirm(Lang.get("menu.plugin.uninstall"), content))
			return;
		ThreadUtil.run(() -> {
			try {
				applyUninstall(file);
			} catch (IOException | PluginException ex) {
				logger.error("Failed to uninstall plugin: {}", file.info().id(), ex);
				FxThreadUtil.run(() -> ErrorDialogs.show(getBinding("menu.plugin.error"),
						literalBinding(file.displayName()),
						getBinding("menu.plugin.error.uninstall"), ex));
			}
			refresh();
		});
	}

	/**
	 * Unloads the plugin <i>(if loaded)</i> and deletes its jar file. Synchronous, no UI.
	 *
	 * @param file
	 * 		Plugin file to remove.
	 *
	 * @throws IOException
	 * 		If the file could not be deleted.
	 * @throws PluginException
	 * 		If the plugin could not be unloaded.
	 */
	void applyUninstall(@Nonnull LocalPluginFile file) throws IOException, PluginException {
		unloadIfLoaded(file.info().id());
		Files.deleteIfExists(file.path());
	}

	private void unloadIfLoaded(@Nonnull String id) throws PluginException {
		if (pluginManager.isPluginLoaded(id))
			pluginManager.unloaderFor(id).commit();
	}

	/**
	 * @param id
	 * 		Plugin identifier.
	 *
	 * @return Names of loaded plugins depending on the given plugin.
	 */
	@Nonnull
	private List<String> dependantNames(@Nonnull String id) {
		if (!pluginManager.isPluginLoaded(id))
			return List.of();
		PluginUnloader unloader = pluginManager.unloaderFor(id);
		return unloader.dependants()
				.map(info -> info.name().isBlank() ? info.id() : info.name())
				.collect(Collectors.toList());
	}

	/**
	 * Opens the local plugins directory.
	 */
	private void browse() {
		try {
			DesktopUtil.showDocument(directories.getPluginDirectory().toUri());
		} catch (IOException ex) {
			logger.error("Failed to show plugins directory", ex);
		}
	}

	private boolean confirm(@Nonnull String title, @Nonnull String content) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO);
		alert.setTitle(title);
		javafx.stage.Stage stage = (javafx.stage.Stage) alert.getDialogPane().getScene().getWindow();
		stage.getIcons().add(Icons.getImage(Icons.LOGO));
		return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
	}

	@Nonnull
	private static PluginDiscoverer singleFileDiscoverer(@Nonnull Path path) {
		return new PathPluginDiscoverer() {
			@Nonnull
			@Override
			protected Stream<Path> stream() {
				return Stream.of(path);
			}
		};
	}

	@Nonnull
	private static StringBinding literalBinding(@Nonnull String text) {
		return new StringBinding() {
			@Override
			protected String computeValue() {
				return text;
			}
		};
	}

	/**
	 * Model of a plugin jar on disk.
	 *
	 * @param path
	 * 		Path to the plugin jar.
	 * @param info
	 * 		Parsed plugin information.
	 * @param enabled
	 *        {@code true} when the file resides in the scanned plugin directory.
	 */
	record LocalPluginFile(@Nonnull Path path, @Nonnull PluginInfo info, boolean enabled) {
		@Nonnull
		String displayName() {
			return info.name().isBlank() ? info.id() : info.name();
		}
	}

	/**
	 * Entry showing the plugin details + enable/uninstall actions.
	 */
	private class PluginEntry extends BorderPane {
		private PluginEntry(@Nonnull LocalPluginFile file) {
			setPadding(new Insets(10));
			getStyleClass().add("tooltip");

			PluginInfo info = file.info();
			Label nameLabel = new Label(file.displayName());
			nameLabel.setWrapText(true);
			nameLabel.setMinSize(350, 20);
			nameLabel.setMaxWidth(550);
			nameLabel.getStyleClass().add(Styles.TITLE_3);

			VBox infoBox = new VBox();
			infoBox.getChildren().add(nameLabel);
			if (!info.description().isBlank())
				infoBox.getChildren().add(makeAttribLabel(null, info.description()));
			if (!info.author().isBlank())
				infoBox.getChildren().add(makeAttribLabel(getBinding("menu.plugin.author"), info.author()));
			if (!info.version().isBlank())
				infoBox.getChildren().add(makeAttribLabel(getBinding("menu.plugin.version"), info.version()));
			if (!info.dependencies().isEmpty())
				infoBox.getChildren().add(makeAttribLabel(getBinding("menu.plugin.dependencies"),
						String.join(", ", info.dependencies())));

			CheckBox enabledCheck = new CheckBox();
			enabledCheck.textProperty().bind(getBinding("menu.plugin.enabled"));
			enabledCheck.setSelected(file.enabled());
			enabledCheck.selectedProperty().addListener((ob, old, cur) -> setPluginEnabled(file, cur));

			ActionButton uninstallButton = new ActionButton(CarbonIcons.TRASH_CAN,
					getBinding("menu.plugin.uninstall"), () -> uninstallPlugin(file));
			uninstallButton.setAlignment(Pos.CENTER_LEFT);
			uninstallButton.setPrefSize(130, 30);

			VBox actions = new VBox();
			actions.setSpacing(8);
			actions.setAlignment(Pos.CENTER_RIGHT);
			actions.getChildren().addAll(enabledCheck, uninstallButton);

			setLeft(infoBox);
			setRight(actions);

			prefWidthProperty().bind(widthProperty());
		}

		/**
		 * Used to display bullet point format.
		 *
		 * @param langBinding
		 * 		Language binding for label display.
		 * @param secondaryText
		 * 		Text to appear after the initial binding text.
		 *
		 * @return Label bound to translatable text.
		 */
		@Nonnull
		private static Label makeAttribLabel(@Nullable StringBinding langBinding, @Nonnull String secondaryText) {
			Label label = new Label(secondaryText);
			label.setWrapText(true);
			label.setMaxWidth(550);
			if (langBinding != null) {
				label.textProperty().bind(new StringBinding() {
					{
						bind(langBinding);
					}

					@Override
					protected String computeValue() {
						return String.format("  • %s: %s", langBinding.get(), secondaryText);
					}
				});
			}
			return label;
		}
	}
}
