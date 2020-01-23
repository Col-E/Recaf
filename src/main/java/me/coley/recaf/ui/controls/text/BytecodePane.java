package me.coley.recaf.ui.controls.text;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.ui.controls.text.model.Languages;
import me.coley.recaf.util.*;
import me.coley.recaf.workspace.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.time.Duration;

/**
 * Bytecode-focused text editor.
 *
 * @author Matt
 */
public class BytecodePane extends TextPane<AssemblerException, BytecodeErrorHandling> {
	public static final int HOVER_ERR_TIME = 50;
	private final BytecodeSuggestHandler suggestHandler = new BytecodeSuggestHandler(this);
	private final String className;
	private final String methodName;
	private final String methodDesc;
	private ParseResult<RootAST> lastParse;
	private MethodNode current;

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param className
	 * 		Name of class containing the method.
	 * @param methodName
	 * 		Target method name.
	 * @param methodDesc
	 * 		Target method descriptor.
	 */
	public BytecodePane(GuiController controller, String className, String methodName, String methodDesc) {
		super(controller, Languages.find("bytecode"));
		setErrorHandler(new BytecodeErrorHandling(this));
		codeArea.setMouseOverTextDelay(Duration.ofMillis(HOVER_ERR_TIME));
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		setOnCodeChange(text -> getErrorHandler().onCodeChange(text, () -> {
			// Reset current cache
			current = null;
			// Setup assembler
			ParseResult<RootAST> result = Parse.parse(getText());
			lastParse = result;
			Assembler assembler = new Assembler(className);
			// Recompile & verify code
			MethodNode generated = assembler.compile(result);
			// Store result
			current = generated;
			current.name = methodName;
		}));
		// Setup auto-complete
		suggestHandler.setup();

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
		MethodNode method  = ClassUtil.getMethod(cr, ClassReader.SKIP_FRAMES, methodName, methodDesc);
		if (method == null){
			setEditable(false);
			setText("# Failed to fetch method: " + className + "." + methodName + methodDesc);
			forgetHistory();
			return false;
		}
		try {
			Disassembler disassembler = new Disassembler();
			setText(disassembler.disassemble(method));
			forgetHistory();
			return true;
		} catch(Exception ex) {
			setText("# Failed to disassemble method: " + className + "." + methodName + methodDesc);
			Log.error(ex, "Failed disassembly of '{}.{}{}'\nReason: ", className,
					methodName, methodDesc, ex.getMessage());
			return false;
		}
	}

	/**
	 * @return Modified class bytecode.
	 */
	public byte[] assemble() {
		// Skip of not saved
		if (current == null)
			return null;
		boolean found = false;
		ClassReader cr  = controller.getWorkspace().getClassReader(className);
		ClassNode node = ClassUtil.getNode(cr, ClassReader.EXPAND_FRAMES);
		for(int i = 0; i < node.methods.size(); i++) {
			MethodNode mn = node.methods.get(i);
			if(mn.name.equals(methodName) && mn.desc.equals(methodDesc)) {
				ClassUtil.copyMethodMetadata(current, mn);
				node.methods.set(i, current);
				found = true;
				break;
			}
		}
		// Skip if no method match
		if(!found) {
			Log.error("No method match for {}.{}{}", className, methodName, methodDesc);
			return null;
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
