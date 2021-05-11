package me.coley.recaf.config;

import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.ReflectionUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Config value persistence and general management.
 *
 * @author Matt Coley
 */
public class ConfigRegistry {
	private static final Logger logger = Logging.get(ConfigRegistry.class);
	private static final Map<String, String> idToDisplay = new TreeMap<>();
	private static final Map<String, Supplier<?>> idToGetter = new TreeMap<>();
	private static final Map<String, Consumer<?>> idToSetter = new TreeMap<>();
	private static final List<ConfigContainer> containers = new ArrayList<>();

	/**
	 * @param container
	 * 		Field container instance.
	 */
	public static void register(ConfigContainer container) {
		logger.debug("Register config container: " + container.displayName());
		// Cache fields
		for (Field field : container.getClass().getDeclaredFields()) {
			field.setAccessible(true);
			String id = getFieldID(container, field);
			if (id == null)
				continue;
			idToDisplay.put(id, getFieldDisplay(field, id));
			idToGetter.put(id, createGetter(container, field));
			idToSetter.put(id, createSetter(container, field));
		}
		// Store ref to container
		containers.add(container);
	}

	/**
	 * Save registered {@link ConfigContainer} values.
	 */
	public static void load() {
		for (ConfigContainer container : containers) {
			// TODO: JSON persistence
		}
	}

	/**
	 * Save registered {@link ConfigContainer} values.
	 */
	public static void save() {
		for (ConfigContainer container : containers) {
			// TODO: JSON persistence
		}
	}

	/**
	 * @param id
	 * 		Config field id, see {@link #getFieldID(ConfigContainer, Field)}
	 * @param <T>
	 * 		Field type.
	 *
	 * @return Getter for the field.
	 */
	@SuppressWarnings("unchecked")
	public <T> Supplier<T> getter(String id) {
		return (Supplier<T>) idToGetter.get(id);
	}

	/**
	 * @param id
	 * 		Config field id, see {@link #getFieldID(ConfigContainer, Field)}
	 * @param <T>
	 * 		Field type.
	 *
	 * @return Setter for the field.
	 */
	@SuppressWarnings("unchecked")
	public <T> Consumer<T> setter(String id) {
		return (Consumer<T>) idToSetter.get(id);
	}


	/**
	 * @param container
	 * 		Field container instance.
	 * @param field
	 * 		Field instance.
	 *
	 * @return Getter for field value.
	 */
	private static Supplier<?> createGetter(ConfigContainer container, Field field) {
		return () -> ReflectionUtil.quietGet(container, field);
	}

	/**
	 * @param container
	 * 		Field container instance.
	 * @param field
	 * 		Field instance.
	 *
	 * @return Setter for field value.
	 */
	private static Consumer<?> createSetter(ConfigContainer container, Field field) {
		return value -> ReflectionUtil.quietSet(container, field, value);
	}

	/**
	 * @param field
	 * 		Field instance.
	 * @param key
	 * 		Translation key lookup.
	 *
	 * @return Display text for the configurable field.
	 */
	public static String getFieldDisplay(Field field, String key) {
		// Check cached values
		if (idToDisplay.containsKey(key))
			return idToDisplay.get(key);
		// Get display
		ConfigID id = field.getAnnotation(ConfigID.class);
		if (id == null)
			return null;
		// Get translatable text, if possible. Otherwise use the value as-is.
		if (id.translatable())
			return Lang.get(key);
		else
			return id.value();
	}

	/**
	 * @param container
	 * 		Field container instance.
	 * @param field
	 * 		Field instance.
	 *
	 * @return Internal identifier string, or {@code null} if the field is not marked with {@link ConfigID}.
	 */
	public static String getFieldID(ConfigContainer container, Field field) {
		// Get config field identity
		ConfigID id = field.getAnnotation(ConfigID.class);
		if (id == null)
			return null;
		String key = id.value();
		// Add sub-group
		Group group = field.getAnnotation(Group.class);
		if (group != null)
			key = group.value() + "." + key;
		// Add main group
		key = container.internalName() + "." + key;
		return key;
	}
}
