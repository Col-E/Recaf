package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableMap;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicMapConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Config for {@link TransformationPresetManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TransformationPresetManagerConfig extends BasicConfigContainer implements ServiceConfig {
	private final PresetMap presets = new PresetMap();

	@Inject
	public TransformationPresetManagerConfig() {
		super(ConfigGroups.SERVICE_UI, TransformationPresetManager.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicMapConfigValue<>("preset-map", PresetMap.class, String.class, TransformationPreset.class, presets, true));
	}

	/**
	 * @return Map of saved presets, keyed by normalized preset name.
	 */
	@Nonnull
	public PresetMap getPresets() {
		return presets;
	}

	/**
	 * Map type to hold transformation presets.
	 */
	public static class PresetMap extends ObservableMap<String, TransformationPreset, Map<String, TransformationPreset>> {
		public PresetMap() {
			super(HashMap::new);
		}
	}
}
