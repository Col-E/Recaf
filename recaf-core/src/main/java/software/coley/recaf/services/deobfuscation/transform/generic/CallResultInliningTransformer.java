package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
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
import software.coley.recaf.services.transform.TransformationParameter;
import software.coley.recaf.util.ClassMethodPair;
import software.coley.recaf.util.analysis.ReAnalyzer;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.eval.EvaluationResult;
import software.coley.recaf.util.analysis.eval.EvaluationYieldResult;
import software.coley.recaf.util.analysis.eval.Evaluator;
import software.coley.recaf.util.analysis.eval.FieldCacheManager;
import software.coley.recaf.util.analysis.lookup.GetStaticLookup;
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
	public static final String IDENTIFIER = "peephole.data.callinline";
	public static final String KEY_MAX_STEPS = IDENTIFIER + ".max-steps";
	public static final String KEY_EVALUATE_CLASS_INITIALIZERS = IDENTIFIER + ".evaluate-class-initializers";

	private static final int DEFAULT_MAX_STEPS = 20_000;
	private static final boolean DEFAULT_EVALUATE_CLASS_INITIALIZERS = false;
	private static final TransformationParameter<Integer> MAX_STEPS_PARAMETER =
			new TransformationParameter<>(KEY_MAX_STEPS, int.class, DEFAULT_MAX_STEPS);
	private static final TransformationParameter<Boolean> EVALUATE_CLASS_INITIALIZERS_PARAMETER =
			new TransformationParameter<>(KEY_EVALUATE_CLASS_INITIALIZERS, boolean.class, DEFAULT_EVALUATE_CLASS_INITIALIZERS);

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
		StaticValueCollectionTransformer staticValueCollector = context.getOptionalTransformer(StaticValueCollectionTransformer.class);

		// The transformer instance is shared across classes transformed in parallel, so the evaluator
		// and its field cache must be scoped to this invocation rather than stored as instance state.
		// We used to have a shared evaluator + cache, but that caused issues with the parallel evaluation
		// of multiple classes, where the field cache would be polluted by other threads.
		int maxSteps = context.getParameters().getInt(KEY_MAX_STEPS, DEFAULT_MAX_STEPS);
		boolean evaluateClassInitializers = context.getParameters().getBoolean(KEY_EVALUATE_CLASS_INITIALIZERS, DEFAULT_EVALUATE_CLASS_INITIALIZERS);
		FieldCacheManager fieldCacheManager = new FieldCacheManager();

		// If we have the static value collection transformer active we want to pull values from it when evaluating static fields.
		// So we'll create a lookup wrapper that bundles both the static value collection and the default lookup together.
		ReInterpreter evaluatorInterpreter = context.newInterpreter(inheritanceGraph);
		installStaticValueCollector(evaluatorInterpreter, staticValueCollector);

		Evaluator evaluator = new Evaluator(workspace, evaluatorInterpreter, fieldCacheManager, maxSteps, false, evaluateClassInitializers);
		for (MethodNode method : node.methods) {
			// Skip if abstract.
			InsnList instructions = method.instructions;
			if (instructions == null)
				continue;

			Frame<ReValue>[] frames;
			try {
				ReAnalyzer analyzer = context.newAnalyzer(inheritanceGraph, node, method);
				installStaticValueCollector(analyzer.getInterpreter(), staticValueCollector);
				frames = analyzer.analyze(node.name, method);
			} catch (Throwable ex) {
				// Analysis failed, skip this method.
				continue;
			}
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

	private static void installStaticValueCollector(@Nonnull ReInterpreter interpreter,
	                                                @Nullable StaticValueCollectionTransformer collector) {
		// Static value collector transformer isn't active.
		if (collector == null)
			return;

		// If the interpreter has no existing lookup, we can just set the collector as the lookup.
		GetStaticLookup fallback = interpreter.getGetStaticLookup();
		if (fallback == null) {
			interpreter.setGetStaticLookup(collector);
			return;
		}

		// Combine both the collector and the existing lookup so the evaluator can benefit from both.
		interpreter.setGetStaticLookup(new GetStaticLookup() {
			@Nonnull
			@Override
			public ReValue get(@Nonnull FieldInsnNode field) {
				return collector.hasLookup(field) ? collector.get(field) : fallback.get(field);
			}

			@Override
			public boolean hasLookup(@Nonnull FieldInsnNode field) {
				return collector.hasLookup(field) || fallback.hasLookup(field);
			}
		});
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
	public String identifier() {
		return IDENTIFIER;
	}

	@Nonnull
	@Override
	public String beforeExample() {
		return """
				// Assuming 'xor(String, int)' is a static method, and the inputs are known
				// the result is also effectively known, so the call can be replaced with the result.
				return xor("Idmmn!vnsme", 0x010101);
				""";
	}

	@Nonnull
	@Override
	public String afterExample() {
		return """
				return "Hello world";
				""";
	}

	@Nonnull
	@Override
	public List<TransformationParameter<?>> getParameterDefinitions() {
		return List.of(MAX_STEPS_PARAMETER, EVALUATE_CLASS_INITIALIZERS_PARAMETER);
	}
}
