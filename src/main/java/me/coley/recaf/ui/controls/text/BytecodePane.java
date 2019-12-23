package me.coley.recaf.ui.controls.text;

import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.assembly.*;
import me.coley.recaf.parse.assembly.visitors.AssemblyVisitor;
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
public class BytecodePane extends TextPane {
	public static final int HOVER_ERR_TIME = 50;
	private final BytecodeErrorHandling errHandler = new BytecodeErrorHandling(this);
	private final String className;
	private final String methodName;
	private final String methodDesc;
	private MethodNode current;

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param resource
	 * 		Resource containing the containing class.
	 * @param className
	 * 		Name of class containing the method.
	 * @param methodName
	 * 		Target method name.
	 * @param methodDesc
	 * 		Target method descriptor.
	 * @param access
	 * 		Target method access.
	 */
	public BytecodePane(GuiController controller, JavaResource resource, String className,
						String methodName, String methodDesc, int access) {
		super(controller, Languages.find("bytecode"));
		codeArea.setMouseOverTextDelay(Duration.ofMillis(HOVER_ERR_TIME));
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		setOnCodeChange(text -> errHandler.onCodeChange(text, () -> {
			// Reset current cache
			current = null;
			// Setup assembler
			AssemblyVisitor vis = new AssemblyVisitor();
			vis.setupMethod(access, methodDesc);
			vis.setDoAddVariables(true);
			try {
				// Recompile & verify code
				vis.visit(getText());
				vis.verify();
				// Store result
				current = vis.getMethod();
				current.name = methodName;
			} catch(VerifyException ex) {
				AbstractInsnNode insn = ex.getInsn();
				if (insn == null) {
					throw new LineParseException("Unknown line", ex.getMessage());
				}
				// Transform to LineParseException
				String[] lines = StringUtil.splitNewline(getText());
				int line = vis.getLine(insn);
				throw new LineParseException(line, lines[line], ex.getMessage());
			}
		}));
	}

	@Override
	protected boolean hasError(int line) {
		return errHandler.hasError(line);
	}

	@Override
	protected String getLineComment(int line) {
		return errHandler.getLineComment(line);
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
			return false;
		}
		MethodNode method  = ClassUtil.getMethod(cr, ClassReader.SKIP_FRAMES, methodName, methodDesc);
		if (method == null){
			setEditable(false);
			setText("# Failed to fetch method: " + className + "." + methodName + methodDesc);
			return false;
		}
		try {
			Disassembler disassembler = new Disassembler();
			setText(disassembler.disassemble(method));
			return true;
		} catch(LineParseException ex) {
			Log.error("Failed disassembly of '{}.{}{}'\nReason: Line {}: {}", className,
					methodName, methodDesc, ex.getLine(), ex.getText());
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
}
