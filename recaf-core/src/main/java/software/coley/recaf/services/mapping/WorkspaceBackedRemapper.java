package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.util.Handles;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Enhanced {@link BasicMappingsRemapper} for cases where additional information
 * needs to be pulled from a {@link Workspace}.
 *
 * @author Matt Coley
 */
public class WorkspaceBackedRemapper extends BasicMappingsRemapper {
	private final Workspace workspace;

	/**
	 * @param workspace
	 * 		Workspace to pull class info from when additional context is needed.
	 * @param mappings
	 * 		Mappings wrapper to pull values from.
	 */
	public WorkspaceBackedRemapper(@Nonnull Workspace workspace,
	                               @Nonnull Mappings mappings) {
		super(mappings);
		this.workspace = workspace;
	}

	@Override
	public String mapAnnotationAttributeName(String descriptor, String name) {
		String annotationName = Type.getType(descriptor).getInternalName();
		ClassPathNode classPath = workspace.findClass(annotationName);

		// Not found, probably not intended to be renamed.
		if (classPath == null)
			return name;

		// Get the declaration and, if found, treat as normal method mapping.
		ClassInfo info = classPath.getValue();
		MethodMember attributeMethod = info.getMethods().stream()
				.filter(method -> method.getName().equals(name))
				.findFirst().orElse(null);

		// Not found, shouldn't generally happen.
		if (attributeMethod == null)
			return name;

		// Use the method mapping from the annotation class's declared methods.
		return mapMethodName(annotationName, name, attributeMethod.getDescriptor());
	}

	@Override
	@SuppressWarnings("deprecation")
	public String mapInvokeDynamicMethodName(String name, String descriptor) {
		// Deprecated in ASM 9.9 - Keeping this here for a bit to ensure nobody accidentally calls it.
		throw new IllegalStateException("Enhanced 'mapInvokeDynamicMethodName(...)' usage required, missing handle arg");
	}

	/**
	 * @param name
	 * 		The name of the method.
	 * @param descriptor
	 * 		The descriptor of the method.
	 * @param bsm
	 * 		The bootstrap method handle.
	 * @param bsmArguments
	 * 		The arguments to the bsm.
	 *
	 * @return New name of the method.
	 */
	@Nonnull
	@Override
	public String mapInvokeDynamicMethodName(@Nonnull String name, @Nonnull String descriptor, @Nonnull Handle bsm, @Nonnull Object[] bsmArguments) {
		if (bsm.equals(Handles.META_FACTORY)) {
			// Get the interface from the descriptor return type.
			String interfaceOwner = Type.getReturnType(descriptor).getInternalName();

			// Get the method descriptor from the implementation handle (2nd arg value)
			if (bsmArguments[1] instanceof Handle implementationHandle) {
				String interfaceMethodDesc = implementationHandle.getDesc();
				return mapMethodName(interfaceOwner, name, interfaceMethodDesc);
			}
		}

		// Not a known method handle type, so we do not know how to bootstrapMethodHandle renaming it.
		return name;
	}

	/**
	 * @param className
	 * 		Internal name of the class defining the method the variable resides in.
	 * @param methodName
	 * 		Name of the method.
	 * @param methodDesc
	 * 		Descriptor of the method.
	 * @param name
	 * 		Name of the variable.
	 * @param desc
	 * 		Descriptor of the variable.
	 * @param index
	 * 		Index of the variable.
	 *
	 * @return Mapped name of the variable, or the existing name if no mapping exists.
	 */
	@Nonnull
	public String mapVariableName(@Nonnull String className, @Nonnull String methodName, @Nonnull String methodDesc,
	                              String name, String desc, int index) {
		String mapped = mappings.getMappedVariableName(className, methodName, methodDesc, name, desc, index);
		if (mapped != null) {
			markModified();
			return mapped;
		}
		// Use existing variable name.
		return name;
	}
}
