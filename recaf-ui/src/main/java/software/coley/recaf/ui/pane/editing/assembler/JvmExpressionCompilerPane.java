package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.control.SplitPane;
import software.coley.collections.Lists;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.services.assembler.ExpressionCompileException;
import software.coley.recaf.services.assembler.ExpressionCompiler;
import software.coley.recaf.services.assembler.ExpressionResult;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.info.association.FileTypeAssociationService;
import software.coley.recaf.ui.LanguageStylesheets;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.BracketMatchGraphicFactory;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.problem.*;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;
import software.coley.recaf.ui.control.richtext.syntax.RegexSyntaxHighlighter;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.threading.ThreadPoolFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * Component panel for the assembler which shows the variables of the currently selected method.
 *
 * @author Matt Coley
 */
@Dependent
public class JvmExpressionCompilerPane extends AstBuildConsumerComponent {
	private static final ExecutorService compilePool = ThreadPoolFactory.newSingleThreadExecutor("expr-compile");
	protected final ProblemTracking problemTracking = new ProblemTracking();
	private final ExpressionCompiler expressionCompiler;
	private final Editor javaEditor = new Editor();
	private final Editor jasmEditor = new Editor();

	@Inject
	public JvmExpressionCompilerPane(@Nonnull ExpressionCompiler expressionCompiler,
									 @Nonnull FileTypeAssociationService languageAssociation,
									 @Nonnull Instance<SearchBar> searchBarProvider) {
		this.expressionCompiler = expressionCompiler;

		languageAssociation.configureEditorSyntax("java", javaEditor);
		javaEditor.setSelectedBracketTracking(new SelectedBracketTracking());
		javaEditor.setProblemTracking(problemTracking);
		javaEditor.getRootLineGraphicFactory().addLineGraphicFactories(
				new BracketMatchGraphicFactory(),
				new ProblemGraphicFactory()
		);
		javaEditor.setText(Lang.get("assembler.playground.comment").replace("\\n", "\n")); // TODO: The comment should reflect what contexts are allowed
		jasmEditor.getCodeArea().getStylesheets().add(LanguageStylesheets.getJasmStylesheet());
		jasmEditor.setSelectedBracketTracking(new SelectedBracketTracking());
		jasmEditor.setSyntaxHighlighter(new RegexSyntaxHighlighter(RegexLanguages.getJasmLanguage()));
		jasmEditor.getRootLineGraphicFactory().addLineGraphicFactories(
				new BracketMatchGraphicFactory()
		);
		searchBarProvider.get().install(javaEditor);
		searchBarProvider.get().install(jasmEditor);

		SplitPane split = new SplitPane(javaEditor, jasmEditor);
		setCenter(split);

		javaEditor.getTextChangeEventStream()
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.MEDIUM_DELAY_MS))
				.addObserver(unused -> scheduleCompile());
	}

	@Override
	protected void onClassSelected() {
		expressionCompiler.clearContext();
		if (canAssignClassContext())
			expressionCompiler.setClassContext(currentClass.asJvmClass());
		scheduleCompile();
	}

	@Override
	protected void onMethodSelected() {
		expressionCompiler.clearContext();
		if (canAssignClassContext()) {
			expressionCompiler.setClassContext(currentClass.asJvmClass());
			if (canAssignMethodContext()) {
				expressionCompiler.setMethodContext(currentMethod);
			}
		}
		scheduleCompile();
	}

	@Override
	protected void onFieldSelected() {
		expressionCompiler.clearContext();
		if (canAssignClassContext())
			expressionCompiler.setClassContext(currentClass.asJvmClass());
		scheduleCompile();
	}

	@Override
	protected void onPipelineOutputUpdate() {
		// no-op
	}

	/**
	 * Checks for things in the {@link #currentClass} which would prevent its use in the expression compiler as context.
	 *
	 * @return {@code true} when the current class can be used as context in the expression compiler.
	 */
	private boolean canAssignClassContext() {
		// We cannot have duplicate field names.
		Set<String> names = new HashSet<>();
		for (FieldMember field : currentClass.getFields()) {
			if (!names.add(field.getName()))
				return false;
		}
		return true;
	}

	/**
	 * Checks for things in the {@link #currentMethod} which would prevent its use in the expression compiler as context.
	 *
	 * @return {@code true} when the current method can be used as context in the expression compiler.
	 */
	private boolean canAssignMethodContext() {
		// If we find things that cannot be allowed as method context, add the checks here
		return true;
	}

	private void scheduleCompile() {
		compilePool.submit(this::compile);
	}

	private void compile() {
		ExpressionResult result = expressionCompiler.compile(javaEditor.getText());
		FxThreadUtil.run(() -> {
			problemTracking.clear();

			// Validate no compiler errors occurred
			List<CompilerDiagnostic> diagnostics = result.getDiagnostics();
			if (!diagnostics.isEmpty()) {
				Animations.animateFailure(javaEditor, 1000);
				for (CompilerDiagnostic diagnostic : diagnostics) {
					Problem problem = Problem.fromDiagnostic(diagnostic);
					problemTracking.add(problem);
				}
				return;
			}

			// Validate no compile exception was thrown.
			ExpressionCompileException exception = result.getException();
			if (exception != null) {
				Animations.animateFailure(javaEditor, 1000);
				problemTracking.add(new Problem(-1, -1, ProblemLevel.ERROR, ProblemPhase.BUILD, StringUtil.traceToString(exception)));
				return;
			}

			// Should have a result, but null check just to be safe.
			String assembly = result.getAssembly();
			jasmEditor.setText(Objects.requireNonNullElse(assembly, "<no-output>"));
		});
	}
}