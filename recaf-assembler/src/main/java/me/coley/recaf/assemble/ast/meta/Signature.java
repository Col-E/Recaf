package me.coley.recaf.assemble.ast.meta;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.CodeEntry;

/**
 * Generic signature for members.
 *
 * @author Matt Coley
 */
public class Signature extends BaseElement implements CodeEntry {
	private final String signature;

	/**
	 * @param signature
	 * 		Generic signature.
	 */
	public Signature(String signature) {
		this.signature = signature;
	}

	@Override
	public void insertInto(Code code) {
		code.setSignature(this);
	}

	@Override
	public String print() {
		return "SIGNATURE " + getSignature();
	}

	/**
	 * @return Comment text.
	 */
	public String getSignature() {
		return signature;
	}
}
