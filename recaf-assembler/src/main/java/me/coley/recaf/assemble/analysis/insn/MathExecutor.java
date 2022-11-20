package me.coley.recaf.assemble.analysis.insn;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.util.NumberUtil;

import static me.coley.recaf.assemble.analysis.CodeExecutionUtils.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * Executor for primitive math operations.
 *
 * @author Matt Coley
 */
public class MathExecutor implements InstructionExecutor{
	@Override
	public void handle(Frame frame, AbstractInstruction instruction) throws AnalysisException {
		int op = instruction.getOpcodeVal();
		switch (op) {
			case IADD:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, INT_TYPE, 1);
				binaryOp(frame, INT_TYPE, NumberUtil::add);
				break;
			case LADD:
				validateStackType(frame, LONG_TYPE, 1);
				validateStackType(frame, LONG_TYPE, 3);
				binaryOpWide(frame, LONG_TYPE, NumberUtil::add);
				break;
			case FADD:
				validateStackType(frame, FLOAT_TYPE, 0);
				validateStackType(frame, FLOAT_TYPE, 1);
				binaryOp(frame, FLOAT_TYPE, NumberUtil::add);
				break;
			case DADD:
				validateStackType(frame, DOUBLE_TYPE, 1);
				validateStackType(frame, DOUBLE_TYPE, 3);
				binaryOpWide(frame, DOUBLE_TYPE, NumberUtil::add);
				break;
			case ISUB:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, INT_TYPE, 1);
				binaryOp(frame, INT_TYPE, NumberUtil::sub);
				break;
			case LSUB:
				validateStackType(frame, LONG_TYPE, 1);
				validateStackType(frame, LONG_TYPE, 3);
				binaryOpWide(frame, LONG_TYPE, NumberUtil::sub);
				break;
			case FSUB:
				validateStackType(frame, FLOAT_TYPE, 0);
				validateStackType(frame, FLOAT_TYPE, 1);
				binaryOp(frame, FLOAT_TYPE, NumberUtil::sub);
				break;
			case DSUB:
				validateStackType(frame, DOUBLE_TYPE, 1);
				validateStackType(frame, DOUBLE_TYPE, 3);
				binaryOpWide(frame, DOUBLE_TYPE, NumberUtil::sub);
				break;
			case IMUL:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, INT_TYPE, 1);
				binaryOp(frame, INT_TYPE, NumberUtil::mul);
				break;
			case LMUL:
				validateStackType(frame, LONG_TYPE, 1);
				validateStackType(frame, LONG_TYPE, 3);
				binaryOpWide(frame, LONG_TYPE, NumberUtil::mul);
				break;
			case FMUL:
				validateStackType(frame, FLOAT_TYPE, 0);
				validateStackType(frame, FLOAT_TYPE, 1);
				binaryOp(frame, FLOAT_TYPE, NumberUtil::mul);
				break;
			case DMUL:
				validateStackType(frame, DOUBLE_TYPE, 1);
				validateStackType(frame, DOUBLE_TYPE, 3);
				binaryOpWide(frame, DOUBLE_TYPE, NumberUtil::mul);
				break;
			case IDIV:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, INT_TYPE, 1);
				binaryOp(frame, INT_TYPE, NumberUtil::div);
				break;
			case LDIV:
				validateStackType(frame, LONG_TYPE, 1);
				validateStackType(frame, LONG_TYPE, 3);
				binaryOpWide(frame, LONG_TYPE, NumberUtil::div);
				break;
			case FDIV:
				validateStackType(frame, FLOAT_TYPE, 0);
				validateStackType(frame, FLOAT_TYPE, 1);
				binaryOp(frame, FLOAT_TYPE, NumberUtil::div);
				break;
			case DDIV:
				validateStackType(frame, DOUBLE_TYPE, 1);
				validateStackType(frame, DOUBLE_TYPE, 3);
				binaryOpWide(frame, DOUBLE_TYPE, NumberUtil::div);
				break;
			case IREM:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, INT_TYPE, 1);
				binaryOp(frame, INT_TYPE, NumberUtil::rem);
				break;
			case LREM:
				validateStackType(frame, LONG_TYPE, 1);
				validateStackType(frame, LONG_TYPE, 3);
				binaryOpWide(frame, LONG_TYPE, NumberUtil::rem);
				break;
			case FREM:
				validateStackType(frame, FLOAT_TYPE, 0);
				validateStackType(frame, FLOAT_TYPE, 1);
				binaryOp(frame, FLOAT_TYPE, NumberUtil::rem);
				break;
			case DREM:
				validateStackType(frame, DOUBLE_TYPE, 1);
				validateStackType(frame, DOUBLE_TYPE, 3);
				binaryOpWide(frame, DOUBLE_TYPE, NumberUtil::rem);
				break;
			case IAND:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, INT_TYPE, 1);
				binaryOp(frame, INT_TYPE, NumberUtil::and);
				break;
			case LAND:
				validateStackType(frame, LONG_TYPE, 1);
				validateStackType(frame, LONG_TYPE, 3);
				binaryOpWide(frame, LONG_TYPE, NumberUtil::and);
				break;
			case IOR:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, INT_TYPE, 1);
				binaryOp(frame, INT_TYPE, NumberUtil::or);
				break;
			case LOR:
				validateStackType(frame, LONG_TYPE, 1);
				validateStackType(frame, LONG_TYPE, 3);
				binaryOpWide(frame, LONG_TYPE, NumberUtil::or);
				break;
			case IXOR:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, INT_TYPE, 1);
				binaryOp(frame, INT_TYPE, NumberUtil::xor);
				break;
			case LXOR:
				validateStackType(frame, LONG_TYPE, 1);
				validateStackType(frame, LONG_TYPE, 3);
				binaryOpWide(frame, LONG_TYPE, NumberUtil::xor);
				break;
			case ISHL:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, INT_TYPE, 1);
				binaryOp(frame, INT_TYPE, NumberUtil::shiftLeft);
				break;
			case LSHL:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, LONG_TYPE, 2);
				binaryOpWide(false, frame, LONG_TYPE, NumberUtil::shiftLeft);
				break;
			case ISHR:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, INT_TYPE, 1);
				binaryOp(frame, INT_TYPE, NumberUtil::shiftRight);
				break;
			case LSHR:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, LONG_TYPE, 2);
				binaryOpWide(false, frame, LONG_TYPE, NumberUtil::shiftRight);
				break;
			case IUSHR:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, INT_TYPE, 1);
				binaryOp(frame, INT_TYPE, NumberUtil::shiftRightU);
				break;
			case LUSHR:
				validateStackType(frame, INT_TYPE, 0);
				validateStackType(frame, LONG_TYPE, 2);
				binaryOpWide(false, frame, LONG_TYPE, NumberUtil::shiftRightU);
				break;
			case LCMP:
				validateStackType(frame, LONG_TYPE, 1);
				validateStackType(frame, LONG_TYPE, 3);
				binaryOpWide(frame, INT_TYPE, NumberUtil::cmp);
				break;
			case FCMPL:
				validateStackType(frame, FLOAT_TYPE, 0);
				validateStackType(frame, FLOAT_TYPE, 1);
				binaryOp(frame, INT_TYPE, (n1, n2) -> {
					if (Float.isNaN(n1.floatValue()) || Float.isNaN(n2.floatValue())) return -1;
					return NumberUtil.cmp(n1, n2);
				});
				break;
			case FCMPG:
				validateStackType(frame, FLOAT_TYPE, 0);
				validateStackType(frame, FLOAT_TYPE, 1);
				binaryOp(frame, INT_TYPE, (n1, n2) -> {
					if (Float.isNaN(n1.floatValue()) || Float.isNaN(n2.floatValue())) return 1;
					return NumberUtil.cmp(n1, n2);
				});
				break;
			case DCMPL:
				validateStackType(frame, DOUBLE_TYPE, 1);
				validateStackType(frame, DOUBLE_TYPE, 3);
				binaryOpWide(frame, INT_TYPE, (n1, n2) -> {
					if (Double.isNaN(n1.doubleValue()) || Double.isNaN(n2.doubleValue())) return -1;
					return NumberUtil.cmp(n1, n2);
				});
				break;
			case DCMPG:
				validateStackType(frame, DOUBLE_TYPE, 1);
				validateStackType(frame, DOUBLE_TYPE, 3);
				binaryOpWide(frame, INT_TYPE, (n1, n2) -> {
					if (Double.isNaN(n1.doubleValue()) || Double.isNaN(n2.doubleValue())) return 1;
					return NumberUtil.cmp(n1, n2);
				});
				break;
			case INEG:
				validateStackType(frame, INT_TYPE, 0);
				unaryOp(frame, INT_TYPE, NumberUtil::neg);
				break;
			case LNEG:
				validateStackType(frame, LONG_TYPE, 1);
				unaryOpWide(frame, LONG_TYPE, NumberUtil::neg);
				break;
			case FNEG:
				validateStackType(frame, FLOAT_TYPE, 0);
				unaryOp(frame, FLOAT_TYPE, NumberUtil::neg);
				break;
			case DNEG:
				validateStackType(frame, DOUBLE_TYPE, 1);
				unaryOpWide(frame, DOUBLE_TYPE, NumberUtil::neg);
				break;
			default:
				throw new AnalysisException(instruction, "Unsupported math operation: " + instruction.getOpcode());
		}
	}
}
