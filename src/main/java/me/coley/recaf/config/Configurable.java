package me.coley.recaf.config;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import me.coley.recaf.util.Resource;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static me.coley.recaf.util.Log.error;
import static me.coley.recaf.util.Log.warn;

/**
 * A class that has configurable fields that are resolved at runtime.
 *
 * @author Matt
 */
public interface Configurable {
	/**
	 * @param path
	 * 		Path to json file of config.
	 *
	 * @throws IOException
	 * 		When the file cannot be read.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	default void load(Path path) throws IOException {
		JsonObject json = Json.parse(FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8)).asObject();
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
					else if(type.equals(Resource.class)) {
						JsonObject object = value.asObject();
						String resPath = object.getString("path", null);
						if(object.getBoolean("internal", true))
							field.set(Resource.internal(resPath));
						else
							field.set(Resource.external(resPath));
					} else if(type.equals(List.class)) {
						List<Object> list = new ArrayList<>();
						JsonArray array = value.asArray();
						// We're gonna assume our lists just hold strings
						array.forEach(v -> {
							if(v.isString())
								list.add(v.asString());
							else
								warn("Didn't properly load config for {}, expected all string arguments", name);
						});
						field.set(list);
					} else if(supported(type))
						loadType(field, type, value);
					else
						warn("Didn't load config for {}, unsure how to serialize.", name);
				} catch(Exception ex) {
					error(ex, "Skipping bad option: {} - {}", path.getFileName(), name);
				}
			}
		}
		onLoad();
	}

	/**
	 * @param path
	 * 		Path to json file of config.
	 *
	 * @throws IOException
	 * 		When the file cannot be written to.
	 */
	@SuppressWarnings("rawtypes")
	default void save(Path path) throws IOException {
		JsonObject json = Json.object();
		for(FieldWrapper field : getConfigFields()) {
			String name = field.key();
			if(name == null)
				continue;
			Object value = field.get();
			if (value == null)
				continue;
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
			else if(type.equals(Resource.class)) {
				Resource resource = (Resource) value;
				JsonObject object = new JsonObject();
				object.set("path", resource.getPath());
				object.set("internal", resource.isInternal());
				json.set(name, object);
			} else if(type.equals(List.class)) {
				JsonArray array = Json.array();
				List<?> list = field.get();
				// Don't write if empty/null
				if (list == null || list.isEmpty())
					continue;
				// We're gonna assume our lists just hold strings
				// TODO: Proper generic list writing
				list.forEach(v -> array.add(v.toString()));
				json.set(name, array);
			} else if(supported(type))
				saveType(field, type, value, json);
			else
				warn("Didn't write config for {}, unsure how to serialize type {}.", name, type.getName());
		}
		StringWriter w = new StringWriter();
		json.writeTo(w, WriterConfig.PRETTY_PRINT);
		FileUtils.write(path.toFile(), w.toString(), StandardCharsets.UTF_8);
	}

	/**
	 * @param type
	 * 		Some type.
	 *
	 * @return Config implementation supports serialization of the type.
	 */
	default boolean supported(Class<?> type) {
		if (type.equals(ConfKeybinding.Binding.class))
			return true;
		return false;
	}

	/**
	 * @param field
	 * 		Field accessor.
	 * @param type
	 * 		Field type.
	 * @param value
	 * 		Serialized representation.
	 */
	default void loadType(FieldWrapper field, Class<?> type, JsonValue value) {
		if (type.equals(ConfKeybinding.Binding.class)) {
			List<String> list = new ArrayList<>();
			JsonArray array = value.asArray();
			String name = field.key();
			array.forEach(v -> {
				if(v.isString())
					list.add(v.asString());
				else
					warn("Didn't properly load config for {}, expected all string arguments", name);
			});
			field.set(ConfKeybinding.Binding.from(list));
		}
	}

	/**
	 * @param field
	 * 		Field accessor.
	 * @param type
	 * 		Field type.
	 * @param value
	 * 		Field value.
	 * @param json
	 * 		Json to write value to.
	 */
	default void saveType(FieldWrapper field, Class<?> type, Object value, JsonObject json) {
		if (type.equals(ConfKeybinding.Binding.class)) {
			JsonArray array = Json.array();
			ConfKeybinding.Binding list = field.get();
			// Don't write if empty/null
			if (list == null || list.isEmpty())
				return;
			// Write keys
			list.forEach(array::add);
			json.set(field.key(), array);
		}
	}

	/**
	 * Called on a successful load.
	 */
	default void onLoad() {}

	/**
	 * @return Configurable fields.
	 */
	default List<FieldWrapper> getConfigFields() {
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
