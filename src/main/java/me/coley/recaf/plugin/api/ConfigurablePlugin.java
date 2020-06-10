package me.coley.recaf.plugin.api;

import javafx.scene.Node;
import me.coley.recaf.config.Conf;
import me.coley.recaf.config.Configurable;
import me.coley.recaf.config.FieldWrapper;

import java.util.Map;
import java.util.function.Function;

/**
 * Allow plugins to automatically support persistent config using the {@link Conf} annotation on fields.
 *
 * @author Matt
 */
public interface ConfigurablePlugin extends BasePlugin, Configurable {
	/**
	 * @return Title of tab to put in config menu.
	 */
	String getConfigTabTitle();

	/**
	 * Add custom controls to support editors for the configurable fields.
	 * <br>
	 * Map keys should match {@link Conf#value()}.
	 *
	 * @param editors
	 * 		Existing map to populate.
	 */
	default void addFieldEditors(Map<String, Function<FieldWrapper, Node>> editors) {}
}
