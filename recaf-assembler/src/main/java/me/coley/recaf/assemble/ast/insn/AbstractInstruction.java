package me.coley.recaf.assemble.ast.insn;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.CodeEntry;
import me.coley.recaf.util.OpcodeUtil;

/**
 * Base instruction outline.
 *
 * @author Matt Coley
 */
public abstract class AbstractInstruction extends BaseElement implements CodeEntry {
	private final String opcode;
	private final int opcodeVal;

	/**
	 * @param opcode
	 * 		Opcode name.
	 */
	public AbstractInstruction(String opcode) {
		this(opcode, OpcodeUtil.nameToOpcode(opcode));
	}

	/**
	 * @param opcode
	 * 		Opcode name.
	 * @param opcodeVal
	 * 		Opcode value.
	 */
	protected AbstractInstruction(String opcode, int opcodeVal) {
		this.opcode = opcode;
		this.opcodeVal = opcodeVal;
	}

	@Override
	public void insertInto(Code code) {
		code.addInstruction(this);
	}

	/**
	 * @return Opcode name.
	 */
	public String getOpcode() {
		return opcode;
	}

	/**
	 * @return Opcode value.
	 */
	public int getOpcodeVal() {
		return opcodeVal;
	}
}
