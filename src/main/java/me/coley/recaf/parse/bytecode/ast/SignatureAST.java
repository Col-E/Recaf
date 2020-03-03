package me.coley.recaf.parse.bytecode.ast;

/**
 * Member generic signature AST.
 *
 * @author Matt
 */
public class SignatureAST extends AST {
	private final String signature;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param signature
	 * 		Member generic signature.
	 */
	public SignatureAST(int line, int start, String signature) {
		super(line, start);
		this.signature = signature;
	}

	/**
	 * @return Member descriptor.
	 */
	public String getSignature() {
		return signature;
	}

	@Override
	public String print() {
		return "SIGNATURE " + signature;
	}
}
