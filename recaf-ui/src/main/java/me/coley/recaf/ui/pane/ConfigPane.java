package me.coley.recaf.ui.pane;

import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.Group;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.Threads;

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
			String groupKey = key + '.' + group.value();
			List<Field> fieldGroup = groupedFieldMap.computeIfAbsent(groupKey, n -> new ArrayList<>());
			fieldGroup.add(field);
		}
		// Create the content
		int baseRow = 0;
		GridPane content = new GridPane();
		content.setVgap(15);
		content.setPadding(new Insets(25));
		for (Map.Entry<String, List<Field>> e : groupedFieldMap.entrySet()) {
			String groupKey = e.getKey();
			List<Field> fields = e.getValue();
			Label groupLabel = new Label(Lang.get(groupKey));
			groupLabel.getStyleClass().add("h1");
			groupLabel.getStyleClass().add("b");
			content.add(groupLabel, 0, baseRow);
			int i = baseRow + 1;
			for (Field field : fields) {
				ConfigID id = field.getAnnotation(ConfigID.class);
				String idKey = groupKey + '.' + id.value();
				content.add(new Label(idKey), 0, i);
				i++;
			}
			baseRow = i;
		}
		// Creating horizontal tabs is an absolute hack: https://stackoverflow.com/a/24219414
		String title = container.displayName();
		String iconPath = container.iconPath();
		Node graphic = Icons.getIconView(iconPath, 28);
		Tab tab = new Tab(title, content);
		tab.setClosable(false);
		tab.setGraphic(graphic);
		tab.setContent(content);
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
}
