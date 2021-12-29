package me.coley.recaf.assemble.ast.meta;

import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.InstructionType;

/**
 * An in-line expression to compile down to bytecode.
 *
 * @author Matt Coley
 */
public class Expression extends AbstractInstruction {
	private final String code;

	/**
	 * @param code
	 * 		Expression code.
	 */
	public Expression(String code) {
		super("EXPR", -1);
		this.code = code;
	}

	/**
	 * @return Expression code.
	 */
	public String getCode() {
		return code;
	}

	@Override
	public void insertInto(Code code) {
		code.addExpression(this);
	}

	@Override
	public InstructionType getInsnType() {
		return InstructionType.EXPRESSION;
	}

	@Override
	public String print() {
		if (code.contains("\n")) {
			return String.format("EXPR {\n%s\n}", code);
		} else {
			return "EXPR " + code;
		}
	}
}
