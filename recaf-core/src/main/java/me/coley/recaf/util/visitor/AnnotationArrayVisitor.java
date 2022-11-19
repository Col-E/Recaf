package me.coley.recaf.util.visitor;

import me.coley.recaf.RecafConstants;
import org.objectweb.asm.AnnotationVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Visitor to extract values from annotation fields of array types.
 *
 * @author Matt Coley
 */
public class AnnotationArrayVisitor<T> extends AnnotationVisitor {
	private final List<T> values = new ArrayList<>();
	private final Consumer<List<T>> onComplete;

	/**
	 * @param visitor
	 * 		Parent visitor.
	 */
	public AnnotationArrayVisitor(AnnotationVisitor visitor) {
		this(visitor, null);
	}

	/**
	 * @param visitor
	 * 		Parent visitor.
	 * @param onComplete
	 * 		Action to run on completed array contents.
	 */
	public AnnotationArrayVisitor(AnnotationVisitor visitor, Consumer<List<T>> onComplete) {
		super(RecafConstants.getAsmVersion(), visitor);
		this.onComplete = onComplete;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visit(String name, Object value) {
		values.add((T) value);
		super.visit(name, value);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		if (onComplete != null)
			onComplete.accept(values);
	}

	/**
	 * @return Collected values.
	 */
	public List<T> getValues() {
		return values;
	}
}
