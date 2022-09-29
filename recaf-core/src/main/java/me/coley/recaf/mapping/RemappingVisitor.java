package me.coley.recaf.mapping;

import me.coley.recaf.RecafConstants;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;

/**
 * A {@link ClassRemapper} implementation that delegates to a provided {@link Mappings} via {@link RemapperImpl}.
 * <br>
 * When applied to a class you can check if any modifications have been made via {@link #hasMappingBeenApplied()}.
 *
 * @author Matt Coley
 */
public class RemappingVisitor extends ClassRemapper {
	private final RemapperImpl remapper;

	/**
	 * @param cv
	 * 		Class to visit and rename mapped items of.
	 * @param mappings
	 * 		Mappings to apply.
	 */
	public RemappingVisitor(ClassVisitor cv, Mappings mappings) {
		super(RecafConstants.getAsmVersion(), cv, new RemapperImpl(mappings));
		// Shadow the parent type's remapper locally so we can use our newly defined methods
		this.remapper = ((RemapperImpl) super.remapper);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		// The super implementation of this should yield a "MethodRemapper",
		// so we will create an adapter visitor that delegates to it, but also handles variables.
		MethodVisitor methodRemapper = super.visitMethod(access, name, desc, signature, exceptions);
		return methodRemapper == null ? null :
				new VariableRenamingMethodVisitor(className, name, desc, methodRemapper);
	}

	/**
	 * @return {@code true} when any mapping has been found and used.
	 */
	public boolean hasMappingBeenApplied() {
		return remapper.hasMappingBeenApplied();
	}

	/**
	 * Method visitor that functions as an adapter for the
	 * {@link #visitMethod(int, String, String, String, String[]) standard method remapping visitor}
	 * that additionally supports variable renaming.
	 */
	private class VariableRenamingMethodVisitor extends MethodVisitor {
		private final String className;
		private final String methodName;
		private final String methodDesc;

		public VariableRenamingMethodVisitor(String className, String methodName, String methodDesc, MethodVisitor mv) {
			super(RecafConstants.getAsmVersion(), mv);
			this.className = className;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
		}

		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
			String mappedName = remapper.mapVariableName(className, methodName, methodDesc, name, desc, index);
			super.visitLocalVariable(mappedName, desc, signature, start, end, index);
		}
	}
}
