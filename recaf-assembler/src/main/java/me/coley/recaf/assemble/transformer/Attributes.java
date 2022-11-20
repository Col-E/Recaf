package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.arch.Annotation;
import me.coley.recaf.assemble.ast.arch.Definition;
import me.coley.recaf.assemble.ast.arch.ThrownException;
import me.coley.recaf.assemble.ast.meta.Signature;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold attributes about {@link Definition}s that appear before the actual definition.
 *
 * @author Justus Garbe
 */
public class Attributes {
	private final List<ThrownException> thrownExceptions = new ArrayList<>();
	private final List<Annotation> annotations = new ArrayList<>();
	private Signature signature;

	/**
	 * @return Generic signature.
	 */
	public Signature getSignature() {
		return signature;
	}

	/**
	 * @param signature
	 * 		Generic signature.
	 */
	public void setSignature(Signature signature) {
		this.signature = signature;
	}

	/**
	 * @return Current thrown exceptions.
	 */
	public List<ThrownException> getThrownExceptions() {
		return thrownExceptions;
	}

	/**
	 * @param exception
	 * 		Thrown exception to add.
	 */
	public void addThrownException(ThrownException exception) {
		thrownExceptions.add(exception);
	}

	/**
	 * @return Current annotations.
	 */
	public List<Annotation> getAnnotations() {
		return annotations;
	}

	/**
	 * @param annotation
	 * 		Annotation to add.
	 */
	public void addAnnotation(Annotation annotation) {
		annotations.add(annotation);
	}

	/**
	 * Reset all attributes.
	 */
	public void clear() {
		signature = null;
		thrownExceptions.clear();
		annotations.clear();
	}
}
