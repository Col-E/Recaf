package me.coley.recaf.parse.bytecode.ast;

/**
 * Try-catch AST.
 *
 * @author Matt
 */
public class TryCatchAST extends AST {
	private final TypeAST type;
	private final NameAST lblStart;
	private final NameAST lblEnd;
	private final NameAST lblHandler;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param lblStart
	 * 		Try block starting label.
	 * @param lblEnd
	 * 		Try block ending label.
	 * @param type
	 * 		Type of exception caught.
	 * @param lblHandler
	 * 		Catch block starting label.
	 */
	public TryCatchAST(int line, int start, NameAST lblStart, NameAST lblEnd,
					   TypeAST type, NameAST lblHandler) {
		super(line, start);
		this.lblStart = lblStart;
		this.lblEnd = lblEnd;
		this.type = type;
		this.lblHandler = lblHandler;
		addChild(lblStart);
		addChild(lblEnd);
		addChild(type);
		addChild(lblHandler);
	}

	/**
	 * @return Type of exception caught.
	 */
	public TypeAST getType() {
		return type;
	}

	/**
	 * @return Try block starting label name AST.
	 */
	public NameAST getLblStart() {
		return lblStart;
	}

	/**
	 * @return Try block ending label name AST.
	 */
	public NameAST getLblEnd() {
		return lblEnd;
	}

	/**
	 * @return Catch block starting label name AST.
	 */
	public NameAST getLblHandler() {
		return lblHandler;
	}

	@Override
	public String print() {
		return "TRY " + lblStart.getName() + " " + lblEnd.getName() +
				" CATCH(" + type.getType() + ") " + lblHandler.getName();
	}
}
