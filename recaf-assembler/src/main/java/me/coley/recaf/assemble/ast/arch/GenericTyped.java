package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.meta.Signature;

public interface GenericTyped extends Attributable {

	Signature getSignature();

	void setSignature(Signature signature);

}
