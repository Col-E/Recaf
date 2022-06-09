package me.coley.recaf.ui.pane;

import dev.xdark.recaf.plugin.Plugin;
import dev.xdark.recaf.plugin.PluginContainer;
import dev.xdark.recaf.plugin.PluginInformation;
import dev.xdark.recaf.plugin.RecafPluginManager;
import dev.xdark.recaf.plugin.repository.CommonPluginRepository;
import dev.xdark.recaf.plugin.repository.PluginRepository;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.PluginConfig;
import me.coley.recaf.ui.dialog.ConfirmDialog;
import me.coley.recaf.ui.plugin.item.InstalledPluginItem;
import me.coley.recaf.ui.plugin.item.RemotePluginItem;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Labels;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.DesktopUtil;
import me.coley.recaf.util.Directories;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Displays local and remote plugins.
 *
 * @author Matt Coley
 * @author xtherk
 */
public class PluginManagerPane extends BorderPane {
	private static final Logger logger = Logging.get(PluginManagerPane.class);
	private static final RecafPluginManager MANAGER = RecafPluginManager.getInstance();
	private static final PluginConfig CONFIG = Configs.plugin();
	private static final PluginManagerPane INSTANCE = new PluginManagerPane();

	private PluginManagerPane() {
		TabPane tabs = new TabPane();
		tabs.getTabs().add(new InstalledTab());
		tabs.getTabs().add(new RemoteTab());
		setCenter(tabs);
	}

	/**
	 * @return Manager UI instance.
	 */
	public static PluginManagerPane getInstance() {
		return INSTANCE;
	}

	private static class InstalledTab extends Tab {
		private final VBox pluginsList = new VBox();

		private InstalledTab() {
			textProperty().bind(Lang.getBinding("menu.plugin.installed"));
			setGraphic(Icons.getIconView(Icons.PLUGIN));
			setClosable(false);

			ScrollPane scrollPane = new ScrollPane(pluginsList);
			scrollPane.setFitToWidth(true);
			scrollPane.setStyle("-fx-background-insets: -1");

			Button browseScripts = new Button();
			browseScripts.textProperty().bind(Lang.getBinding("menu.plugin.browse"));
			browseScripts.setGraphic(Icons.getIconView(Icons.FOLDER));
			browseScripts.setOnAction(event -> browsePlugins());
			HBox actions = new HBox();
			actions.setPadding(new Insets(2, 2, 2, 2));
			actions.getChildren().add(browseScripts);

			BorderPane content = new BorderPane();
			content.setCenter(scrollPane);
			content.setBottom(actions);


			content.setBottom(actions);
			setContent(content);
			// TODO: Use watch service to handle new plugins
			//    - load new plugins
			//    - unload removed plugins
			//    - reload plugins if the hash changes

			update();
		}

		/**
		 * Open the 'plugins' directory in the file explorer.
		 */
		private void browsePlugins() {
			try {
				DesktopUtil.showDocument(Directories.getPluginDirectory().toUri());
			} catch (IOException ex) {
				logger.error("Failed to show plugins directory", ex);
			}
		}

		/**
		 * Refresh plugins list.
		 */
		private void update() {
			pluginsList.getChildren().clear();

			List<InstalledPluginItem> pluginItems = createPluginItems();
			for (InstalledPluginItem pluginItem : pluginItems) {
				BorderPane pluginRow = new BorderPane();
				pluginRow.setPadding(new Insets(4, 4, 4, 4));

				Label nameLabel = new Label(pluginItem.getName());
				nameLabel.setMinSize(350, 20);
				nameLabel.getStyleClass().addAll("b", "h1");

				VBox info = new VBox();
				info.getChildren().add(nameLabel);

				String description = pluginItem.getDescription();
				String author = pluginItem.getAuthor();
				String version = pluginItem.getVersion();

				if (description != null)
					info.getChildren().add(Labels.makeAttribLabel(null, description));
				if (author != null)
					info.getChildren().add(Labels.makeAttribLabel(Lang.getBinding("menu.scripting.author"), author));
				if (version != null)
					info.getChildren().add(Labels.makeAttribLabel(Lang.getBinding("menu.scripting.version"), version));

				pluginRow.setLeft(info);

				VBox actions = new VBox();
				actions.setSpacing(4);
				actions.setAlignment(Pos.CENTER_LEFT);

				CheckBox enabledToggle = new CheckBox();
				enabledToggle.setSelected(pluginItem.isEnabled());
				enabledToggle.textProperty().bind(Lang.getBinding("menu.plugin.enabled"));
				enabledToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
					if (newValue) {
						pluginItem.enable();
					} else {
						pluginItem.disable();
					}
				});
				Button uninstall = new Button();
				uninstall.textProperty().bind(Lang.getBinding("menu.plugin.uninstall"));
				uninstall.setGraphic(Icons.getIconView(Icons.ACTION_DELETE));
				uninstall.setOnAction(e -> {
					StringBinding title = Lang.getBinding("menu.plugin.uninstall");
					StringBinding header = Lang.getBinding("menu.plugin.uninstall.warning");
					ConfirmDialog deleteDialog = new ConfirmDialog(title, header, Icons.getImageView(Icons.ACTION_DELETE));
					boolean remove = deleteDialog.showAndWait().orElse(false);
					if (remove) {
						pluginItem.uninstall();
						update();
					}
				});

				actions.getChildren().add(enabledToggle);
				actions.getChildren().add(uninstall);

				pluginRow.setRight(actions);
				pluginRow.prefWidthProperty().bind(pluginsList.widthProperty());

				Separator separator = new Separator(Orientation.HORIZONTAL);
				separator.prefWidthProperty().bind(pluginsList.widthProperty());

				pluginsList.getChildren().addAll(pluginRow, separator);
			}
		}

		/**
		 * Create PluginItem from loaded plugins
		 *
		 * @return Loaded plugins
		 */
		private static List<InstalledPluginItem> createPluginItems() {
			return MANAGER.getPluginContainerPathMap().entrySet().stream()
					.map(entry -> {
						PluginContainer<? extends Plugin> value = entry.getKey();
						PluginInformation info = value.getInformation();
						URI uri = entry.getValue().toUri();
						return new InstalledPluginItem(uri, info);
					}).collect(Collectors.toList());
		}
	}

	@SuppressWarnings("all") // TODO: Remove suppression once implemented
	private static class RemoteTab extends Tab {
		private static final PluginRepository REPOSITORY = new CommonPluginRepository();
		private static final long ONE_DAY_SEC = 86_400;

		private RemoteTab() {
			textProperty().bind(Lang.getBinding("menu.plugin.remote"));
			setGraphic(Icons.getIconView(Icons.DOWNLOAD));
			setClosable(false);
			setContent(new BorderPane(new Label("Remote plugin listing disabled.\nThis will be enabled later.")));
		}

		// TODO: Access this on a background thread if 'shouldRequestRemote() == true'
		private static List<RemotePluginItem> createPluginItems() {
			if (shouldRequestRemote()) {
				logger.info("Requesting remote plugins...");
				CONFIG.cachedRemoteTime = System.currentTimeMillis();
				CONFIG.cachedRemotePlugins = REPOSITORY.pluginItems().stream()
						.map(item -> new RemotePluginItem(item))
						.collect(Collectors.toList());
				logger.info("Discovered {} remote plugin", CONFIG.cachedRemotePlugins.size());
			}
			return CONFIG.cachedRemotePlugins;
		}

		private static boolean shouldRequestRemote() {
			// TODO: Once we offer some actual plugins, remove this hook
			if (true)
				return false;
			long now = System.currentTimeMillis();
			long diff = now - CONFIG.cachedRemoteTime;
			return diff > ONE_DAY_SEC * 1000;
		}
	}
}
