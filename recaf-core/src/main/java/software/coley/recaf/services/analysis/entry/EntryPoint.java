package software.coley.recaf.services.analysis.entry;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;

/**
 * Discovered entry point.
 *
 * @param kind
 * 		Entry point kind descriptor.
 * @param classPath
 * 		Path to the owning class.
 * @param memberPath
 * 		Optional path to the entry point member.
 * 		May be {@code null} when the class itself is considered the entry point target.
 *
 * @author Matt Coley
 */
public record EntryPoint(@Nonnull EntryPointKind kind,
                         @Nonnull ClassPathNode classPath,
                         @Nullable ClassMemberPathNode memberPath) {
	/**
	 * @return Path to the specific entry target.
	 */
	@Nonnull
	public PathNode<?> targetPath() {
		return memberPath != null ? memberPath : classPath;
	}
}
