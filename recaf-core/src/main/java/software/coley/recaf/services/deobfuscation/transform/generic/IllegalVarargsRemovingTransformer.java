package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.visitors.IllegalVarargsRemovingVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * A transformer that removes any invalid use of varargs from methods.
 *
 * @author Matt Coley
 */
@Dependent
public class IllegalVarargsRemovingTransformer implements JvmClassTransformer {
	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		// First scan the model to see if we need to actually reparse and patch the bytecode.
		boolean hasInvalidVarargs = false;
		for (MethodMember method : initialClassState.getMethods()) {
			if (method.hasVarargsModifier()) {
				Type methodType = Type.getMethodType(method.getDescriptor());
				Type[] argumentTypes = methodType.getArgumentTypes();
				if (argumentTypes.length == 0 || argumentTypes[argumentTypes.length - 1].getSort() != Type.ARRAY) {
					hasInvalidVarargs = true;
					break;
				}
			}
		}

		// If we found an invalid use case, we'll do the work to remove it.
		if (hasInvalidVarargs) {
			ClassReader reader = new ClassReader(context.getBytecode(bundle, initialClassState));
			ClassWriter writer = new ClassWriter(reader, 0);
			IllegalVarargsRemovingVisitor remover = new IllegalVarargsRemovingVisitor(writer);
			reader.accept(remover, 0);
			if (remover.hasDetectedIllegalVarargs()) // Should always occur given the circumstances
				context.setBytecode(bundle, initialClassState, writer.toByteArray());
		}
	}

	@Nonnull
	@Override
	public String name() {
		return "Illegal varargs removal";
	}
}
