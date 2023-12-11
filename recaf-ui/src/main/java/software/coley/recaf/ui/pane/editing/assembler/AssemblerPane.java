package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.analysis.AnalysisException;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.Location;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.assembler.AssemblerPipeline;
import software.coley.recaf.services.assembler.AssemblerPipelineManager;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.LanguageStylesheets;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.BracketMatchGraphicFactory;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.problem.*;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;
import software.coley.recaf.ui.control.richtext.syntax.RegexSyntaxHighlighter;
import software.coley.recaf.ui.pane.editing.AbstractContentPane;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Unchecked;
import software.coley.recaf.workspace.model.bundle.Bundle;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Display dissassembled {@link ClassInfo} and {@link ClassMember} content.
 *
 * @author Matt Coley
 */
@Dependent
public class AssemblerPane extends AbstractContentPane<PathNode<?>> implements UpdatableNavigable, ClassNavigable {
	private static final Logger logger = Logging.get(AssemblerPane.class);

	private final AssemblerPipelineManager pipelineManager;
	private final AssemblerToolTabs assemblerToolTabs;
	private final ProblemTracking problemTracking = new ProblemTracking();
	private final Editor editor = new Editor();
	private final AtomicBoolean updateLock = new AtomicBoolean();
	private AssemblerPipeline<? extends ClassInfo, ? extends ClassRepresentation> pipeline;
	private ClassRepresentation lastAssembledClassRepresentation;
	private ClassInfo lastAssembledClass;
	private List<Token> lastTokens;
	private List<ASTElement> lastRoughAst;
	private List<ASTElement> lastPartialAst;
	private List<ASTElement> lastConcreteAst;

	@Inject
	public AssemblerPane(@Nonnull AssemblerPipelineManager pipelineManager,
						 @Nonnull AssemblerToolTabs assemblerToolTabs,
						 @Nonnull SearchBar searchBar,
						 @Nonnull KeybindingConfig keys) {
		this.pipelineManager = pipelineManager;
		this.assemblerToolTabs = assemblerToolTabs;

		int timeToWait = pipelineManager.getServiceConfig().getDisassemblyAstParseDelay().getValue();

		editor.getCodeArea().getStylesheets().add(LanguageStylesheets.getJasmStylesheet());
		editor.setSelectedBracketTracking(new SelectedBracketTracking());
		editor.setSyntaxHighlighter(new RegexSyntaxHighlighter(RegexLanguages.getJasmLanguage()));
		editor.setProblemTracking(problemTracking);
		editor.getRootLineGraphicFactory().addLineGraphicFactories(
				new BracketMatchGraphicFactory(),
				new ProblemGraphicFactory()
		);
		editor.getTextChangeEventStream()
				.successionEnds(Duration.ofMillis(timeToWait))
				.addObserver(e -> assemble());

		searchBar.install(editor);

		setOnKeyPressed(event -> {
			if (keys.getSave().match(event))
				assembleAndUpdateWorkspace();
		});

		// TODO: For class level assemblers we need to track the 'editor' caret position and update the tool tabs
		//  to hold a ClassMemberPathNode to the current member the caret position is over
	}

	@Override
	protected void generateDisplay() {
		if (!hasDisplay()) {
			// TODO: Re-create 'SideTabs' but vertical instead of split-pane
			//  - The UX should mirror the tab system you see in IDE's and SplitPane isn't like that
			SplitPane split = new SplitPane(editor, assemblerToolTabs);
			split.setOrientation(Orientation.VERTICAL);
			setDisplay(split);

			// Trigger a disassembly so the initial text is set in the editor.
			disassemble().whenComplete((unused, error) -> {
				if (error == null) assemble();
			});
		}
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return path;
	}

	@Nonnull
	@Override
	public ClassPathNode getClassPath() {
		return Objects.requireNonNull(path.getParentOfType(ClassInfo.class), "Missing class parent path");
	}

	@Override
	public void requestFocus(@Nonnull ClassMember member) {
		if (lastRoughAst != null) {
			System.out.println(lastRoughAst);
		}
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		this.path = path;
		if (!updateLock.get()) {
			pipeline = pipelineManager.getPipeline(path);

			// Setup from existing class data from the path
			lastAssembledClass = path.getValueOfType(ClassInfo.class);
			lastAssembledClassRepresentation = pipeline.getRepresentation(Unchecked.cast(lastAssembledClass));
			assemblerToolTabs.onUpdatePath(path);
			assemblerToolTabs.consumeClass(lastAssembledClassRepresentation, lastAssembledClass);

			// Some sub-components in the tabs are not initialized immediately, so we install the component here.
			assemblerToolTabs.install(editor);

			refreshDisplay();
		}
	}

	/**
	 * Disassemble the content of the {@link #path} and set the editor text to the resulting output.
	 *
	 * @return Future of disassembling completion.
	 */
	@Nonnull
	private CompletableFuture<Result<String>> disassemble() {
		problemTracking.removeByPhase(ProblemPhase.LINT);
		return CompletableFuture.supplyAsync(() -> pipeline.disassemble(path))
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

				BiConsumer<List<ASTElement>, AstPhase> astConsumer = assemblerToolTabs::consumeAst;

				// Attempt to parse the token list into 'rough' AST.
				acceptResult(pipeline.roughParse(lastTokens), roughAst -> {
					lastRoughAst = roughAst;

					// Attempt to complete parsing and transform the 'rough' AST into a 'concrete' AST.
					acceptResult(pipeline.concreteParse(roughAst), concreteAst -> {
						// The transform was a success.
						lastConcreteAst = concreteAst;
						astConsumer.accept(concreteAst, AstPhase.CONCRETE);
					}, pAst -> {
						// The transform failed.
						lastPartialAst = pAst;
						astConsumer.accept(pAst, AstPhase.CONCRETE_PARTIAL);
					}, ProblemPhase.LINT);
				}, pAst -> {
					// We failed to parse the token list fully, but may have a partial result.
					astConsumer.accept(pAst, AstPhase.ROUGH_PARTIAL);
					lastPartialAst = pAst;
				}, ProblemPhase.LINT);
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
	private CompletableFuture<Void> assemble() {
		// Ensure the AST is up-to-date before moving onto build stage.
		return parseAST().whenComplete((unused, error) -> {
			if (!problemTracking.getProblems().isEmpty() && lastConcreteAst == null)
				return;

			// Clear build errors since we are running the build process again.
			problemTracking.removeByPhase(ProblemPhase.BUILD);

			try {
				pipeline.assemble(lastConcreteAst, path).ifOk(representation -> {
					lastAssembledClassRepresentation = representation;

					if (representation instanceof JavaClassRepresentation javaClassRep) {
						lastAssembledClass = pipeline.getClassInfo(Unchecked.cast(javaClassRep));
						for (var methodEntry : javaClassRep.analysisLookup().allResults().entrySet()) {
							String methodName = methodEntry.getKey().name();
							AnalysisException failure = methodEntry.getValue().getAnalysisFailure();
							if (failure != null) {
								Animations.animateWarn(this, 1000);
								logger.warn("Method analysis on '{}' found potential problem: {}", methodName, failure.getMessage(), failure);
							}
						}
					}
					/*
					else if (representation instanceof AndroidClassRepresentation androidClassRep) {
						lastAssembledClass = pipeline.getClassInfo(Unchecked.cast(androidClassRep));
					}
					 */

					assemblerToolTabs.consumeClass(representation, lastAssembledClass);
				}).ifErr(errors -> processErrors(errors, ProblemPhase.BUILD));
			} catch (Throwable ex) {
				logger.error("Uncaught exception when assembling contents of {}", path, ex);
				Animations.animateFailure(editor, 1000);
			}
		});
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	private CompletableFuture<Void> assembleAndUpdateWorkspace() {
		return assemble().whenComplete((unused, error) -> {
			if (lastAssembledClass != null && problemTracking.getProblemsByLevel(ProblemLevel.ERROR).isEmpty()) {
				updateLock.set(true);
				try {
					Bundle<ClassInfo> bundle = path.getValueOfType(Bundle.class);
					bundle.put(lastAssembledClass);
					Animations.animateSuccess(editor, 1000);
				} catch (Throwable t) {
					logger.error("Uncaught exception when updating class of {}", lastAssembledClass.getName(), t);
					Animations.animateWarn(editor, 1000);
				} finally {
					updateLock.set(false);
				}
			} else {
				Animations.animateFailure(editor, 1000);
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
				int line = location == null ? 1 : location.line();
				int column = location == null ? 1 : location.column();
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

	private <T> void acceptResult(Result<T> result, Consumer<T> acceptor, ProblemPhase phase) {
		result.ifOk(acceptor).ifErr(errors -> processErrors(errors, phase));
	}

	private <T> void acceptResult(Result<T> result, Consumer<T> acceptor, Consumer<T> pAcceptor, ProblemPhase phase) {
		result.ifOk(acceptor).ifErr((pOk, errors) -> {
			pAcceptor.accept(pOk);
			processErrors(errors, phase);
		});
	}
}
