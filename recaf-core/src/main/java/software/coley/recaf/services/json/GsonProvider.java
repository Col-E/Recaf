package software.coley.recaf.services.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.services.Service;

import java.lang.reflect.Type;
import java.util.function.Consumer;

/**
 * Manages a common {@link Gson} instance for consistent handling across Recaf.
 *
 * @author Matt Coley
 * @author Justus Garbe
 */
@ApplicationScoped
public class GsonProvider implements Service {
	public static final String SERVICE_ID = "gson-provider";
	private final GsonProviderConfig config;
	private GsonBuilder builder;

	@Inject
	public GsonProvider(@Nonnull GsonProviderConfig config) {
		this.config = config;
	}

	/**
	 * @return A gson instance from the current builder parameters.
	 */
	@Nonnull
	public Gson getGson() {
		GsonBuilder copy = getBuilderCopy();

		// Apply config to a copy of the builder.
		// We do this at this step because you cannot unset pretty-printing from the builder.
		// Thus, we'll add it as post-processing when the user requests a gson instance.
		if (config.getPrettyPrint().getValue())
			copy.setPrettyPrinting();

		return copy.create();
	}

	/**
	 * Register a type adapter factory for application-wide (de)serialization support
	 * for supported types from the factory.
	 *
	 * @param factory
	 * 		Adapter factory to register.
	 *
	 * @see GsonBuilder#registerTypeAdapterFactory(TypeAdapterFactory)
	 */
	public void addTypeAdapterFactory(@Nonnull TypeAdapterFactory factory) {
		updateBuilder(builder -> builder.registerTypeAdapterFactory(factory));
	}

	/**
	 * Register a type adapter for application-wide serialization support.
	 *
	 * @param type
	 * 		Type definition for the type adapter being registered.
	 * @param adapter
	 * 		Adapter implementation for the given type.
	 * @param <T>
	 * 		Type to adapt.
	 *
	 * @see GsonBuilder#registerTypeAdapter(Type, Object)
	 */
	public <T> void addTypeAdapter(@Nonnull Class<T> type, @Nonnull TypeAdapter<T> adapter) {
		register(type, adapter);
	}

	/**
	 * Register an instance creator for application-wide support.
	 *
	 * @param type
	 * 		Type definition for the instance creator being registered.
	 * @param creator
	 * 		Instance creator implementation for the given type.
	 * @param <T>
	 * 		Type to adapt.
	 *
	 * @see GsonBuilder#registerTypeAdapter(Type, Object)
	 */
	public <T> void addTypeInstanceCreator(@Nonnull Class<T> type, @Nonnull InstanceCreator<T> creator) {
		register(type, creator);
	}

	/**
	 * Register a type deserializer for application-wide support.
	 *
	 * @param type
	 * 		Type definition for the type deserializer being registered.
	 * @param deserializer
	 * 		Deserializer implementation for the given type.
	 * @param <T>
	 * 		Type to adapt.
	 *
	 * @see GsonBuilder#registerTypeAdapter(Type, Object)
	 */
	public <T> void addTypeDeserializer(@Nonnull Class<T> type, @Nonnull JsonDeserializer<T> deserializer) {
		register(type, deserializer);
	}

	/**
	 * Register a type serializer for application-wide support.
	 *
	 * @param type
	 * 		Type definition for the type serializer being registered.
	 * @param serializer
	 * 		Serializer implementation for the given type.
	 * @param <T>
	 * 		Type to adapt.
	 *
	 * @see GsonBuilder#registerTypeAdapter(Type, Object)
	 */
	public <T> void addTypeSerializer(@Nonnull Class<T> type, @Nonnull JsonSerializer<T> serializer) {
		register(type, serializer);
	}

	/**
	 * Register a type adapter <i>(Loose terminology, multiple types are supported)</i>
	 * for application-wide serialization support.
	 *
	 * @param type
	 * 		Type definition for the type adapter being registered.
	 * @param adapter
	 * 		Adapter implementation for the given type. Must be a {@link TypeAdapter},
	 *        {@link InstanceCreator}, {@link JsonSerializer},or {@link JsonDeserializer}.
	 *
	 * @see GsonBuilder#registerTypeAdapter(Type, Object)
	 */
	private void register(@Nonnull Class<?> type, @Nonnull Object adapter) {
		updateBuilder(builder -> builder.registerTypeAdapter(type, adapter));
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public GsonProviderConfig getServiceConfig() {
		return config;
	}

	/**
	 * @return Copy of the current {@link #builder}.
	 */
	@Nonnull
	private GsonBuilder getBuilderCopy() {
		GsonBuilder local = builder;

		// No builder set up yet, so it'd be the default.
		if (local == null)
			return new GsonBuilder();

		// Create a copy of the builder instance.
		// It has the same setup as the managed builder instance.
		return local.create().newBuilder();
	}

	/**
	 * Update the {@link #builder} instance.
	 *
	 * @param consumer
	 * 		Consumer to adapt the builder config with.
	 */
	private void updateBuilder(@Nullable Consumer<GsonBuilder> consumer) {
		GsonBuilder newBuilder = getBuilderCopy();
		if (consumer != null) consumer.accept(newBuilder);
		builder = newBuilder;
	}
}
