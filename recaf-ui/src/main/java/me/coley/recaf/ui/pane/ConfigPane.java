package me.coley.recaf.ui.pane;

import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.Group;
import me.coley.recaf.config.container.KeybindConfig;
import me.coley.recaf.ui.control.config.ConfigBinding;
import me.coley.recaf.ui.control.config.ConfigBoolean;
import me.coley.recaf.ui.control.config.ConfigRanged;
import me.coley.recaf.ui.control.config.Unlabeled;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.Threads;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Display for config values defined in {@link ConfigContainer} instances.
 *
 * @author Matt Coley
 */
public class ConfigPane extends BorderPane {
	private static final Logger logger = Logging.get(ConfigPane.class);
	private static final String TAB_TITLE_PADDING = "  ";
	private static final int WIDTH = 200;

	/**
	 * New config pane instance. Generates items from {@link Configs#containers()}.
	 */
	public ConfigPane() {
		TabPane tabPane = new TabPane();
		tabPane.setSide(Side.LEFT);
		tabPane.setRotateGraphic(true);
		// This actually does the width now... I know its odd...
		tabPane.setTabMinHeight(WIDTH);
		tabPane.setTabMaxHeight(WIDTH);
		// Extra CSS
		tabPane.getStyleClass().add("horizontal-tab-pane");
		for (ConfigContainer container : Configs.containers()) {
			// Skip internal containers
			if (container.isInternal())
				continue;
			Tab tab = createContainerTab(container);
			tabPane.getTabs().add(tab);
		}
		setCenter(tabPane);
	}

	private static Tab createContainerTab(ConfigContainer container) {
		String key = container.internalName();
		// Fields are put into groups, so we will want to get that grouping information first.
		Map<String, List<Field>> groupedFieldMap = new TreeMap<>();
		for (Field field : container.getClass().getDeclaredFields()) {
			field.setAccessible(true);
			Group group = field.getAnnotation(Group.class);
			if (group == null) {
				logger.trace("Skip field, missing config annotations: " + container.getClass() + "#" + field.getName());
				continue;
			}
			String groupKey = key + '.' + group.value();
			List<Field> fieldGroup = groupedFieldMap.computeIfAbsent(groupKey, n -> new ArrayList<>());
			fieldGroup.add(field);
		}
		// Create the content
		int baseRow = 0;
		GridPane content = new GridPane();
		content.getColumnConstraints().add(new ColumnConstraints(5));
		content.getColumnConstraints().add(new ColumnConstraints(240));
		content.getColumnConstraints().add(new ColumnConstraints(455));
		content.setHgap(15);
		content.setVgap(15);
		content.setPadding(new Insets(25));
		for (Map.Entry<String, List<Field>> e : groupedFieldMap.entrySet()) {
			String groupKey = e.getKey();
			List<Field> fields = e.getValue();
			Label groupLabel = new Label(Lang.get(groupKey));
			groupLabel.getStyleClass().add("h1");
			groupLabel.getStyleClass().add("b");
			content.add(groupLabel, 0, baseRow, 3, 1);
			int i = baseRow + 1;
			for (Field field : fields) {
				ConfigID id = field.getAnnotation(ConfigID.class);
				String idKey = groupKey + '.' + id.value();
				Node editor = getConfigComponent(container, field, idKey);
				if (editor instanceof Unlabeled) {
					content.add(new Label(Lang.get(idKey)), 1, i, 1, 1);
					content.add(editor, 2, i, 1, 1);
				} else {
					content.add(editor, 1, i, 2, 1);
				}
				i++;
			}
			baseRow = i;
		}
		// Creating horizontal tabs is an absolute hack: https://stackoverflow.com/a/24219414
		String title = TAB_TITLE_PADDING + container.displayName();
		String iconPath = container.iconPath();
		Node graphic = Icons.getIconView(iconPath, 28);
		Tab tab = new Tab(title, content);
		tab.setClosable(false);
		tab.setContent(new ScrollPane(content));
		tab.setGraphic(graphic);
		Threads.runFx(() -> {
			// Get the "tab-container" node. This is what we want to rotate/shift.
			Parent tabContainer = tab.getGraphic().getParent().getParent();
			tabContainer.setRotate(90);
			// By default the display will originate from the center.
			// Applying a negative Y transformation will move it left.
			// Should be the 'TabMinHeight/2'
			tabContainer.setTranslateY(-(WIDTH >> 1));
		});
		return tab;
	}

	private static Node getConfigComponent(ConfigContainer container, Field field, String idKey) {
		Class<?> type = field.getType();
		// String
		// int
		// WorkspaceAction
		// Binding
		if (boolean.class.equals(type) || Boolean.class.equals(type)) {
			return new ConfigBoolean(container, field, Lang.get(idKey));
		} else if (ConfigRanged.hasBounds(field)) {
			return new ConfigRanged(container, field);
		} else if (KeybindConfig.Binding.class.equals(type)) {
			return new ConfigBinding(container, field);
		}
		Label fallback = new Label(idKey + " - Unsupported field type: " + type);
		fallback.setStyle("-fx-text-fill: orange;");
		return fallback;
	}
}
