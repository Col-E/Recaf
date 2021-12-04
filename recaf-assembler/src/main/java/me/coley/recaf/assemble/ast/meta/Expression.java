package me.coley.recaf.assemble.ast.meta;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.CodeEntry;

/**
 * An in-line expression to compile down to bytecode.
 *
 * @author Matt Coley
 */
public class Expression extends BaseElement implements CodeEntry {
	private final String code;

	/**
	 * @param code
	 * 		Expression code.
	 */
	public Expression(String code) {
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
	public String print() {
		if (code.contains("\n")) {
			return String.format("EXPR {\n%s\n}", code);
		} else {
			return "EXPR " + code;
		}
	}
}
