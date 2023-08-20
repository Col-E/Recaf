package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.ArgType;
import me.coley.recaf.assemble.ast.BaseArg;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.util.EscapeUtil;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * LDC instruction.
 *
 * @author Matt Coley
 */
public class LdcInstruction extends AbstractInstruction {
	private final BaseArg arg;
	private final Object value;

	/**
	 * @param opcode
	 * 		LDC instruction opcode.
	 * @param value
	 * 		String value.
	 */
	public LdcInstruction(int opcode, Object value) {
		super(opcode);
		this.arg = BaseArg.of(LdcArg::new, value);
		this.value = value;
	}

	/**
	 * @param value
	 * 		Value of some unknown type.
	 *
	 * @return Ldc AST instance based on value.
	 */
	public static LdcInstruction of(Object value) {
		return new LdcInstruction(Opcodes.LDC, value);
	}

	/**
	 * <b>Note</b>: Strings are not unescaped. They appear as the literal text that was present at parse-time.
	 *
	 * @return Constant value.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @return Type of content of the {@link #getValue() constant value}.
	 */
	public ArgType getValueType() {
		return arg.getType();
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.LDC;
	}

	@Override
	public String print(PrintContext context) {
		return getOpcode() + " " + arg.print(context);
	}

	private static class LdcArg extends BaseArg {

		/**
		 * @param type  Type of value.
		 * @param value Value instance.
		 */
		public LdcArg(ArgType type, Object value) {
			super(type, value);
		}
	}
}
