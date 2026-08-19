package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import me.darknet.dex.tree.definitions.instructions.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.TryCatchBlockNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.query.structure.TryCatchBlockMatcher;

/**
 * Matches a dex protected range and handler.
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
public record DexTryCatchBlockMatcher(@Nonnull Matcher<Label> start,
                                      @Nonnull Matcher<Label> end,
                                      @Nonnull Matcher<Label> handler,
                                      @Nonnull Matcher<Type> type) implements TryCatchBlockMatcher {
	@Override
	public boolean matchesJvm(@Nonnull TryCatchBlockNode block) {
		return false;
	}

	@Override
	public boolean matchesAndroid(@Nonnull TryCatch tryCatch, @Nonnull Handler handler) {
		Type exceptionType = handler.exceptionType() == null ?
				null :
				Type.getType(handler.exceptionType().descriptor());
		return start.matches(tryCatch.begin())
				&& end.matches(tryCatch.end())
				&& type.matches(exceptionType)
				&& this.handler.matches(handler.handler());
	}
}
