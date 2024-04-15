package software.coley.recaf.services.decompile.filter;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.decompile.JvmDecompiler;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Collection;

/**
 * Used to allow interception of bytecode passed to {@link JvmDecompiler} instances.
 *
 * @author Matt Coley
 */
public interface JvmBytecodeFilter {
	/**
	 * @param workspace
	 * 		The workspace the class is from.
	 * @param initialClassInfo
	 * 		Initial information about the class, before any filtering <i>(by other filters)</i> has been applied.
	 * 		Contains a reference to the original bytecode.
	 * @param bytecode
	 * 		Input JVM class bytecode. May already be modified from the original bytecode by another filter.
	 *
	 * @return Output JVM class bytecode.
	 */
	@Nonnull
	byte[] filter(@Nonnull Workspace workspace, @Nonnull JvmClassInfo initialClassInfo, @Nonnull byte[] bytecode);

	/**
	 * @param workspace
	 * 		The workspace the class is from.
	 * @param initialClassInfo
	 * 		Initial information about the class, before any filtering <i>(by other filters)</i> has been applied.
	 * @param bytecodeFilters
	 * 		Collection of filters to apply to the class.
	 *
	 * @return Filtered class model.
	 */
	@Nonnull
	static JvmClassInfo applyFilters(@Nonnull Workspace workspace, @Nonnull JvmClassInfo initialClassInfo,
	                                 @Nonnull Collection<JvmBytecodeFilter> bytecodeFilters) {
		JvmClassInfo filteredBytecode;
		if (bytecodeFilters.isEmpty()) {
			filteredBytecode = initialClassInfo;
		} else {
			boolean dirty = false;
			byte[] bytecode = initialClassInfo.getBytecode();
			for (JvmBytecodeFilter filter : bytecodeFilters) {
				byte[] filtered = filter.filter(workspace, initialClassInfo, bytecode);
				if (filtered != bytecode) {
					bytecode = filtered;
					dirty = true;
				}
			}
			filteredBytecode = dirty ? initialClassInfo.toJvmClassBuilder().adaptFrom(bytecode).build() : initialClassInfo;
		}
		return filteredBytecode;
	}
}
