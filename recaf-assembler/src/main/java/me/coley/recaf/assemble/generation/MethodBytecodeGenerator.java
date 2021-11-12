package me.coley.recaf.assemble.generation;

import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.CodeEntry;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.Instruction;
import me.coley.recaf.assemble.ast.meta.Label;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Visits our bytecode AST {@link Unit} and transforms it into a normal method.
 *
 * @author Matt Coley
 */
public class MethodBytecodeGenerator {
	private final Map<String, LabelNode> labelMap = new HashMap<>();
	private final Variables variables = new Variables();
	private final Unit unit;
	private final String selfType;
	// For quick reference
	private final MethodDefinition definition;
	private final Code code;

	/**
	 * @param selfType
	 * 		The internal type of the class defining the method.
	 * @param unit
	 * 		The unit to pull data from.
	 */
	public MethodBytecodeGenerator(String selfType, Unit unit) {
		this.unit = Objects.requireNonNull(unit);
		this.selfType = selfType;
		this.definition = (MethodDefinition) unit.getDefinition();
		this.code = unit.getCode();
	}

	/**
	 * @throws MethodCompileException
	 * 		When the variable type usage is inconsistent/illegal,
	 * 		or when a variable index is already reserved by a wide variable of the prior slot.
	 */
	public void visit() throws MethodCompileException {
		createLabels();
		createVariables();
		createInstructions();
	}

	/**
	 * Populate the label name to instance map.
	 */
	private void createLabels() {
		for (String labelName : unit.getCode().getLabels().keySet()) {
			labelMap.put(labelName, new LabelNode());
		}
	}

	/**
	 * Populate the variables and lookup information.
	 *
	 * @throws MethodCompileException
	 * 		When the variable type usage is inconsistent/illegal,
	 * 		or when a variable index is already reserved by a wide variable of the prior slot.
	 */
	private void createVariables() throws MethodCompileException {
		variables.visitDefinition(selfType, definition);
		variables.visitParams(definition);
		variables.visitCode(code);
	}

	/**
	 * Generate actual instructions.
	 *
	 * @return ASM instruction list.
	 */
	private InsnList createInstructions() {
		InsnList list = new InsnList();
		for (CodeEntry entry : code.getEntries()) {
			if (entry instanceof Label) {
				String labelName = ((Label) entry).getName();
				LabelNode labelInstance = labelMap.get(labelName);
				list.add(labelInstance);
			} else if (entry instanceof AbstractInstruction) {
				// TODO: Extract ast info into ASM instructions now that we know the data is valid
				AbstractInstruction instruction = (AbstractInstruction) entry;
				switch (instruction.getInsnType()) {
					case FIELD:
						break;
					case METHOD:
						break;
					case INDY:
						break;
					case VAR:
						break;
					case IINC:
						break;
					case INT:
						break;
					case LDC:
						break;
					case TYPE:
						break;
					case MULTIARRAY:
						break;
					case NEWARRAY:
						break;
					case INSN:
						break;
					case JUMP:
						break;
					case LOOKUP:
						break;
					case TABLE:
						break;
					case LABEL:
						break;
					case LINE:
						break;
				}
			}
		}
		return list;
	}
}
