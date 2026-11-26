package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;

/**
 * Model of a parameter that a {@link ClassTransformer} can read from the {@link TransformationParameters}
 * provided to each transformation run.
 *
 * @param key
 * 		Parameter key, assumed to also be prefixed with the transformer's identifier.
 * @param type
 * 		Type of the value.
 * @param defaultValue
 * 		Value used when a run or preset omits the parameter.
 * @param <T>
 * 		Parameter value type.
 *
 * @author Matt Coley
 * @see ClassTransformer#getParameterDefinitions()
 */
public record TransformationParameter<T>(@Nonnull String key, @Nonnull Class<T> type, @Nonnull T defaultValue) {
	/**
	 * @return Translation key for the parameter.
	 */
	@Nonnull
	public String translationKey() {
		return "deobf." + key();
	}
}
