package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.RecafConstants;

/**
 * Simple visitor for inserting a method.
 *
 * @author Matt Coley
 */
public class MethodInsertingVisitor extends ClassVisitor {
	private final MethodNode inserted;

	/**
	 * @param cv
	 * 		Parent visitor.
	 * @param inserted
	 * 		Method to insert.
	 */
	public MethodInsertingVisitor(@Nullable ClassVisitor cv,
								  @Nonnull MethodNode inserted) {
		super(RecafConstants.getAsmVersion(), cv);
		this.inserted = inserted;
	}

	@Override
	public void visitEnd() {
		visitMethod(inserted.access, inserted.name, inserted.desc, inserted.signature,
				inserted.exceptions.toArray(new String[0]));
		super.visitEnd();
	}
}