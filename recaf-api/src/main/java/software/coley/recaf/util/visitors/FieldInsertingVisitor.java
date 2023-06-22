package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.FieldNode;
import software.coley.recaf.RecafConstants;

/**
 * Simple visitor for inserting a field.
 *
 * @author Matt Coley
 */
public class FieldInsertingVisitor extends ClassVisitor {
	private final FieldNode inserted;

	/**
	 * @param cv
	 * 		Parent visitor.
	 * @param inserted
	 * 		Field to insert.
	 */
	public FieldInsertingVisitor(@Nullable ClassVisitor cv,
								 @Nonnull FieldNode inserted) {
		super(RecafConstants.getAsmVersion(), cv);
		this.inserted = inserted;
	}

	@Override
	public void visitEnd() {
		visitField(inserted.access, inserted.name, inserted.desc, inserted.signature, inserted.value);
		super.visitEnd();
	}
}