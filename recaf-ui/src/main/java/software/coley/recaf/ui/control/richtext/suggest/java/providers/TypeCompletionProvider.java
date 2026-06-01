package software.coley.recaf.ui.control.richtext.suggest.java.providers;

import jakarta.annotation.Nonnull;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletion;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionSession;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaLexicalContext;
import software.coley.recaf.ui.control.richtext.suggest.java.lookups.VisibleTypeLookup;

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
	private final VisibleTypeLookup visibleTypeLookup = new VisibleTypeLookup();

	@Nonnull
	@Override
	public List<JavaCompletion> complete(@Nonnull JavaCompletionSession session, @Nonnull JavaLexicalContext context) {
		Map<String, JavaCompletion> completions = new LinkedHashMap<>();
		visibleTypeLookup.addVisibleTypeCompletions(session, completions, context.partialText(), context.annotationOnly(), 30);
		return new ArrayList<>(completions.values());
	}
}
