package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.workspace.model.Workspace;

/**
 * A {@link ClassRemapper} implementation that delegates to a provided {@link Mappings} via {@link WorkspaceBackedRemapper}.
 * <br>
 * When applied to a class you can check if any modifications have been made via {@link #hasMappingBeenApplied()}.
 *
 * @author Matt Coley
 */
public class WorkspaceClassRemapper extends ClassRemapper {
	private final WorkspaceBackedRemapper workspaceRemapper;

	/**
	 * @param cv
	 * 		Class to visit and rename mapped items of.
	 * @param workspace
	 * 		Workspace to pull class info from when additional context is needed.
	 * @param mappings
	 * 		Mappings to apply.
	 */
	public WorkspaceClassRemapper(ClassVisitor cv, @Nonnull Workspace workspace, @Nonnull Mappings mappings) {
		super(RecafConstants.getAsmVersion(), cv, new WorkspaceBackedRemapper(workspace, mappings));
		// Shadow the parent type's remapper locally,
		// allowing us to use our more specific methods with additional context.
		this.workspaceRemapper = ((WorkspaceBackedRemapper) super.remapper);
	}

	@Override
	protected MethodVisitor createMethodRemapper(MethodVisitor mv) {
		return new BetterMethodRemapper(mv, workspaceRemapper);
	}

	/**
	 * @return {@code true} when any mapping has been found and used.
	 */
	public boolean hasMappingBeenApplied() {
		return workspaceRemapper.hasMappingBeenApplied();
	}

	/**
	 * {@link MethodRemapper} pointing to enhanced remapping methods to allow for more context-sensitive behavior.
	 */
	private class BetterMethodRemapper extends MethodRemapper {
		protected BetterMethodRemapper(MethodVisitor methodVisitor, Remapper remapper) {
			super(RecafConstants.getAsmVersion(), methodVisitor, remapper);
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
			// Modified variant of impl in 'MethodRemapper' to call our 'workspaceRemapper' specific methods
			Object[] remappedBootstrapMethodArguments = new Object[bootstrapMethodArguments.length];
			for (int i = 0; i < bootstrapMethodArguments.length; ++i) {
				remappedBootstrapMethodArguments[i] = remapper.mapValue(bootstrapMethodArguments[i]);
			}
			mv.visitInvokeDynamicInsn(
					workspaceRemapper.mapInvokeDynamicMethodName(name, descriptor,
							bootstrapMethodHandle, remappedBootstrapMethodArguments),
					remapper.mapMethodDesc(descriptor),
					(Handle) remapper.mapValue(bootstrapMethodHandle),
					remappedBootstrapMethodArguments);
		}
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
			String mappedName = workspaceRemapper.mapVariableName(className, methodName, methodDesc, name, desc, index);
			super.visitLocalVariable(mappedName, desc, signature, start, end, index);
		}
	}
}
