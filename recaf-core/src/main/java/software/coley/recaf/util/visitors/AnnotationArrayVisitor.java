package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import software.coley.recaf.RecafConstants;

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
	 * @param onComplete
	 * 		Action to run on completed array contents.
	 */
	public AnnotationArrayVisitor(@Nullable AnnotationVisitor visitor, @Nullable Consumer<List<T>> onComplete) {
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
