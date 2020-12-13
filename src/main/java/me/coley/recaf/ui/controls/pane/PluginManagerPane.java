package me.coley.recaf.ui.controls.pane;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.BasePlugin;
import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.ui.controls.SubLabeled;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.util.UiUtil;

import java.awt.image.BufferedImage;

/**
 * UI for managing plugins.
 *
 * @author Matt
 */
public class PluginManagerPane extends BorderPane {
	private static final PluginsManager manager = PluginsManager.getInstance();
	private final ListView<BasePlugin> list = new ListView<>();
	private final BorderPane view = new BorderPane();

	/**
	 * Create the plugin pane.
	 */
	public PluginManagerPane() {
		setup();
	}

	private void setup() {
		list.setCellFactory(cb -> new PluginCell());
		list.getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> {
			view.setCenter(createPluginView(n == null ? o : n));
		});
		list.setItems(FXCollections.observableArrayList(PluginsManager.getInstance().visiblePlugins().values()));
		view.getStyleClass().add("plugin-view");
		SplitPane split = new SplitPane(list, view);
		SplitPane.setResizableWithParent(list, Boolean.FALSE);
		split.setDividerPositions(0.37);
		setCenter(split);
	}

	private Node createPluginView(BasePlugin plugin) {
		BorderPane pane = new BorderPane();
		pane.setPadding(new Insets(15));
		// Content
		VBox box = new VBox();
		box.setSpacing(10);
		HBox title = new HBox();
		BufferedImage icon = manager.getPluginIcons().get(plugin.getName());
		if (icon != null) {
			IconView iconView = new IconView(UiUtil.toFXImage(icon), 32);
			BorderPane wrapper = new BorderPane(iconView);
			wrapper.setPadding(new Insets(4, 8, 0, 0));
			title.getChildren().add(wrapper);
		}
		title.getChildren().add(new SubLabeled(plugin.getName(), plugin.getDescription()));
		box.getChildren().add(title);
		pane.setCenter(box);
		// Controls
		HBox horizontal = new HBox();
		CheckBox chkEnabled = new CheckBox(LangUtil.translate("misc.enabled"));
		chkEnabled.setSelected(manager.getPluginStates().get(plugin.getName()));
		chkEnabled.selectedProperty()
				.addListener((ob, o, n) -> manager.getPluginStates().put(plugin.getName(), n));
		horizontal.getChildren().add(chkEnabled);
		box.getChildren().add(horizontal);
		return pane;
	}

	private static class PluginCell extends ListCell<BasePlugin> {
		@Override
		public void updateItem(BasePlugin item, boolean empty) {
			super.updateItem(item, empty);
			if(!empty) {
				String name = item.getName();
				String version = item.getVersion();
				BufferedImage icon = manager.getPluginIcons().get(name);
				setText(name + " - " + version);
				if (icon != null)
					setGraphic(new IconView(UiUtil.toFXImage(icon)));
				else
					setGraphic(UiUtil.createFileGraphic("plugin.jar"));
			}
		}
	}
}
