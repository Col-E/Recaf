package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * Preset configuration for a transformation run.
 *
 * @param transformers
 * 		Transformers to run in the preset.
 * @param maxPasses
 * 		Maximum number of passes to run the transformers.
 * @param dropFaultyClasses
 * 		Whether to drop classes that fail to transform.
 * @param scopeMode
 * 		Package filtering mode for the run.
 * @param packagePrefixes
 * 		Package prefixes used for filtering classes based on {@link #scopeMode}.
 *
 * @author Matt Coley
 */
public record TransformationPreset(@Nonnull List<TransformerPreset> transformers, int maxPasses,
                                   boolean dropFaultyClasses, @Nonnull ScopeMode scopeMode,
                                   @Nonnull List<String> packagePrefixes) {
	/**
	 * Package filtering mode for the run.
	 */
	public enum ScopeMode {
		/** No package filtering is applied. */
		NONE,
		/** Classes matching any package prefix are skipped. */
		BLACKLIST,
		/** Only classes matching a package prefix are transformed; an empty list matches all classes. */
		WHITELIST
	}

	/**
	 * Transformer configuration for a single transformer in the preset.
	 *
	 * @param transformerClassName
	 * 		Class name of the transformer to run.
	 * @param parameters
	 * 		Transformer-specific parameters to apply.
	 */
	public record TransformerPreset(@Nonnull String transformerClassName, @Nonnull Map<String, Object> parameters) {}
}
