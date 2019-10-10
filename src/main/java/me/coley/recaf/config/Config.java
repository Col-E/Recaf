package me.coley.recaf.config;

import com.eclipsesource.json.*;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static me.coley.recaf.util.Log.*;

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

	@SuppressWarnings("unchecked")
	void load(File file) throws IOException {
		final JsonObject json = Json.parse(FileUtils.readFileToString(file, UTF_8)).asObject();
		for(FieldWrapper field : getConfigFields()) {
			String name = field.key();
			if(name == null)
				continue;
			final JsonValue value = json.get(name);
			if(value != null) {
				try {
					Class<?> type = field.type();
					if(type.equals(Boolean.TYPE))
						field.set(value.asBoolean());
					else if(type.equals(Integer.TYPE))
						field.set(value.asInt());
					else if(type.equals(Long.TYPE))
						field.set(value.asLong());
					else if(type.equals(Float.TYPE))
						field.set(value.asFloat());
					else if(type.equals(Double.TYPE))
						field.set(value.asDouble());
					else if(type.equals(String.class))
						field.set(value.asString());
					else if(type.isEnum())
						field.set(Enum.valueOf((Class<? extends Enum>) (Class<?>) field.type(), value.asString()));
					/*
					else {
						Object parsed = parse(field, value);
						if(parsed != null) {
							field.set(parsed);
						}
					}
					*/
				} catch(Exception ex) {
					error(ex, "Skipping bad option: {} - {}", file.getName(), name);
				}
			}
		}
		onLoad();
	}

	void save(File file) throws IOException {
		final JsonObject json = Json.object();
		for(FieldWrapper field : getConfigFields()) {
			String name = field.key();
			if(name == null)
				continue;
			Object value = field.get();
			Class<?> type = field.type();
			if(type.equals(Boolean.TYPE))
				json.set(name, (boolean) value);
			else if(type.equals(Integer.TYPE))
				json.set(name, (int) value);
			else if(type.equals(Long.TYPE))
				json.set(name, (long) value);
			else if(type.equals(Float.TYPE))
				json.set(name, (float) value);
			else if(type.equals(Double.TYPE))
				json.set(name, (double) value);
			else if(type.equals(String.class))
				json.set(name, (String) value);
			else if(type.isEnum())
				json.set(name, ((Enum) value).name());
			/*
			else {
				JsonValue converted = convert(field, value);
				if(converted != null) {
					json.set(name, converted);
				}
			}
			*/
		}
		StringWriter w = new StringWriter();
		json.writeTo(w, WriterConfig.PRETTY_PRINT);
		FileUtils.write(file, w.toString(), UTF_8);
	}

	/**
	 * Called on a successful load.
	 */
	protected void onLoad() {}

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
