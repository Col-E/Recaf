package software.coley.recaf.services.deobfuscation.transform.generic;

import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.analysis.ReEvaluationException;
import software.coley.recaf.util.analysis.ReEvaluator;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A transformer that inlines method calls that can be fully evaluated.
 *
 * @author Matt Coley
 */
@Dependent
public class CallResultInliningTransformer implements JvmClassTransformer {
	private final static int MAX_STEPS = 20_000; // TODO: Make configurable
	private final InheritanceGraphService graphService;
	private final Object2BooleanMap<String> canBeEvaluatedMap = new Object2BooleanArrayMap<>();
	private InheritanceGraph inheritanceGraph;
	private ReEvaluator evaluator;

	@Inject
	public CallResultInliningTransformer(@Nonnull InheritanceGraphService graphService) {
		this.graphService = graphService;
	}

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {
		inheritanceGraph = graphService.getOrCreateInheritanceGraph(workspace);
		evaluator = new ReEvaluator(workspace, context.newInterpreter(inheritanceGraph), MAX_STEPS);
	}

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

			Frame<ReValue>[] frames = context.analyze(inheritanceGraph, node, method);
			for (int i = instructions.size() - 1; i >= 0; i--) {
				AbstractInsnNode insn = instructions.get(i);
				if (insn.getOpcode() == Opcodes.INVOKESTATIC && insn instanceof MethodInsnNode min) {
					Frame<ReValue> frame = frames[i];
					if (frame == null)
						continue;

					// Collect arguments.
					Type methodType = Type.getMethodType(min.desc);
					List<ReValue> arguments = new ArrayList<>(methodType.getArgumentCount());
					for (int j = 0; j < methodType.getArgumentCount(); j++)
						arguments.addFirst(frame.getStack(frame.getStackSize() - 1 - j));

					// All arguments must have known values.
					if (arguments.stream().anyMatch(v -> !v.hasKnownValue()))
						continue;

					// Target method must be able to be evaluated.
					if (!canEvaluate(min))
						continue;

					try {
						ReValue retVal = evaluator.evaluate(min.owner, min.name, min.desc, null, arguments);
						AbstractInsnNode replacement = OpaqueConstantFoldingTransformer.toInsn(retVal);
						if (replacement != null) {
							for (int arg = arguments.size() - 1; arg >= 0; arg--) {
								ReValue argValue = arguments.get(arg);
								if (argValue instanceof LongValue || argValue instanceof DoubleValue)
									instructions.insertBefore(min, new InsnNode(Opcodes.POP2));
								else
									instructions.insertBefore(min, new InsnNode(Opcodes.POP));
							}
							instructions.set(min, replacement);
							dirty = true;
						}
					} catch (ReEvaluationException ex) {
						continue;
					}
				}
			}
		}
		if (dirty)
			context.setNode(bundle, initialClassState, node);
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> recommendedSuccessors() {
		// This transformer results in the creation of a lot of POP/POP2 instructions.
		// The stack-operation folding transformer can clean up afterward.
		return Collections.singleton(OpaqueConstantFoldingTransformer.class);
	}

	@Nonnull
	@Override
	public String name() {
		return "Call result inlining";
	}

	private boolean canEvaluate(@Nonnull MethodInsnNode min) {
		String key = min.owner + "." + min.name + min.desc;
		return canBeEvaluatedMap.computeIfAbsent(key, k -> evaluator.canEvaluate(min.owner, min.name, min.desc));
	}
}
