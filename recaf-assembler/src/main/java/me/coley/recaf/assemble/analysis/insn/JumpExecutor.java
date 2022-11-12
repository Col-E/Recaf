package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.CodeExecutor;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;

/**
 * Executor for validating jump instruction stack operands.
 * The actual branching logic is handled internally by {@link CodeExecutor#execute(int, int)}
 *
 * @author Matt Coley
 */
public class JumpExecutor implements InstructionExecutor{
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		int op = instruction.getOpcodeVal();
		switch (op) {
			case GOTO:
				// no-op
				break;
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE: {
				Value value = frame.pop();
				if (!value.isNumeric()) {
					frame.markWonky("unary conditional jump has non-numeric value on the stack");
				}
				break;
			}
			case IFNULL:
			case IFNONNULL: {
				// TODO: Mark jump as always (not)-taken if top value meets expectation
				Value value = frame.pop();
				if (!(value.isObject() || value.isArray() || value.isNull())) {
					frame.markWonky("null-check conditional jump has non-object value on the stack");
				}
				break;
			}
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE: {
				Value value1 = frame.pop();
				if (!value1.isNumeric()) {
					frame.markWonky("binary conditional jump has non-numeric value on the stack");
				}
				Value value2 = frame.pop();
				if (!value2.isNumeric()) {
					frame.markWonky("binary conditional jump has non-numeric value on the stack");
				}
				break;
			}
			case IF_ACMPEQ:
			case IF_ACMPNE: {
				// TODO: Mark jump as always (not)-taken if top value meets expectation
				Value value1 = frame.pop();
				if (!(value1.isObject() || value1.isArray() || value1.isNull())) {
					frame.markWonky("null-check conditional jump has non-object value on the stack");
				}
				Value value2 = frame.pop();
				if (!(value2.isObject() || value2.isArray() || value2.isNull())) {
					frame.markWonky("null-check conditional jump has non-object value on the stack");
				}
				break;
			}
		}
	}
}
