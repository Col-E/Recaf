package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableMap;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.config.ConfigManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages saved transformation presets.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TransformationPresetManager implements Service {
	public static final String SERVICE_ID = "transformation-presets";
	private final TransformationPresetManagerConfig config;

	@Inject
	public TransformationPresetManager(@Nonnull TransformationPresetManagerConfig config) {
		this.config = config;
	}

	/**
	 * @return Saved preset names, sorted.
	 */
	@Nonnull
	public List<String> getPresetNames() {
		return config.getPresets().keySet().stream().sorted().toList();
	}

	/**
	 * @param name
	 * 		Preset name.
	 *
	 * @return Preset for the given name, or {@code null} if none exists.
	 */
	@Nullable
	public TransformationPreset getPreset(@Nonnull String name) {
		return config.getPresets().get(ConfigManager.normalizeProfileName(name));
	}

	/**
	 * Applies persisted values to the current parameter definitions.
	 * <p>
	 * Missing and invalid values fall back to the definition default.
	 *
	 * @param definitions
	 * 		Current transformer parameter definitions.
	 * @param savedValues
	 * 		Persisted parameter values.
	 *
	 * @return Mutable parameter values initialized from defaults and overlaid with valid saved values.
	 */
	@Nonnull
	public static Map<String, Object> mapParameters(@Nonnull List<TransformationParameter<?>> definitions,
	                                                @Nonnull Map<String, Object> savedValues) {
		Map<String, Object> values = new HashMap<>();
		for (TransformationParameter<?> definition : definitions) {
			Object raw = savedValues.get(definition.key());
			values.put(definition.key(), mapValue(definition, raw));
		}
		return values;
	}

	@Nonnull
	private static Object mapValue(@Nonnull TransformationParameter<?> definition, @Nullable Object raw) {
		final Class<?> type = definition.type();
		return switch (raw) {
			case Number number when (type == int.class || type == Integer.class) -> number.intValue();
			case Boolean bool when (type == boolean.class || type == Boolean.class) -> bool;
			case Enum<?> enumValue when type.isInstance(enumValue) -> enumValue;
			case String string when type == String.class -> string;
			case String string when type.isEnum() -> {
				Object defaultValue = definition.defaultValue();
				for (Object constant : type.getEnumConstants())
					if (constant instanceof Enum<?> enumConstant && enumConstant.name().equals(string))
						yield constant;
				yield defaultValue;
			}
			case null, default -> definition.defaultValue();
		};
	}

	/**
	 * Save or replace a preset under the given name.
	 *
	 * @param name
	 * 		Preset name. Must not be {@code null} or empty.
	 * @param preset
	 * 		Preset to store.
	 *
	 * @return The normalized name the preset was stored under.
	 *
	 * @throws IllegalArgumentException
	 * 		When the name cannot be normalized into a valid preset identifier.
	 */
	@Nonnull
	public String putPreset(@Nonnull String name, @Nonnull TransformationPreset preset) {
		String normalized = ConfigManager.normalizeProfileName(name);
		config.getPresets().put(normalized, preset);
		return normalized;
	}

	/**
	 * Remove a preset by name.
	 *
	 * @param name
	 * 		Preset name to remove.
	 */
	public void removePreset(@Nonnull String name) {
		config.getPresets().remove(ConfigManager.normalizeProfileName(name));
	}

	/**
	 * @return Observable map of presets.
	 */
	@Nonnull
	public ObservableMap<String, TransformationPreset, Map<String, TransformationPreset>> getPresets() {
		return config.getPresets();
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public TransformationPresetManagerConfig getServiceConfig() {
		return config;
	}
}
