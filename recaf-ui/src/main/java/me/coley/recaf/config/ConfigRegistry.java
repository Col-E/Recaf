package me.coley.recaf.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import javafx.beans.property.*;
import javafx.beans.value.*;
import me.coley.recaf.ui.pane.OutlinePane;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.Directories;
import me.coley.recaf.util.ReflectUtil;
import me.coley.recaf.util.UncheckedBiConsumer;
import me.coley.recaf.util.UncheckedFunction;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
	private static final Gson gson;

	static {
		GsonBuilder builder = new GsonBuilder();
		registerTypeAdapter(builder, WritableDoubleValue.class, (w, v) -> w.value(v.get()), r -> new SimpleDoubleProperty(r.nextDouble()));
		registerTypeAdapter(builder, WritableFloatValue.class, (w, v) -> w.value(v.get()), r -> new SimpleFloatProperty(((float) r.nextDouble())));
		registerTypeAdapter(builder, WritableIntegerValue.class, (w, v) -> w.value(v.get()), r -> new SimpleIntegerProperty(r.nextInt()));
		registerTypeAdapter(builder, WritableLongValue.class, (w, v) -> w.value(v.get()), r -> new SimpleLongProperty(r.nextLong()));
		registerTypeAdapter(builder, WritableBooleanValue.class, (w, v) -> w.value(v.get()), r -> new SimpleBooleanProperty(r.nextBoolean()));
		registerTypeAdapter(builder, WritableStringValue.class, (w, v) -> w.value(v.get()), r -> new SimpleStringProperty(r.nextString()));
		registerEnumTypeAdapter(builder, OutlinePane.MemberType.class);
		registerEnumTypeAdapter(builder, OutlinePane.Visibility.class);
		gson = builder.setPrettyPrinting().create();
	}

	private static final Map<String, String> idToDisplay = new TreeMap<>();
	private static final Map<String, Supplier<?>> idToGetter = new TreeMap<>();
	private static final Map<String, Consumer<?>> idToSetter = new TreeMap<>();
	private static final List<ConfigContainer> containers = new ArrayList<>();

	/**
	 * @param container
	 * 		Field container instance.
	 */
	public static void register(ConfigContainer container) {
		logger.debug("Register config container: " + container.internalName());
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
	 *
	 * @throws IOException
	 * 		When the config folder cannot be created, or when a {@link ConfigContainer} cannot be read.
	 */
	public static void load() throws IOException {
		logger.debug("Loading stored config containers");
		Path configDirectory = Directories.getConfigDirectory();
		if (!Files.isDirectory(configDirectory)) {
			Files.createDirectories(configDirectory);
		}
		GsonBuilder builder = ConfigRegistry.gson.newBuilder();
		for (ConfigContainer container : containers) {
			builder.registerTypeAdapter(container.getClass(), (InstanceCreator<ConfigContainer>) __ -> container);
		}
		Gson gson = builder.create();
		for (ConfigContainer container : containers) {
			Path containerPath = configDirectory.resolve(container.internalName() + ".json");
			if (Files.isRegularFile(containerPath)) {
				try (BufferedReader reader = Files.newBufferedReader(containerPath, StandardCharsets.UTF_8)) {
					gson.fromJson(reader, container.getClass());
				}
				container.onLoad();
			}
		}
	}

	/**
	 * Save registered {@link ConfigContainer} values.
	 *
	 * @throws IOException
	 * 		When the config folder cannot be created, or when a {@link ConfigContainer} cannot be written.
	 */
	public static void save() throws IOException {
		logger.debug("Saving config containers");
		Path configDirectory = Directories.getConfigDirectory();
		if (!Files.isDirectory(configDirectory)) {
			Files.createDirectories(configDirectory);
		}
		for (ConfigContainer container : containers) {
			Path containerPath = configDirectory.resolve(container.internalName() + ".json");
			String json = gson.toJson(container);
			Files.write(containerPath, json.getBytes(StandardCharsets.UTF_8));
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
		return () -> ReflectUtil.quietGet(container, field);
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
		return value -> ReflectUtil.quietSet(container, field, value);
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

	private static <T> void registerTypeAdapter(GsonBuilder builder, Class<T> base, UncheckedBiConsumer<JsonWriter, T> write, UncheckedFunction<JsonReader, T> read) {
		builder.registerTypeAdapterFactory(TypeAdapters.newTypeHierarchyFactory(base, new TypeAdapter<>() {
			@Override
			public void write(JsonWriter out, T value) throws IOException {
				write.accept(out, value);
			}

			@Override
			public T read(JsonReader in) throws IOException {
				return read.apply(in);
			}
		}));
	}

	private static <E extends Enum<E>> void registerEnumTypeAdapter(GsonBuilder builder, Class<E> enumClazz) {
		builder.registerTypeAdapterFactory(TypeAdapters.newFactory((TypeToken<ObjectProperty<E>>) TypeToken.getParameterized(ObjectProperty.class, enumClazz), new TypeAdapter<>() {
			@Override
			public void write(JsonWriter out, ObjectProperty<E> value) throws IOException {
				out.value(value.get().name());
			}

			@Override
			public ObjectProperty<E> read(JsonReader in) throws IOException {
				return new SimpleObjectProperty<>(Enum.valueOf(enumClazz, in.nextString()));
			}
		}));
	}
}
