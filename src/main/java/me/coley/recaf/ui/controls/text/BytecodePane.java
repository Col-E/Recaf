package me.coley.recaf.ui.controls.text;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.ui.controls.text.model.Languages;
import me.coley.recaf.util.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.time.Duration;

/**
 * Bytecode-focused text editor.
 *
 * @author Matt
 */
public class BytecodePane extends TextPane<BytecodeErrorHandling, BytecodeContextHandling> {
	public static final int HOVER_ERR_TIME = 50;
	private final String className;
	private final String memberName;
	private final String memberDesc;
	private final boolean isMethod;
	private ParseResult<RootAST> lastParse;
	private MethodNode currentMethod;
	private FieldNode currentField;

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
	public BytecodePane(GuiController controller, String className, String memberName, String memberDesc) {
		super(controller, Languages.find("bytecode"), BytecodeContextHandling::new);
		setErrorHandler(new BytecodeErrorHandling(this));
		codeArea.setMouseOverTextDelay(Duration.ofMillis(HOVER_ERR_TIME));
		this.className = className;
		this.memberName = memberName;
		this.memberDesc = memberDesc;
		this.isMethod = memberDesc.contains("(");
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
				MethodAssembler assembler = new MethodAssembler(className, controller.config().assembler());
				// Recompile & verify code
				currentMethod = assembler.compile(result);
				currentMethod.name = memberName;
			} else {
				FieldAssembler assembler = new FieldAssembler();
				// Recompile
				currentField = assembler.compile(result);
				currentField.name = memberName;
			}
		}));
		setOnKeyReleased(e -> {
			if(controller.config().keys().gotoDef.match(e))
				contextHandler.gotoSelectedDef();
		});
		// Setup auto-complete
		new BytecodeSuggestHandler(this).setup();
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
			MethodNode method = ClassUtil.getMethod(cr, ClassReader.SKIP_FRAMES, memberName, memberDesc);
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
		boolean found = false;
		ClassReader cr  = controller.getWorkspace().getClassReader(className);
		ClassNode node = ClassUtil.getNode(cr, ClassReader.EXPAND_FRAMES);
		if (isMethod) {
			for(int i = 0; i < node.methods.size(); i++) {
				MethodNode mn = node.methods.get(i);
				if(mn.name.equals(memberName) && mn.desc.equals(memberDesc)) {
					ClassUtil.copyMethodMetadata(currentMethod, mn);
					node.methods.set(i, currentMethod);
					found = true;
					break;
				}
			}
			// Skip if no method match
			if(!found) {
				Log.error("No method match for {}.{}{}", className, memberName, memberDesc);
				return null;
			}
		} else {
			for(int i = 0; i < node.fields.size(); i++) {
				FieldNode fn = node.fields.get(i);
				if(fn.name.equals(memberName) && fn.desc.equals(memberDesc)) {
					ClassUtil.copyFieldMetadata(currentField, fn);
					node.fields.set(i, currentField);
					found = true;
					break;
				}
			}
			// Skip if no field match
			if(!found) {
				Log.error("No field match for {}.{}", className, memberName);
				return null;
			}
		}
		// Compile changes
		ClassWriter cw = controller.getWorkspace().createWriter(ClassWriter.COMPUTE_FRAMES);
		node.accept(cw);
		return cw.toByteArray();
	}

	/**
	 * @return Last assembler parse result.
	 */
	public ParseResult<RootAST> getLastParse() {
		return lastParse;
	}
}
