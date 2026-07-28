package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;

/**
 * Service outline for patching intentionally malformed Java bytecode to be compliant with ASM.
 *
 * @author Matt Coley
 */
public interface ClassPatcher {
	/**
	 * Pre-processing stage before ASM reads the bytecode.
	 *
	 * @param name
	 * 		Name given by user for logging purposes.
	 * @param code
	 * 		Input bytecode.
	 *
	 * @return Output filtered bytecode, or {@code null} if no filtering was needed.
	 *
	 * @throws IOException
	 * 		When an exception patching the bytecode occurs.
	 */
	@Nullable
	byte[] prefilter(@Nullable String name, @Nonnull byte[] code) throws IOException;

	/**
	 * Patches the bytecode to be compliant with ASM.
	 *
	 * @param name
	 * 		Name given by user for logging purposes.
	 * @param code
	 * 		Input bytecode.
	 *
	 * @return Output filtered bytecode.
	 *
	 * @throws IOException
	 * 		When an exception patching the bytecode occurs.
	 */
	@Nonnull
	byte[] patch(@Nullable String name, @Nonnull byte[] code) throws IOException;
}
