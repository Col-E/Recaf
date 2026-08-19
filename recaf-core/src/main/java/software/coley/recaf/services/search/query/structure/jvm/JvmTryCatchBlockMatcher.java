package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.TryCatchBlockNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.TryCatchBlockMatcher;

/**
 * Matches JVM try/catch ranges and their handler labels.
 *
 * @param start
 * 		Protected-range start matcher.
 * @param end
 * 		Protected-range end matcher.
 * @param handler
 * 		Handler label matcher.
 * @param type
 * 		Exception type matcher.
 *
 * @author Matt Coley
 */
public record JvmTryCatchBlockMatcher(@Nonnull Matcher<Label> start,
                                      @Nonnull Matcher<Label> end,
                                      @Nonnull Matcher<Label> handler,
                                      @Nonnull Matcher<Type> type) implements TryCatchBlockMatcher {
	/**
	 * @param type
	 * 		Internal name of the exception type to match, or {@code null} to match against catch-all handlers.
	 *
	 * @return Matcher against the exact exception type.
	 */
	@Nonnull
	public static JvmTryCatchBlockMatcher type(@Nullable String type) {
		if (type == null)
			return new JvmTryCatchBlockMatcher(
					Matchers.any(),
					Matchers.any(),
					Matchers.any(),
					Matchers.exact(null)
			);

		return type(Type.getObjectType(type));
	}

	/**
	 * @param type
	 * 		Exception type to match.
	 *
	 * @return Matcher against the exact exception type.
	 */
	@Nonnull
	public static JvmTryCatchBlockMatcher type(@Nonnull Type type) {
		return new JvmTryCatchBlockMatcher(
				Matchers.any(),
				Matchers.any(),
				Matchers.any(),
				Matchers.exact(type)
		);
	}

	@Override
	public boolean matchesJvm(@Nonnull TryCatchBlockNode block) {
		Type exceptionType = block.type == null ?
				null :
				Type.getObjectType(block.type);
		return start.matches(block.start.getLabel())
				&& end.matches(block.end.getLabel())
				&& type.matches(exceptionType)
				&& handler.matches(block.handler.getLabel());
	}

	@Override
	public boolean matchesAndroid(@Nonnull TryCatch tryCatch,
	                              @Nonnull Handler handler) {
		return false;
	}
}
