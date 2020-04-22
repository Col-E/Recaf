package me.coley.recaf.ui.controls;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.PluginBase;
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
	private final ListView<PluginBase> list = new ListView<>();
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
		view.getStyleClass().add("plugin-view");
		SplitPane split = new SplitPane(list, view);
		SplitPane.setResizableWithParent(list, Boolean.FALSE);
		split.setDividerPositions(0.37);
		setCenter(split);
	}

	private Node createPluginView(PluginBase plugin) {
		BorderPane pane = new BorderPane();
		// Content
		BorderPane display = new BorderPane();
		display.setTop(new SubLabeled(plugin.getName(), plugin.getDescription()));
		pane.setCenter(display);
		// Controls
		HBox horizontal = new HBox();
		CheckBox chkEnabled = new CheckBox(LangUtil.translate("misc.enabled"));
		chkEnabled.selectedProperty()
				.addListener((ob, o, n) -> manager.getPluginStates().put(plugin.getName(), n));
		horizontal.getChildren().add(chkEnabled);
		pane.setBottom(horizontal);
		return pane;
	}

	private static class PluginCell extends ListCell<PluginBase> {
		@Override
		public void updateItem(PluginBase item, boolean empty) {
			super.updateItem(item, empty);
			if(!empty) {
				String name = item.getName();
				BufferedImage icon = manager.getPluginIcons().get(name);
				setText(name);
				setGraphic(new IconView(UiUtil.toFXImage(icon)));
			}
		}
	}
}
