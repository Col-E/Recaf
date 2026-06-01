package software.coley.recaf.ui.control.richtext.suggest.java;

import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.suggest.CompletionPopup;
import software.coley.recaf.ui.control.richtext.suggest.TabCompleter;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.ui.control.richtext.suggest.java.providers.IdentifierCompletionProvider;
import software.coley.recaf.ui.control.richtext.suggest.java.providers.ImportCompletionProvider;
import software.coley.recaf.ui.control.richtext.suggest.java.providers.MemberCompletionProvider;
import software.coley.recaf.ui.control.richtext.suggest.java.providers.PackageCompletionProvider;
import software.coley.recaf.ui.control.richtext.suggest.java.providers.TypeCompletionProvider;
import software.coley.recaf.ui.control.richtext.suggest.java.typeindex.JavaTypeIndexService;

import java.util.List;

/**
 * Tab completion for Java, backed by workspace type information and source-model resolutions.
 *
 * @author Matt Coley
 */
public class JavaTabCompleter implements TabCompleter<JavaCompletion> {
	private final JavaCompletionContext contextActionSupport;
	private final JavaTypeIndexService typeIndexService;
	private final JavaLexicalContextParser lexicalContextParser;
	private final JavaCompletionEngine completionEngine;
	private final JavaCompletionRanker completionRanker;
	private final CellConfigurationService configurationService;
	private final TabCompletionConfig config;
	private CompletionPopup<JavaCompletion> completionPopup;
	private CodeArea area;
	private JavaLexicalContext context = JavaLexicalContext.none();

	public JavaTabCompleter(@Nonnull JavaCompletionContext contextActionSupport,
	                        @Nonnull CellConfigurationService configurationService,
	                        @Nonnull JavaTypeIndexService typeIndexService,
	                        @Nonnull TabCompletionConfig config) {
		this(contextActionSupport, configurationService, typeIndexService, config, createEngineAndRanker(config));
	}

	private JavaTabCompleter(@Nonnull JavaCompletionContext contextActionSupport,
	                         @Nonnull CellConfigurationService configurationService,
	                         @Nonnull JavaTypeIndexService typeIndexService,
	                         @Nonnull TabCompletionConfig config,
	                         @Nonnull CompletionImplementation completionImplementation) {
		this(contextActionSupport,
				typeIndexService,
				new JavaLexicalContextParser(),
				completionImplementation.engine(),
				completionImplementation.ranker(),
				null,
				configurationService,
				config);
	}

	private JavaTabCompleter(@Nonnull JavaCompletionContext contextActionSupport,
	                         @Nonnull JavaTypeIndexService typeIndexService,
	                         @Nonnull JavaLexicalContextParser lexicalContextParser,
	                         @Nonnull JavaCompletionEngine completionEngine,
	                         @Nonnull JavaCompletionRanker completionRanker,
	                         @Nullable CompletionPopup<JavaCompletion> completionPopup,
	                         @Nonnull CellConfigurationService configurationService,
	                         @Nonnull TabCompletionConfig config) {
		this.contextActionSupport = contextActionSupport;
		this.typeIndexService = typeIndexService;
		this.lexicalContextParser = lexicalContextParser;
		this.completionEngine = completionEngine;
		this.completionRanker = completionRanker;
		this.completionPopup = completionPopup;
		this.configurationService = configurationService;
		this.config = config;
	}

	@Override
	public boolean requestCompletion(@Nonnull KeyEvent event) {
		if (!contextActionSupport.isCompletionAvailable())
			return false;

		recomputeContext();

		return context.kind() != ContextKind.NONE &&
				getPopup().isShowing() &&
				getPopup().doComplete(context.partialText());
	}

	@Nonnull
	@Override
	public List<JavaCompletion> computeCurrentCompletions() {
		if (!contextActionSupport.isCompletionAvailable())
			return List.of();
		return completionEngine.compute(new JavaCompletionSession(contextActionSupport, typeIndexService,
				area == null ? -1 : area.getCaretPosition()), context);
	}

	@Override
	public void onFineTextUpdate(@Nonnull PlainTextChange changes) {
		recomputeContext();
	}

	@Override
	public void onRoughTextUpdate(@Nonnull List<PlainTextChange> changes) {
		// no-op
	}

	@Override
	public boolean isSpecialCompletableKeyCode(@Nullable KeyCode code) {
		return code == KeyCode.SEMICOLON;
	}

	@Override
	public void install(@Nonnull Editor editor) {
		// Get the area and install the completion popup on it.
		area = editor.getCodeArea();
		getPopup().install(area, this);

		// Recompute the context to ensure it's up-to-date with the current editor state.
		recomputeContext();
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		// Remove the completion popup from the editor.
		getPopup().uninstall();

		// Clear references to the editor and context.
		area = null;
		context = JavaLexicalContext.none();
	}

	/**
	 * Recompute the lexical context based on the current state of the editor.
	 */
	private void recomputeContext() {
		CodeArea localArea = area;
		if (localArea == null) {
			context = JavaLexicalContext.none();
			return;
		}
		context = lexicalContextParser.parse(localArea.getText(), localArea.getCaretPosition());
	}

	/**
	 * @return Popup instance, lazily created on first access.
	 */
	@Nonnull
	private CompletionPopup<JavaCompletion> getPopup() {
		// This lazy initialization pattern is mostly used to avoid an immediate instantiation of the popup
		// for when we want to unit test completion capabilities. Tests are headless, which doesn't gel with popups.
		CompletionPopup<JavaCompletion> localPopup = completionPopup;
		if (localPopup == null) {
			localPopup = new JavaCompletionPopup(config, configurationService, lexicalContextParser,
					() -> context, completionRanker);
			completionPopup = localPopup;
		}
		return localPopup;
	}

	/**
	 * @param context
	 * 		New context to set.
	 */
	@VisibleForTesting
	protected void setContext(@Nonnull JavaLexicalContext context) {
		this.context = context;
	}

	/**
	 * @param config
	 * 		Configuration to use for creating the completion support.
	 *
	 * @return Engine and ranker pair to use for computing and ranking completions.
	 */
	@Nonnull
	private static CompletionImplementation createEngineAndRanker(@Nonnull TabCompletionConfig config) {
		JavaCompletionRanker ranker = new JavaCompletionRanker(config);
		return new CompletionImplementation(
				new JavaCompletionEngine(config, ranker,
						new MemberCompletionProvider(),
						new IdentifierCompletionProvider(),
						new TypeCompletionProvider(),
						new ImportCompletionProvider(),
						new PackageCompletionProvider()),
				ranker
		);
	}

	/**
	 * Wrapper for {@link #createEngineAndRanker(TabCompletionConfig)}.
	 *
	 * @param engine
	 * 		Completion engine to use for computing completions.
	 * @param ranker
	 * 		Completion ranker to use for sorting completions.
	 */
	private record CompletionImplementation(@Nonnull JavaCompletionEngine engine,
	                                        @Nonnull JavaCompletionRanker ranker) {}
}
