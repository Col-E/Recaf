package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.ui.control.richtext.suggest.java.providers.IdentifierCompletionProvider;
import software.coley.recaf.ui.control.richtext.suggest.java.providers.ImportCompletionProvider;
import software.coley.recaf.ui.control.richtext.suggest.java.providers.JavaCompletionProvider;
import software.coley.recaf.ui.control.richtext.suggest.java.providers.MemberCompletionProvider;
import software.coley.recaf.ui.control.richtext.suggest.java.providers.PackageCompletionProvider;
import software.coley.recaf.ui.control.richtext.suggest.java.providers.TypeCompletionProvider;

import java.util.List;

/**
 * Dispatches completion work to the provider matching the current lexical context.
 *
 * @author Matt Coley
 */
public class JavaCompletionEngine {
	private final TabCompletionConfig config;
	private final JavaCompletionRanker completionRanker;
	private final JavaCompletionProvider memberProvider;
	private final JavaCompletionProvider identifierProvider;
	private final JavaCompletionProvider typeProvider;
	private final JavaCompletionProvider importProvider;
	private final JavaCompletionProvider packageProvider;

	public JavaCompletionEngine(@Nonnull TabCompletionConfig config) {
		this(config,
				new JavaCompletionRanker(config),
				new MemberCompletionProvider(),
				new IdentifierCompletionProvider(),
				new TypeCompletionProvider(),
				new ImportCompletionProvider(),
				new PackageCompletionProvider());
	}

	public JavaCompletionEngine(@Nonnull TabCompletionConfig config,
	                            @Nonnull JavaCompletionRanker completionRanker,
	                            @Nonnull JavaCompletionProvider memberProvider,
	                            @Nonnull JavaCompletionProvider identifierProvider,
	                            @Nonnull JavaCompletionProvider typeProvider,
	                            @Nonnull JavaCompletionProvider importProvider,
	                            @Nonnull JavaCompletionProvider packageProvider) {
		this.config = config;
		this.completionRanker = completionRanker;
		this.memberProvider = memberProvider;
		this.identifierProvider = identifierProvider;
		this.typeProvider = typeProvider;
		this.importProvider = importProvider;
		this.packageProvider = packageProvider;
	}

	/**
	 * Compute completions for the given session and context.
	 *
	 * @param session
	 * 		The session is used to track state across multiple completion requests, such as caching results or tracking user selection history.
	 * @param context
	 * 		The context provides information about the current lexical state at the caret.
	 *
	 * @return List of sorted and filtered completions based on the context and session state.
	 */
	@Nonnull
	public List<JavaCompletion> compute(@Nonnull JavaCompletionSession session, @Nonnull JavaLexicalContext context) {
		// Delegate out to relevant provider based on the context kind.
		// Each provider will have its own logic for how to compute completions.
		List<JavaCompletion> completions = switch (context.kind()) {
			case NONE -> List.of();
			case MEMBER, METHOD_REFERENCE -> memberProvider.complete(session, context);
			case IMPORT -> importProvider.complete(session, context);
			case PACKAGE -> packageProvider.complete(session, context);
			case TYPE -> typeProvider.complete(session, context);
			case IDENTIFIER -> identifierProvider.complete(session, context);
		};
		return completionRanker.rank(completions.stream()
				.filter(completion -> completion.insertionText().length() <= config.getMaxCompletionLength())
				.toList());
	}
}
