package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.meta.Signature;

import java.util.List;

public interface Attributable {

	/**
	 * @return element's annotations.
	 */
	List<Annotation> getAnnotations();

	/**
	 * @return element's signature.
	 */
	Signature getSignature();

}
