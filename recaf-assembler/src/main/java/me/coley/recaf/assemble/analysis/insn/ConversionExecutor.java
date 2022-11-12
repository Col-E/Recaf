package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

import static me.coley.recaf.assemble.analysis.CodeExecutionUtils.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * Executor for primitive conversions.
 *
 * @author Matt Coley
 */
public class ConversionExecutor implements InstructionExecutor {
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		int op = instruction.getOpcodeVal();
		switch (op) {
			case D2L:
				validateStackType(frame, DOUBLE_TYPE, 1);
				unaryOpWide(frame, LONG_TYPE, Number::longValue);
				break;
			case F2L:
				validateStackType(frame, FLOAT_TYPE, 0);
				unaryOp(frame, LONG_TYPE, Number::longValue);
				break;
			case I2L:
				validateStackType(frame, INT_TYPE, 0);
				unaryOp(frame, LONG_TYPE, Number::longValue);
				break;
			case D2F:
				validateStackType(frame, DOUBLE_TYPE, 1);
				unaryOpWide(frame, FLOAT_TYPE, Number::floatValue);
				break;
			case L2F:
				validateStackType(frame, LONG_TYPE, 1);
				unaryOpWide(frame, FLOAT_TYPE, Number::floatValue);
				break;
			case I2F:
				validateStackType(frame, INT_TYPE, 0);
				unaryOp(frame, FLOAT_TYPE, Number::floatValue);
				break;
			case L2D:
				validateStackType(frame, LONG_TYPE, 1);
				unaryOpWide(frame, DOUBLE_TYPE, Number::doubleValue);
				break;
			case F2D:
				validateStackType(frame, FLOAT_TYPE, 0);
				unaryOp(frame, DOUBLE_TYPE, Number::doubleValue);
				break;
			case I2D:
				validateStackType(frame, INT_TYPE, 0);
				unaryOp(frame, DOUBLE_TYPE, Number::doubleValue);
				break;
			case L2I:
				validateStackType(frame, LONG_TYPE, 1);
				unaryOpWide(frame, INT_TYPE, Number::intValue);
				break;
			case D2I:
				validateStackType(frame, DOUBLE_TYPE, 1);
				unaryOpWide(frame, INT_TYPE, Number::intValue);
				break;
			case F2I:
				validateStackType(frame, FLOAT_TYPE, 0);
				unaryOp(frame, INT_TYPE, Number::intValue);
				break;
			case I2B:
				validateStackType(frame, INT_TYPE, 0);
				unaryOp(frame, INT_TYPE, n -> (byte) n.intValue());
				break;
			case I2C:
				validateStackType(frame, INT_TYPE, 0);
				unaryOp(frame, INT_TYPE, n -> (int) (char) n.intValue());
				break;
			case I2S:
				validateStackType(frame, INT_TYPE, 0);
				unaryOp(frame, INT_TYPE, n -> (short) n.intValue());
				break;
		}
	}
}
