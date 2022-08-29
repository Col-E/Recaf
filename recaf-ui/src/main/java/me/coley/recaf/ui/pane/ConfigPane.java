package me.coley.recaf.ui.pane;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import me.coley.recaf.config.*;
import me.coley.recaf.config.binds.Binding;
import me.coley.recaf.ui.behavior.WindowShownListener;
import me.coley.recaf.ui.control.BoundLabel;
import me.coley.recaf.ui.control.config.*;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.WindowBase;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Display for config values defined in {@link ConfigContainer} instances.
 *
 * @author Matt Coley
 */
public class ConfigPane extends BorderPane implements WindowShownListener {
	private static final Logger logger = Logging.get(ConfigPane.class);
	private static final Map<String, BiFunction<ConfigContainer, Field, Node>> ID_OVERRIDES = new HashMap<>();
	private static final String TAB_TITLE_PADDING = "  ";
	private static final int WIDTH = 200;
	private final List<Runnable> onShownQueue = new ArrayList<>();

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

	private Tab createContainerTab(ConfigContainer container) {
		String key = container.internalName();
		// Fields are put into groups, so we will want to get that grouping information first.
		//  - Each container has multiple groups sorted by name.
		//  - But we always want the 'general' item on top.
		String preferredGroupSuffix = ".general";
		Map<String, List<Field>> groupedFieldMap = new TreeMap<>((o1, o2) -> {
			if (o1.endsWith(preferredGroupSuffix))
				return o2.endsWith(preferredGroupSuffix) ? 0 : -1;
			else if (o2.endsWith(preferredGroupSuffix))
				return 1;
			else
				return o1.compareTo(o2);
		});
		for (Field field : container.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Ignore.class)) continue;
			if (Modifier.isTransient(field.getModifiers()))
				continue;
			field.setAccessible(true);
			Group group = field.getAnnotation(Group.class);
			if (group == null) {
				logger.trace("Skip field, missing config annotations: " + container.getClass() + "#" + field.getName()
						+ " - Use @Ignore to ignore this field");
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
			Label groupLabel = new BoundLabel(Lang.getBinding(groupKey));
			groupLabel.getStyleClass().add("h1");
			groupLabel.getStyleClass().add("b");
			content.add(groupLabel, 0, baseRow, 3, 1);
			int i = baseRow + 1;
			for (Field field : fields) {
				ConfigID id = field.getAnnotation(ConfigID.class);
				String idKey = groupKey + '.' + id.value();
				Node editor = getConfigComponent(container, field, idKey);
				Node tooltipElement = editor;
				if (editor instanceof Unlabeled) {
					Label editorLabel = new BoundLabel(Lang.getBinding(idKey));
					content.add(editorLabel, 1, i, 1, 1);
					content.add(editor, 2, i, 1, 1);
					tooltipElement = editorLabel;
				} else {
					content.add(editor, 1, i, 2, 1);
				}
				// check if a description translation is available
				if (Lang.has(idKey + ".description")) {
					// if there is, create a tooltip for the element
					Tooltip tooltip = new Tooltip(Lang.get(idKey + ".description"));
					tooltip.setGraphic(Icons.getIconView(Icons.INFO));
					tooltip.setShowDelay(Duration.ZERO);
					tooltip.setHideDelay(Duration.ZERO);
					tooltip.setAutoHide(false);
					Tooltip.install(tooltipElement, tooltip);
				}
				i++;
			}
			baseRow = i;
		}
		// Creating horizontal tabs is an absolute hack: https://stackoverflow.com/a/24219414
		StringBinding title = Lang.formatBy(TAB_TITLE_PADDING + "%s",
				container.displayNameBinding());
		String iconPath = container.iconPath();
		Node graphic = Icons.getIconView(iconPath, 32);
		Tab tab = new Tab();
		tab.textProperty().bind(title);
		tab.setContent(content);
		tab.setClosable(false);
		tab.setContent(new ScrollPane(content));
		tab.setGraphic(graphic);
		onShownQueue.add(() -> {
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

	@SuppressWarnings("unchecked")
	private static Node getConfigComponent(ConfigContainer container, Field field, String idKey) {
		Class<?> type = field.getType();
		// seems like StringProperty is not supported
		if (ID_OVERRIDES.containsKey(idKey)) {
			return ID_OVERRIDES.get(idKey).apply(container, field);
		} else if (boolean.class.equals(type) || Boolean.class.equals(type) || BooleanProperty.class.isAssignableFrom(type)) {
			return new ConfigBoolean(container, field, Lang.getBinding(idKey));
		} else if (ConfigRanged.hasBounds(field)) {
			return new ConfigRanged(container, field);
		} else if (Binding.class.equals(type)) {
			return new ConfigBinding(container, field);
		} else if (Pos.class.equals(type)) {
			return new ConfigPos(container, field);
		} else if (type.isEnum()) {
			return new ConfigEnum(container, field);
		} else if (ObjectProperty.class.equals(type) && field.getGenericType() instanceof ParameterizedType) {
			// need to be ObjectProperty<EnumTypeHere>
			ParameterizedType pt = (ParameterizedType) field.getGenericType();
			Type[] types = pt.getActualTypeArguments();
			Type genericType = types.length >= 1 ? types[0] : null;
			if (genericType instanceof Class) {
				if (((Class<?>) genericType).isEnum()) {
					// should be an enum
					return new ConfigEnumProperty((Class<Enum<?>>) genericType, container, field);
				} else {
					logger.trace("Skip field, provided generic type is not an enum: {}#{} - {}",
							field.getName(), container.getClass(), type.getName());
				}
			} else {
				logger.trace("Skip field, missing generic class type: {}#{} - needs to be ObjectProperty<EnumTypeHere>",
						container.getClass(), field.getName());
			}
		} else if (int.class.equals(type) || Integer.class.equals(type) || IntegerProperty.class.equals(type)) {
			return new ConfigInt(container, field);
		} else if (long.class.equals(type) || Long.class.equals(type) || LongProperty.class.equals(type)) {
			return new ConfigLong(container, field);
		}
		Label fallback = new Label(idKey + " - Unsupported field type: " + type);
		fallback.setStyle("-fx-text-fill: orange;");
		return fallback;
	}

	@Override
	public void onShown(WindowEvent e) {
		onShownQueue.forEach(Runnable::run);
	}

	static {
		ID_OVERRIDES.put("conf.compiler.general.impl", ConfigCompiler::new);
		ID_OVERRIDES.put("conf.decompiler.general.impl", ConfigDecompiler::new);
		ID_OVERRIDES.put("conf.display.general.language", ConfigLanguage::new);
		ID_OVERRIDES.put("conf.editor.assoc.fileextassociations", ConfigLanguageAssociation::new);
		ID_OVERRIDES.put("conf.ssvm.remote.active", (c, f) ->
				new ConfigActionableBoolean(c, f, Lang.getBinding("conf.ssvm.remote.active"),
						value -> Configs.ssvm().updateIntegration()));
		ID_OVERRIDES.put("conf.ssvm.remote.path", ConfigPath::new);
		ID_OVERRIDES.put("conf.ssvm.access.read", (c, f) ->
				new ConfigActionableBoolean(c, f, Lang.getBinding("conf.ssvm.access.read"), value -> {
					if (value) {
						Alert a = new Alert(Alert.AlertType.WARNING);
						WindowBase.installStyle(a.getDialogPane().getStylesheets());
						WindowBase.installLogo((Stage) a.getDialogPane().getScene().getWindow());
						a.headerTextProperty().bind(Lang.getBinding("conf.ssvm.access"));
						a.contentTextProperty().bind(Lang.getBinding("conf.ssvm.access.read.warn"));
						a.show();
					}
					Configs.ssvm().updateIntegration();
				}));
		ID_OVERRIDES.put("conf.ssvm.access.write", (c, f) ->
				new ConfigActionableBoolean(c, f, Lang.getBinding("conf.ssvm.access.write"), value -> {
					if (value) {
						Alert a = new Alert(Alert.AlertType.WARNING);
						WindowBase.installStyle(a.getDialogPane().getStylesheets());
						WindowBase.installLogo((Stage) a.getDialogPane().getScene().getWindow());
						a.headerTextProperty().bind(Lang.getBinding("conf.ssvm.access"));
						a.contentTextProperty().bind(Lang.getBinding("conf.ssvm.access.write.warn"));
						a.show();
					}
					Configs.ssvm().updateIntegration();
				}));
	}
}
