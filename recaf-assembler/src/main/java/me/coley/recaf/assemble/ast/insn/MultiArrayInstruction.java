package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.util.EscapeUtil;

/**
 * Multi array instruction.
 *
 * @author Matt Coley
 */
public class MultiArrayInstruction extends AbstractInstruction {
	private final String desc;
	private final int dimensions;

	/**
	 * @param opcode
	 * 		Multi array instruction opcode.
	 * @param desc
	 * 		Array element type.
	 * @param dimensions
	 * 		Dimension count.
	 */
	public MultiArrayInstruction(int opcode, String desc, int dimensions) {
		super(opcode);
		this.desc = desc;
		this.dimensions = dimensions;
	}

	/**
	 * @return Array element type.
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * @return Dimension count.
	 */
	public int getDimensions() {
		return dimensions;
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.MULTIARRAY;
	}

	@Override
	public String print(PrintContext context) {
		return getOpcode() + " " + context.fmtIdentifier(getDesc()) + " " + getDimensions();
	}
}
