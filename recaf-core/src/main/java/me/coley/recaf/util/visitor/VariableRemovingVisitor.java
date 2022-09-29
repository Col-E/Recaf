package me.coley.recaf.util.visitor;

import me.coley.recaf.RecafConstants;
import org.objectweb.asm.*;

/**
 * A visitor that strips variable data from all methods in a class.
 *
 * @author Matt Coley
 */
public class VariableRemovingVisitor extends ClassVisitor {
	/**
	 * @param cv
	 * 		Parent visitor.
	 */
	public VariableRemovingVisitor(ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		return new MethodVisitor(RecafConstants.getAsmVersion(), mv) {
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
		};
	}
}
