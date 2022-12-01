package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.ArgType;
import me.coley.recaf.assemble.ast.BaseArg;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.util.EscapeUtil;

import java.util.List;

/**
 * Invoke dynamic instruction.
 *
 * @author Matt Coley
 */
public class IndyInstruction extends AbstractInstruction {
	private final String name;
	private final String desc;
	private final HandleInfo bsmHandle;
	private final List<BsmArg> bsmArguments;

	/**
	 * @param opcode
	 * 		Invoke dynamic opcode.
	 * @param name
	 * 		Name of target method.
	 * @param desc
	 * 		Descriptor of target method.
	 * @param bsmHandle
	 * 		Bootstrap method handle.
	 * @param bsmArguments
	 * 		Bootstrap method arguments.
	 */
	public IndyInstruction(int opcode, String name, String desc, HandleInfo bsmHandle, List<BsmArg> bsmArguments) {
		super(opcode);
		this.name = name;
		this.desc = desc;
		this.bsmHandle = child(bsmHandle);
		this.bsmArguments = bsmArguments;
	}

	/**
	 * @return Name of target method.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Descriptor of target method.
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * @return Bootstrap method handle.
	 */
	public HandleInfo getBsmHandle() {
		return bsmHandle;
	}

	/**
	 * @return Bootstrap method arguments.
	 */
	public List<BsmArg> getBsmArguments() {
		return bsmArguments;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.INDY;
	}

	@Override
	public String print(PrintContext context) {
		StringBuilder sb = new StringBuilder();
		String handle = bsmHandle.print(context);
		StringBuilder args = new StringBuilder();
		for (BsmArg bsmArgument : bsmArguments) {
			args.append(bsmArgument.print(context));
			args.append(" ");
		}
		sb.append(getOpcode()).append(' ');
		sb.append(context.fmtIdentifier(name)).append(' ');
		sb.append(context.fmtIdentifier(desc)).append(' ');
		sb.append(context.fmtKeyword("handle ")).append(handle).append(' ');
		sb.append(context.fmtKeyword("args ")).append(args).append(context.fmtKeyword("end"));
		return sb.toString();
	}

	/**
	 * Helper for determining arg value types.
	 */
	public static class BsmArg extends BaseArg {
		/**
		 * @param type
		 * 		Type of value.
		 * @param value
		 * 		Value instance.
		 */
		public BsmArg(ArgType type, Object value) {
			super(type, value);
		}

		@Override
		public String toString() {
			return "ARG[" + getType() + ":" + getValue() + ']';
		}
	}
}
