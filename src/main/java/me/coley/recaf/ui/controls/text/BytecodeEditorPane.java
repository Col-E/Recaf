package me.coley.recaf.ui.controls.text;

import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.effect.ColorAdjust;
import me.coley.recaf.metadata.Comments;
import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.ClassVisitorPlugin;
import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.ui.controls.text.model.Languages;
import me.coley.recaf.util.*;
import me.coley.recaf.util.struct.LineException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.time.Duration;
import java.util.List;

/**
 * Bytecode-focused text editor.
 *
 * @author Matt
 */
public class BytecodeEditorPane extends EditorPane<BytecodeErrorHandling, BytecodeContextHandling> {
	private static final double DEFAULT_BOTTOM_DISPLAY_PERCENT = 0.72;
	public static final int HOVER_ERR_TIME = 50;
	private BytecodeStackHelper stackHelper;
	private BytecodeLocalHelper localHelper;
	private IconView errorGraphic;
	private ParseResult<RootAST> lastParse;
	protected final String className;
	protected final boolean isMethod;
	protected String memberName;
	protected String memberDesc;
	protected MethodNode currentMethod;
	protected FieldNode currentField;

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param className
	 * 		Name of class containing the method.
	 * @param memberName
	 * 		Target member name.
	 * @param memberDesc
	 * 		Target member descriptor.
	 */
	public BytecodeEditorPane(GuiController controller, String className, String memberName, String memberDesc) {
		super(controller, Languages.find("bytecode"), BytecodeContextHandling::new);
		setErrorHandler(new BytecodeErrorHandling(this));
		codeArea.setMouseOverTextDelay(Duration.ofMillis(HOVER_ERR_TIME));
		this.className = className;
		this.memberName = memberName;
		this.memberDesc = memberDesc;
		this.isMethod = memberDesc.contains("(");
		final Assembler<?> ass = isMethod ?
				new MethodAssembler(className, controller) : new FieldAssembler();

		setOnCodeChange(text -> getErrorHandler().onCodeChange(() -> {
			// Reset current cache
			currentField = null;
			currentMethod = null;
			// Setup assembler & context handling
			ParseResult<RootAST> result = Parse.parse(getText());
			if (result.isSuccess())
				contextHandler.setAST(result.getRoot());
			lastParse = result;
			if(isMethod) {
				MethodAssembler assembler = (MethodAssembler) ass;
				if (controller.config().assembler().useExistingData) {
					MethodNode existingMethod = ClassUtil.getMethod(controller.getWorkspace()
							.getClassReader(className), 0, memberName, memberDesc);
					if (existingMethod != null && existingMethod.localVariables != null) {
						// We call the disassembler's methods here so that any changes the disassembler
						// makes to the local variables is what gets populated as default information
						Disassembler.splitSameIndexedVariablesOfDiffNames(existingMethod);
						Disassembler.splitSameNamedVariablesOfDiffTypes(existingMethod);
						assembler.setDefaultVariables(existingMethod.localVariables);
					}
				}
				// Recompile & verify code
				currentMethod = assembler.compile(result);
				stackHelper.setMethodAssembler(assembler);
				localHelper.setMethodAssembler(assembler);
			} else {
				FieldAssembler assembler = (FieldAssembler) ass;
				// Recompile
				currentField = assembler.compile(result);
			}
		}));
		setOnKeyReleased(e -> {
			if(controller.config().keys().gotoDef.match(e))
				contextHandler.gotoSelectedDef();
		});
		codeArea.caretPositionProperty().addListener((n, o, v) -> {
			stackHelper.setLine(codeArea.getCurrentParagraph() + 1);
		});
		// Setup auto-complete
		new BytecodeSuggestHandler(this).setup();
	}

	@Override
	protected void setupBottomContent() {
		errorGraphic = new IconView("icons/error.png");
		stackHelper = new BytecodeStackHelper(this);
		stackHelper.getStyleClass().add("stack-helper");
		localHelper = new BytecodeLocalHelper(this);
		localHelper.getStyleClass().add("local-helper");
		split.setDividerPositions(DEFAULT_BOTTOM_DISPLAY_PERCENT);
		// Done in runLater so we have proper access to "isMethod"
		Platform.runLater(() -> {
			Tab tabErrors = new Tab(LangUtil.translate("misc.errors"));
			tabErrors.setGraphic(errorGraphic);
			tabErrors.setContent(errorList);
			tabErrors.setClosable(false);
			TabPane tabs = new TabPane();
			if (isMethod) {
				Tab tabStack = new Tab(LangUtil.translate("ui.edit.method.stackhelper"));
				tabStack.setGraphic(new IconView("icons/stack.png"));
				tabStack.setContent(stackHelper);
				tabStack.setClosable(false);
				Tab tabLocals = new Tab(LangUtil.translate("ui.bean.method.localvariables.name"));
				tabLocals.setGraphic(new IconView("icons/variable.png"));
				tabLocals.setContent(localHelper);
				tabLocals.setClosable(false);
				tabs.getTabs().addAll(tabErrors, tabStack, tabLocals);
			} else {
				tabs.getTabs().addAll(tabErrors);
			}
			bottomContent.setCenter(tabs);
			onErrorsReceived(null);
		});
	}

	/**
	 * Display disassembled code in the code-area.
	 *
	 * @return {@code true} if disassembly was successful.
	 */
	public boolean disassemble() {
		ClassReader cr  = controller.getWorkspace().getClassReader(className);
		if(cr == null) {
			setEditable(false);
			setText("# Failed to fetch class: " + className);
			forgetHistory();
			return false;
		}
		if (isMethod) {
			int flags = ClassReader.SKIP_FRAMES;
			if (controller.config().assembler().stripDebug)
				flags |= ClassReader.SKIP_DEBUG;
			MethodNode method = ClassUtil.getMethod(cr, flags, memberName, memberDesc);
			if(method == null) {
				setEditable(false);
				setText("# Failed to fetch method: " + className + "." + memberName + memberDesc);
				forgetHistory();
				return false;
			}
			try {
				Disassembler disassembler = new Disassembler();
				setText(disassembler.disassemble(method));
				forgetHistory();
				return true;
			} catch(Exception ex) {
				setText("# Failed to disassemble method: " + className + "." + memberName + memberDesc);
				Log.error(ex, "Failed disassembly of '{}.{}{}'\nReason: ", className,
						memberName, memberDesc, ex.getMessage());
				return false;
			}
		} else {
			FieldNode field = ClassUtil.getField(cr, ClassReader.SKIP_FRAMES, memberName, memberDesc);
			if(field == null) {
				setEditable(false);
				setText("# Failed to fetch field: " + className + "." + memberName);
				forgetHistory();
				return false;
			}
			try {
				Disassembler disassembler = new Disassembler();
				setText(disassembler.disassemble(field));
				forgetHistory();
				return true;
			} catch(Exception ex) {
				setText("# Failed to disassemble field: " + className + "." + memberName);
				Log.error(ex, "Failed disassembly of '{}.{}'\nReason: ", className, memberName, ex.getMessage());
				return false;
			}
		}
	}

	/**
	 * @return Modified class bytecode.
	 */
	public byte[] assemble() {
		if((isMethod && currentMethod == null) || (!isMethod && currentField == null)) {
			// Skip of not saved
			return null;
		}
		// Don't use the final member name/desc, use whatever has been assembled
		String newMemberName = null;
		String newMemberDesc = null;
		if (isMethod) {
			newMemberName = currentMethod.name;
			newMemberDesc = currentMethod.desc;
		} else {
			newMemberName = currentField.name;
			newMemberDesc = currentField.desc;
		}
		// Check if user changed the name
		ClassReader cr  = controller.getWorkspace().getClassReader(className);
		ClassNode existingNode = ClassUtil.getNode(cr, ClassReader.EXPAND_FRAMES);
		int removedIndex = removeIfRenamed(newMemberName, newMemberDesc, existingNode);
		// Update last used name
		memberName = newMemberName;
		memberDesc = newMemberDesc;
		updateOrInsert(newMemberName, newMemberDesc, existingNode, removedIndex);
		// Compile changes
		ClassWriter cw = controller.getWorkspace().createWriter(ClassWriter.COMPUTE_FRAMES);
		ClassVisitor visitor = cw;
		for (ClassVisitorPlugin visitorPlugin : PluginsManager.getInstance()
				.ofType(ClassVisitorPlugin.class)) {
			visitor = visitorPlugin.intercept(visitor);
		}
		existingNode.accept(visitor);
		return cw.toByteArray();
	}


	protected int removeIfRenamed(String newMemberName, String newMemberDesc, ClassNode existingNode) {
		if ((memberName != null && !memberName.equals(newMemberName)) ||
				(memberDesc != null && !memberDesc.equals(newMemberDesc))) {
			// Remove the old member
			Log.debug("User changed member definition name or desc when inserting a new member");
			if (isMethod) {
				for(int i = 0; i < existingNode.methods.size(); i++) {
					MethodNode existingMethod = existingNode.methods.get(i);
					if(existingMethod.name.equals(memberName) && existingMethod.desc.equals(memberDesc)) {
						existingNode.methods.remove(i);
						return i;
					}
				}
			} else {
				for(int i = 0; i < existingNode.fields.size(); i++) {
					FieldNode existingField = existingNode.fields.get(i);
					if(existingField.name.equals(memberName) && existingField.desc.equals(memberDesc)) {
						existingNode.fields.remove(i);
						return i;
					}
				}
			}
		}
		return -1;
	}

	protected void updateOrInsert(String newMemberName, String newMemberDesc, ClassNode existingNode, int removedIdx) {
		boolean found = false;
		if (isMethod) {
			// Reinsert at the location if the method was removed due to a definition change
			if (removedIdx >= 0) {
				existingNode.methods.add(removedIdx, currentMethod);
				return;
			}
			// Overwrite if its been added and we're making an change
			for(int i = 0; i < existingNode.methods.size(); i++) {
				MethodNode existingMethod = existingNode.methods.get(i);
				if(existingMethod.name.equals(newMemberName) && existingMethod.desc.equals(newMemberDesc)) {
					Comments.removeComments(existingMethod);
					ClassUtil.copyMethodMetadata(existingMethod, currentMethod);
					existingNode.methods.set(i, currentMethod);
					found = true;
					break;
				}
			}
			// Add if no method match
			if(!found) {
				existingNode.methods.add(currentMethod);
			}
		} else {
			// Reinsert at the location if the field was removed due to a definition change
			if (removedIdx >= 0) {
				existingNode.fields.add(removedIdx, currentField);
				return;
			}
			// Overwrite if its been added and we're making an change
			for(int i = 0; i < existingNode.fields.size(); i++) {
				FieldNode existingField = existingNode.fields.get(i);
				if(existingField.name.equals(newMemberName) && existingField.desc.equals(newMemberDesc)) {
					ClassUtil.copyFieldMetadata(currentField, existingField);
					existingNode.fields.set(i, currentField);
					found = true;
					break;
				}
			}
			// Add if no field match
			if(!found) {
				existingNode.fields.add(currentField);
			}
		}
	}

	/**
	 * @return Last assembler parse result.
	 */
	public ParseResult<RootAST> getLastParse() {
		return lastParse;
	}

	/**
	 * Called by the error handler to notify the UI of error status.
	 *
	 * @param exceptions
	 * 		Errors reported.
	 */
	public void onErrorsReceived(List<LineException> exceptions) {
		if (exceptions == null || exceptions.isEmpty()) {
			ColorAdjust colorAdjust = new ColorAdjust();
			colorAdjust.setBrightness(-0.7);
			colorAdjust.setSaturation(-0.8);
			errorGraphic.setEffect(colorAdjust);
		} else {
			errorGraphic.setEffect(null);
			stackHelper.setErrored();
		}
	}
}
