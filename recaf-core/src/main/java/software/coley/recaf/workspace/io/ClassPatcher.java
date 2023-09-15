package software.coley.recaf.workspace.io;

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
