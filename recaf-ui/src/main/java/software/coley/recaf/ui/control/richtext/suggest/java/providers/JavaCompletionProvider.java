package software.coley.recaf.ui.control.richtext.suggest.java.providers;

import jakarta.annotation.Nonnull;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletion;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionSession;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaLexicalContext;

import java.util.List;

/**
 * Provider outline for Java completions.
 *
 * @author Matt Coley
 */
public interface JavaCompletionProvider {
	/**
	 * Generate completions for the given session and context.
	 *
	 * @param session
	 * 		The session to pull data from, like the resolver and type index.
	 * @param context
	 * 		The context to generate completions for.
	 *
	 * @return Completions for the given session and context.
	 */
	@Nonnull
	List<JavaCompletion> complete(@Nonnull JavaCompletionSession session, @Nonnull JavaLexicalContext context);
}
