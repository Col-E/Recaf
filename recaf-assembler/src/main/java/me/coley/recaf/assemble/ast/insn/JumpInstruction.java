package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.IllegalAstException;
import me.coley.recaf.assemble.ast.FlowControl;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.meta.Label;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Jump instruction.
 *
 * @author Matt Coley
 */
public class JumpInstruction extends AbstractInstruction implements FlowControl {
	private final String label;

	/**
	 * @param opcode
	 * 		Jump instruction opcode.
	 * @param label
	 * 		Jump target label name.
	 */
	public JumpInstruction(int opcode, String label) {
		super(opcode);
		this.label = label;
	}

	/**
	 * @return Jump target label name.
	 */
	public String getLabel() {
		return label;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.JUMP;
	}

	@Override
	public String print(PrintContext context) {
		return getOpcode() + ' ' + getLabel();
	}

	@Override
	public List<Label> getTargets(Map<String, Label> labelMap) throws IllegalAstException {
		Label label = labelMap.get(getLabel());
		if (label == null)
			throw new IllegalAstException(this, "Could not find instance for label: " + getLabel());
		return Collections.singletonList(label);
	}

	@Override
	public boolean isForced() {
		// Only forced on GOTO
		return getOpcodeVal() == Opcodes.GOTO;
	}
}
