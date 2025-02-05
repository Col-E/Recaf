package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static org.objectweb.asm.Opcodes.NOP;

/**
 * A transformer that removes dead code.
 *
 * @author Matt Coley
 */
@Dependent
public class DeadCodeRemovingTransformer implements JvmClassTransformer {
	private final InheritanceGraphService graphService;
	private final WorkspaceManager workspaceManager;
	private InheritanceGraph inheritanceGraph;

	@Inject
	public DeadCodeRemovingTransformer(@Nonnull WorkspaceManager workspaceManager, @Nonnull InheritanceGraphService graphService) {
		this.workspaceManager = workspaceManager;
		this.graphService = graphService;
	}

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {
		inheritanceGraph = workspace == workspaceManager.getCurrent() ?
				graphService.getCurrentWorkspaceInheritanceGraph() :
				graphService.newInheritanceGraph(workspace);
	}

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		boolean dirty = false;
		String className = initialClassState.getName();
		ClassNode node = context.getNode(bundle, initialClassState);
		for (MethodNode method : node.methods) {
			InsnList instructions = method.instructions;
			if (instructions == null)
				continue;
			try {
				Frame<ReValue>[] frames = context.analyze(inheritanceGraph, node, method);
				for (int i = instructions.size() - 1; i >= 0; i--) {
					AbstractInsnNode insn = instructions.get(i);
					if (frames[i] == null || insn.getOpcode() == NOP) {
						instructions.remove(insn);
						dirty = true;
					}
				}
			} catch (Throwable t) {
				throw new TransformationException("Error encountered when removing dead code", t);
			}
		}
		if (dirty)
			context.setNode(bundle, initialClassState, node);
	}

	@Nonnull
	@Override
	public String name() {
		return "Dead code removal";
	}
}
