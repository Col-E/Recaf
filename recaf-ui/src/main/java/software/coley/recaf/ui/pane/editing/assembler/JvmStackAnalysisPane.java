package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.MethodAnalysisResult;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.ValuedLocal;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.TypedFrame;
import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.reactfx.EventStreams;
import org.slf4j.Logger;
import software.coley.collections.Lists;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.Types;
import software.coley.recaf.workspace.model.Workspace;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;

/**
 * Component panel for the assembler which shows the data from stack analysis of the currently selected method.
 *
 * @author Matt Coley
 */
@Dependent
public class JvmStackAnalysisPane extends AstBuildConsumerComponent {
	private static final Logger logger = Logging.get(JvmStackAnalysisPane.class);
	private final SimpleObjectProperty<Object> notifyQueue = new SimpleObjectProperty<>(new Object());
	private final TableView<JvmVariableState> varTable = new TableView<>();
	private final TableView<JvmStackState> stackTable = new TableView<>();
	private ASTInstruction lastSelectedInsn;
	private MethodAnalysisResult lastAnalysisResults;

	@Inject
	@SuppressWarnings("unchecked")
	public JvmStackAnalysisPane(@Nonnull CellConfigurationService cellConfigurationService,
	                            @Nonnull TextFormatConfig formatConfig,
	                            @Nonnull WorkspaceManager workspaceManager) {
		Workspace workspace = workspaceManager.getCurrent();

		TableColumn<JvmVariableState, String> columnName = new TableColumn<>(Lang.get("assembler.variables.name"));
		TableColumn<JvmVariableState, Type> columnType = new TableColumn<>(Lang.get("assembler.variables.type"));
		TableColumn<JvmVariableState, ValueTableCell.ValueWrapper> columnValue = new TableColumn<>(Lang.get("assembler.variables.value"));
		columnName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().name));
		columnType.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().value instanceof Value.NullValue ? TypeTableCell.NULL_TYPE : param.getValue().type));
		columnValue.setCellValueFactory(param -> new SimpleObjectProperty<>(new ValueTableCell.ValueWrapper(param.getValue().value, param.getValue().priorValue)));
		columnType.setCellFactory(param -> new TypeTableCell<>(cellConfigurationService, formatConfig, workspace));
		columnValue.setCellFactory(param -> new ValueTableCell<>());
		varTable.getColumns().addAll(columnName, columnType, columnValue);

		TableColumn<JvmStackState, Type> columnTypeStack = new TableColumn<>(Lang.get("assembler.analysis.type"));
		TableColumn<JvmStackState, ValueTableCell.ValueWrapper> columnValueStack = new TableColumn<>(Lang.get("assembler.analysis.value"));
		columnTypeStack.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().value instanceof Value.NullValue ? TypeTableCell.NULL_TYPE : param.getValue().type));
		columnValueStack.setCellValueFactory(param -> new SimpleObjectProperty<>(new ValueTableCell.ValueWrapper(param.getValue().value, param.getValue().priorValue)));
		columnTypeStack.setCellFactory(param -> new TypeTableCell<>(cellConfigurationService, formatConfig, workspace));
		columnValueStack.setCellFactory(param -> new ValueTableCell<>());
		stackTable.getColumns().addAll(columnTypeStack, columnValueStack);

		varTable.getStyleClass().addAll(Styles.STRIPED, Tweaks.EDGE_TO_EDGE, "variable-table");
		varTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		stackTable.getStyleClass().addAll(Styles.STRIPED, Tweaks.EDGE_TO_EDGE, "variable-table");
		stackTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

		SplitPane split = new SplitPane(stackTable, varTable);
		setCenter(split);

		EventStreams.changesOf(notifyQueue)
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.SHORTER_DELAY_MS))
				.addObserver(unused -> {
					try {
						updateTable();
					} catch (Throwable t) {
						logger.error("Error updating stack analysis table", t);
					}
				});
	}

	private void updateTable() {
		stackTable.setDisable(false);
		varTable.setDisable(false);

		// Compute what instruction index the caret is at.
		ASTInstruction selectedInsn = null;
		findIndex:
		{
			for (ASTElement astElement : astElements) {
				if (astElement instanceof ASTMethod astMethod) {
					int index = getSelectedInsnIndexOfMethod(astMethod);
					if (index >= 0) {
						selectedInsn = astMethod.code().instructions().get(index);
						break;
					}
				} else if (astElement instanceof ASTClass astClass) {
					for (ASTElement child : astClass.children()) {
						if (child instanceof ASTMethod astMethod) {
							int index = getSelectedInsnIndexOfMethod(astMethod);
							if (index >= 0) {
								selectedInsn = astMethod.code().instructions().get(index);
								break findIndex;
							}
						}
					}
				}
			}
		}

		// Resolve the analysis before deciding whether the table is already current.
		MethodAnalysisResult analysisResults = null;
		if (selectedInsn != null && currentMethod != null && analysisLookup != null)
			analysisResults = (MethodAnalysisResult) analysisLookup.results(currentMethod.getName(), currentMethod.getDescriptor());

		// The AST and result are identity-bound, so an equal-looking instruction can still require a refresh.
		if (lastSelectedInsn == selectedInsn && lastAnalysisResults == analysisResults)
			return;
		lastSelectedInsn = selectedInsn;
		lastAnalysisResults = analysisResults;

		// Clear stale data when the caret is outside an instruction or the current AST has no matching analysis.
		if (selectedInsn == null || analysisResults == null) {
			clearData();
			return;
		}

		// Clear stale data when analysis has not produced any frames yet.
		NavigableMap<Integer, Frame> frames = analysisResults.frames();
		if (frames.isEmpty()) {
			clearData();
			return;
		}

		// Resolve the frame after the selected instruction so the table shows its immediate effect.
		List<JvmVariableState> varItems = new ArrayList<>();
		List<JvmStackState> stackItems = new ArrayList<>();
		FrameSelection frameSelection = getFrameSelection(analysisResults, selectedInsn, frames);
		Frame thisFrame = frameSelection.current();
		if (thisFrame == null) {
			// Partial or stale ASTs shouldn't show the previously selected instruction's state visible.
			clearData();
			return;
		}

		if (thisFrame instanceof TypedFrame typedFrame) {
			// Type-only analysis is basic.
			for (Type classType : typedFrame.getStack())
				stackItems.add(new JvmStackState(classType, Values.valueOf(classType), null));
			for (Local local : typedFrame.getLocals().values())
				varItems.add(new JvmVariableState(local.name(), local.safeType(), Values.valueOf(local.safeType()), null));
		} else if (thisFrame instanceof ValuedFrame valuedFrame) {
			// Compare against the frame before the selected instruction to highlight its effect.
			ValuedFrame lastFrame = frameSelection.prior() instanceof ValuedFrame valuedPriorFrame ? valuedPriorFrame : null;

			// Fill out stack.
			Value[] lastStack = lastFrame == null ? new Value[0] : lastFrame.getStack().toArray(Value[]::new);
			Value[] stack = valuedFrame.getStack().toArray(Value[]::new);
			for (int i = 0; i < stack.length; i++) {
				Value lastValue = i <= lastStack.length - 1 ? lastStack[i] : null;
				Value value = stack[i];
				stackItems.add(new JvmStackState(Objects.requireNonNullElse(value.type(), Types.OBJECT_TYPE), value, lastValue));
			}

			// And fill out the variables.
			Map<Integer, ValuedLocal> lastLocals = lastFrame == null ? Collections.emptyMap() : lastFrame.getLocals();
			Map<Integer, ValuedLocal> locals = valuedFrame.getLocals();
			for (ValuedLocal local : locals.values()) {
				ValuedLocal lastLocal = lastLocals.get(local.index());
				varItems.add(new JvmVariableState(local.name(), local.safeType(), local.value(),
						lastLocal == null ? null : lastLocal.value()));
			}
		}
		varTable.getItems().setAll(varItems);
		stackTable.getItems().setAll(stackItems);
	}

	@Nonnull
	private FrameSelection getFrameSelection(@Nonnull MethodAnalysisResult analysisResults,
	                                         @Nonnull ASTInstruction selectedInsn,
	                                         @Nonnull NavigableMap<Integer, Frame> frames) {
		// Resolve the generated instruction index because analysis positions include labels and metadata nodes.
		Map<ASTInstruction, AbstractInsnNode> executableInstructions = analysisResults.getAstToExecutableInstructionMap();
		AbstractInsnNode mappedInsn = executableInstructions.get(selectedInsn);
		boolean executable = mappedInsn != null;
		if (mappedInsn == null)
			mappedInsn = analysisResults.getAstToLabelMap().get(selectedInsn);
		if (mappedInsn == null)
			mappedInsn = analysisResults.getAstToLineNumberMap().get(selectedInsn);
		if (mappedInsn == null)
			return new FrameSelection(analysisResults.getFrame(selectedInsn), null);
		Integer instructionIndex = analysisResults.getInstructionIndex(mappedInsn);
		if (instructionIndex == null)
			return new FrameSelection(analysisResults.getFrame(selectedInsn), null);

		// Labels and metadata already point at the state they introduce, while executable instructions need
		// the next frame to showcase their effect. If the next frame is not available, fall back to the current frame.
		Map.Entry<Integer, Frame> currentEntry;
		if (executable) {
			currentEntry = frames.floorEntry(instructionIndex + 1);
			if (currentEntry == null || currentEntry.getKey() != instructionIndex + 1)
				currentEntry = frames.floorEntry(instructionIndex);
		} else {
			currentEntry = frames.floorEntry(instructionIndex);
		}
		if (currentEntry == null)
			return new FrameSelection(null, null);

		// The prior frame is the state immediately before the displayed effect.
		int frameIndex = currentEntry.getKey();
		Map.Entry<Integer, Frame> priorEntry = frames.lowerEntry(frameIndex);
		return new FrameSelection(currentEntry.getValue(), priorEntry == null ? null : priorEntry.getValue());
	}

	private int getSelectedInsnIndexOfMethod(@Nonnull ASTMethod method) {
		int pos = editor.getCodeArea().getCaretPosition();
		if (!method.range().within(pos))
			return -1;
		ASTCode code = method.code();
		if (code == null)
			return -1;
		List<ASTInstruction> instructions = code.instructions();
		if (instructions.isEmpty())
			return -1;

		// Find the first instruction that starts after the caret.
		int low = 0;
		int high = instructions.size();
		while (low < high) {
			int middle = (low + high) >>> 1;
			if (instructions.get(middle).range().start() <= pos)
				low = middle + 1;
			else
				high = middle;
		}

		// Prefer the instruction at or before the caret.
		// This gives stable behavior at adjacent range boundaries.
		int prior = low - 1;
		if (prior >= 0 && instructions.get(prior).range().within(pos))
			return prior;

		// A caret before the first instruction can still be inside that instruction's range.
		if (low < instructions.size() && instructions.get(low).range().within(pos))
			return low;

		// Whitespace between instructions has no instruction state to display.
		return -1;
	}

	private void clearData() {
		FxThreadUtil.run(() -> {
			stackTable.setDisable(true);
			varTable.setDisable(true);
			stackTable.getItems().clear();
			varTable.getItems().clear();
		});
		lastSelectedInsn = null;
		lastAnalysisResults = null;
	}

	private void scheduleTableUpdate() {
		// Queue updates even while analysis is temporarily unavailable so stale rows are cleared.
		if (currentMethod == null || editor == null) return;
		FxThreadUtil.run(() -> notifyQueue.set(new Object()));
	}

	@Override
	protected void onClassSelected() {
		clearData();
	}

	@Override
	protected void onMethodSelected() {
		lastSelectedInsn = null;
		lastAnalysisResults = null;
		scheduleTableUpdate();
	}

	@Override
	protected void onFieldSelected() {
		clearData();
	}

	@Override
	protected void onPipelineOutputUpdate() {
		lastSelectedInsn = null;
		lastAnalysisResults = null;
		scheduleTableUpdate();
	}

	@Override
	public void install(@Nonnull Editor editor) {
		super.install(editor);

		// Not reusing this pane, so we don't need to track for removal
		editor.getCaretPosEventStream()
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.SHORT_DELAY_MS))
				.filter(c -> editor.getCodeArea().getSelection().getLength() == 0) // Skip updates while user is selecting text
				.addObserver(e -> scheduleTableUpdate());
	}

	/**
	 * Holds the displayed state and the state used to identify changes caused by the selected instruction.
	 *
	 * @param current
	 * 		Frame currently displayed, or {@code null} when analysis data is incomplete.
	 * @param prior
	 * 		Frame immediately preceding the displayed state, or {@code null} when unavailable.
	 */
	private record FrameSelection(@Nullable Frame current, @Nullable Frame prior) {}

	/**
	 * Models the state of a variable.
	 *
	 * @param name
	 * 		Variable name.
	 * @param type
	 * 		Variable type.
	 * @param value
	 * 		Variable value.
	 * @param priorValue
	 * 		Prior state in previous frame, if known.
	 */
	private record JvmVariableState(@Nonnull String name, @Nonnull Type type, @Nonnull Value value,
	                                @Nullable Value priorValue) {}

	/**
	 * Models an item on the stack.
	 *
	 * @param type
	 * 		Type of item.
	 * @param value
	 * 		Value of item.
	 * @param priorValue
	 * 		Prior state in previous frame, if known.
	 */
	private record JvmStackState(@Nonnull Type type, @Nonnull Value value, @Nullable Value priorValue) {}
}
