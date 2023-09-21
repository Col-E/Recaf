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
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.assembler.AssemblerPipeline;
import software.coley.recaf.services.assembler.AssemblerPipelineManager;
import software.coley.recaf.services.navigation.NavigationManager;
import software.coley.recaf.ui.LanguageStylesheets;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.BracketMatchGraphicFactory;
import software.coley.recaf.ui.control.richtext.problem.*;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;
import software.coley.recaf.ui.control.richtext.syntax.RegexSyntaxHighlighter;
import software.coley.recaf.ui.pane.editing.AbstractContentPane;
import software.coley.recaf.util.FxThreadUtil;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Dependent
public class AssemblerPane extends AbstractContentPane<PathNode<?>> {

    private final AssemblerPipelineManager pipelineManager;
    private final ProblemTracking problemTracking = new ProblemTracking();
    private final Editor editor = new Editor();
    private AssemblerPipeline<? extends ClassInfo, ? extends ClassRepresentation> pipeline;
    private ClassInfo classInfo;
    private List<Token> lastTokens;
    private List<ASTElement> lastRoughAst;
    private List<ASTElement> lastPartialAst;
    private List<ASTElement> lastAst;

    @Inject
    public AssemblerPane(@Nonnull AssemblerPipelineManager pipelineManager) {
        this.pipelineManager = pipelineManager;

        this.editor.getCodeArea().getStylesheets().add(LanguageStylesheets.getJasmStylesheet());

        this.editor.setSyntaxHighlighter(new RegexSyntaxHighlighter(RegexLanguages.getJasmLanguage()));
        this.editor.setProblemTracking(problemTracking);

        this.editor.getRootLineGraphicFactory().addLineGraphicFactories(
                new BracketMatchGraphicFactory(),
                new ProblemGraphicFactory()
        );

        this.editor.getTextChangeEventStream().subscribe(event -> {
            parseAST(ast -> {});
        });
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
        this.classInfo = path.getValueOfType(ClassInfo.class);
        refreshDisplay();
    }

    private void disassemble() {
        problemTracking.removeByPhase(ProblemPhase.LINT);

        CompletableFuture.supplyAsync(() -> pipeline.disassemble(path))
                .whenCompleteAsync((result, unused) ->
                        acceptResult(result, editor::setText, ProblemPhase.LINT), FxThreadUtil.executor());
    }

    private void parseAST(Consumer<List<ASTElement>> acceptor) {
        if(editor.getText().isBlank()) return;
        CompletableFuture.runAsync(() -> {
            problemTracking.removeByPhase(ProblemPhase.LINT);
            this.lastTokens = pipeline.tokenize(editor.getText(), "<assembler>");

            acceptResult(pipeline.roughParse(this.lastTokens), roughAst -> {
                this.lastRoughAst = roughAst;

                acceptResult(pipeline.concreteParse(roughAst), ast -> {
                    this.lastAst = ast;

                    acceptor.accept(ast);
                }, pAst -> this.lastPartialAst = pAst, ProblemPhase.LINT);
            }, pAst -> this.lastPartialAst = pAst, ProblemPhase.LINT);

            this.editor.redrawParagraphGraphics();
        }, FxThreadUtil.executor());
    }

    private void assemble() {

    }

    void processErrors(Collection<Error> errors, ProblemPhase phase) {
        for (Error error : errors) {
            Location location = error.getLocation();
            int line = location == null ? 1 : location.getLine();
            int column = location == null ? 1 : location.getColumn();
            Problem problem = new Problem(line, column, ProblemLevel.ERROR, phase,
                    error.getMessage());
            problemTracking.add(problem);

            Throwable trace = new Throwable();
            trace.setStackTrace(error.getInCodeSource());
            trace.printStackTrace();

            System.err.println(error);
        }
        if(!errors.isEmpty())
            this.editor.redrawParagraphGraphics();
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

    @Override
    protected void generateDisplay() {
        disassemble();

        setDisplay(editor);
    }
}
