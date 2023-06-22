package software.coley.recaf.services.decompile;

import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.properties.builtin.CachedDecompileProperty;

/**
 * Subtype of {@link ConfigContainer} for use by {@link Decompiler} implementations.
 * <br>
 * Tracks the hash of all contained {@link ConfigValue} so that when decompilers check for
 * {@link CachedDecompileProperty} they can see if the {@link DecompileResult#getConfigHash()}
 * matches the current one of {@link #getConfigHash()}.
 *
 * @author Matt Coley
 */
public interface DecompilerConfig extends ConfigContainer {
	/**
	 * This value is compared to {@link DecompileResult#getConfigHash()} when a {@link Decompiler} implementation
	 * looks to decompile a {@link ClassInfo} and finds an existing entry in {@link CachedDecompileProperty}.
	 * <br>
	 * If the values match, the cached result can be used.
	 * Otherwise, the result must be ignored since the config difference can yield a different result.
	 *
	 * @return Unique hash of all contained {@link ConfigValue}.
	 */
	int getConfigHash();

	/**
	 * @param hash
	 * 		New hash value.
	 *
	 * @see #getConfigHash() For more detail.
	 */
	void setConfigHash(int hash);

	/**
	 * Called by implementations after they add all their values to the container.
	 *
	 * @see #getConfigHash() For more detail.
	 */
	default void registerConfigValuesHashUpdates() {
		// Initial value computation.
		update();

		// Register listeners to ensure hash is up-to-date.
		getValues().values().forEach(value ->
				value.getObservable().addChangeListener((ob, old, cur) -> update()));
	}

	private void update() {
		getValues().values().stream()
				.map(ConfigValue::getValue)
				.mapToInt(value -> value == null ? 0 : value.hashCode())
				.reduce((a, b) -> (31 * a) + b)
				.ifPresent(this::setConfigHash);
	}
}
