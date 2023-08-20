package me.coley.recaf.assemble.ast.arch;

import java.util.List;

public interface Annotatable extends Attributable {
	List<Annotation> getAnnotations();

	void setAnnotations(List<Annotation> annotations);

	void addAnnotation(Annotation annotation);
}
