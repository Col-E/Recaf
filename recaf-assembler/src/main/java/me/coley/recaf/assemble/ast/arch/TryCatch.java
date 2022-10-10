package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.CodeEntry;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.util.EscapeUtil;

/**
 * An abstraction of a try-catch range of a given type using named labels.
 *
 * @author Matt Coley
 */
public class TryCatch extends BaseElement implements CodeEntry {
	private static final String ANY_TYPE = "*";
	private final String startLabel;
	private final String endLabel;
	private final String handlerLabel;
	private final String exceptionType;

	/**
	 * @param startLabel
	 * 		Label name denoting the start of the try block.
	 * @param endLabel
	 * 		Label name denoting the end of the try block.
	 * @param handlerLabel
	 * 		Label name of the start of the catch block.
	 * @param exceptionType
	 * 		Handled exception type. {@code null} or {@link #ANY_TYPE} for wildcard <i>(any type)</i>.
	 */
	public TryCatch(String startLabel, String endLabel, String handlerLabel, String exceptionType) {
		if (ANY_TYPE.equals(exceptionType))
			exceptionType = null;
		this.startLabel = startLabel;
		this.endLabel = endLabel;
		this.handlerLabel = handlerLabel;
		this.exceptionType = exceptionType;
	}

	/**
	 * @return Label name denoting the start of the try block.
	 */
	public String getStartLabel() {
		return startLabel;
	}

	/**
	 * @return Label name denoting the end of the try block.
	 */
	public String getEndLabel() {
		return endLabel;
	}

	/**
	 * @return Label name of the start of the catch block.
	 */
	public String getHandlerLabel() {
		return handlerLabel;
	}

	/**
	 * @return Handled exception type. {@code null} for wildcard <i>(any type)</i>.
	 */
	public String getExceptionType() {
		return exceptionType;
	}

	@Override
	public void insertInto(Code code) {
		code.addTryCatch(this);
	}

	@Override
	public String print(PrintContext context) {
		String type = exceptionType;
		if (type == null)
			type = ANY_TYPE;
		return String.format("%s %s %s %s %s", context.fmtKeyword("catch"),
				EscapeUtil.escapeNonValid(type), startLabel, endLabel, handlerLabel);
	}
}
