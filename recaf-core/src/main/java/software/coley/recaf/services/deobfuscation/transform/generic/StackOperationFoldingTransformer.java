package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static software.coley.recaf.services.deobfuscation.transform.generic.LinearOpaqueConstantFoldingTransformer.isSupportedValueProducer;
import static software.coley.recaf.util.AsmInsnUtil.POP;

/**
 * A transformer that folds redundant stack operations.
 *
 * @author Matt Coley
 */
@Dependent
public class StackOperationFoldingTransformer implements JvmClassTransformer {
	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		boolean dirty = false;
		String className = initialClassState.getName();
		ClassNode node = context.getNode(bundle, initialClassState);
		for (MethodNode method : node.methods) {
			// Skip if abstract.
			InsnList instructions = method.instructions;
			if (instructions == null)
				continue;

			for (int i = instructions.size() - 2; i > 0; i--) {
				AbstractInsnNode insn = instructions.get(i);

				// TODO: Make this not shit, just getting this to work for the demo cases in the transformer tests.
				//  - A more proper implementation will have the same behavior, but with more proper edge case handling
				if (isSupportedValueProducer(insn) && insn.getNext().getOpcode() == POP) {
					instructions.remove(insn.getNext());
					instructions.remove(insn);
					dirty = true;
				}
			}
		}
		if (dirty)
			context.setNode(bundle, initialClassState, node);
	}

	@Nonnull
	@Override
	public String name() {
		return "Stack folding";
	}
}
