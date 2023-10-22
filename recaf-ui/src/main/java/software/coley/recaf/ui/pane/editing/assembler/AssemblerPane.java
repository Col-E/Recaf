package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.Location;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.assembler.AssemblerPipeline;
import software.coley.recaf.services.assembler.AssemblerPipelineManager;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.LanguageStylesheets;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.BracketMatchGraphicFactory;
import software.coley.recaf.ui.control.richtext.problem.*;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;
import software.coley.recaf.ui.control.richtext.syntax.RegexSyntaxHighlighter;
import software.coley.recaf.ui.pane.editing.AbstractContentPane;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.workspace.model.bundle.Bundle;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Dependent
public class AssemblerPane extends AbstractContentPane<PathNode<?>> implements UpdatableNavigable {
	private static final Logger logger = Logging.get(AssemblerPane.class);

	private final AssemblerPipelineManager pipelineManager;
	private final ProblemTracking problemTracking = new ProblemTracking();
	private final Editor editor = new Editor();
	private AssemblerPipeline<? extends ClassInfo, ? extends ClassRepresentation> pipeline;
	private ClassInfo lastAssembledClass;
	private List<Token> lastTokens;
	private List<ASTElement> lastRoughAst;
	private List<ASTElement> lastPartialAst;
	private List<ASTElement> lastConcreteAst;

	@Inject
	public AssemblerPane(@Nonnull AssemblerPipelineManager pipelineManager,
						 @Nonnull KeybindingConfig keys) {
		this.pipelineManager = pipelineManager;

		int timeToWait = pipelineManager.getServiceConfig().getDisassemblyAstParseDelay().getValue();

		editor.getCodeArea().getStylesheets().add(LanguageStylesheets.getJasmStylesheet());
		editor.setSyntaxHighlighter(new RegexSyntaxHighlighter(RegexLanguages.getJasmLanguage()));
		editor.setProblemTracking(problemTracking);
		editor.getRootLineGraphicFactory().addLineGraphicFactories(
				new BracketMatchGraphicFactory(),
				new ProblemGraphicFactory()
		);
		editor.getTextChangeEventStream().successionEnds(Duration.ofMillis(timeToWait)).addObserver(e -> parseAST());

		setOnKeyPressed(event -> {
			if (keys.getSave().match(event))
				assemble();
		});
	}

	@Override
	protected void generateDisplay() {
		disassemble();
		setDisplay(editor);
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return path;
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		this.path = path;
		this.pipeline = pipelineManager.getPipeline(path);
		this.lastAssembledClass = path.getValueOfType(ClassInfo.class);
		refreshDisplay();
	}

	/**
	 * Disassemble the content of the {@link #path} and set the editor text to the resulting output.
	 */
	private void disassemble() {
		problemTracking.removeByPhase(ProblemPhase.LINT);
		CompletableFuture.supplyAsync(() -> pipeline.disassemble(path))
				.whenCompleteAsync((result, unused) ->
						acceptResult(result, editor::setText, ProblemPhase.LINT), FxThreadUtil.executor());
	}

	/**
	 * Parse the current editor's text into AST.
	 *
	 * @return Future of parse completion.
	 */
	@Nonnull
	private CompletableFuture<Void> parseAST() {
		// Nothing to parse
		if (editor.getText().isBlank()) return CompletableFuture.completedFuture(null);

		// Clear lint errors since we are running the linter again.
		if (problemTracking.removeByPhase(ProblemPhase.LINT))
			FxThreadUtil.run(editor::redrawParagraphGraphics);

		return CompletableFuture.runAsync(() -> {
			try {
				// Tokenize the current input.
				Result<List<Token>> tokenResult = pipeline.tokenize(editor.getText(), "<assembler>");

				// Process any errors and assign the latest token list.
				if (tokenResult.hasErr())
					processErrors(tokenResult.errors(), ProblemPhase.LINT);
				lastTokens = tokenResult.get();

				// Attempt to parse the token list into 'rough' AST.
				acceptResult(pipeline.roughParse(lastTokens), roughAst -> {
					lastRoughAst = roughAst;

					// Attempt to complete parsing and transform the 'rough' AST into a 'concrete' AST.
					acceptResult(pipeline.concreteParse(roughAst), concreteAst -> {
						lastConcreteAst = concreteAst;
					}, pAst -> this.lastPartialAst = pAst, ProblemPhase.LINT);
				}, pAst -> this.lastPartialAst = pAst, ProblemPhase.LINT);
			} catch (Exception ex) {
				logger.error("Failed to parse assembler", ex);
			}
		});
	}

	/**
	 * Build the contents of the editor's text and update the workspace when successful.
	 *
	 * @return Future of parse completion.
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	private CompletableFuture<Void> assemble() {
		if (!problemTracking.getProblems().isEmpty())
			return CompletableFuture.completedFuture(null);

		// Ensure the AST is up-to-date before moving onto build stage.
		return parseAST().whenComplete((unused, error) -> {
			if (!problemTracking.getProblems().isEmpty() && lastConcreteAst == null)
				return;

			// Clear build errors since we are running the build process again.
			problemTracking.removeByPhase(ProblemPhase.BUILD);

			try {
				pipeline.assemble(lastConcreteAst, path).ifOk(info -> {
					lastAssembledClass = info;

					Bundle<ClassInfo> bundle = path.getValueOfType(Bundle.class);
					bundle.put(info);

					Animations.animateSuccess(editor, 1000);
				}).ifErr(errors -> {
					processErrors(errors, ProblemPhase.BUILD);

					Animations.animateFailure(editor, 1000);
				});
			} catch (Throwable ex) {
				logger.error("Uncaught exception when assembling contents of {}", path, ex);
			}
		});
	}

	/**
	 * Add the given errors to {@link #problemTracking} and refresh the UI.
	 *
	 * @param errors
	 * 		Problems to add.
	 * @param phase
	 * 		Phase the problems belong to.
	 */
	private void processErrors(@Nonnull Collection<Error> errors, @Nonnull ProblemPhase phase) {
		FxThreadUtil.run(() -> {
			for (Error error : errors) {
				Location location = error.getLocation();
				int line = location == null ? 1 : location.getLine();
				int column = location == null ? 1 : location.getColumn();
				Problem problem = new Problem(line, column, ProblemLevel.ERROR, phase,
						error.getMessage());
				problemTracking.add(problem);

				// REMOVE IS TRACING PARSER ERRORS
				/*
				Throwable trace = new Throwable();
				trace.setStackTrace(error.getInCodeSource());
				logger.trace("Assembler error", trace);
				System.err.println(error);
				*/
			}

			if (!errors.isEmpty())
				editor.redrawParagraphGraphics();
		});
	}

	<T> void acceptResult(Result<T> result, Consumer<T> acceptor, ProblemPhase phase) {
		result.ifOk(acceptor).ifErr(errors -> processErrors(errors, phase));
	}

	<T> void acceptResult(Result<T> result, Consumer<T> acceptor, Consumer<T> pAcceptor, ProblemPhase phase) {
		result.ifOk(acceptor).ifErr((pOk, errors) -> {
			pAcceptor.accept(pOk);
			processErrors(errors, phase);
		});
	}
}
