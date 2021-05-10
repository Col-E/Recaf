package me.coley.recaf.ui.controls.pane;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import me.coley.recaf.config.*;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.plugin.api.ConfigurablePlugin;
import me.coley.recaf.ui.controls.Toggle;
import me.coley.recaf.ui.controls.*;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.OSUtil;

import java.util.*;
import java.util.function.Function;

/**
 * Pane for some config.
 *
 * @author Matt
 */
public class ConfigPane extends ColumnPane {
	private static final Map<Class<?>, Function<FieldWrapper, Node>> DEFAULT_EDITORS = new HashMap<>();
	private final Map<String, Function<FieldWrapper, Node>> editorOverrides = new HashMap<>();
	private final Set<String> ignoredItems = new HashSet<>();
	private final Configurable config;
	private boolean hideUnsupported;

	/**
	 * @param config
	 * 		Config of the pane.
	 */
	private ConfigPane(Configurable config) {
		this.config = config;
	}

	/**
	 * @param controller
	 * 		Gui controller.
	 * @param config
	 * 		Display config.
	 */
	public ConfigPane(GuiController controller, ConfDisplay config) {
		this(config);
		editorOverrides.put("display.language", LanguageCombo::new);
		editorOverrides.put("display.uifontsize", v -> new FontSlider(controller, v));
		editorOverrides.put("display.monofontsize", v -> new FontSlider(controller, v));
		editorOverrides.put("display.uifont", v -> new FontComboBox(controller, v, config.uiFont));
		editorOverrides.put("display.monofont", v -> new FontComboBox(controller, v, config.monoFont));
		editorOverrides.put("display.textstyle", v -> new ThemeCombo(controller, v));
		editorOverrides.put("display.appstyle", v -> new StyleCombo(controller, v));
		editorOverrides.put("display.classmode", EnumComboBox::new);
		editorOverrides.put("display.filemode", EnumComboBox::new);
		editorOverrides.put("display.forceWordWrap", Toggle::new);
		editorOverrides.put("display.suggest.classerrors", Toggle::new);
		editorOverrides.put("display.maxrecent", v -> new NumberSlider<>(controller, v, 0, 20, 2));
		editorOverrides.put("display.maxtreedepth", v -> new NumberSlider<>(controller, v, 10, 100, 10));
		if (OSUtil.getOSType() == OSUtil.MAC) {
			editorOverrides.put("display.usesystemmenubar", Toggle::new);
		} else {
			ignoredItems.add("display.usesystemmenubar");
		}
		setupConfigControls(config);
	}

	/**
	 * @param controller
	 * 		Gui controller.
	 * @param config
	 * 		Keybind config.
	 */
	public ConfigPane(GuiController controller, ConfKeybinding config) {
		this(config);
		editorOverrides.put("binding.saveapp", KeybindField::new);
		editorOverrides.put("binding.save", KeybindField::new);
		editorOverrides.put("binding.find", KeybindField::new);
		editorOverrides.put("binding.rename", KeybindField::new);
		editorOverrides.put("binding.undo", KeybindField::new);
		editorOverrides.put("binding.gotodef", KeybindField::new);
		editorOverrides.put("binding.close.window", KeybindField::new);
		editorOverrides.put("binding.close.tab", KeybindField::new);
		setupConfigControls(config);
	}

	/**
	 * @param controller
	 * 		Gui controller.
	 * @param config
	 * 		Decompiler config.
	 */
	public ConfigPane(GuiController controller, ConfDecompile config) {
		this(config);
		// TODO: When the decompiler is changed, switch options are displayed
		editorOverrides.put("decompile.decompiler", EnumComboBox::new);
		editorOverrides.put("decompile.showsynthetics", Toggle::new);
		editorOverrides.put("decompile.stripdebug", Toggle::new);
		editorOverrides.put("decompile.showname", Toggle::new);
		editorOverrides.put("decompile.timeout", (w) -> new NumberSlider<>(controller, w, 1_000, 20_000, 1_000));
		hideUnsupported = true;
		setupConfigControls(config);
	}

	/**
	 * @param controller
	 * 		Gui controller.
	 * @param config
	 * 		Assembler config.
	 */
	public ConfigPane(GuiController controller, ConfAssembler config) {
		this(config);
		editorOverrides.put("assembler.verify", Toggle::new);
		editorOverrides.put("assembler.variables", Toggle::new);
		editorOverrides.put("assembler.stripdebug", Toggle::new);
		editorOverrides.put("assembler.useexistingdata", Toggle::new);
		editorOverrides.put("assembler.phantoms", Toggle::new);
		setupConfigControls(config);
	}

	/**
	 * @param controller
	 * 		Gui controller.
	 * @param config
	 * 		Updates config.
	 */
	public ConfigPane(GuiController controller, ConfUpdate config) {
		this(config);
		editorOverrides.put("update.active", Toggle::new);
		editorOverrides.put("update.lastcheck", TimestampLabel::new);
		editorOverrides.put("update.frequency", EnumComboBox::new);
		setupConfigControls(config);
	}

	/**
	 * Create a config pane for a configurable plugin.
	 *
	 * @param controller
	 * 		Gui controller.
	 * @param config
	 * 		Configurable plugin.
	 */
	public ConfigPane(GuiController controller, ConfigurablePlugin config) {
		this(config);
		config.addFieldEditors(editorOverrides);
		setupConfigControls(config);
	}

	private void setupConfigControls(Configurable config) {
		for(FieldWrapper field : config.getConfigFields()) {
			// Skip hidden values
			if(field.hidden())
				continue;
			// Skip ignored items
			if(ignoredItems.contains(field.key()))
				continue;
			// Create label node
			Node label = null;
			if (field.isTranslatable()) {
				label = new SubLabeled(field.name(), field.description());
			} else {
				label = new Label(field.key());
				label.getStyleClass().add("h2");
			}
			// Check for override editor
			if (editorOverrides.containsKey(field.key())) {
				// Add label/editor
				Node editor = editorOverrides.get(field.key()).apply(field);
				add(label, editor);
			} else if (DEFAULT_EDITORS.containsKey(field.type())) {
				Log.debug("Using default editor for value '{}', type: {}", field.key(), field.type().getName());
				Node editor = DEFAULT_EDITORS.get(field.type()).apply(field);
				add(label, editor);
			} else if (field.type().isEnum()) {
				Node editor = DEFAULT_EDITORS.get(Enum.class).apply(field);
				add(label, editor);
			}else if (!hideUnsupported) {
				add(label, new Label("Unsupported: " + field.type() + " - " + field.key()));
			}
		}
	}

	/**
	 * @return Wrapped config.
	 */
	public Configurable getConfig() {
		return config;
	}

	/**
	 * Refresh editor values.
	 */
	public void refresh() {
		Platform.runLater(() -> {
			row = 0;
			grid.getChildren().clear();
			setupConfigControls(config);
		});
	}

	static {
		// Populate default editors
		DEFAULT_EDITORS.put(boolean.class, Toggle::new);
		DEFAULT_EDITORS.put(Boolean.class, Toggle::new);
		DEFAULT_EDITORS.put(Enum.class, EnumComboBox::new);
		DEFAULT_EDITORS.put(ConfKeybinding.Binding.class, KeybindField::new);
	}
}
