package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import org.objectweb.asm.tree.MethodNode;

/**
 * Backend-aware matcher for method debug information.
 *
 * @author Matt Coley
 */
public interface MethodDebugInfoMatcher {
	/** Matcher which accepts debug information from either backend, including its absence. */
	MethodDebugInfoMatcher ANY = new MethodDebugInfoMatcher() {
		@Override
		public boolean matchesJvm(@Nonnull MethodNode method) {
			return true;
		}

		@Override
		public boolean matchesAndroid(@Nullable DebugInformation debugInfo) {
			return true;
		}
	};

	/**
	 * @param method
	 * 		JVM method node.
	 *
	 * @return {@code true} when this matcher accepts the method's debug information.
	 */
	boolean matchesJvm(@Nonnull MethodNode method);

	/**
	 * @param debugInfo
	 * 		DEX method debug information, or {@code null} when the method has none.
	 *
	 * @return {@code true} when this matcher accepts the method's debug information.
	 */
	boolean matchesAndroid(@Nullable DebugInformation debugInfo);
}
