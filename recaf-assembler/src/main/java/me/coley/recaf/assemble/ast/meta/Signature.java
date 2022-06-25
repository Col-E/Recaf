package me.coley.recaf.assemble.ast.meta;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.CodeEntry;
import me.coley.recaf.assemble.ast.PrintContext;

/**
 * Generic signature for members.
 *
 * @author Matt Coley
 */
public class Signature extends BaseElement {
	private final String signature;

	/**
	 * @param signature
	 * 		Generic signature.
	 */
	public Signature(String signature) {
		this.signature = signature;
	}

	@Override
	public String print(PrintContext context) {
		return context.fmtKeyword("signature ") + getSignature() + "\n";
	}

	/**
	 * @return Comment text.
	 */
	public String getSignature() {
		return signature;
	}
}
