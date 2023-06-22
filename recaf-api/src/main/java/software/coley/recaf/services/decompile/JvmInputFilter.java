package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;

/**
 * Used to allow interception of bytecode passed to {@link JvmDecompiler} instances.
 *
 * @author Matt Coley
 */
public interface JvmInputFilter {
	/**
	 * @param bytecode
	 * 		Input JVM class bytecode.
	 *
	 * @return Output JVM class bytecode.
	 */
	@Nonnull
	byte[] filter(@Nonnull byte[] bytecode);
}
