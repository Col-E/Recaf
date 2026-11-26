package software.coley.recaf.services.deobfuscation.transform.specific.zkm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.deobfuscation.transform.generic.InvokeDynamicInliningTransformer;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.analysis.eval.InstancedObjectValue;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.util.analysis.value.impl.ObjectValueBoxImpl;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.objectweb.asm.Opcodes.*;
import static software.coley.recaf.util.AsmInsnUtil.intToInsn;
import static software.coley.recaf.util.AsmInsnUtil.longToInsn;

/**
 * Discovers and inlines values carried by ZKM's synthetic method parameters used to strengthen reference and string encryption.
 * <p>
 * From their documentation:
 * <blockquote>
 * Additional parameters are added to the methods that make use of String Encryption, Integer Constant Encryption and/or
 * Reference Obfuscation and these new parameters are used to pass decryption keys to the supported functions.
 * <p>
 * Where possible, extra parameters will also be added to calling methods such that the decryption keys will be passed
 * through a chain of methods. The objective is to interlink the obfuscated methods and classes such that they must be
 * attacked as a whole rather than as more manageable, individual units.
 * </blockquote>
 * Due to the interlinked nature of the method data flow, this transformer analyzes the full workspace during
 * {@link #transform(JvmTransformerContext, Workspace, WorkspaceResource, JvmClassBundle, JvmClassInfo)} rather
 * than an isolated class at a time. Only one call to {@code transform} per pass is let through.
 *
 * @author Matt Coley
 */
@Dependent
public class ZkmParameterUnpackingTransformer implements JvmClassTransformer {
	public static final String IDENTIFIER = "specific.zkm.methodparams";

	// The loop exists to let chained payload steps settle, but it has to stay small: past a handful of passes we
	// are no longer making progress, we are just burning time on a graph that will never converge.
	private static final int MAX_PASSES = 8;
	private static final String OBJECT_ARRAY_DESCRIPTOR = "[Ljava/lang/Object;";
	private static final Type OBJECT_ARRAY_TYPE = Type.getType(OBJECT_ARRAY_DESCRIPTOR);
	private static final Type STRING_TYPE = Type.getObjectType("java/lang/String");

	private final InheritanceGraphService graphService;
	private InheritanceGraph inheritanceGraph;
	private boolean processed;
	private Map<String, NodeState> processedNodes = Map.of();
	private Set<String> participantClasses = Set.of();
	private final Set<ParameterKey> seededParameters = new HashSet<>();

	@Inject
	public ZkmParameterUnpackingTransformer(@Nonnull InheritanceGraphService graphService) {
		this.graphService = graphService;
	}

	@Override
	public synchronized void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) throws TransformationException {
		inheritanceGraph = graphService.getOrCreateInheritanceGraph(workspace);
		processed = false;
		processedNodes = Map.of();
		participantClasses = Set.of();
		seededParameters.clear();
	}

	@Override
	public synchronized void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                                   @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                                   @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		// Per the "interlinked" nature of "Method Parameter Changes" we analyze the full workspace here.
		// So once one transform call is on the job, block any following calls until the next pass (reset in setup)
		// or if the workspace has changed since the last pass, which means we need to re-analyze.
		if (processed && !hasCurrentNodesChanged(context, resource))
			return;
		processed = true;

		// Snapshot classes from the workspace, which should contain the original class state regardless of our
		// position in a transformation plan. Another transformer that registers a class change can break our analysis.
		Map<String, WorkingClass> classes = snapshotClasses(context, resource);

		// Participant classes accumulate in the context of a multi-pass transformation run.
		// If we just recomputed the participants locally, we would lose the previous participants
		// after inlining the parameter values, which would break the data flow chain.
		Set<String> currentParticipants = findParticipants(classes);
		if (participantClasses.isEmpty())
			participantClasses = Set.copyOf(currentParticipants);
		else if (!currentParticipants.isEmpty()) {
			Set<String> allParticipants = new TreeSet<>(participantClasses);
			allParticipants.addAll(currentParticipants);
			participantClasses = Set.copyOf(allParticipants);
		}

		// Filter the accumulated participants to only those that still exist in the workspace.
		// If another transformer (primarily the zkm cleanup one) removed a class, we don't want to try to analyze it.
		Set<String> participants = new TreeSet<>();
		for (String participant : participantClasses)
			if (classes.containsKey(participant))
				participants.add(participant);
		if (participants.isEmpty()) {
			rememberNodes(context, classes);
			return;
		}

		// Each pass folds what the previous pass exposed, such as a payload built from a value
		// that only became a constant in the pass before. Repeat until no changes are observed, or we hit the limit.
		InheritanceGraph graph = inheritanceGraph;
		if (graph == null)
			graph = graphService.getOrCreateInheritanceGraph(workspace);
		for (int pass = 0; pass < MAX_PASSES; pass++) {
			Map<MethodKey, MethodFacts> facts = collectFacts(context, graph, classes, participants);
			if (!applyFacts(classes, participants, facts))
				break;
		}

		// Commit changed classes.
		for (WorkingClass workingClass : classes.values()) {
			if (!workingClass.changed)
				continue;
			context.setRecomputeFrames(workingClass.node.name);
			context.setNode(workingClass.bundle, workingClass.info, workingClass.node);
		}

		// Record workspace class state so that future passes can detect changes.
		rememberNodes(context, classes);
	}

	@Override
	@Nonnull
	public Set<Class<? extends ClassTransformer>> recommendedSuccessors() {
		return Set.of(InvokeDynamicInliningTransformer.class);
	}

	@Override
	@Nonnull
	public String identifier() {
		return IDENTIFIER;
	}

	/**
	 * Records the current state of the workspace classes so that future passes can detect changes.
	 *
	 * @param context
	 * 		Transformation context for the current run.
	 * @param classes
	 * 		Working classes to record.
	 */
	private void rememberNodes(@Nonnull JvmTransformerContext context, @Nonnull Map<String, WorkingClass> classes) {
		Map<String, NodeState> committedNodes = new HashMap<>();
		for (WorkingClass workingClass : classes.values()) {
			ClassNode node = context.getNode(workingClass.bundle, workingClass.info);
			committedNodes.put(workingClass.node.name, new NodeState(node, fingerprint(node)));
		}
		processedNodes = Map.copyOf(committedNodes);
	}

	/**
	 * @param context
	 * 		Transformation context for the current run.
	 * @param resource
	 * 		Resource whose classes should be compared against the last recorded state.
	 *
	 * @return {@code true} when the workspace has changed since the last pass, {@code false} otherwise.
	 */
	private boolean hasCurrentNodesChanged(@Nonnull JvmTransformerContext context, @Nonnull WorkspaceResource resource) {
		Map<String, NodeState> currentNodes = new HashMap<>();
		resource.jvmAllClassBundleStreamRecursive().forEach(bundle -> bundle.forEach(info -> {
			if (!currentNodes.containsKey(info.getName())) {
				ClassNode node = context.getNode(bundle, info);
				currentNodes.put(info.getName(), new NodeState(node, fingerprint(node)));
			}
		}));
		if (currentNodes.size() != processedNodes.size())
			return true;
		for (Map.Entry<String, NodeState> entry : currentNodes.entrySet()) {
			NodeState previous = processedNodes.get(entry.getKey());
			if (previous == null || previous.node != entry.getValue().node || previous.fingerprint != entry.getValue().fingerprint)
				return true;
		}
		return false;
	}

	/**
	 * Hashes structurally relevant class content so in-place edits made by other transformers become visible.
	 *
	 * @param node
	 * 		Class to fingerprint.
	 *
	 * @return Content hash of the class.
	 */
	private static int fingerprint(@Nonnull ClassNode node) {
		int hash = 1;
		hash = 31 * hash + node.version;
		hash = 31 * hash + node.access;
		hash = 31 * hash + Objects.hashCode(node.name);
		for (MethodNode method : node.methods) {
			hash = 31 * hash + Objects.hashCode(method.name);
			hash = 31 * hash + Objects.hashCode(method.desc);
			hash = 31 * hash + method.access;
			if (method.instructions == null)
				continue;

			// Only hash instructions relevant to parameter data flow.
			for (AbstractInsnNode instruction : method.instructions) {
				hash = 31 * hash + instruction.getType();
				hash = 31 * hash + instruction.getOpcode();
				switch (instruction) {
					case VarInsnNode variable -> hash = 31 * hash + variable.var;
					case IntInsnNode integer -> hash = 31 * hash + integer.operand;
					case LdcInsnNode ldc -> hash = 31 * hash + Objects.hashCode(ldc.cst);
					case TypeInsnNode type -> hash = 31 * hash + Objects.hashCode(type.desc);
					case MethodInsnNode call -> {
						hash = 31 * hash + Objects.hashCode(call.owner);
						hash = 31 * hash + Objects.hashCode(call.name);
						hash = 31 * hash + Objects.hashCode(call.desc);
					}
					case InvokeDynamicInsnNode indy -> {
						hash = 31 * hash + Objects.hashCode(indy.name);
						hash = 31 * hash + Objects.hashCode(indy.desc);
						hash = 31 * hash + Objects.hashCode(indy.bsm);
					}
					default -> {}
				}
			}
		}
		return hash;
	}

	/**
	 * @param context
	 * 		Transformation context for the current run.
	 * @param resource
	 * 		Resource whose classes should be snapshotted.
	 *
	 * @return Class snapshot map.
	 */
	@Nonnull
	private static Map<String, WorkingClass> snapshotClasses(@Nonnull JvmTransformerContext context,
	                                                         @Nonnull WorkspaceResource resource) {
		Map<String, WorkingClass> classes = new TreeMap<>();
		resource.jvmAllClassBundleStreamRecursive().forEach(bundle -> bundle.forEach(info -> {
			if (classes.containsKey(info.getName()))
				return;
			ClassNode current = context.getNode(bundle, info);
			ClassNode copy = new ClassNode();
			current.accept(copy);
			classes.put(info.getName(), new WorkingClass(bundle, info, copy));
		}));
		return classes;
	}

	/**
	 * Finds classes that contain at least one ZKM {@code invokedynamic} call.
	 *
	 * @param classes
	 * 		Working classes to inspect.
	 *
	 * @return Names of classes belonging to the generated caller region.
	 */
	@Nonnull
	private static Set<String> findParticipants(@Nonnull Map<String, WorkingClass> classes) {
		// Classes that declare the bootstrap methods isn't a good qualifier because in different configurations
		// the bootstrapping may be in a separate class, and not part of the obfuscated application code.
		// So instead we need to look for references to the bootstraps.
		Set<String> participants = new TreeSet<>();
		for (WorkingClass workingClass : classes.values()) {
			for (MethodNode method : workingClass.node.methods) {
				if (method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions) {
					if (instruction instanceof InvokeDynamicInsnNode indy && ZkmInvokeDynamicResolver.isZkmSite(indy)) {
						participants.add(workingClass.node.name);
						break;
					}
				}
				if (participants.contains(workingClass.node.name))
					break;
			}
		}
		return participants;
	}

	/**
	 * Collects parameter facts by scanning calls made from participant classes.
	 *
	 * @param context
	 * 		Transformation context for the current run.
	 * @param graph
	 * 		Workspace inheritance graph used to fan virtual calls out to implementations.
	 * @param classes
	 * 		Working classes keyed by name.
	 * @param participants
	 * 		Names of classes in the generated caller region.
	 *
	 * @return Facts keyed by target method identity.
	 */
	@Nonnull
	private static Map<MethodKey, MethodFacts> collectFacts(@Nonnull JvmTransformerContext context,
	                                                        @Nonnull InheritanceGraph graph,
	                                                        @Nonnull Map<String, WorkingClass> classes,
	                                                        @Nonnull Set<String> participants) {
		Map<MethodKey, MethodFacts> facts = new TreeMap<>();
		for (String participant : participants) {
			WorkingClass workingClass = classes.get(participant);
			if (workingClass == null)
				continue;
			for (MethodNode method : workingClass.node.methods) {
				if (method.instructions == null || method.instructions.size() == 0)
					continue;

				// Analyze to get stack frame content (call parameters) at ZKM call sites.
				Frame<ReValue>[] frames;
				try {
					frames = context.analyze(graph, workingClass.node, method);
				} catch (Throwable t) {
					continue;
				}

				// For each call site, read the explicit arguments from the frame and merge them into the facts of every implementation it can reach.
				for (int index = 0; index < method.instructions.size(); index++) {
					AbstractInsnNode instruction = method.instructions.get(index);
					if (!(instruction instanceof MethodInsnNode call))
						continue;

					Type[] argumentTypes;
					try {
						argumentTypes = Type.getArgumentTypes(call.desc);
					} catch (Throwable t) {
						continue;
					}

					// In some cases the frame at the positon may be null (dead code).
					// In that case we still want to merge the call into the facts, but we won't have any known arguments to contribute.
					Frame<ReValue> frame = index < frames.length ? frames[index] : null;
					List<ReValue> arguments = frame == null ? null : readExplicitArguments(frame, call, argumentTypes);
					mergeCallFacts(facts, call, argumentTypes, arguments, classes, participants, graph);
				}
			}
		}
		return facts;
	}

	/**
	 * Merges one call's argument values into the facts of every implementation it can reach.
	 *
	 * @param facts
	 * 		Facts being built for this pass.
	 * @param call
	 * 		Call site providing the arguments.
	 * @param argumentTypes
	 * 		Explicit parameter types of the call site.
	 * @param arguments
	 * 		Known arguments, or {@code null} when the call's frame was unavailable.
	 * @param classes
	 * 		Working classes keyed by name.
	 * @param participants
	 * 		Names of classes in the generated caller region.
	 * @param graph
	 * 		Workspace inheritance graph used for virtual dispatch lookups.
	 */
	private static void mergeCallFacts(@Nonnull Map<MethodKey, MethodFacts> facts,
	                                   @Nonnull MethodInsnNode call,
	                                   @Nonnull Type[] argumentTypes,
	                                   @Nullable List<ReValue> arguments,
	                                   @Nonnull Map<String, WorkingClass> classes,
	                                   @Nonnull Set<String> participants,
	                                   @Nonnull InheritanceGraph graph) {
		for (MethodKey target : resolveTargets(call, classes, participants, graph)) {
			WorkingClass targetClass = classes.get(target.owner);
			MethodNode targetMethod = targetClass == null ? null : findDeclaredMethod(targetClass.node, target.name, target.descriptor);

			// When the target would unbox an incompatible wrapper, this call throws before it could ever use the value,
			// so letting it contribute facts would poison a parameter that other callers use correctly.
			if (targetMethod == null || (arguments != null && !payloadShapeMatches(targetMethod, argumentTypes, arguments)))
				continue;

			MethodFacts targetFacts = facts.computeIfAbsent(target, ignored -> new MethodFacts(target.owner));
			for (int argumentIndex = 0; argumentIndex < argumentTypes.length; argumentIndex++) {
				// Unknown arguments intentionally force a conflict with known values, otherwise one unanalyzable caller
				// would let a constant from a different caller look like it applies everywhere.
				ReValue argument = arguments == null ? null : arguments.get(argumentIndex);
				targetFacts.merge(argumentIndex, argumentTypes[argumentIndex], argument);
			}
		}
	}

	/**
	 * Reads the explicit arguments of a call from the frame immediately before it.
	 *
	 * @param frame
	 * 		Frame immediately before the call.
	 * @param call
	 * 		Call site being inspected.
	 * @param argumentTypes
	 * 		Explicit parameter types of the call site.
	 *
	 * @return Argument values in declaration order, or {@code null} when the frame is too shallow to hold them.
	 */
	@Nullable
	private static List<ReValue> readExplicitArguments(@Nonnull Frame<ReValue> frame,
	                                                   @Nonnull MethodInsnNode call,
	                                                   @Nonnull Type[] argumentTypes) {
		// TODO: Seems like this sorta method would be useful to have in a util somewhere.
		//  - We don't have a dedicated util for ReFrame/ReValue though...
		//    Maybe when we have enough goodies to put in such a class we can make one.
		int receiverCount = call.getOpcode() == INVOKESTATIC ? 0 : 1;
		int requiredValues = argumentTypes.length + receiverCount;
		int stackSize = frame.getStackSize();
		if (stackSize < requiredValues)
			return null;
		int firstArgument = stackSize - argumentTypes.length;
		List<ReValue> arguments = new ArrayList<>(argumentTypes.length);
		for (int index = 0; index < argumentTypes.length; index++)
			arguments.add(frame.getStack(firstArgument + index));
		return arguments;
	}

	/**
	 * Resolves which workspace methods a call site can reach.
	 *
	 * @param call
	 * 		Call site being resolved.
	 * @param classes
	 * 		Working classes keyed by name.
	 * @param participants
	 * 		Names of classes in the generated caller region.
	 * @param graph
	 * 		Workspace inheritance graph used for virtual dispatch lookups.
	 *
	 * @return Set of potential target methods, empty when the call is not to a participant or the target is not concrete.
	 */
	@Nonnull
	private static Set<MethodKey> resolveTargets(@Nonnull MethodInsnNode call,
	                                             @Nonnull Map<String, WorkingClass> classes,
	                                             @Nonnull Set<String> participants,
	                                             @Nonnull InheritanceGraph graph) {
		Set<MethodKey> targets = new TreeSet<>();
		int opcode = call.getOpcode();

		// invokestatic and invokespecial are always direct calls, so we only need to check the owner class.
		if (opcode == INVOKESTATIC || opcode == INVOKESPECIAL) {
			// Must be a participant.
			if (!participants.contains(call.owner))
				return targets;

			// Must be defined in the workspace.
			WorkingClass owner = classes.get(call.owner);
			if (owner == null)
				return targets;

			// Must have a body.
			MethodNode target = findDeclaredMethod(owner.node, call.name, call.desc);
			if (isConcrete(target))
				targets.add(new MethodKey(call.owner, call.name, call.desc));
			return targets;
		}
		if (opcode != INVOKEVIRTUAL && opcode != INVOKEINTERFACE)
			return targets;

		// We cannot easily prove which implementation of a virtual call is actually reached,
		// so instead we need to consider all possible implementations of the target method.
		for (String candidateName : participants) {
			// Must be defined in the workspace.
			WorkingClass candidate = classes.get(candidateName);
			if (candidate == null)
				continue;

			// Must be assignable to the call's owner.
			if (!graph.isAssignableFrom(call.owner, candidateName))
				continue;

			// Must have a body.
			MethodNode target = findDeclaredMethod(candidate.node, call.name, call.desc);
			if (isConcrete(target))
				targets.add(new MethodKey(candidateName, call.name, call.desc));
		}
		return targets;
	}

	/**
	 * Checks whether a call's payload entries are compatible with the wrapper types the target actually unboxes.
	 *
	 * @param target
	 * 		Target method receiving the payload.
	 * @param argumentTypes
	 * 		Explicit parameter types of the call site.
	 * @param arguments
	 * 		Known arguments of the call site.
	 *
	 * @return {@code true} when the call can reach the target's extraction code without a cast failure.
	 */
	private static boolean payloadShapeMatches(@Nonnull MethodNode target,
	                                           @Nonnull Type[] argumentTypes,
	                                           @Nonnull List<ReValue> arguments) {
		// Map payload Object[] local slots to parameter indices.
		Map<Integer, Integer> payloadSlots = payloadParameterSlots(target);
		if (payloadSlots.isEmpty())
			return true;

		// Without a corresponding Object[] argument, payload filtering does not apply.
		int parameterIndex = payloadSlots.values().iterator().next();
		if (parameterIndex >= argumentTypes.length || !OBJECT_ARRAY_TYPE.equals(argumentTypes[parameterIndex]))
			return true;

		// Get the payload Object[].
		ReValue payload = arguments.get(parameterIndex);
		if (!(payload instanceof ArrayValue array) || array.isNull() || array.getFirstDimensionLength().isEmpty())
			return true;

		// Analyze the method instructions to discover each payload[N] to a type.
		Map<Integer, FactKind> expectedKinds = expectedPayloadKinds(target);
		if (expectedKinds.isEmpty())
			return true;

		// Check that the actual payload content matches the expected shape.
		for (Map.Entry<Integer, FactKind> entry : expectedKinds.entrySet()) {
			int index = entry.getKey();
			FactKind kind = entry.getValue();

			// If we cannot determine the actual payload content, we cannot prove a mismatch.
			PrimitiveFact actual = normalizePrimitive(array.getValue(index));
			if (actual.kind == FactKind.UNKNOWN || actual.kind == FactKind.CONFLICT)
				continue;

			// Strings can be null, so we don't consider that a mismatch.
			if (kind == FactKind.STRING && actual.kind == FactKind.NULL)
				continue;

			// Mismatch, the shape does not match.
			if (actual.kind != kind)
				return false;
		}
		return true;
	}

	/**
	 *
	 * @param target
	 * 		Target method to inspect.
	 *
	 * @return Expected element kinds keyed by {@code Object[]} payload index, empty when the target does not extract anything.
	 */
	@Nonnull
	private static Map<Integer, FactKind> expectedPayloadKinds(@Nonnull MethodNode target) {
		// For the following example:
		//
		// void consume(Object[] payload) {
		//    int  a = ((Integer) payload[0]).intValue();
		//    long b = ((Long)    payload[1]).longValue();
		//    String c = (String) payload[2];
		// }
		//
		// payload[0] is a boxed integer.
		// payload[1] is a boxed long.
		// payload[2] is a string.
		//
		// We can look for patterns in the bytecode to showcase this. For instance:
		//
		// ALOAD payload
		// ICONST_0
		// AALOAD
		// CHECKCAST java/lang/Integer
		// INVOKEVIRTUAL java/lang/Integer.intValue()I
		Map<Integer, FactKind> expected = new HashMap<>();
		Map<Integer, Integer> payloadSlots = payloadParameterSlots(target); // Map of local slots to Object[]
		for (AbstractInsnNode instruction : target.instructions) {
			// Skip if the insn can't be 'ALOAD payload'
			if (!(instruction instanceof VarInsnNode load) || load.getOpcode() != Opcodes.ALOAD)
				continue;

			// Must be an Object[] payload load.
			Integer parameterIndex = payloadSlots.get(load.var);
			if (parameterIndex == null)
				continue;

			// Skip forward to find the array index + array load.
			AbstractInsnNode cursor = load.getNext();
			if (cursor instanceof InsnNode dup && dup.getOpcode() == DUP)
				cursor = cursor.getNext();
			Integer index = integerConstant(cursor);
			if (index == null)
				continue;
			AbstractInsnNode arrayLoad = cursor.getNext();
			if (!(arrayLoad instanceof InsnNode) || arrayLoad.getOpcode() != AALOAD)
				continue;

			// Check for a cast to the intended type.
			AbstractInsnNode cast = arrayLoad.getNext();
			if (!(cast instanceof TypeInsnNode typeCast) || typeCast.getOpcode() != CHECKCAST)
				continue;

			// Based on the cast we can map Object[N] to the kinds of content.
			FactKind kind;
			if ("java/lang/Integer".equals(typeCast.desc)
					&& cast.getNext() instanceof MethodInsnNode call
					&& call.getOpcode() == INVOKEVIRTUAL && "intValue".equals(call.name) && "()I".equals(call.desc))
				kind = FactKind.INT;
			else if ("java/lang/Long".equals(typeCast.desc)
					&& cast.getNext() instanceof MethodInsnNode call
					&& call.getOpcode() == INVOKEVIRTUAL && "longValue".equals(call.name) && "()J".equals(call.desc))
				kind = FactKind.LONG;
			else if ("java/lang/String".equals(typeCast.desc))
				kind = FactKind.STRING;
			else
				continue;
			expected.putIfAbsent(index, kind);
		}
		return expected;
	}

	/**
	 * @param method
	 * 		Method to check.
	 *
	 * @return {@code true} when the method has a real body that could be rewritten.
	 */
	private static boolean isConcrete(@Nullable MethodNode method) {
		return method != null
				&& (method.access & (ACC_ABSTRACT | ACC_NATIVE)) == 0
				&& method.instructions != null
				&& method.instructions.size() > 0;
	}

	/**
	 * @param classNode
	 * 		Class to search.
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 *
	 * @return Declared method with the exact identity, or {@code null} when absent.
	 */
	@Nullable
	private static MethodNode findDeclaredMethod(@Nonnull ClassNode classNode,
	                                             @Nonnull String name,
	                                             @Nonnull String descriptor) {
		// TODO: Another thing that should be a util somewhere. Closest we have is AsmInsnUtil but thats for instructions...
		for (MethodNode method : classNode.methods)
			if (name.equals(method.name) && descriptor.equals(method.desc))
				return method;
		return null;
	}

	/**
	 * Applies collected facts to every method in the generated caller region.
	 *
	 * @param classes
	 * 		Working classes keyed by name.
	 * @param participants
	 * 		Names of classes in the generated caller region.
	 * @param facts
	 * 		Facts collected for this pass.
	 *
	 * @return {@code true} when at least one method changed.
	 */
	private boolean applyFacts(@Nonnull Map<String, WorkingClass> classes,
	                           @Nonnull Set<String> participants,
	                           @Nonnull Map<MethodKey, MethodFacts> facts) {
		boolean changed = false;
		for (String participant : participants) {
			WorkingClass workingClass = classes.get(participant);
			if (workingClass == null)
				continue;
			for (MethodNode method : workingClass.node.methods) {
				if (method.instructions == null || method.instructions.size() == 0)
					continue;

				// Must have parameter facts to apply.
				MethodFacts methodFacts = facts.get(new MethodKey(workingClass.node.name, method.name, method.desc));
				if (methodFacts == null)
					continue;

				// Both halves run unconditionally so a method can be seeded and rewritten in the same pass.
				boolean methodChanged = seedPrimitiveParameters(method, methodFacts) | rewritePayloadReads(method, methodFacts);
				if (methodChanged) {
					workingClass.changed = true;
					changed = true;
				}
			}
		}
		return changed;
	}

	/**
	 * Writes known primitive parameter values into the existing local slots at method entry.
	 * <p>
	 * The descriptor is never touched, so the new stores have to match the slots the JVM already assigns:
	 * <pre> {@code
	 *     // Before: the value is only known from callers.
	 *     ILOAD parameterValue
	 *     INVOKESTATIC Target.use (I)V
	 * }</pre>
	 * <pre>{@code
	 *     // After: the value is pinned to its known value without changing the signature.
	 *     ICONST_3
	 *     ISTORE parameterValue
	 *     ILOAD parameterValue
	 *     INVOKESTATIC Target.use (I)V
	 * </pre>
	 *
	 * @param method
	 * 		Method to seed.
	 * @param facts
	 * 		Facts collected for the method.
	 *
	 * @return {@code true} when at least one parameter store was inserted.
	 */
	private boolean seedPrimitiveParameters(@Nonnull MethodNode method, @Nonnull MethodFacts facts) {
		MethodKey key = new MethodKey(facts.owner, method.name, method.desc);
		Type[] parameters;
		try {
			parameters = Type.getArgumentTypes(method.desc);
		} catch (Throwable t) {
			return false;
		}

		// Generate instruction sequence that seeds the parameters with known constants.
		int slot = (method.access & ACC_STATIC) == 0 ? 1 : 0;
		InsnList stores = new InsnList();
		for (int index = 0; index < parameters.length; index++) {
			ParameterKey parameterKey = new ParameterKey(key, index);

			// Track seeds per parameter rather than per method, so a value discovered on a later pass can still be
			// seeded without duplicating the stores of parameters that were already handled.
			Type parameter = parameters[index];
			if (seededParameters.contains(parameterKey)) {
				slot += parameter.getSize();
				continue;
			}

			// We can only seed a parameter if we have a constant to store, and the parameter's local slot is valid.
			PrimitiveFact fact = facts.primitive(index);
			AbstractInsnNode constant = primitiveConstant(parameter, fact);
			if (constant != null && slot >= 0 && slot + parameter.getSize() <= method.maxLocals) {
				stores.add(constant);
				stores.add(AsmInsnUtil.createVarStore(slot, parameter));
				seededParameters.add(parameterKey);
			}
			slot += parameter.getSize();
		}
		if (stores.size() == 0)
			return false;

		// Insert the stores at the start of the method, before any other instructions.
		method.instructions.insertBefore(method.instructions.getFirst(), stores);
		return true;
	}

	/**
	 * Builds the constant instruction for a known primitive fact.
	 *
	 * @param parameterType
	 * 		Parameter type the constant must satisfy.
	 * @param fact
	 * 		Known fact for the parameter.
	 *
	 * @return Constant instruction, or {@code null} when the fact cannot be expressed for this parameter type.
	 */
	@Nullable
	private static AbstractInsnNode primitiveConstant(@Nonnull Type parameterType, @Nullable PrimitiveFact fact) {
		// Skip unknown or conflicting facts, since they cannot be expressed as a constant.
		if (fact == null || fact.kind == FactKind.UNKNOWN || fact.kind == FactKind.CONFLICT)
			return null;

		// Map supported parameter types to constant providing instructions.
		return switch (parameterType.getSort()) {
			case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT ->
					fact.kind == FactKind.INT ? intToInsn((Integer) fact.value) : null;
			case Type.LONG -> fact.kind == FactKind.LONG ? longToInsn((Long) fact.value) : null;
			case Type.OBJECT -> {
				if (STRING_TYPE.equals(parameterType) && fact.kind == FactKind.STRING)
					yield new LdcInsnNode(fact.value);
				if (fact.kind == FactKind.NULL)
					yield new InsnNode(ACONST_NULL);
				yield null;
			}
			case Type.ARRAY -> fact.kind == FactKind.NULL ? new InsnNode(ACONST_NULL) : null;
			default -> null;
		};
	}

	/**
	 * Replaces direct reads of known payload elements with the corresponding constants.
	 *
	 * @param method
	 * 		Method to rewrite.
	 * @param facts
	 * 		Facts collected for the method.
	 *
	 * @return {@code true} when at least one extraction was replaced.
	 */
	private static boolean rewritePayloadReads(@Nonnull MethodNode method, @Nonnull MethodFacts facts) {
		Map<Integer, Integer> payloadSlots = payloadParameterSlots(method);
		if (payloadSlots.isEmpty())
			return false;

		// If the payload is ever written to, a later read may not see the value the callers passed, so do nothing.
		if (hasPayloadMutation(method, payloadSlots.keySet()))
			return false;

		boolean changed = false;
		Map<AbstractInsnNode, ArrayFact> continuationFacts = new HashMap<>();
		AbstractInsnNode current = method.instructions.getFirst();
		while (current != null) {
			AbstractInsnNode next = current.getNext();
			ExtractionMatch match = null;
			ArrayFact array = null;

			// Match a read starting at a direct ALOAD of a payload parameter,
			// or a continuation from a DUP that carries the value of a previous read.
			if (current instanceof VarInsnNode load && load.getOpcode() == Opcodes.ALOAD) {
				Integer parameterIndex = payloadSlots.get(load.var);
				array = parameterIndex == null ? null : facts.array(parameterIndex);
				if (array != null && !array.unknown)
					match = matchPayloadRead(load, array);
			} else if (current instanceof InsnNode duplicate && duplicate.getOpcode() == DUP) {
				array = continuationFacts.get(current);
				if (array != null && !array.unknown)
					match = matchPayloadContinuation(duplicate, array);
			}

			// If a match was found, replace the range with the constant instructions.
			// Move forward to the instruction after the replaced range, and continue scanning.
			if (match != null) {
				AbstractInsnNode after = match.end.getNext();
				replaceRange(method.instructions, match.start, match.end, match.replacement);
				if (AsmInsnUtil.isWideConstant(match.replacement))
					method.maxStack++;
				if (match.continuation) {
					AbstractInsnNode following = after == null ? null : after.getNext();
					if (following instanceof InsnNode duplicate && duplicate.getOpcode() == DUP)
						continuationFacts.put(duplicate, array);
				}
				changed = true;
				current = after;
				continue;
			}
			current = next;
		}
		return changed;
	}

	/**
	 * Detects writes that mutate a payload array instead of an unrelated array.
	 *
	 * @param method
	 * 		Method to inspect.
	 * @param payloadSlots
	 * 		Local slots holding payload parameters.
	 *
	 * @return {@code true} when a payload array may be written to.
	 */
	private static boolean hasPayloadMutation(@Nonnull MethodNode method, @Nonnull Set<Integer> payloadSlots) {
		if (payloadSlots.isEmpty())
			return false;

		// Source tracking is used instead of counting AASTORE instructions, because storing into a freshly allocated
		// array is normal and only a write through the payload alias actually invalidates our facts.
		Frame<SourceValue>[] frames;
		try {
			frames = new Analyzer<>(new SourceInterpreter()).analyze(method.name, method);
		} catch (Throwable t) {
			// Without a reliable alias picture we cannot prove the payload is safe to rewrite.
			return true;
		}

		for (int index = 0; index < method.instructions.size(); index++) {
			if (method.instructions.get(index).getOpcode() != Opcodes.AASTORE || index >= frames.length)
				continue;

			// For the AASTORE we expect a frame that has:
			//  - The array reference three values down the stack.
			//  - The index two values down the stack.
			//  - The value to store on top of the stack.
			Frame<SourceValue> frame = frames[index];
			if (frame == null || frame.getStackSize() < 3)
				continue;

			// If the array reference was loaded from a local slot that is a payload parameter, this is a mutation we cannot rewrite.
			SourceValue arraySource = frame.getStack(frame.getStackSize() - 3);
			for (AbstractInsnNode source : arraySource.insns)
				if (source instanceof VarInsnNode load
						&& load.getOpcode() == Opcodes.ALOAD
						&& payloadSlots.contains(load.var))
					return true;
		}
		return false;
	}

	/**
	 * @param method
	 * 		Method to inspect.
	 *
	 * @return {@code Object[]} payload slots keyed by local slot, empty when the method takes no payload.
	 */
	@Nonnull
	private static Map<Integer, Integer> payloadParameterSlots(@Nonnull MethodNode method) {
		Type[] parameters;
		try {
			parameters = Type.getArgumentTypes(method.desc);
		} catch (Throwable t) {
			// TODO: Returning empty is probably not ideal here, but realistically shouldn't happen.
			//  - Null and handle in usage context? Or is that just bloat?
			return Collections.emptyMap();
		}

		// Check parameters for Object[] and map from local var slots to parameter indices.
		Map<Integer, Integer> slots = new HashMap<>();
		int slot = (method.access & ACC_STATIC) == 0 ? 1 : 0;
		for (int index = 0; index < parameters.length; index++) {
			Type parameter = parameters[index];
			if (OBJECT_ARRAY_TYPE.equals(parameter))
				slots.put(slot, index);
			slot += parameter.getSize();
		}
		return slots;
	}

	/**
	 * Matches a payload extraction that starts at a direct {@code ALOAD} of a payload parameter.
	 *
	 * @param load
	 * 		Local load of the payload parameter.
	 * @param array
	 * 		Known contents of the payload.
	 *
	 * @return Match describing the range to replace, or {@code null} when the shape is unsupported.
	 */
	@Nullable
	private static ExtractionMatch matchPayloadRead(@Nonnull VarInsnNode load,
	                                                @Nonnull ArrayFact array) {
		AbstractInsnNode cursor = load.getNext();
		boolean preserveDuplicate = cursor instanceof InsnNode dup && dup.getOpcode() == DUP;
		if (preserveDuplicate)
			cursor = cursor.getNext();

		// Starting at DUP removes the extra copy but leaves the original ALOAD value for the existing suffix, often POP.
		AbstractInsnNode start = preserveDuplicate ? load.getNext() : load;
		return matchPayloadReadTail(cursor, start, array, preserveDuplicate);
	}

	/**
	 * Matches a payload extraction that continues from a duplicated array reference.
	 *
	 * @param duplicate
	 * 		Duplication of the payload reference.
	 * @param array
	 * 		Known contents of the payload.
	 *
	 * @return Match describing the range to replace, or {@code null} when the shape is unsupported.
	 */
	@Nullable
	private static ExtractionMatch matchPayloadContinuation(@Nonnull InsnNode duplicate,
	                                                        @Nonnull ArrayFact array) {
		return matchPayloadReadTail(duplicate.getNext(), duplicate, array, true);
	}

	/**
	 * Matches the extraction body shared by direct loads and duplicated continuations.
	 *
	 * @param cursor
	 * 		Instruction expected to be the payload index.
	 * @param start
	 * 		First instruction of the range to replace.
	 * @param array
	 * 		Known contents of the payload.
	 * @param continuation
	 * 		Whether a following duplicate may continue reading the same payload.
	 *
	 * @return Match describing the range to replace, or {@code null} when the shape is unsupported.
	 */
	@Nullable
	private static ExtractionMatch matchPayloadReadTail(@Nullable AbstractInsnNode cursor,
	                                                    @Nonnull AbstractInsnNode start,
	                                                    @Nonnull ArrayFact array,
	                                                    boolean continuation) {
		// Only immediate integer indexes are supported.
		// Computed or aliased indexes stay untouched.
		Integer index = integerConstant(cursor);
		if (index == null || index < 0)
			return null;

		// If the array contents at the index are unknown, we cannot replace anything.
		PrimitiveFact fact = array.elements.get(index);
		if (fact == null || fact.kind == FactKind.UNKNOWN || fact.kind == FactKind.CONFLICT)
			return null;

		// The next instruction must be an array load.
		AbstractInsnNode arrayLoad = cursor.getNext();
		if (!(arrayLoad instanceof InsnNode) || arrayLoad.getOpcode() != AALOAD)
			return null;

		// The patterns observed in ZKM are always a direct cast to the wrapper type, or a bare AALOAD for strings and null.
		// The cast must match the wrapper type that the fact actually carries, otherwise the site is doing something else with the element and must be left alone.
		AbstractInsnNode terminal = arrayLoad;
		AbstractInsnNode following = terminal.getNext();
		AbstractInsnNode replacement;
		if (following instanceof TypeInsnNode cast && cast.getOpcode() == CHECKCAST) {
			if ("java/lang/Long".equals(cast.desc)) {
				AbstractInsnNode unbox = cast.getNext();
				if (!(unbox instanceof MethodInsnNode call)
						|| call.getOpcode() != INVOKEVIRTUAL
						|| !"java/lang/Long".equals(call.owner)
						|| !"longValue".equals(call.name)
						|| !"()J".equals(call.desc)
						|| fact.kind != FactKind.LONG)
					return null;
				terminal = unbox;
				replacement = longToInsn((Long) fact.value);
			} else if ("java/lang/Integer".equals(cast.desc)) {
				AbstractInsnNode unbox = cast.getNext();
				if (!(unbox instanceof MethodInsnNode call)
						|| call.getOpcode() != INVOKEVIRTUAL
						|| !"java/lang/Integer".equals(call.owner)
						|| !"intValue".equals(call.name)
						|| !"()I".equals(call.desc)
						|| fact.kind != FactKind.INT)
					return null;
				terminal = unbox;
				replacement = intToInsn((Integer) fact.value);
			} else if ("java/lang/String".equals(cast.desc)
					&& (fact.kind == FactKind.STRING || fact.kind == FactKind.NULL)) {
				terminal = cast;
				replacement = fact.kind == FactKind.STRING ? new LdcInsnNode(fact.value) : new InsnNode(ACONST_NULL);
			} else {
				return null;
			}
		} else if (fact.kind == FactKind.NULL) {
			replacement = new InsnNode(ACONST_NULL);
		} else if (fact.kind == FactKind.STRING) {
			replacement = new LdcInsnNode(fact.value);
		} else {
			return null;
		}
		return new ExtractionMatch(start, terminal, replacement, continuation);
	}

	/**
	 * Removes a range of instructions and inserts one replacement in its place.
	 *
	 * @param instructions
	 * 		Instruction list to modify.
	 * @param start
	 * 		First instruction of the removed range.
	 * @param end
	 * 		Last instruction of the removed range.
	 * @param replacement
	 * 		Instruction inserted where the range was.
	 */
	private static void replaceRange(@Nonnull InsnList instructions,
	                                 @Nonnull AbstractInsnNode start,
	                                 @Nonnull AbstractInsnNode end,
	                                 @Nonnull AbstractInsnNode replacement) {
		// Remove the old nodes first so the saved successor remains a stable insertion point while the list is detached.
		AbstractInsnNode after = end.getNext();
		AbstractInsnNode current = start;
		while (current != after) {
			AbstractInsnNode next = current.getNext();
			instructions.remove(current);
			current = next;
		}
		if (after == null)
			instructions.add(replacement);
		else
			instructions.insertBefore(after, replacement);
	}

	/**
	 * @param instruction
	 * 		Instruction to inspect.
	 *
	 * @return Integer value pushed by the instruction, or {@code null} when it is not an immediate integer.
	 */
	@Nullable
	private static Integer integerConstant(@Nullable AbstractInsnNode instruction) {
		if (instruction == null)
			return null;
		return switch (instruction.getOpcode()) {
			case Opcodes.ICONST_M1 -> -1;
			case Opcodes.ICONST_0 -> 0;
			case Opcodes.ICONST_1 -> 1;
			case Opcodes.ICONST_2 -> 2;
			case Opcodes.ICONST_3 -> 3;
			case Opcodes.ICONST_4 -> 4;
			case Opcodes.ICONST_5 -> 5;
			case Opcodes.BIPUSH, Opcodes.SIPUSH -> ((IntInsnNode) instruction).operand;
			case Opcodes.LDC -> {
				Object constant = ((LdcInsnNode) instruction).cst;
				yield constant instanceof Integer value ? value : null;
			}
			default -> null;
		};
	}

	/**
	 * Reduces an analyzed value to the small set of constants this transformer is willing to emit.
	 *
	 * @param input
	 * 		Analyzed value, or {@code null} when the caller could not be analyzed.
	 *
	 * @return Normalized fact, never {@code null}.
	 */
	@Nonnull
	private static PrimitiveFact normalizePrimitive(@Nullable ReValue input) {
		if (input == null)
			return PrimitiveFact.UNKNOWN;

		// Unmap any instance wrappers to get the real value, since we only care about the actual content.
		if (input instanceof InstancedObjectValue<?> instanced && instanced.getRealInstance() != null) {
			ReValue unmapped = instanced.unmap();
			if (unmapped != input)
				return normalizePrimitive(unmapped);
		}

		// Null
		if (input instanceof ObjectValue object && object.isNull())
			return PrimitiveFact.NULL;

		// Other primitives when value is known, otherwise unknown.
		if (input instanceof IntValue integer)
			return integer.value().isPresent() ? PrimitiveFact.integer(integer.value().getAsInt()) : PrimitiveFact.UNKNOWN;
		if (input instanceof LongValue longValue)
			return longValue.value().isPresent() ? PrimitiveFact.longValue(longValue.value().getAsLong()) : PrimitiveFact.UNKNOWN;
		if (input instanceof StringValue string)
			return string.getText().isPresent() ? PrimitiveFact.string(string.getText().get()) : PrimitiveFact.UNKNOWN;

		// Boxed types when value is known, otherwise unknown.
		if (input instanceof ObjectValueBoxImpl<?> box) {
			if (!box.hasKnownValue())
				return PrimitiveFact.UNKNOWN;
			Object value;
			try {
				value = box.unbox();
			} catch (RuntimeException ignored) {
				return PrimitiveFact.UNKNOWN;
			}
			if (value instanceof Integer integer)
				return PrimitiveFact.integer(integer);
			if (value instanceof Long longValue)
				return PrimitiveFact.longValue(longValue);
		}

		return PrimitiveFact.UNKNOWN;
	}

	/**
	 * Combines two facts from different callers.
	 * <p>
	 * Two callers passing {@code 5} agree and stay rewritable, while {@code 5} and {@code 6} conflict and the
	 * affected use is left untouched. Anything unknown always erases an optimistic value, so a caller we could
	 * not analyze can never be silently replaced by a value observed somewhere else.
	 *
	 * @param current
	 * 		Fact accumulated so far, or {@code null} when nothing has been recorded yet.
	 * @param incoming
	 * 		Fact being merged in.
	 *
	 * @return Merged fact, never {@code null}.
	 */
	@Nonnull
	private static PrimitiveFact merge(@Nullable PrimitiveFact current, @Nonnull PrimitiveFact incoming) {
		if (current == null)
			return incoming;
		if (current.kind == FactKind.CONFLICT || incoming.kind == FactKind.CONFLICT)
			return PrimitiveFact.CONFLICT;
		if (current.kind == FactKind.UNKNOWN || incoming.kind == FactKind.UNKNOWN)
			return current.kind == incoming.kind ? PrimitiveFact.UNKNOWN : PrimitiveFact.CONFLICT;
		return current.kind == incoming.kind && Objects.equals(current.value, incoming.value) ? current : PrimitiveFact.CONFLICT;
	}

	/**
	 * Recorded node identity and content marker used to detect outside edits between applier passes.
	 *
	 * @param node
	 * 		Class node as seen when the batch was recorded.
	 * @param fingerprint
	 * 		Content hash of that node.
	 */
	private record NodeState(@Nonnull ClassNode node, int fingerprint) {}

	/**
	 * Mutable working copy of one class, committed only when something actually changed.
	 */
	private static final class WorkingClass {
		private final JvmClassBundle bundle;
		private final JvmClassInfo info;
		private final ClassNode node;
		private boolean changed;

		private WorkingClass(@Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo info, @Nonnull ClassNode node) {
			this.bundle = bundle;
			this.info = info;
			this.node = node;
		}
	}

	/**
	 * Facts collected for one target method, keyed by explicit parameter index.
	 */
	private static final class MethodFacts {
		private final String owner;
		private final Map<Integer, ParameterFact> parameters = new HashMap<>();

		private MethodFacts(@Nonnull String owner) {
			this.owner = owner;
		}

		private void merge(int index, @Nonnull Type formalType, @Nullable ReValue value) {
			ParameterFact parameter = parameters.computeIfAbsent(index, ignored -> new ParameterFact());
			// Payload parameters are tracked element-by-element, while everything else collapses to one primitive.
			if (OBJECT_ARRAY_TYPE.equals(formalType))
				parameter.mergeArray(value);
			else
				parameter.merge(normalizePrimitive(value));
		}

		@Nullable
		private PrimitiveFact primitive(int index) {
			ParameterFact fact = parameters.get(index);
			return fact == null ? null : fact.primitive;
		}

		@Nullable
		private ArrayFact array(int index) {
			ParameterFact fact = parameters.get(index);
			return fact == null ? null : fact.array;
		}
	}

	/**
	 * Facts for one explicit parameter, holding either a primitive value or payload element values.
	 */
	private static final class ParameterFact {
		@Nullable
		private PrimitiveFact primitive;
		@Nullable
		private ArrayFact array;

		private void merge(@Nullable PrimitiveFact value) {
			if (value != null)
				primitive = ZkmParameterUnpackingTransformer.merge(primitive, value);
		}

		private void mergeArray(@Nullable ReValue value) {
			if (array == null)
				array = new ArrayFact();
			array.merge(value);
		}
	}

	/**
	 * Known contents of one payload array.
	 */
	private static final class ArrayFact {
		private final Map<Integer, PrimitiveFact> elements = new HashMap<>();
		private boolean unknown;
		private Integer length;

		private void merge(@Nullable ReValue value) {
			if (unknown)
				return;

			// Unmap host-backed arrays first so their elements are real JVM values rather than host objects.
			if (value instanceof InstancedObjectValue<?> instanced && instanced.getRealInstance() != null)
				value = instanced.unmap();

			// If the value is not an array, or is null, or has no known length, we cannot trust any of its elements.
			if (!(value instanceof ArrayValue arrayValue) || arrayValue.isNull()) {
				unknown = true;
				return;
			}
			OptionalInt lengthValue = arrayValue.getFirstDimensionLength();
			if (lengthValue.isEmpty() || lengthValue.getAsInt() < 0) {
				unknown = true;
				return;
			}

			// Callers that disagree about how many elements they send give us no safe indices to rewrite.
			int currentLength = lengthValue.getAsInt();
			if (length != null && length != currentLength) {
				unknown = true;
				return;
			}

			// Merge each element with the existing fact, or create a new fact if this is the first caller.
			length = currentLength;
			for (int index = 0; index < currentLength; index++)
				elements.put(index, ZkmParameterUnpackingTransformer.merge(elements.get(index), normalizePrimitive(arrayValue.getValue(index))));
		}
	}

	/**
	 * Kinds of value this transformer is willing to materialize.
	 */
	private enum FactKind {
		/** Known integer constant. */
		INT,
		/** Known long constant. */
		LONG,
		/** Known string constant. */
		STRING,
		/** Canonical null reference. */
		NULL,
		/** No usable value, either unsupported or never observed. */
		UNKNOWN,
		/** Callers supplied values that are not all equal. */
		CONFLICT
	}

	/**
	 * Normalized constant produced from an analyzed value.
	 *
	 * @param kind
	 * 		Kind of the value.
	 * @param value
	 * 		Concrete value for known kinds, or {@code null} otherwise.
	 */
	private record PrimitiveFact(@Nonnull FactKind kind, @Nullable Object value) {
		private static final PrimitiveFact UNKNOWN = new PrimitiveFact(FactKind.UNKNOWN, null);
		private static final PrimitiveFact CONFLICT = new PrimitiveFact(FactKind.CONFLICT, null);
		private static final PrimitiveFact NULL = new PrimitiveFact(FactKind.NULL, null);

		@Nonnull
		private static PrimitiveFact integer(int value) {
			return new PrimitiveFact(FactKind.INT, value);
		}

		@Nonnull
		private static PrimitiveFact longValue(long value) {
			return new PrimitiveFact(FactKind.LONG, value);
		}

		@Nonnull
		private static PrimitiveFact string(@Nonnull String value) {
			return new PrimitiveFact(FactKind.STRING, value);
		}
	}

	/**
	 * Workspace method identity used to key collected facts.
	 *
	 * @param owner
	 * 		Internal owner name.
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 */
	private record MethodKey(@Nonnull String owner, @Nonnull String name,
	                         @Nonnull String descriptor) implements Comparable<MethodKey> {
		@Override
		public int compareTo(MethodKey o) {
			int cmp = owner.compareTo(o.owner);
			if (cmp != 0) return cmp;
			cmp = name.compareTo(o.name);
			if (cmp != 0) return cmp;
			return descriptor.compareTo(o.descriptor);
		}
	}

	/**
	 * Identity of one seeded formal parameter.
	 *
	 * @param method
	 * 		Owning method identity.
	 * @param index
	 * 		Explicit parameter index.
	 */
	private record ParameterKey(@Nonnull MethodKey method, int index) {}

	/**
	 * One direct payload extraction range that can be replaced by a constant.
	 *
	 * @param start
	 * 		First instruction of the replaced range.
	 * @param end
	 * 		Last instruction of the replaced range.
	 * @param replacement
	 * 		Constant instruction inserted in place of the range.
	 * @param continuation
	 * 		Whether a following duplicate may continue reading the same payload.
	 */
	private record ExtractionMatch(@Nonnull AbstractInsnNode start,
	                               @Nonnull AbstractInsnNode end,
	                               @Nonnull AbstractInsnNode replacement,
	                               boolean continuation) {}
}