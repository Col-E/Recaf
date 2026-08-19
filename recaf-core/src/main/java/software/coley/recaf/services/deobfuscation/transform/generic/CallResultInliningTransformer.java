package software.coley.recaf.services.deobfuscation.transform.generic;

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
import software.coley.recaf.util.ClassMethodPair;
import software.coley.recaf.util.analysis.eval.EvaluationResult;
import software.coley.recaf.util.analysis.eval.EvaluationYieldResult;
import software.coley.recaf.util.analysis.eval.Evaluator;
import software.coley.recaf.util.analysis.eval.FieldCacheManager;
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
	/** Key for the maximum number of steps to allow when evaluating a method. */
	public static final String KEY_MAX_STEPS = "call-result-inlining.max-steps";
	private static final int DEFAULT_MAX_STEPS = 20_000;

	private final InheritanceGraphService graphService;

	private InheritanceGraph inheritanceGraph;

	@Inject
	public CallResultInliningTransformer(@Nonnull InheritanceGraphService graphService) {
		this.graphService = graphService;
	}

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {
		inheritanceGraph = graphService.getOrCreateInheritanceGraph(workspace);
	}

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		boolean dirty = false;
		String className = initialClassState.getName();
		ClassNode node = context.getNode(bundle, initialClassState);

		// The transformer instance is shared across classes transformed in parallel, so the evaluator
		// and its field cache must be scoped to this invocation rather than stored as instance state.
		// We used to have a shared evaluator + cache, but that caused issues with the parallel evaluation
		// of multiple classes, where the field cache would be polluted by other threads.
		int maxSteps = context.getParameters().getInt(KEY_MAX_STEPS, DEFAULT_MAX_STEPS);
		FieldCacheManager fieldCacheManager = new FieldCacheManager();
		Evaluator evaluator = new Evaluator(workspace, context.newInterpreter(inheritanceGraph), fieldCacheManager, maxSteps, false, false);
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

					// Either we need zero arguments, or all arguments that have known values.
					if (!arguments.isEmpty() && arguments.stream().anyMatch(v -> !v.hasKnownValue()))
						continue;

					// Target method must be able to be evaluated.
					ClassMethodPair target = context.resolveMethod(min);
					if (target == null)
						continue;
					if (!evaluator.canEvaluate(target.methodNode()))
						continue;

					// Reset instance support before each evaluation to prevent state pollution.
					fieldCacheManager.reset();

					// Seed the call stack so trace-dependent operations can be evaluated at depth [1].
					evaluator.setCallStackSeed(List.of(new ClassMethodPair(node, method)));

					// Attempt evaluation. If it yields a value, replace the call with the result.
					EvaluationResult result = evaluator.evaluate(target.classNode(), target.methodNode(), null, arguments);
					if (result instanceof EvaluationYieldResult(ReValue retVal)) {
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

}
