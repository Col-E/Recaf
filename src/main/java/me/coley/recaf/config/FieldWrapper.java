package me.coley.recaf.config;

import me.coley.recaf.util.LangUtil;

import java.lang.reflect.Field;

import static me.coley.recaf.util.Log.*;

/**
 * Config field wrapper.
 *
 * @author Matt
 */
public class FieldWrapper {
	private final Configurable instance;
	private final Field field;
	private final Conf conf;

	/**
	 * @param instance
	 * 		Configurable object instance.
	 * @param field
	 * 		Field of configurable value.
	 * @param conf
	 * 		Annotation on field.
	 */
	public FieldWrapper(Configurable instance, Field field, Conf conf) {
		this.instance = instance;
		this.field = field;
		this.conf = conf;
	}

	/**
	 * @return Translation key.
	 */
	public final String key() {
		return conf.value();
	}

	/**
	 * @return {@code true} when the config key is translatable.
	 */
	public final boolean isTranslatable() {
		return !conf.noTranslate();
	}

	/**
	 * @return Translated name.
	 */
	public final String name() {
		return LangUtil.translate(key() + ".name");
	}

	/**
	 * @return Translated description.
	 */
	public final String description() {
		return LangUtil.translate(key() + ".desc");
	}

	/**
	 * @return {@code true} if the field is not intended to be shown.
	 */
	public final boolean hidden() {
		return conf.hide();
	}

	/**
	 * @param <T>
	 * 		Generic type of class.
	 *
	 * @return Declared type of the field.
	 */
	@SuppressWarnings("unchecked")
	public final <T> Class<T> type() {
		return (Class<T>) field.getType();
	}

	/**
	 * @param <T>
	 * 		Field value type.
	 *
	 * @return Field value.
	 */
	@SuppressWarnings("unchecked")
	public final <T> T get() {
		try {
			return (T) field.get(instance);
		} catch(IllegalAccessException ex) {
			error(ex, "Failed to fetch field value: {}", key());
			return null;
		}
	}

	/**
	 * @param value
	 * 		Value to set.
	 */
	public final void set(Object value) {
		try {
			field.set(instance, value);
		} catch(IllegalAccessException ex) {
			error(ex, "Failed to set field value: {}", key());
		}
	}
}
