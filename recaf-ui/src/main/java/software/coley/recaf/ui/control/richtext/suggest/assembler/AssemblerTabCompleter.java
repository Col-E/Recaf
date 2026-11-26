package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.target.TargetContext;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.suggest.CompletionPopup;
import software.coley.recaf.ui.control.richtext.suggest.CompletionResolver;
import software.coley.recaf.ui.control.richtext.suggest.TabCompleter;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.ui.control.richtext.suggest.ranking.CompletionRanker;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeIndexService;
import software.coley.recaf.ui.pane.editing.assembler.AssemblerPane;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.SVG;
import software.coley.recaf.workspace.model.Workspace;

import java.util.List;
import java.util.function.Predicate;

/**
 * Tab completion for JASM assembly language, with both dialects of {@link JvmTargetContext} and {@link me.darknet.assembler.backend.dalvik.DalvikTargetContext} supported.
 * Ties into the {@link AssemblerPane} to provide access to the current target context and AST.
 *
 * @author Matt Coley
 */
public class AssemblerTabCompleter implements TabCompleter<AssemblerCompletion>, CompletionResolver<AssemblerCompletion> {
	private final Workspace workspace;
	private final InheritanceGraph inheritanceGraph;
	private final TypeIndexService typeIndexService;
	private final CellConfigurationService configurationService;
	private final TabCompletionConfig config;
	private final CompletionRanker<AssemblerCompletion> ranker;
	private String editedClassName;
	private AssemblerCompletionEngine engine;
	private CompletionPopup<AssemblerCompletion> completionPopup;
	private CodeArea area;
	private List<ASTElement> ast;
	private AssemblerCompletionContext context = new AssemblerCompletionContext.Empty();

	/**
	 * @param workspace
	 * 		Workspace to pull type and member information from.
	 * @param inheritanceGraph
	 * 		Inheritance graph for member lookups.
	 * @param typeIndexService
	 * 		Type index service for workspace type lookups.
	 * @param configurationService
	 * 		Service to pull cell graphics from.
	 * @param config
	 * 		Completion popup configuration.
	 */
	public AssemblerTabCompleter(@Nonnull Workspace workspace,
	                             @Nonnull InheritanceGraph inheritanceGraph,
	                             @Nonnull TypeIndexService typeIndexService,
	                             @Nonnull CellConfigurationService configurationService,
	                             @Nonnull TabCompletionConfig config) {
		this.workspace = workspace;
		this.inheritanceGraph = inheritanceGraph;
		this.configurationService = configurationService;
		this.config = config;
		this.ranker = AssemblerCompletionRanking.create(config);
		this.typeIndexService = typeIndexService;
		this.engine = new AssemblerCompletionEngine(typeIndexService, workspace, inheritanceGraph, JvmTargetContext.INSTANCE);
	}

	/**
	 * @param ast
	 * 		AST to work off of.
	 */
	public void setAst(@Nonnull List<ASTElement> ast) {
		this.ast = ast;
	}

	/**
	 * Remove AST to work off of.
	 */
	public void clearAst() {
		ast = null;
	}

	/**
	 * @param targetContext
	 * 		Instruction language used by the currently edited class.
	 */
	public void setTargetContext(@Nonnull TargetContext targetContext) {
		if (engine.targetContext() == targetContext)
			return;

		engine = new AssemblerCompletionEngine(typeIndexService, workspace, inheritanceGraph, targetContext);
		if (area != null)
			recomputeLineContext();
	}

	/**
	 * @param editedClass
	 * 		Class currently assembled in the pane, or {@code null} if there is none.
	 */
	public void setEditedClass(@Nullable ClassInfo editedClass) {
		editedClassName = editedClass == null ? null : editedClass.getName();
		if (area != null)
			recomputeLineContext();
	}

	@Override
	public boolean requestCompletion(@Nonnull KeyEvent event) {
		// Recompute line context to ensure its up-to-date.
		recomputeLineContext();

		// Skip if no text context or empty text context.
		if (context instanceof AssemblerCompletionContext.Empty)
			return false;

		// Complete if the completion popup is showing.
		return completionPopup != null && completionPopup.isShowing()
				&& completeFromContext(context.partialMatchedText(), completionPopup::doComplete);
	}

	@Nonnull
	@Override
	public List<AssemblerCompletion> computeCurrentCompletions() {
		return ranker.rank(context.complete().stream()
				.filter(completion -> completion.text().length() <= config.getMaxCompletionLength())
				.toList());
	}

	@Nonnull
	@Override
	public List<AssemblerCompletion> resolve(@Nonnull String source, int caret) {
		return ranker.rank(engine.resolve(source, caret, ast, editedClassName).complete().stream()
				.filter(completion -> completion.text().length() <= config.getMaxCompletionLength())
				.toList());
	}

	@Override
	public void onFineTextUpdate(@Nonnull PlainTextChange change) {
		recomputeLineContext();
	}

	@Override
	public void onRoughTextUpdate(@Nonnull List<PlainTextChange> changes) {
		// no-op
	}

	@Override
	public boolean isSpecialCompletableCharacter(char character) {
		// Support completing:
		// - '/' which is used to separate packages in class names
		// - '[' which is used in array descriptors
		// - ';' which is used to mark the end of object type descriptors
		// - '-' and '_' which appear in opcode names and identifiers
		// - '(' and ')' which delimit method descriptors
		return switch (character) {
			case '/', '[', ';', '-', '_', '(', ')' -> true;
			default -> false;
		};
	}

	@Override
	public void install(@Nonnull Editor editor) {
		if (completionPopup != null)
			completionPopup.uninstall();
		area = editor.getCodeArea();
		completionPopup = new AssemblerCompletionPopup(config);
		completionPopup.install(area, this);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		if (completionPopup != null) {
			completionPopup.uninstall();
			completionPopup = null;
		}
		area = null;
		context = new AssemblerCompletionContext.Empty();
	}

	/**
	 * Recompute the context of the current line to determine what kind of completions to offer.
	 */
	private void recomputeLineContext() {
		if (area == null) {
			context = new AssemblerCompletionContext.Empty();
			return;
		}
		context = engine.resolve(area.getText(), area.getCaretPosition(), ast, editedClassName);
	}

	/**
	 * @param context
	 * 		Partially matched text from the completion context.
	 * @param completionHandler
	 * 		Handler to perform the completion.
	 *
	 * @return Result of the handler, which is {@code true} if a completion was performed.
	 */
	private static boolean completeFromContext(@Nonnull String context, @Nonnull Predicate<String> completionHandler) {
		return completionHandler.test(context.trim());
	}

	/**
	 * Completion popup for assembler content.
	 */
	private class AssemblerCompletionPopup extends CompletionPopup<AssemblerCompletion> {
		private AssemblerCompletionPopup(@Nonnull TabCompletionConfig config) {
			super(config, STANDARD_CELL_SIZE, AssemblerCompletion::text, completion -> switch (completion) {
				case AssemblerCompletion.Opcode opcode -> {
					String text = opcode.text();
					if (text.startsWith("invoke"))
						yield Icons.getIconView(Icons.METHOD);
					if (text.startsWith("get") || text.startsWith("put"))
						yield Icons.getIconView(Icons.FIELD);
					if (text.endsWith("load"))
						yield SVG.ofIconFile(SVG.REF_READ);
					if (text.endsWith("store") || text.equals("iinc"))
						yield SVG.ofIconFile(SVG.REF_WRITE);
					if (text.contains("const") || text.equals("ldc"))
						yield Icons.getIconView(Icons.PRIMITIVE);
					if (text.startsWith("if") || text.endsWith("switch"))
						yield new FontIconView(CarbonIcons.FLOW, Color.STEELBLUE);
					if (text.endsWith("add"))
						yield new FontIconView(CarbonIcons.ADD, Color.LIMEGREEN);
					if (text.endsWith("sub") || text.endsWith("neg"))
						yield new FontIconView(CarbonIcons.SUBTRACT, Color.RED);
					if (text.startsWith("dup"))
						yield new FontIconView(CarbonIcons.COPY, Color.DARKGRAY);
					if (text.endsWith("return"))
						yield new FontIconView(CarbonIcons.TEXT_NEW_LINE, Color.STEELBLUE);
					if (text.equals("new") || text.equals("checkcast") || text.equals("instanceof"))
						yield Icons.getIconView(Icons.CLASS);
					if (text.indexOf('2') == 1) // x2y conversions
						yield SVG.ofIconFile(SVG.TYPE_CONVERSION);
					if (text.contains("cmp"))
						yield new FontIconView(CarbonIcons.COMPARE, Color.DARKGRAY);
					if (text.contains("array"))
						yield Icons.getIconView(Icons.ARRAY);
					if (text.equals("athrow"))
						yield SVG.ofIconFile(SVG.EXCEPTION_BREAKPOINT);
					yield Icons.getIconView(Icons.INTERNAL);
				}
				case AssemblerCompletion.Type type -> configurationService.graphicOf(type.path());
				case AssemblerCompletion.Member member -> configurationService.graphicOf(member.path());
				case AssemblerCompletion.Operand operand -> switch (operand.kind()) {
					case REGISTER -> Icons.getIconView(Icons.PRIMITIVE);
					case LABEL -> new FontIconView(CarbonIcons.FLOW, Color.STEELBLUE);
					case LOCAL -> Icons.getIconView(Icons.INTERNAL);
					case STRUCTURED_CONSTANT -> Icons.getIconView(Icons.INTERNAL);
				};
			});
		}

		@Override
		protected boolean complete(@Nonnull String partialText, @Nonnull AssemblerCompletion selected,
		                           @Nonnull String fullText) {
			if (selected instanceof AssemblerCompletion.Type type && type.replacePartialText())
				return completeByReplacingPartialText(partialText, fullText);
			return super.complete(partialText, selected, fullText);
		}

		@Override
		public boolean doComplete(@Nonnull String partialText) {
			AssemblerCompletion selected = getSelected();
			boolean completed = super.doComplete(partialText);
			if (completed && selected != null)
				ranker.recordSelection(selected);
			return completed;
		}

		@Override
		public void completeCurrentSelection() {
			completeFromContext(context.partialMatchedText(), this::doComplete);
		}
	}
}
