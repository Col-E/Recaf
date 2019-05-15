package me.coley.recaf.parse.assembly;

import me.coley.recaf.bytecode.insn.NamedLabelRefInsn;
import me.coley.recaf.bytecode.insn.NamedVarRefInsn;
import me.coley.recaf.parse.assembly.exception.*;
import me.coley.recaf.parse.assembly.impl.*;
import me.coley.recaf.parse.assembly.util.LineData;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.Function;

import static org.objectweb.asm.tree.AbstractInsnNode.*;

// TODO: support for the following:
// - try-catch blocks

/**
 * Java bytecode assembly utility. Generated ASM MethodNodes from a given method declaration and
 * code <i>(represented as text)</i>.
 *
 * @author Matt
 */
public class Assembly {
	/**
	 * Map of instruction assemblers.
	 */
	private static final Map<Integer, Function<Integer, AbstractAssembler>> assemblers
			= new HashMap<>();
	/**
	 * List of exceptions thrown when attempting to assemble the method, from
	 * {@link #parseInstructions(String[])}.
	 */
	private final List<ExceptionWrapper> exceptionWrappers = new ArrayList<>();
	/**
	 * Generated method to return. see {@link #getMethod()}.
	 */
	private MethodNode method;
	/**
	 * Generate local variable table.
	 */
	private boolean locals;

	/**
	 * Define the method declaration. Assumes no exceptions in the definition.
	 *
	 * @param access
	 * 		Method access.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 */
	public void setMethodDeclaration(int access, String name, String desc) {
		setMethodDeclaration(access, name, desc, new String[0]);
	}

	/**
	 * Define the method declaration.
	 *
	 * @param access
	 * 		Method access.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 * @param exceptions
	 * 		Thrown exceptions.
	 */
	public void setMethodDeclaration(int access, String name, String desc, String[] exceptions) {
		this.method = new MethodNode(access, name, desc, null, exceptions);
	}

	/**
	 * Assembles the given lines of code. Output will be accessible via {@link #getMethod()}
	 * assuming the assembly was successful <i>(See return value)</i>. If errors occured during
	 * assembly they can be fetched via: {@link #getExceptions()}.
	 *
	 * @param lines
	 * 		Java bytecode text to assemble, split by newline.
	 *
	 * @return {@code true} if assembly was validated. {@code false} if exceptions were thrown, or
	 * a method declaration wasn't defined.
	 */
	public boolean parseInstructions(String[] lines) {
		// Ensure method was defined
		if (method == null)
			return false;
		// Reset
		exceptionWrappers.clear();
		method.instructions = null;
		// Parse opcodes of each line
		Map<AbstractInsnNode, Integer> insnToLine = new LinkedHashMap<>();
		InsnList insns = new InsnList();
		for(int i = 0; i < lines.length; i++) {
			try {
				String lineText = lines[i];
				if (lineText.matches("//[^\n]*")) {
					// Skip comment lines
					continue;
				}
				LineData lineData = LineData.from(lineText);
				if(lineData == null) {
					// Skip lines that are empty
					continue;
				}
				String opText = lineData.optext;
				int opcode = lineData.opcode;
				int type = lineData.type;
				// Get assembler for opcode and attempt to assemble the instruction
				Function<Integer, AbstractAssembler> func = assemblers.get(type);
				if(func != null) {
					AbstractAssembler assembler = func.apply(opcode);
					if(assembler == null)
						throw new UnsupportedOperationException("Missing assembler for: " + opText);
					String args = lineText.substring(opText.length()).trim();
					AbstractInsnNode insn = assembler.parse(args);
					if(insn == null)
						throw new UnsupportedOperationException("Unfinished ssembler for: " + opText);
					insnToLine.put(insn, i + 1);
					insns.add(insn);
				} else {
					throw new UnsupportedOperationException("Unknown opcode type: " + type);
				}
			} catch(Exception e) {
				addTrackedError(i + 1, e);
			}
		}
		try {
			// Create map of named labels and populate the instruction with label instances
			Map<String, LabelNode> labels = NamedLabelRefInsn.getLabels(insns.toArray());
			NamedLabelRefInsn.setupLabels(labels, insns.toArray());
			// Replace serialization-intended named instructions with standard instances
			Map<LabelNode, LabelNode> replace = new HashMap<>();
			for (LabelNode value : labels.values())
				replace.put(value, new LabelNode());
			insns = NamedLabelRefInsn.clean(replace, insns);
			// Replace named variables with proper indices
			insns = NamedVarRefInsn.clean(method, insns, locals);
			method.instructions = insns;
			//
			//System.out.println(FormatFactory.opcodeCollectionString(Arrays.asList(insns.toArray()), method));
		} catch(LabelLinkageException e) {
			// insnToLine isn't updated with the replaced insns, but since this exception would
			// be caused by the original insn and not the replaced one, this is correct.
			int line = insnToLine.getOrDefault(e.getInsn(), -1);
			addTrackedError(line, e);
		} catch(AssemblyResolveError e) {
			// insnToLine isn't updated with the replaced insns, but since this exception would
			// be caused by the original insn and not the replaced one, this is correct.
			int line = insnToLine.getOrDefault(e.getInsn(), -1);
			addTrackedError(line, e);
		}
		return exceptionWrappers.isEmpty();
	}

	/**
	 * Requires calling the following:
	 * <ol>
	 * <li>{@link #setMethodDeclaration(int, String, String, String[])} or
	 * {@link #setMethodDeclaration(int, String, String)}</li>
	 * <li>{@link #parseInstructions(String[])}</li>
	 * </ol>
	 *
	 * @return Generted method.
	 */
	public MethodNode getMethod() {
		return method;
	}

	/**
	 * @return List of exceptions raised when parsing the assembly.
	 */
	public List<ExceptionWrapper> getExceptions() {
		return exceptionWrappers;
	}

	/**
	 * @return {@code true} if local-variables are added to the
	 * {@link #getMethod() generated method}. {@code false} otherwise.
	 */
	public boolean doGenerateLocals() {
		return locals;
	}

	/**
	 * Update status of emitting local variables.
	 *
	 * @param locals
	 * 		Value for determining if locals be emitted.
	 */
	public void setDoGenerateLocals(boolean locals) {
		this.locals = locals;
	}

	/**
	 * @param line
	 * 		Line that the error occured on.
	 * @param e
	 * 		Exception that occured.
	 */
	private void addTrackedError(int line, Exception e) {
		exceptionWrappers.add(new ExceptionWrapper(line, e));
	}

	/**
	 * @param type
	 * 		Instruction type <i>(ASM grouping)</i>
	 * @param opcode
	 * 		Instruction opcode.
	 *
	 * @return Assembler for the given opcode.
	 */
	public static AbstractAssembler getAssembler(int type, int opcode) {
		return assemblers.get(type).apply(opcode);
	}

	static {
		assemblers.put(INSN, Insn::new);
		assemblers.put(JUMP_INSN, Jump::new);
		assemblers.put(VAR_INSN, Var::new);
		assemblers.put(FIELD_INSN, Field::new);
		assemblers.put(METHOD_INSN, Method::new);
		assemblers.put(INVOKE_DYNAMIC_INSN, InvokeDynamic::new);
		assemblers.put(LABEL, Label::new);
		assemblers.put(LINE, Line::new);
		assemblers.put(TYPE_INSN, Type::new);
		assemblers.put(MULTIANEWARRAY_INSN, MultiANewArray::new);
		assemblers.put(IINC_INSN, Iinc::new);
		assemblers.put(LDC_INSN, Ldc::new);
		assemblers.put(INT_INSN, Int::new);
		assemblers.put(TABLESWITCH_INSN, TableSwitch::new);
		assemblers.put(LOOKUPSWITCH_INSN, LookupSwitch::new);
	}
}
