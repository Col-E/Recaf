package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.TypeInstruction;
import org.objectweb.asm.Type;

/**
 * Executor for {@code NEW} instruction.
 *
 * @author Matt Coley
 */
public class NewExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		if (!(instruction instanceof TypeInstruction))
			throw new AnalysisException(instruction, "Expected new insn");
		TypeInstruction typeInstruction = (TypeInstruction) instruction;
		Type newType = Type.getObjectType(typeInstruction.getType());
		frame.push(new Value.ObjectValue(newType));
	}
}
