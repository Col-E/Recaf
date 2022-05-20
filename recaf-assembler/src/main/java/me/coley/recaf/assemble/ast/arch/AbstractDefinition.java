package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.meta.Signature;

import java.util.ArrayList;
import java.util.List;

/**
 * Common base implementation for definition types.
 *
 * @author Matt Coley
 * @author Nowilltolife
 */
public abstract class AbstractDefinition extends BaseElement implements Definition {
	private final List<Annotation> annotations = new ArrayList<>();
	private Modifiers modifiers;
	private Signature signature;

	@Override
	public Modifiers getModifiers() {
		return modifiers;
	}

	@Override
	public List<Annotation> getAnnotations() {
		return annotations;
	}

	@Override
	public Signature getSignature() {
		return signature;
	}

	public void addAnnotation(Annotation annotation) {
		annotations.add(annotation);
	}

	public void setModifiers(Modifiers modifiers) {
		this.modifiers = modifiers;
	}

	public void setAnnotations(List<Annotation> annotations) {
		this.annotations.clear();
		this.annotations.addAll(annotations);
	}

	public void setSignature(Signature signature) {
		this.signature = signature;
	}
}
