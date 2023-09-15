package software.coley.recaf.info.properties;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Basic property outline.
 *
 * @param <V>
 * 		Value type.
 *
 * @author Matt Coley
 */
public interface Property<V> {
	/**
	 * @return Property key.
	 */
	@Nonnull
	String key();

	/**
	 * @return Property value.
	 */
	@Nullable
	V value();

	/**
	 * Generally, when an info type is updated in a workspace it will be copying state from the original value.
	 * When that occurs properties of the original info instance get copied to the new one. Not all properties should
	 * be copied though. Consider these two cases:
	 * <ul>
	 *     <li>{@link software.coley.recaf.info.properties.builtin.ZipCommentProperty} -
	 *     Is based on the input file content. Will never change based on info state, thus
	 *     should be copied between info instances.</li>
	 *     <li>{@link software.coley.recaf.info.properties.builtin.CachedDecompileProperty} -
	 *     Caches the decompiled code of a class file to prevent duplicate work. Changes when info state
	 *     <i>(bytecode)</i> is updated, thus should <b>NOT</b> be copied between info instances</li>
	 * </ul>
	 * By default, this returns {@code true}.
	 *
	 * @return {@code true} to represent content that should be persisted across instances.
	 * {@code false} to represent content that should be dropped between instances.
	 */
	default boolean persistent() {
		return true;
	}
}
