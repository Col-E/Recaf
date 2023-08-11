package me.coley.recaf.ui.control.code.bytecode;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import me.coley.recaf.RecafUI;
import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.BytecodeException;
import me.coley.recaf.assemble.ContextualPipeline;
import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.analysis.Analysis;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.insn.*;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.assemble.pipeline.*;
import me.coley.recaf.assemble.suggestions.Suggestions;
import me.coley.recaf.assemble.suggestions.SuggestionsResults;
import me.coley.recaf.assemble.suggestions.type.NoSuggestionsSuggestion;
import me.coley.recaf.assemble.suggestions.type.Suggestion;
import me.coley.recaf.assemble.transformer.BytecodeToAstTransformer;
import me.coley.recaf.assemble.transformer.JasmTransformUtil;
import me.coley.recaf.assemble.validation.MessageLevel;
import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.Validator;
import me.coley.recaf.code.*;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.AssemblerConfig;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.control.VirtualizedContextMenu;
import me.coley.recaf.ui.control.code.*;
import me.coley.recaf.ui.pane.assembler.FlowHighlighter;
import me.coley.recaf.ui.pane.assembler.SelectedUpdater;
import me.coley.recaf.ui.pane.assembler.VariableHighlighter;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.NodeEvents;
import me.coley.recaf.util.StackTraceUtil;
import me.coley.recaf.util.WorkspaceTreeService;
import me.coley.recaf.util.logging.DebuggingLogger;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.DelayedExecutor;
import me.coley.recaf.util.threading.DelayedRunnable;
import me.coley.recaf.util.visitor.FieldReplacingVisitor;
import me.coley.recaf.util.visitor.MethodReplacingVisitor;
import me.coley.recaf.util.visitor.SingleMemberVisitor;
import me.coley.recaf.util.visitor.WorkspaceClassWriter;
import me.coley.recaf.workspace.resource.Resource;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.instructions.*;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.TwoDimensional;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static me.coley.recaf.ui.control.code.ProblemOrigin.BYTECODE_PARSING;
import static me.coley.recaf.ui.control.code.ProblemOrigin.BYTECODE_VALIDATION;
import static me.coley.recaf.ui.util.Menus.action;
import static me.darknet.assembler.parser.Group.GroupType;

/**
 * Text editing portion of the assembler UI.
 *
 * @author Matt Coley
 */
public class AssemblerArea extends SyntaxArea implements MemberEditor, PipelineCompletionListener,
		AstValidationListener, BytecodeValidationListener, ParserFailureListener, BytecodeFailureListener,
		ParserCompletionListener {
	private static final DebuggingLogger logger = Logging.get(AssemblerArea.class);
	private final DelayedExecutor updatePipelineInput;
	private final ProblemTracking problemTracking;
	private final ContextualPipeline pipeline;
	private final Suggestions suggestions;
	private VirtualizedContextMenu<Suggestion> suggestionsMenu;
	private ClassInfo classInfo;
	private MemberInfo targetMember;
	private ContextMenu menu;

	/**
	 * Sets up the editor area.
	 *
	 * @param problemTracking
	 * 		Optional problem tracking implementation to enable line problem indicators.
	 * @param pipeline
	 * 		Assembler pipeline.
	 */
	public AssemblerArea(ProblemTracking problemTracking, ContextualPipeline pipeline) {
		super(Languages.JAVA_BYTECODE, problemTracking);
		this.problemTracking = problemTracking;
		this.pipeline = pipeline;
		this.updatePipelineInput = new DelayedRunnable(config().updateDelayMs, () -> {
			pipeline.setText(getText());
			handleAstUpdate();
		});
		// Setup selection updater for full-class assembling.
		// This handles which member is currently active for things like the analysis tab.
		SelectedUpdater selectedUpdater = new SelectedUpdater(pipeline);
		selectedUpdater.addCaretPositionListener(caretPositionProperty());
		// Setup variable highlighting
		VariableHighlighter variableHighlighter = new VariableHighlighter(pipeline, this);
		variableHighlighter.addIndicator(getIndicatorFactory());
		variableHighlighter.addCaretPositionListener(caretPositionProperty());
		// Setup flow highlighting
		FlowHighlighter flowHighlighter = new FlowHighlighter(pipeline, this);
		flowHighlighter.addIndicator(getIndicatorFactory());
		flowHighlighter.addCaretPositionListener(caretPositionProperty());
		// Context menu support
		setOnContextMenuRequested(this::onMenuRequested);
		// Register listeners to hook into problem tracking
		pipeline.addParserFailureListener(this);
		pipeline.addBytecodeFailureListener(this);
		pipeline.addPipelineCompletionListener(this);
		pipeline.addParserCompletionListener(this);
		boolean validateAst = config().astValidation;
		if (validateAst) {
			pipeline.addAstValidationListener(this);
		}
		boolean analyzeBytecode = config().bytecodeAnalysis;
		pipeline.setDoUseAnalysis(analyzeBytecode);
		if (analyzeBytecode) {
			pipeline.addBytecodeValidationListener(this);
		}
		WorkspaceTreeService treeService = RecafUI.getController().getServices().getTreeService();
		suggestions = new Suggestions(treeService.getCurrentClassTree(),
				RecafUI.getController().getWorkspace().getResources()::getClass, null);
		NodeEvents.addKeyPressHandler(this, event -> {
			if (Configs.keybinds().suggest.match(event))
				onSuggestionRequested();
		});
	}

	@Override
	protected void onTextChanged(PlainTextChange change) {
		super.onTextChanged(change);
		// Push back the pipeline update.
		// It'll run when the user stops updating the text.
		updatePipelineInput.delay();
	}

	@Override
	public void cleanup() {
		super.cleanup();
		// Stop update thread
		updatePipelineInput.cancel();
	}

	/**
	 * Updates and validates the AST.
	 */
	protected void handleAstUpdate() {
		try {
			if (pipeline.updateAst(config().usePrefix) && pipeline.validateAst()) {
				logger.debugging(l -> l.trace("AST updated and validated"));
				// Update suggestions data with definition changes
				if (pipeline.getUnit() != null && pipeline.isCurrentMethod())
					suggestions.setMethod(pipeline.getCurrentMethod());
				// Generate the ASM node type from the AST.
				if (pipeline.isMethod() &&
						pipeline.isOutputOutdated() &&
						pipeline.generateMethod())
					logger.debugging(l -> l.trace("AST compiled to method and analysis executed"));
			}
		} catch (Throwable t) {
			// Shouldn't occur, but make sure its known if it does
			logger.error("Unhandled exception in the AST parse thread", t);
		}
	}

	/**
	 * Disassembles the {@link #getTargetMember() target member} and updates the editor's text.
	 */
	public void disassemble() {
		if (classInfo == null) {
			logger.warn("Cannot disassemble, target class info missing");
			return;
		}
		BytecodeToAstTransformer transformer;
		ClassNode node = new ClassNode();
		ClassReader cr = classInfo.getClassReader();
		if (targetMember != null) {
			// Get the target member node
			cr.accept(new SingleMemberVisitor(node, targetMember), ClassReader.SKIP_FRAMES);
			// Since we visit only the target member info, there should only be one member in the list.
			if (targetMember.isMethod() && node.methods.isEmpty()) {
				logger.error("Failed to isolate method for disassembling '{}.{}{}'",
						node.name, targetMember.getName(), targetMember.getDescriptor());
				return;
			} else if (targetMember.isField() && node.fields.isEmpty()) {
				logger.error("Failed to isolate field for disassembling '{}.{} {}'",
						node.name, targetMember.getName(), targetMember.getDescriptor());
				return;
			}
			// Disassemble
			if (targetMember.isField()) {
				FieldNode field = node.fields.get(0);
				transformer = new BytecodeToAstTransformer(field);
			} else {
				MethodNode method = node.methods.get(0);
				transformer = new BytecodeToAstTransformer(method);
			}
		} else {
			// Disassemble class object
			cr.accept(node, ClassReader.SKIP_FRAMES);
			transformer = new BytecodeToAstTransformer(node);
		}
		transformer.visit();
		Unit unit = transformer.getUnit();

		String code = unit.print(config().createContext());
		// Update text
		setText(code);
		// Also attempt to assemble once the code is set.
		// We do not want to update the class, this is to initialize the pipeline state without the user needing
		// to manually trigger a save first.
		if (pipeline.updateAst(config().usePrefix)) {
			// Assemble the target member/class
			if (pipeline.isMethod())
				pipeline.generateMethod();
			else if (pipeline.isField())
				pipeline.generateField();
			else
				pipeline.generateClass();

			if(pipeline.isCurrentMethod()) {
				suggestions.setMethod(pipeline.getCurrentMethod());
			}

			// Run an initial generation within our assembler UI.
			// This will populate the local variable and analysis tab contents.
			SaveResult initialBuild;
			if (pipeline.isMethod())
				initialBuild = generateMethod(false);
			else if (pipeline.isField())
				initialBuild = generateField(false);
			else
				initialBuild = generateClass(false);
			if (initialBuild == SaveResult.SUCCESS)
				logger.debugging(l -> l.trace("Initial build of disassemble successful!"));
			else
				logger.warn("Initial build of disassemble failed!");
		}
	}

	/**
	 * Displays the suggestions menu for content at the current caret position.
	 */
	private void onSuggestionRequested() {
		// Get current cursor position
		int caretPosition = getCaretPosition();
		Position position = offsetToPosition(caretPosition, Bias.Backward);
		// Get AST group at the position
		Group group = pipeline.getASTElementAt(position.getMajor() + 1, position.getMinor());
		if (group != null) {
			Group parent = group.getParent();
			if (parent != null) {
				if (!parent.isType(GroupType.BODY)) {
					group = parent;
				}
			}
		}
		// Show new suggestions' context menu/list
		if (suggestionsMenu != null) suggestionsMenu.hide();
		VirtualizedContextMenu<Suggestion> suggestionsMenu = createSuggestionsMenu(caretPosition, group);
		suggestionsMenu.setAutoHide(true);
		suggestionsMenu.setHideOnEscape(true);
		suggestionsMenu.show(this, getCaretBounds().get().getMinX(), getCaretBounds().get().getMaxY());
		suggestionsMenu.requestFocus();
		this.suggestionsMenu = suggestionsMenu;
	}

	/**
	 * @param position
	 * 		Caret position.
	 * @param suggestionGroup
	 * 		JASM AST at position.
	 *
	 * @return Menu containing completion suggestions.
	 */
	private VirtualizedContextMenu<Suggestion> createSuggestionsMenu(int position, Group suggestionGroup) {
		// Update suggestions
		if(pipeline.isCurrentMethod()) {
			suggestions.setMethod(pipeline.getCurrentMethod());
		}
		// Get suggestions content
		SuggestionsResults results = suggestions.getSuggestion(suggestionGroup);
		Set<Suggestion> set = results.getValues().collect(Collectors.toCollection(TreeSet::new));
		results.invalidate();
		if (set.isEmpty()) {
			return new VirtualizedContextMenu<>(Suggestion::viewAsNode, List.of(new NoSuggestionsSuggestion()));
		}
		// Create the menu populated with completions
		VirtualizedContextMenu<Suggestion> menu = new VirtualizedContextMenu<>(Suggestion::viewAsNode, set);
		menu.setPrefSize(350, Math.min(set.size() * 15, 400));
		// Would be a good idea to work with "contexts" rather than pass this through like so
		menu.setOnAction(e -> e.getSelection().onAction(e, results, position, suggestionGroup, this));
		return menu;
	}

	private void onMenuRequested(ContextMenuEvent e) {
		// Close old menu
		if (menu != null) {
			menu.hide();
		}
		// Check if there is parsable AST info
		if (pipeline.getUnit() == null) {
			logger.warn("Could not request context menu since the code is not parsable!");
			return;
		}
		// Convert the event position to line/column
		CharacterHit hit = hit(e.getX(), e.getY());
		Position hitPos = offsetToPosition(hit.getInsertionIndex(),
				TwoDimensional.Bias.Backward);
		int line = hitPos.getMajor() + 1; // Major is line, position is 0 indexed
		int col = hitPos.getMinor(); // Minor is col
		// Sync caret
		moveTo(hit.getInsertionIndex());
		// Create menu if needed
		Element element = pipeline.getCodeElementAt(hit.getInsertionIndex());
		ContextBuilder menuBuilder = menuFor(element, hit.getInsertionIndex());
		if (menuBuilder != null) {
			// Show if present
			menu = menuBuilder.build();
			menu.setAutoHide(true);
			menu.setHideOnEscape(true);
			menu.show(getScene().getWindow(), e.getScreenX(), e.getScreenY());
			menu.requestFocus();
		} else {
			logger.warn("No recognized element at selected position [line {}, column {}]", line, col);
		}
	}

	public ContextBuilder menuFor(Element element, int position) {
		if (element instanceof FieldInstruction) {
			FieldInstruction fieldInsn = (FieldInstruction) element;
			CommonClassInfo owner = RecafUI.getController().getWorkspace()
					.getResources().getClass(fieldInsn.getOwner());
			if (owner == null) {
				logger.warn("Cannot find owner for field '{}'", fieldInsn.getName());
				return null;
			}
			FieldInfo field = owner.findField(fieldInsn.getName(), fieldInsn.getDesc());
			if (field == null) {
				logger.warn("Cannot find field '{}'", fieldInsn.getName());
				return null;
			}
			return ContextBuilder.forField(owner, field);
		} else if (element instanceof MethodInstruction) {
			MethodInstruction methodInsn = (MethodInstruction) element;
			CommonClassInfo owner = RecafUI.getController().getWorkspace()
					.getResources().getClass(methodInsn.getOwner());
			if (owner == null) {
				logger.warn("Cannot find owner for method '{}'", methodInsn.getName());
				return null;
			}
			MethodInfo method = owner.findMethod(methodInsn.getName(), methodInsn.getDesc());
			if (method == null) {
				logger.warn("Cannot find method '{}'", methodInsn.getName());
				return null;
			}
			return ContextBuilder.forMethod(owner, method);
		} else if (element instanceof TypeInstruction) {
			TypeInstruction typeInsn = (TypeInstruction) element;
			ClassInfo cls = RecafUI.getController().getWorkspace()
					.getResources().getClass(typeInsn.getType());
			if (cls == null) {
				logger.warn("Cannot find class '{}'", typeInsn.getType());
				return null;
			}
			return ContextBuilder.forClass(cls);
		} else if (element instanceof JumpInstruction) {
			JumpInstruction jumpInsn = (JumpInstruction) element;
			String label = jumpInsn.getLabel();
			Unit unit = pipeline.getUnit();
			if (unit == null || !unit.isMethod())
				return null;
			Label lab = unit.getDefinitionAsMethod().getCode().getLabel(label);
			if (lab == null) {
				logger.warn("Cannot find label '{}'", label);
				return null;
			}
			return new LabelContextBuilder(lab);
		} else if (element instanceof LookupSwitchInstruction) {
			Group actual = pipeline.getASTElementAt(position);
			if (actual == null) {
				logger.warn("Cannot find AST element at selected position [{}]", position);
				return null;
			}
			String label;
			if (!actual.isType(GroupType.CASE_LABEL) && !actual.isType(GroupType.DEFAULT_LABEL)) {
				actual = actual.getParent();
			}
			if (actual.isType(GroupType.CASE_LABEL)) {
				CaseLabelGroup caseLabel = (CaseLabelGroup) actual;
				label = caseLabel.getLabelValue().getLabel();
			} else if (actual.isType(GroupType.DEFAULT_LABEL)) {
				DefaultLabelGroup defaultLabel = (DefaultLabelGroup) actual;
				label = defaultLabel.getLabel();
			} else {
				logger.warn("Cannot find label for switch instruction");
				return null;
			}
			Unit unit = pipeline.getUnit();
			if (unit == null || !unit.isMethod())
				return null;
			Label lab = unit.getDefinitionAsMethod().getCode().getLabel(label);
			if (lab == null) {
				logger.warn("Cannot find label '{}'", label);
				return null;
			}
			return new LabelContextBuilder(lab);
		} else if (element instanceof TableSwitchInstruction) {
			Group actual = pipeline.getASTElementAt(position);
			if (actual == null) {
				logger.warn("Cannot find AST element at selected position [{}]", position);
				return null;
			}
			String label = "";
			if (actual.isType(GroupType.LABEL)) {
				label = ((LabelGroup) actual).getLabel();
			} else if (actual.isType(GroupType.DEFAULT_LABEL)) {
				DefaultLabelGroup defaultLabel = (DefaultLabelGroup) actual;
				label = defaultLabel.getLabel();
			}
			Unit unit = pipeline.getUnit();
			if (unit == null || !unit.isMethod())
				return null;
			Label lab = unit.getDefinitionAsMethod().getCode().getLabel(label);
			if (lab == null) {
				logger.warn("Cannot find label '{}'", label);
			}
			return new LabelContextBuilder(lab);
		}
		// aggressively try to find what was selected
		Group actual = pipeline.getASTElementAt(position);
		if (actual == null) {
			return null;
		}
		Group parent = actual.getParent();
		if (parent == null) {
			return null;
		}
		if (parent.isType(GroupType.TYPE)) {
			TypeGroup type = (TypeGroup) parent;
			if (type.getDescriptor().content().startsWith("(")) {
				return null;
			}
			ClassInfo classInfo = RecafUI.getController().getWorkspace()
					.getResources().getClass(type.getDescriptor().content());
			if (classInfo == null) {
				logger.warn("Cannot find class '{}'", type.getDescriptor().content());
				return null;
			}
			return ContextBuilder.forClass(classInfo);
		} else if (parent.isType(GroupType.HANDLE)) {
			HandleGroup handle = (HandleGroup) parent;
			HandleInfo hi = JasmTransformUtil.convertHandle(handle);
			CommonClassInfo ownerInfo = RecafUI.getController().getWorkspace()
					.getResources().getClass(hi.getOwner());
			if (ownerInfo == null) {
				logger.warn("Cannot find class '{}'", hi.getOwner());
				return null;
			}
			switch (handle.getHandleType().content()) {
				case "H_GETFIELD":
				case "H_GETSTATIC":
				case "H_PUTFIELD":
				case "H_PUTSTATIC":
					FieldInfo fieldInfo = ownerInfo.findField(hi.getName(), hi.getDesc());
					if (fieldInfo == null) {
						logger.warn("Cannot find field '{}'", hi.getName());
						return null;
					}
					return ContextBuilder.forField(ownerInfo, fieldInfo);
				case "H_INVOKEINTERFACE":
				case "H_INVOKESPECIAL":
				case "H_INVOKESTATIC":
				case "H_INVOKEVIRTUAL":
				case "H_NEWINVOKESPECIAL":
					MethodInfo methodInfo = ownerInfo.findMethod(hi.getName(), hi.getDesc());
					if (methodInfo == null) {
						logger.warn("Cannot find method '{}'", hi.getName());
						return null;
					}
					return ContextBuilder.forMethod(ownerInfo, methodInfo);
				default:
					logger.warn("Cannot find handle '{}'", handle.getHandleType().content());
					return null;
			}
		}
		return null;
	}

	@Override
	public SaveResult save() {
		// The unit must be updated in order to save.
		if (pipeline.getUnit() == null)
			return SaveResult.FAILURE;
		// Errors must be resolved before generating the field/method
		if (problemTracking.hasProblems(ProblemLevel.ERROR))
			return SaveResult.FAILURE;
		// Generate
		if (targetMember == null) return generateClass(true);
		else {
			if (targetMember.isMethod())
				return generateMethod(true);
			else
				return generateField(true);
		}
	}

	/**
	 * Generates and updates the {@link #getTargetMember() target field} if generation succeeded.
	 *
	 * @param apply
	 *        {@code true} to update the {@link #getCurrentClassInfo() declaring class} with the generated field.
	 *
	 * @return Generation result status.
	 */
	private SaveResult generateField(boolean apply) {
		// Generate field
		if (pipeline.isOutputOutdated() && !pipeline.generateField())
			return SaveResult.FAILURE;
		FieldNode fieldAssembled = pipeline.getLastField();
		if (fieldAssembled == null)
			logger.error("Field was not assembled after pipeline completion!");
		// Check if there were reported errors
		if (problemTracking.hasProblems(ProblemLevel.ERROR))
			return SaveResult.FAILURE;
		// Update field
		if (apply)
			return updateClass(fieldAssembled);
		return SaveResult.SUCCESS;
	}

	/**
	 * Generates and updates the {@link #getTargetMember() target method} if generation succeeded.
	 *
	 * @param apply
	 *        {@code true} to update the {@link #getCurrentClassInfo() declaring class} with the generated method.
	 *
	 * @return Generation result status.
	 */
	private SaveResult generateMethod(boolean apply) {
		// Generate method if not up-to-date
		if (pipeline.isOutputOutdated() && !pipeline.generateMethod())
			return SaveResult.FAILURE;
		MethodNode methodAssembled = pipeline.getLastMethod();
		if (methodAssembled == null)
			logger.error("Class was not assembled after pipeline completion!");
		// Check if there were reported errors
		if (problemTracking.hasProblems(ProblemLevel.ERROR))
			return SaveResult.FAILURE;
		// Update field
		if (apply)
			return updateClass(methodAssembled);
		return SaveResult.SUCCESS;
	}

	/**
	 * Generates and updates the {@link #getCurrentClassInfo() target class} if generation succeeded.
	 *
	 * @param apply
	 *        {@code true} to update the {@link #getCurrentClassInfo() declaring class} with the generated method.
	 *
	 * @return Generation result status.
	 */
	private SaveResult generateClass(boolean apply) {
		// Generate method if not up-to-date
		if (pipeline.isOutputOutdated() && !pipeline.generateClass())
			return SaveResult.FAILURE;
		ClassNode classAssembled = pipeline.getLastClass();
		if (classAssembled == null)
			logger.error("Class was not assembled after pipeline completion!");
		// Check if there were reported errors
		if (problemTracking.hasProblems(ProblemLevel.ERROR))
			return SaveResult.FAILURE;
		// Update class
		if (apply) updateClass(classAssembled);
		return SaveResult.SUCCESS;
	}

	/**
	 * Called to update the {@link #getTargetMember() target field}.
	 *
	 * @param updatedField
	 * 		Field that was compiled.
	 *
	 * @return Result of update operation.
	 */
	private SaveResult updateClass(FieldNode updatedField) {
		return updateClass(cw -> new FieldReplacingVisitor(cw, targetMember, updatedField));
	}

	/**
	 * Called to update the {@link #getTargetMember() target method}.
	 *
	 * @param updatedMethod
	 * 		Method that was compiled.
	 *
	 * @return Result of update operation.
	 */
	private SaveResult updateClass(MethodNode updatedMethod) {
		try {
			return updateClass(cw -> new MethodReplacingVisitor(cw, targetMember, updatedMethod));
		} catch (Throwable t) {
			StackTraceElement[] trace = StackTraceUtil.cutOffToUsage(t, getClass());
			String classLocation = trace[0].getClassName();
			if ("org.objectweb.asm.Frame".equals(classLocation))
				logger.error("Failed to reassemble method (ASM frame generation)", t);
			else
				logger.error("Failed to reassemble method (Unknown)", t);
			return SaveResult.FAILURE;
		}
	}

	/**
	 * Called to update the an entire class <i>(Class level assembly)</i>.
	 *
	 * @param updatedClass
	 * 		Class that was compiled.
	 *
	 * @return Result of update operation.
	 */
	private SaveResult updateClass(ClassNode updatedClass) {
		// Because we are operating on whole class that likely hasn't gotten any frames,
		// we're going to need to compute them.
		int flags = ClassWriter.COMPUTE_FRAMES;
		ClassWriter cw = new WorkspaceClassWriter(RecafUI.getController(), flags);
		try {
			updatedClass.accept(cw);
		} catch (Throwable t) {
			StackTraceElement[] trace = StackTraceUtil.cutOffToUsage(t, getClass());
			String classLocation = trace[0].getClassName();
			if ("org.objectweb.asm.Frame".equals(classLocation))
				logger.error("Failed to reassemble class (ASM frame generation)", t);
			else
				logger.error("Failed to reassemble class (Unknown)", t);
			return SaveResult.FAILURE;
		}
		// Done, update the workspace
		byte[] updatedBytecode = cw.toByteArray();
		Resource resource = RecafUI.getController().getWorkspace().getResources().getPrimary();
		resource.getClasses().put(ClassInfo.read(updatedBytecode));
		return SaveResult.SUCCESS;
	}

	/**
	 * @param replacerProvider
	 * 		Function to map a class-writer to a delegated class visitor.
	 *
	 * @return Result of update operation.
	 *
	 * @see #updateClass(MethodNode)
	 * @see #updateClass(FieldNode)
	 * @see #updateClass(ClassNode)
	 */
	private SaveResult updateClass(Function<ClassWriter, ClassVisitor> replacerProvider) {
		// Because we are creating a new method, we need to generate frames.
		int flags = ClassWriter.COMPUTE_FRAMES;
		ClassWriter cw = new WorkspaceClassWriter(RecafUI.getController(), flags);
		ClassVisitor replacer = replacerProvider.apply(cw);
		ClassReader cr = classInfo.getClassReader();
		// Read and filter through replacer. Skip frames since we're just going to compute them anyways.
		try {
			cr.accept(replacer, ClassReader.SKIP_FRAMES);
		} catch (Exception ex) {
			StackTraceElement[] trace = StackTraceUtil.cutOffToUsage(ex, getClass());
			String classLocation = trace[0].getClassName();
			if ("org.objectweb.asm.Frame".equals(classLocation))
				logger.error("Failed to reassemble class (ASM frame generation)", ex);
			else
				logger.error("Failed to reassemble class (Unknown)", ex);
			return SaveResult.FAILURE;
		}
		// Done, update the workspace
		byte[] updatedBytecode = cw.toByteArray();
		Resource resource = RecafUI.getController().getWorkspace().getResources().getPrimary();
		resource.getClasses().put(ClassInfo.read(updatedBytecode));
		return SaveResult.SUCCESS;
	}

	/**
	 * @param validator
	 * 		Validator that was visited.
	 *
	 * @return {@code true} if errors were reported.
	 * {@code false} if no errors were reported.
	 */
	private boolean reportErrors(ProblemOrigin origin, Validator<?> validator) {
		// These are validation messages that aren't logical killers, but may prevent actual further processing.
		boolean hasErrors = false;
		problemTracking.clearOfType(origin);
		for (ValidationMessage message : validator.getMessages()) {
			if (!hasErrors && message.getLevel() == MessageLevel.ERROR) {
				hasErrors = true;
			}
			int line = message.getSource().getLine();
			String msg = message.getMessage();
			ProblemLevel level;
			switch (message.getLevel()) {
				case INFO:
					level = ProblemLevel.INFO;
					break;
				case WARN:
					level = ProblemLevel.WARNING;
					break;
				case ERROR:
				default:
					level = ProblemLevel.ERROR;
					break;
			}
			ProblemInfo problem = new ProblemInfo(origin, level, line, msg);
			problemTracking.addProblem(line, problem);
		}
		return hasErrors;
	}

	@Override
	public void onAstValidationBegin(Unit unit) {
		// Validation implies parse completed successfully
		problemTracking.clearOfType(BYTECODE_PARSING);
	}

	@Override
	public void onAstValidationComplete(Unit unit, Validator<?> validator) {
		reportErrors(BYTECODE_VALIDATION, validator);
	}

	@Override
	public void onBytecodeValidationBegin(Object object) {
		// no-op, bytecode can be compiled even with validation warnings
	}

	@Override
	public void onBytecodeValidationComplete(Object object, Validator<?> validator) {
		reportErrors(BYTECODE_VALIDATION, validator);
	}

	@Override
	public void onCompletedOutput(Object object) {
		if (!pipeline.isMethod())
			return;
		Analysis analysis = pipeline.getLastAnalysis();
		if (analysis != null) {
			for (int i = 0; i < analysis.getFrames().size(); i++) {
				Frame frame = analysis.frame(i);
				if (frame.isWonky()) {
					AbstractInstruction instruction = pipeline.getUnit().getDefinitionAsMethod().getCode().getInstructions().get(i);
					int line = instruction.getLine();
					ProblemInfo problem = new ProblemInfo(BYTECODE_VALIDATION, ProblemLevel.WARNING, line, frame.getWonkyReason());
					problemTracking.addProblem(line, problem);
				}
			}
		}
	}

	@Override
	public void onParseFail(AssemblerException ex) {
		int line = ex.getLocation().getLine();
		String msg = ex.describe();
		ProblemInfo problem = new ProblemInfo(BYTECODE_PARSING, ProblemLevel.ERROR, line, msg);
		problemTracking.addProblem(line, problem);
	}

	@Override
	public void onParserTransformFail(AssemblerException ex) {
		int line = ex.getLocation().getLine();
		String msg = ex.describe();
		ProblemInfo problem = new ProblemInfo(BYTECODE_PARSING, ProblemLevel.ERROR, line, msg);
		problemTracking.addProblem(line, problem);
	}

	@Override
	public void onAstValidationError(AstException ex) {
		int line = ex.getSource().getLine();
		String msg = ex.getMessage();
		ProblemInfo problem = new ProblemInfo(BYTECODE_VALIDATION, ProblemLevel.ERROR, line, msg);
		problemTracking.addProblem(line, problem);
	}

	@Override
	public void onValidationFailure(Object object, BytecodeException ex) {
		int line = ex.getLine();
		String msg = ex.getMessage();
		ProblemInfo problem = new ProblemInfo(BYTECODE_VALIDATION, ProblemLevel.ERROR, line, msg);
		problemTracking.addProblem(line, problem);
	}

	@Override
	public void onCompileFailure(Unit unit, MethodCompileException ex) {
		int line = ex.getSource().getLine();
		String msg = ex.getMessage();
		ProblemInfo problem = new ProblemInfo(BYTECODE_VALIDATION, ProblemLevel.ERROR, line, msg);
		problemTracking.addProblem(line, problem);
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		pipeline.setType(newValue.getName());
		if (newValue instanceof ClassInfo) {
			classInfo = (ClassInfo) newValue;
		}
	}

	@Override
	public MemberInfo getTargetMember() {
		return targetMember;
	}

	@Override
	public void setTargetMember(MemberInfo targetMember) {
		this.targetMember = targetMember;
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return classInfo;
	}

	@Override
	public boolean supportsEditing() {
		return true;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public boolean supportsMemberSelection() {
		return false;
	}

	@Override
	public boolean isMemberSelectionReady() {
		return false;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		// no-op, this represents an actual member so nothing to select
	}

	private static AssemblerConfig config() {
		return Configs.assembler();
	}

	@Override
	public void onCompleteTokenize(List<Token> tokens) {
		// no-op
	}

	@Override
	public void onCompleteParse(List<Group> groups) {
		problemTracking.clearOfType(BYTECODE_PARSING);
	}

	@Override
	public void onCompleteTransform(Unit unit) {
		// no-op
	}

	public class LabelContextBuilder extends ContextBuilder {
		private final Label label;

		public LabelContextBuilder(Label label) {
			this.label = label;
		}

		@Override
		public ContextMenu build() {
			ContextMenu menu = new ContextMenu();
			menu.getItems().add(action("menu.goto.label", Icons.OPEN, () -> {
				// -1 because positions are 0 indexed
				moveTo(label.getLine() - 1, label.getColumnStart() - 1);
			}));
			return menu;
		}

		@Override
		protected Resource findContainerResource() {
			return null;
		}
	}
}
