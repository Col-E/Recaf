package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import org.objectweb.asm.tree.TryCatchBlockNode;

/**
 * Backend-neutral try/catch matcher.
 *
 * @author Matt Coley
 */
public interface TryCatchBlockMatcher {
	/**
	 * @param block
	 * 		JVM try/catch block.
	 *
	 * @return {@code true} when accepted.
	 */
	boolean matchesJvm(@Nonnull TryCatchBlockNode block);

	/**
	 * @param tryCatch
	 * 		Dex protected range.
	 * @param handler
	 * 		Dex handler.
	 *
	 * @return {@code true} when accepted.
	 */
	boolean matchesAndroid(@Nonnull TryCatch tryCatch, @Nonnull Handler handler);
}
