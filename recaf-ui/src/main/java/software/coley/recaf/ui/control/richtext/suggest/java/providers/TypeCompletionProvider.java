package software.coley.recaf.ui.control.richtext.suggest.java.providers;

import jakarta.annotation.Nonnull;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletion;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionFactory;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionSession;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaLexicalContext;
import software.coley.recaf.ui.control.richtext.suggest.lookups.CompletionMatching;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeCandidate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider for type completions.
 *
 * @author Matt Coley
 */
public final class TypeCompletionProvider implements JavaCompletionProvider {
	@Nonnull
	@Override
	public List<JavaCompletion> complete(@Nonnull JavaCompletionSession session, @Nonnull JavaLexicalContext context) {
		Map<String, JavaCompletion> completions = new LinkedHashMap<>();
		String partial = context.partialText();
		boolean annotationOnly = context.annotationOnly();

		for (TypeCandidate candidate : session.visibleTypes().values()) {
			if (annotationOnly && !candidate.annotation())
				continue;
			if (!CompletionMatching.matchesPrefix(candidate.simpleName(), partial))
				continue;
			JavaCompletion.addOrReplace(completions, JavaCompletionFactory.typeCompletion(candidate,
					30 + CompletionMatching.prefixPenalty(candidate.simpleName(), partial)));
		}
		return new ArrayList<>(completions.values());
	}
}
