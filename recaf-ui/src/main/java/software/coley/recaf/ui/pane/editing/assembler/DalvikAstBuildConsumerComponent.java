package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.SimpleObjectProperty;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikAnalysisResults;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikMethodAnalysisLookup;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikRegisterUsage;
import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikRegisterUsageLookup;
import me.darknet.assembler.compiler.ClassResult;
import org.reactfx.Change;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.slf4j.Logger;
import software.coley.collections.Lists;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.FxThreadUtil;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared AST, compilation-result, and caret lifecycle for Dalvik assembler panes.
 *
 * @author Matt Coley
 */
public abstract class DalvikAstBuildConsumerComponent extends AstBuildConsumerComponent {
	private static final Logger logger = Logging.get(DalvikAstBuildConsumerComponent.class);
	private final SimpleObjectProperty<Object> notifyQueue = new SimpleObjectProperty<>(new Object());
	private final Consumer<Change<Integer>> caretListener = ignored -> {
		Editor currentEditor = editor;
		if (currentEditor != null && currentEditor.getCodeArea().getSelection().getLength() == 0)
			scheduleUpdate();
	};
	@Nullable
	private volatile DalvikRegisterUsageLookup dalvikRegisterUsageLookup;
	@Nullable
	private EventStream<Change<Integer>> caretStream;

	protected DalvikAstBuildConsumerComponent() {
		EventStreams.changesOf(notifyQueue)
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.SHORT_DELAY_MS))
				.addObserver(ignored -> {
					try {
						updatePane();
					} catch (Throwable t) {
						logger.error("Error updating Dalvik AST consumer component: {}", getClass().getSimpleName(), t);
					}
				});
	}

	@Override
	public void consumeAst(@Nonnull List<ASTElement> astElements, @Nonnull AstPhase phase) {
		analysisLookupDalvik = null;
		dalvikRegisterUsageLookup = null;

		// Invalidate the lookups before the parent consumeAst() refreshes the pane.
		super.consumeAst(astElements, phase);
	}

	@Override
	public void consumeClass(@Nonnull ClassResult<?> result, @Nonnull ClassInfo classInfo) {
		if (result instanceof DalvikCompileResult dalvikResult)
			dalvikRegisterUsageLookup = dalvikResult.registerUsageLookup();

		// Similar idea as above, it will specify the analysis lookup.
		// We just need the usage lookup assigned beforehand.
		super.consumeClass(result, classInfo);
	}

	/**
	 * @return AST method matching the selected workspace method, if available.
	 */
	@Nullable
	protected final ASTMethod findCurrentAstMethod() {
		MethodMember method = currentMethod;
		if (method == null)
			return null;

		String methodName = method.getName();
		String methodDescriptor = method.getDescriptor();
		for (ASTElement astElement : astElements) {
			if (astElement instanceof ASTMethod astMethod && matchesMethod(astMethod, methodName, methodDescriptor))
				return astMethod;
			if (astElement instanceof ASTClass astClass) {
				for (ASTElement child : astClass.children()) {
					if (child instanceof ASTMethod astMethod && matchesMethod(astMethod, methodName, methodDescriptor))
						return astMethod;
				}
			}
		}

		return null;
	}

	/**
	 * @param method
	 * 		Current method AST to inspect.
	 *
	 * @return Source instruction at the caret, or {@code null} when the caret is outside an instruction.
	 */
	@Nullable
	protected final ASTInstruction findSelectedAstInstruction(@Nonnull ASTMethod method) {
		Editor currentEditor = editor;
		ASTCode code = method.getCode();
		if (currentEditor == null || code == null)
			return null;

		// Must have instructions to select from.
		List<ASTInstruction> instructions = code.getInstructions();
		if (instructions.isEmpty())
			return null;

		// Check if the caret is within the method's range.
		int caret = currentEditor.getCodeArea().getCaretPosition();
		if (!method.range().within(caret))
			return null;

		// Binary search for the instruction at the caret.
		int low = 0;
		int high = instructions.size();
		while (low < high) {
			int middle = (low + high) >>> 1;
			if (instructions.get(middle).range().start() <= caret)
				low = middle + 1;
			else
				high = middle;
		}

		// Check the instruction at the caret, or the one before it if the caret is at the end of an instruction.
		ASTInstruction selected = null;
		int prior = low - 1;
		if (prior >= 0 && instructions.get(prior).range().within(caret))
			selected = instructions.get(prior);
		else if (low < instructions.size() && instructions.get(low).range().within(caret))
			selected = instructions.get(low);
		return selected;
	}

	/**
	 * @return Analysis results for the selected method, when compilation has produced them.
	 */
	@Nullable
	protected final DalvikAnalysisResults getCurrentAnalysisResults() {
		MethodMember method = currentMethod;
		DalvikMethodAnalysisLookup lookup = analysisLookupDalvik;
		if (method == null || lookup == null)
			return null;
		return lookup.getResults(method.getName(), method.getDescriptor());
	}

	/**
	 * @return Register usage summaries for the current source AST method, or {@code null} before compilation.
	 */
	@Nullable
	protected final List<DalvikRegisterUsage> getCurrentRegisterUsages() {
		ASTMethod method = findCurrentAstMethod();
		DalvikRegisterUsageLookup lookup = dalvikRegisterUsageLookup;
		if (method == null || lookup == null)
			return null;
		return lookup.getUsages(method);
	}

	/**
	 * Schedules a pane refresh on the JavaFX thread.
	 */
	protected final void scheduleUpdate() {
		FxThreadUtil.run(() -> notifyQueue.set(new Object()));
	}

	@Override
	protected void onClassSelected() {
		scheduleUpdate();
	}

	@Override
	protected void onMethodSelected() {
		scheduleUpdate();
	}

	@Override
	protected void onFieldSelected() {
		scheduleUpdate();
	}

	@Override
	protected void onPipelineOutputUpdate() {
		scheduleUpdate();
	}

	@Override
	public void install(@Nonnull Editor editor) {
		super.install(editor);
		caretStream = editor.getCaretPosEventStream().successionEnds(Duration.ofMillis(Editor.SHORT_DELAY_MS));
		caretStream.addObserver(caretListener);
		scheduleUpdate();
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		EventStream<Change<Integer>> stream = caretStream;
		if (stream != null)
			stream.removeObserver(caretListener);
		caretStream = null;
		super.uninstall(editor);
		scheduleUpdate();
	}

	/**
	 * Updates the contents of the specialized pane.
	 */
	protected abstract void updatePane();

	/**
	 * @param astMethod
	 * 		AST method to compare.
	 * @param name
	 * 		Workspace method name.
	 * @param descriptor
	 * 		Workspace method descriptor.
	 *
	 * @return {@code true} when the method signatures match.
	 */
	private static boolean matchesMethod(@Nonnull ASTMethod astMethod, @Nonnull String name, @Nonnull String descriptor) {
		return Objects.equals(name, astMethod.getName().literal()) &&
				Objects.equals(descriptor, astMethod.getDescriptor().literal());
	}
}
