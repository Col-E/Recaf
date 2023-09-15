package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;
import software.coley.recaf.RecafConstants;

/**
 * A visitor that strips variable data from all methods.
 *
 * @author Matt Coley
 */
public class VariableRemovingMethodVisitor extends MethodVisitor {
	/**
	 * @param mv
	 * 		Parent visitor.
	 */
	public VariableRemovingMethodVisitor(@Nullable MethodVisitor mv) {
		super(RecafConstants.getAsmVersion(), mv);
	}

	@Override
	public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath,
														  Label[] start, Label[] end, int[] index,
														  String descriptor, boolean visible) {
		return null;
	}

	@Override
	public void visitLocalVariable(String name, String desc, String sig, Label start, Label end, int index) {
		// Do not visit
	}
}
