package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.CodeEntry;

/**
 * Details a single type thrown by a {@link MethodDefinition}.
 *
 * @author Matt Coley
 */
public class ThrownException extends BaseElement implements CodeEntry {
	private final String exceptionType;

	/**
	 * @param exceptionType
	 * 		Thrown exception type.
	 */
	public ThrownException(String exceptionType) {
		this.exceptionType = exceptionType;
	}

	/**
	 * @return Thrown exception type.
	 */
	public String getExceptionType() {
		return exceptionType;
	}

	@Override
	public void insertInto(Code code) {
		code.addThrownException(this);
	}

	@Override
	public String print() {
		return String.format("THROWS %s", exceptionType);
	}
}
