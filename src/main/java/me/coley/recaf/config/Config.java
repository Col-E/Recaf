package me.coley.recaf.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Config base.
 *
 * @author Matt
 */
public abstract class Config {
	private final String name;

	/**
	 * @param name
	 * 		Group name.
	 */
	Config(String name) {
		this.name = name;
	}

	/**
	 * @return Group name.
	 */
	public String getName() {
		return name;
	}

	void load(File file) throws IOException {
		// TODO: config loading
	}

	void save(File file) throws IOException {
		// TODO: config saving
	}

	/**
	 * @return Configurable fields.
	 */
	public List<FieldWrapper> getConfigFields() {
		List<FieldWrapper> fields = new ArrayList<>();
		for (Field field : getClass().getDeclaredFields()){
			Conf conf = field.getAnnotation(Conf.class);
			if (conf == null)
				continue;
			field.setAccessible(true);
			fields.add(new FieldWrapper(this, field, conf));
		}
		return fields;
	}
}
