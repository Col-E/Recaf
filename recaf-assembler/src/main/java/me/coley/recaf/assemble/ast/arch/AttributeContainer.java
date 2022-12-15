package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.meta.Signature;

import java.util.ArrayList;
import java.util.List;

public abstract class AttributeContainer extends BaseElement implements Annotatable, GenericTyped {

	private final List<Annotation> annotations = new ArrayList<>();
	private Signature signature;

	@Override
	public List<Annotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<Annotation> annotations) {
		this.annotations.clear();
		this.annotations.addAll(annotations);
	}

	@Override
	public Signature getSignature() {
		return signature;
	}

	public void setSignature(Signature signature) {
		this.signature = signature;
	}

	public void addAnnotation(Annotation annotation) {
		annotations.add(annotation);
		child(annotation);
	}

	protected String buildDefString(PrintContext context) {
		StringBuilder sb = new StringBuilder();
		if (signature != null)
			sb.append(getSignature().print(context));
		for (Annotation annotation : annotations)
			sb.append(annotation.print(context));
		return sb.toString();
	}

}
