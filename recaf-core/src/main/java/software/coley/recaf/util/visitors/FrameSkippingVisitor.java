package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.FrameNode;
import software.coley.recaf.RecafConstants;

/**
 * Visitor for skipping {@link FrameNode} content.
 *
 * @author Matt Coley
 */
public class FrameSkippingVisitor extends ClassVisitor {
	/**
	 * @param cv
	 * 		Parent visitor.
	 */
	public FrameSkippingVisitor(@Nullable ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new FrameSkippingMethodVisitor(mv);
	}

	/**
	 * Method visitor that does the actual skipping.
	 */
	public static class FrameSkippingMethodVisitor extends MethodVisitor {
		/**
		 * @param mv
		 * 		Parent visitor.
		 */
		public FrameSkippingMethodVisitor(@Nullable MethodVisitor mv) {
			super(RecafConstants.getAsmVersion(), mv);
		}

		@Override
		public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
			// no-op
		}
	}
}