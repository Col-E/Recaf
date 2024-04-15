package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.properties.builtin.CachedDecompileProperty;
import software.coley.recaf.services.decompile.filter.OutputTextFilter;

/**
 * Common decompiler operations.
 *
 * @author Matt Coley
 * @see JvmDecompiler For decompiling JVM bytecode.
 * @see AndroidDecompiler For decompiling Android/Dalvik bytecode.
 * @see DecompilerConfig For config management of decompiler values,
 * and ensuring {@link CachedDecompileProperty} values are compatible with current settings.
 */
public interface Decompiler {
	/**
	 * @return Decompiler name.
	 */
	@Nonnull
	String getName();

	/**
	 * @return Decompiler version.
	 */
	@Nonnull
	String getVersion();

	/**
	 * @return Decompiler config.
	 */
	@Nonnull
	DecompilerConfig getConfig();

	/**
	 * Adds a filter which operates on the decompiler output, before the contents are returned to the user.
	 *
	 * @param filter
	 * 		Filter to add.
	 *
	 * @return {@code true} on successful addition.
	 * {@code false} if the filter has already been added.
	 */
	boolean addOutputTextFilter(@Nonnull OutputTextFilter filter);

	/**
	 * Removes a filter which operates on the decompiler output, before the contents are returned to the user.
	 *
	 * @param filter
	 * 		Filter to remove.
	 *
	 * @return {@code true} on successful removal.
	 * {@code false} if the filter was not already registered.
	 */
	boolean removeOutputTextFilter(@Nonnull OutputTextFilter filter);
}
