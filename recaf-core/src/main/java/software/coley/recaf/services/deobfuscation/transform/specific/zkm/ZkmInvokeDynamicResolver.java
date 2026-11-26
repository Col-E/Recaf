package software.coley.recaf.services.deobfuscation.transform.specific.zkm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.behavior.PriorityKeys;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.deobfuscation.transform.generic.InvokeDynamicInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.InvokeDynamicResolver;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.analysis.eval.EvaluationResult;
import software.coley.recaf.util.analysis.eval.EvaluationYieldResult;
import software.coley.recaf.util.analysis.eval.Evaluator;
import software.coley.recaf.util.analysis.eval.FieldCache;
import software.coley.recaf.util.analysis.eval.FieldCacheManager;
import software.coley.recaf.util.analysis.eval.InstancedObjectValue;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.objectweb.asm.Opcodes.*;
import static software.coley.recaf.util.AsmInsnUtil.getPreviousInsn;
import static software.coley.recaf.util.Types.isPrimitive;

/**
 * Resolves ZKM's metadata-backed {@code invokedynamic} sites.
 * <p>
 * ZKM stores member descriptors in arrays initialized by generated helper methods. This resolver recognizes those
 * arrays and the index helper structurally, evaluates only the bounded helper through the evaluator, and resolves the
 * resulting member against workspace metadata.
 * <p>
 * Tested on samples from:
 * <ul>
 *     <li>ZKM 26.0.0</li>
 * </ul>
 *
 * @author Matt Coley
 */
@Dependent
public class ZkmInvokeDynamicResolver implements InvokeDynamicResolver {
	/** Descriptor used by ZKM's per-class bootstrap method. */
	public static final String BOOTSTRAP_DESCRIPTOR =
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";
	private static final int DEFAULT_MAX_STEPS = 40_000;
	private static final String RESOLVER_DESCRIPTOR =
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;"
					+ "Ljava/lang/String;Ljava/lang/invoke/MethodType;JJ)Ljava/lang/invoke/MethodHandle;";
	private static final String RESOLVER_DESCRIPTOR_ALT =
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;"
					+ "Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/MethodHandle;";

	private final InheritanceGraphService graphService;
	private volatile InheritanceGraph inheritanceGraph;
	private final ConcurrentMap<String, Optional<Metadata>> metadataCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, ClassState> stateCache = new ConcurrentHashMap<>();

	@Inject
	public ZkmInvokeDynamicResolver(@Nonnull InheritanceGraphService graphService) {
		this.graphService = graphService;
	}

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) throws TransformationException {
		inheritanceGraph = graphService.getOrCreateInheritanceGraph(workspace);
		metadataCache.clear();
		stateCache.clear();
	}

	@Override
	public int getPriority() {
		// We want this to run before other more generic resolvers that may claim the site and fail to resolve it.
		return PriorityKeys.EARLIEST;
	}

	@Nullable
	@Override
	public InvokeDynamicResolver.ResolvedInvokeDynamic resolve(@Nonnull JvmTransformerContext context,
	                                                           @Nonnull Workspace workspace,
	                                                           @Nonnull ClassNode classNode,
	                                                           @Nonnull MethodNode method,
	                                                           @Nonnull InvokeDynamicInsnNode instruction,
	                                                           @Nonnull Frame<ReValue> frame) {
		PreparedSite site = prepareSite(context, workspace, classNode, instruction);
		if (site == null)
			return null;
		long[] metadataKeys = keysFromFrame(frame, site.metadataKeyCount);
		if (metadataKeys == null)
			return null;
		return resolveWithKeys(context, workspace, classNode, instruction, site, metadataKeys);
	}

	@Nullable
	@Override
	public InvokeDynamicResolver.ResolvedInvokeDynamic resolveWithoutFrame(@Nonnull JvmTransformerContext context,
	                                                                       @Nonnull Workspace workspace,
	                                                                       @Nonnull ClassNode classNode,
	                                                                       @Nonnull MethodNode method,
	                                                                       @Nonnull InvokeDynamicInsnNode instruction) {
		PreparedSite site = prepareSite(context, workspace, classNode, instruction);
		if (site == null)
			return null;
		long[] metadataKeys = keysFromInstructions(method, instruction, site.metadataKeyCount);
		if (metadataKeys == null)
			return null;
		return resolveWithKeys(context, workspace, classNode, instruction, site, metadataKeys);
	}

	@Nullable
	private PreparedSite prepareSite(@Nonnull JvmTransformerContext context,
	                                 @Nonnull Workspace workspace,
	                                 @Nonnull ClassNode classNode,
	                                 @Nonnull InvokeDynamicInsnNode instruction) {
		// Must look like a ZKM bootstrap and point at a class in the current workspace.
		if (!isZkmSite(instruction))
			return null;

		// Extract the bootstrap resolver owner and find its class node.
		// If the owner is not in the workspace, we cannot resolve it.
		String helperOwner = instruction.bsm.getOwner();
		ClassPathNode helperPath = workspace.findJvmClass(true, helperOwner);
		if (helperPath == null)
			return null;
		JvmClassBundle helperBundle = helperPath.getValueOfType(JvmClassBundle.class);
		if (helperBundle == null)
			return null;
		JvmClassInfo helperInfo = helperPath.getValue().asJvmClass();

		// Keep class-local sites tied to the node currently being transformed, while shared sites use the helper owner node.
		// Some ZKM configurations will put the index helper components in the same class as the call-site, while others will put them in a shared helper class.
		//
		// Generally the structure you're looking for is:
		//  private static Object[] objectCache;
		//  private static String[] descriptorCache;
		//   ...
		//  private static int indexHelper(long key0, long key1) { ... }
		//  private static MethodHandle resolve(Lookup, MutableCallSite, String, MethodType, long, long) { ... }
		ClassNode helperNode = helperOwner.equals(classNode.name) ? classNode : context.getNode(helperBundle, helperInfo);

		// Parse and validate the call-site metadata suffix before either key source is considered.
		Type[] callSiteArguments;
		try {
			callSiteArguments = Type.getArgumentTypes(instruction.desc);
		} catch (RuntimeException ignored) {
			return null;
		}

		// Discover generated helper fields once per bootstrap owner.
		Metadata metadata = metadataCache.computeIfAbsent(helperOwner, _ -> Optional.ofNullable(discoverMetadata(helperNode))).orElse(null);
		if (metadata == null)
			return null;

		// Sanity check that the call-site has enough trailing long arguments to match the discovered metadata keys.
		int metadataKeyCount = metadata.metadataKeyCount;
		if (callSiteArguments.length < metadataKeyCount)
			return null;
		for (int index = callSiteArguments.length - metadataKeyCount; index < callSiteArguments.length; index++)
			if (!Type.LONG_TYPE.equals(callSiteArguments[index]))
				return null;
		return new PreparedSite(helperOwner, helperNode, metadata, metadataKeyCount);
	}

	@Nullable
	private static long[] keysFromFrame(@Nonnull Frame<ReValue> frame, int metadataKeyCount) {
		int stackSize = frame.getStackSize();
		if (stackSize < metadataKeyCount)
			return null;
		long[] metadataKeys = new long[metadataKeyCount];
		for (int index = 0; index < metadataKeyCount; index++) {
			ReValue key = frame.getStack(stackSize - metadataKeyCount + index);
			if (!(key instanceof LongValue longKey))
				return null;
			OptionalLong value = longKey.value();
			if (value.isEmpty())
				return null;
			metadataKeys[index] = value.getAsLong();
		}
		return metadataKeys;
	}

	@Nullable
	private static long[] keysFromInstructions(@Nonnull MethodNode method,
	                                           @Nonnull InvokeDynamicInsnNode instruction,
	                                           int metadataKeyCount) {
		long[] metadataKeys = new long[metadataKeyCount];
		AbstractInsnNode current = getPreviousInsn(instruction);
		for (int index = metadataKeyCount - 1; index >= 0; index--) {
			Long key = longConstant(current);
			if (key == null && current instanceof VarInsnNode load && load.getOpcode() == LLOAD)
				key = findLongLocalConstant(load.var, load);
			if (key == null)
				return null;
			metadataKeys[index] = key;
			current = getPreviousInsn(current);
		}
		return metadataKeys;
	}

	@Nullable
	private static Long findLongLocalConstant(int slot, @Nonnull AbstractInsnNode load) {
		Long candidate = null;
		AbstractInsnNode current = getPreviousInsn(load);
		while (current != null) {
			if (current instanceof VarInsnNode store && store.getOpcode() == LSTORE && store.var == slot) {
				Long literal = longConstant(getPreviousInsn(store));
				if (literal == null || candidate != null)
					return null;
				candidate = literal;
			}
			current = getPreviousInsn(current);
		}
		return candidate;
	}

	@Nullable
	private static Long longConstant(@Nullable AbstractInsnNode instruction) {
		if (instruction == null)
			return null;
		return switch (instruction.getOpcode()) {
			case LCONST_0 -> 0L;
			case LCONST_1 -> 1L;
			case LDC -> instruction instanceof LdcInsnNode ldc && ldc.cst instanceof Long value ? value : null;
			default -> null;
		};
	}

	@Nullable
	private InvokeDynamicResolver.ResolvedInvokeDynamic resolveWithKeys(@Nonnull JvmTransformerContext context,
	                                                                    @Nonnull Workspace workspace,
	                                                                    @Nonnull ClassNode classNode,
	                                                                    @Nonnull InvokeDynamicInsnNode instruction,
	                                                                    @Nonnull PreparedSite site,
	                                                                    @Nonnull long[] metadataKeys) {
		// Decode the metadata keys into a member descriptor using the discovered helper component.
		InheritanceGraph graph = inheritanceGraph;
		if (graph == null)
			graph = graphService.getOrCreateInheritanceGraph(workspace);
		ClassState state = stateCache.computeIfAbsent(site.helperOwner, ignored -> new ClassState(site.helperNode, site.metadata));
		DecodedMember decoded;
		try {
			int maxSteps = Math.max(1, context.getParameters().getInt(InvokeDynamicInliningTransformer.KEY_MAX_STEPS, DEFAULT_MAX_STEPS));
			decoded = state.decode(context, workspace, graph, maxSteps, instruction.name, metadataKeys);
		} catch (Exception ignored) {
			return null;
		}
		if (decoded == null)
			return null;

		// Resolve only declared workspace metadata and enforce lookup access before emitting a handle.
		return resolveMember(workspace, graph, classNode.name, decoded, site.metadata.operationCodes, site.metadataKeyCount);
	}

	/**
	 * Checks whether an instruction uses ZKM's bootstrap shape.
	 *
	 * @param instruction
	 * 		Dynamic instruction to inspect.
	 *
	 * @return {@code true} when the bootstrap has a non-empty owner and ZKM's descriptor.
	 */
	public static boolean isZkmSite(@Nonnull InvokeDynamicInsnNode instruction) {
		if (instruction.bsm == null || !BOOTSTRAP_DESCRIPTOR.equals(instruction.bsm.getDesc()))
			return false;
		String owner = instruction.bsm.getOwner();
		return owner != null && !owner.isEmpty();
	}

	/**
	 * @param classNode
	 * 		Class to inspect for ZKM metadata helpers.
	 *
	 * @return Discovered metadata, or {@code null} when the class does not appear to be a ZKM-generated class.
	 */
	@Nullable
	private static Metadata discoverMetadata(@Nonnull ClassNode classNode) {
		for (MethodNode method : classNode.methods) {
			// The index helper is a static method with one or two long arguments and returns the decoded cache slot.
			if ((method.access & ACC_STATIC) == 0 || !isIndexHelper(classNode, method))
				continue;
			Type[] helperArguments;
			try {
				helperArguments = Type.getArgumentTypes(method.desc);
			} catch (Throwable ignored) {
				continue;
			}

			// The index helper reads two static arrays, one for object values and one for descriptor values.
			Set<FieldKey> objectFields = new LinkedHashSet<>();
			Set<FieldKey> descriptorFields = new LinkedHashSet<>();
			collectCacheFields(classNode, method, objectFields, descriptorFields);
			if (objectFields.size() != 1 || descriptorFields.size() != 1)
				continue;

			// The index helper is used by a static initializer to populate the two arrays, so find that method.
			FieldKey objectField = objectFields.iterator().next();
			FieldKey descriptorField = descriptorFields.iterator().next();
			MethodNode seed = findSeedMethod(classNode, objectField, descriptorField);
			if (seed == null)
				continue;

			// The seed method is the only place where the arrays are populated,
			// so we can discover the class types and operation codes from it.
			Map<Integer, Type> classTypes = discoverClassTypes(seed);
			OperationCodes operationCodes = discoverOperationCodes(classNode);
			return new Metadata(new MethodKey(method.name, method.desc), helperArguments.length, objectField, descriptorField,
					new MethodKey(seed.name, seed.desc), classTypes, operationCodes);
		}
		return null;
	}

	/**
	 * Discovers the generated members that make up this class's ZKM helper component.
	 *
	 * @param classNode
	 * 		Class to inspect.
	 *
	 * @return Structural helper metadata, or {@code null} when the class does not expose a complete helper component.
	 *
	 * @see ZkmDecryptionCleanupTransformer
	 */
	@Nullable
	static CleanupMetadata discoverCleanupMetadata(@Nonnull ClassNode classNode) {
		// Get the index/seed helper methods and object/descriptor cache array fields.
		Metadata metadata = discoverMetadata(classNode);
		if (metadata == null)
			return discoverResidualCleanupMetadata(classNode);

		// Build the set of methods that are part of the helper component.
		Set<MethodKey> helperMethods = new HashSet<>();
		helperMethods.add(metadata.indexMethod);
		helperMethods.add(metadata.seedMethod);
		for (MethodNode method : classNode.methods) {
			boolean privateStatic = (method.access & (ACC_PRIVATE | ACC_STATIC)) == (ACC_PRIVATE | ACC_STATIC);
			boolean sharedEntry = BOOTSTRAP_DESCRIPTOR.equals(method.desc)
					|| isResolverDescriptor(method.desc)
					|| isSpreadBootstrap(method);
			if (!privateStatic && !sharedEntry)
				continue;
			if (sharedEntry || isStringDecoder(method) || isNumericDecoder(method))
				helperMethods.add(new MethodKey(method.name, method.desc));
		}

		// Follow calls between generated static methods so shared helpers include their public lookup adapters too.
		boolean changed;
		do {
			changed = false;
			for (MethodKey helper : List.copyOf(helperMethods)) {
				MethodNode method = findMethod(classNode, helper);
				if (method == null || method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions) {
					if (!(instruction instanceof MethodInsnNode call) || !classNode.name.equals(call.owner))
						continue;
					MethodNode target = findMethod(classNode, new MethodKey(call.name, call.desc));
					if (target == null || (target.access & ACC_STATIC) == 0)
						continue;
					changed |= helperMethods.add(new MethodKey(target.name, target.desc));
				}
			}
		} while (changed);

		// Build the set of fields that are part of the helper component.
		Set<FieldKey> helperFields = new HashSet<>();
		Set<FieldKey> stateFields = new HashSet<>();
		helperFields.add(metadata.objectField);
		helperFields.add(metadata.descriptorField);
		for (FieldNode field : classNode.fields) {
			FieldKey key = new FieldKey(classNode.name, field.name, field.desc);
			if (isPrivateHelperStorage(field) || isInterfaceHelperStorage(classNode, field))
				helperFields.add(key);
			else if ((field.access & ACC_STATIC) != 0
					&& (field.access & ACC_FINAL) == 0
					&& isPrimitive(field.desc))
				// Mutable static primitive fields can be part of ZKM's cross-class control state.
				stateFields.add(key);
		}

		// For all the helper methods, find all the fields they reference and classify them as either
		// part of the helper component or part of the control state.
		for (MethodKey helper : helperMethods) {
			MethodNode method = findMethod(classNode, helper);
			if (method == null || method.instructions == null)
				continue;
			for (AbstractInsnNode instruction : method.instructions) {
				if (!(instruction instanceof FieldInsnNode field) || !classNode.name.equals(field.owner))
					continue;

				FieldKey key = new FieldKey(field.owner, field.name, field.desc);
				FieldNode declared = findField(classNode, field.name, field.desc);
				if (declared == null)
					continue;

				if (isPrimitive(declared.desc))
					// Helper-referenced primitive fields are part of the component's control state.
					stateFields.add(key);
				else if (isPrivateHelperStorage(declared) || isInterfaceHelperStorage(classNode, declared))
					// Helper-referenced object fields are part of the component's helper storage.
					helperFields.add(key);
			}
		}
		return new CleanupMetadata(Set.copyOf(helperMethods), Set.copyOf(helperFields), Set.copyOf(stateFields),
				metadata.objectField, metadata.descriptorField);
	}

	/**
	 * Fallback variant of {@link #discoverMetadata(ClassNode)}.
	 * <p>
	 * It's a bit less strict and will classify any private static methods that look like ZKM helpers,
	 * even if they don't have the index/seed methods or the object/descriptor arrays.
	 *
	 * @param classNode
	 * 		Class to inspect.
	 *
	 * @return Structural helper metadata, or {@code null} when the class does not expose a complete helper component.
	 */
	@Nullable
	private static CleanupMetadata discoverResidualCleanupMetadata(@Nonnull ClassNode classNode) {
		Set<MethodKey> helperMethods = new HashSet<>();
		Set<FieldKey> referencedArrayFields = new HashSet<>();
		for (MethodNode method : classNode.methods) {
			// Skip non-private static methods, since ZKM's helpers are all private static.
			if ((method.access & (ACC_PRIVATE | ACC_STATIC)) != (ACC_PRIVATE | ACC_STATIC))
				continue;

			// Add any private static methods that look like ZKM's string/numeric decoders or seed methods.
			if (isStringDecoder(method) || isNumericDecoder(method))
				helperMethods.add(new MethodKey(method.name, method.desc));

			// Add any private static methods that look like ZKM's seed methods, which are void and write to arrays.
			if ("()V".equals(method.desc) && !"<clinit>".equals(method.name) && hasArrayStore(method))
				helperMethods.add(new MethodKey(method.name, method.desc));

			// Collect any private static methods that reference generated array fields, which are part of the helper component.
			if (method.instructions == null)
				continue;
			for (AbstractInsnNode instruction : method.instructions) {
				if (!(instruction instanceof FieldInsnNode field)
						|| field.getOpcode() != GETSTATIC
						|| !classNode.name.equals(field.owner)
						|| !isGeneratedArrayDescriptor(field.desc))
					continue;
				if (isStringDecoder(method) || isNumericDecoder(method))
					referencedArrayFields.add(new FieldKey(field.owner, field.name, field.desc));
			}
		}
		if (helperMethods.isEmpty())
			return null;

		// Folded helpers may no longer read their arrays, so classify them by their field type and access flags instead.
		Set<FieldKey> helperFields = new HashSet<>();
		for (FieldNode field : classNode.fields) {
			boolean immutable = (field.access & (ACC_PRIVATE | ACC_STATIC | ACC_FINAL))
					== (ACC_PRIVATE | ACC_STATIC | ACC_FINAL);
			if ((immutable && isGeneratedArrayDescriptor(field.desc)) || isInterfaceHelperStorage(classNode, field))
				helperFields.add(new FieldKey(classNode.name, field.name, field.desc));
		}
		if (helperFields.size() < 2
				|| helperFields.stream().noneMatch(field -> isGeneratedArrayDescriptor(field.descriptor()))
				|| !helperFields.containsAll(referencedArrayFields))
			return null;

		// Include private helper calls that survived earlier passes.
		boolean changed;
		do {
			changed = false;
			for (MethodKey helper : List.copyOf(helperMethods)) {
				MethodNode method = findMethod(classNode, helper);
				if (method == null || method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions) {
					if (!(instruction instanceof MethodInsnNode call) || !classNode.name.equals(call.owner))
						continue;
					MethodNode target = findMethod(classNode, new MethodKey(call.name, call.desc));
					if (target == null || (target.access & (ACC_PRIVATE | ACC_STATIC)) != (ACC_PRIVATE | ACC_STATIC))
						continue;
					changed |= helperMethods.add(new MethodKey(target.name, target.desc));
				}
			}
		} while (changed);

		// Collect only primitive fields referenced by helper methods.
		// Unreferenced application fields shouldn't be treated as ZKM state just because they share a class with generated helpers.
		Set<FieldKey> stateFields = new HashSet<>();
		for (MethodKey helper : helperMethods) {
			MethodNode method = findMethod(classNode, helper);
			if (method == null || method.instructions == null)
				continue;

			for (AbstractInsnNode instruction : method.instructions) {
				if (!(instruction instanceof FieldInsnNode field) || !classNode.name.equals(field.owner))
					continue;

				FieldNode declared = findField(classNode, field.name, field.desc);
				if (declared == null)
					continue;

				FieldKey key = new FieldKey(field.owner, field.name, field.desc);
				if (isPrimitive(declared.desc))
					stateFields.add(key);
				else if (isPrivateHelperStorage(declared) || isInterfaceHelperStorage(classNode, declared))
					helperFields.add(key);
			}
		}
		return new CleanupMetadata(Set.copyOf(helperMethods), Set.copyOf(helperFields), Set.copyOf(stateFields), null, null);
	}

	/**
	 * @param method
	 * 		Method to inspect.
	 *
	 * @return {@code true} when the method contains an array store instruction.
	 */
	private static boolean hasArrayStore(@Nonnull MethodNode method) {
		if (method.instructions == null)
			return false;
		for (AbstractInsnNode instruction : method.instructions)
			if (instruction.getOpcode() == AASTORE)
				return true;
		return false;
	}

	/**
	 * @param descriptor
	 * 		Field descriptor to check.
	 *
	 * @return {@code true} when the descriptor is one of ZKM's generated array types.
	 */
	private static boolean isGeneratedArrayDescriptor(@Nonnull String descriptor) {
		return "[Ljava/lang/Object;".equals(descriptor)
				|| "[Ljava/lang/String;".equals(descriptor)
				|| "[J".equals(descriptor)
				|| "[Ljava/lang/Integer;".equals(descriptor)
				|| "[Ljava/lang/Long;".equals(descriptor);
	}

	/**
	 * @param field
	 * 		Field to inspect.
	 *
	 * @return {@code true} when the field is a private static final array or string field in a class.
	 */
	private static boolean isPrivateHelperStorage(@Nonnull FieldNode field) {
		return (field.access & (ACC_PRIVATE | ACC_STATIC | ACC_FINAL))
				== (ACC_PRIVATE | ACC_STATIC | ACC_FINAL)
				&& (isGeneratedArrayDescriptor(field.desc) || isPrimitive(field.desc));
	}

	/**
	 * @param classNode
	 * 		Class to inspect.
	 * @param field
	 * 		Field to inspect.
	 *
	 * @return {@code true} when the field is a static final array or string field in an interface.
	 */
	private static boolean isInterfaceHelperStorage(@Nonnull ClassNode classNode, @Nonnull FieldNode field) {
		return (classNode.access & ACC_INTERFACE) != 0
				&& (field.access & (ACC_STATIC | ACC_FINAL)) == (ACC_STATIC | ACC_FINAL)
				&& (isGeneratedArrayDescriptor(field.desc) || "Ljava/lang/String;".equals(field.desc));
	}

	/**
	 * @param method
	 * 		Method to check.
	 *
	 * @return {@code true} when the method is in the shape of ZKM's spread bootstrap.
	 */
	private static boolean isSpreadBootstrap(@Nonnull MethodNode method) {
		return ("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;"
				+ "Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/Object;").equals(method.desc);
	}

	/**
	 * @param descriptor
	 * 		Method descriptor to check.
	 *
	 * @return {@code true} when the descriptor is one of ZKM's resolver shapes.
	 */
	private static boolean isResolverDescriptor(@Nonnull String descriptor) {
		return RESOLVER_DESCRIPTOR.equals(descriptor) || RESOLVER_DESCRIPTOR_ALT.equals(descriptor);
	}

	/**
	 * @param method
	 * 		Method to check.
	 *
	 * @return {@code true} when the method is in the shape of ZKM's string decoder.
	 */
	private static boolean isStringDecoder(@Nonnull MethodNode method) {
		if (method.instructions == null)
			return false;

		// Descriptor must match one of the two string decoder shapes.
		if (!("(III)Ljava/lang/String;".equals(method.desc) || "(II)Ljava/lang/String;".equals(method.desc)))
			return false;

		// Must read a static array of string values.
		for (AbstractInsnNode instruction : method.instructions)
			if (instruction instanceof FieldInsnNode field
					&& instruction.getOpcode() == GETSTATIC
					&& "[Ljava/lang/String;".equals(field.desc))
				return true;

		// There are other indicators we can also use but so far the above are enough.
		//  - while (n < charArray.length) { /* xor logic*/ }
		//  - stringArrayField[i] = new String(charArray).intern();
		return false;
	}

	/**
	 * @param method
	 * 		Method to check.
	 *
	 * @return {@code true} when the method is in the shape of ZKM's numeric decoder.
	 */
	private static boolean isNumericDecoder(@Nonnull MethodNode method) {
		if (method.instructions == null)
			return false;

		// Descriptor must match one of the two numeric decoder shapes.
		if (!("(IJ)I".equals(method.desc) || "(IJ)J".equals(method.desc)))
			return false;

		// Must read a static array of long or boxed numeric values.
		for (AbstractInsnNode instruction : method.instructions)
			if (instruction instanceof FieldInsnNode field
					&& instruction.getOpcode() == GETSTATIC
					&& ("[J".equals(field.desc)
					|| "[Ljava/lang/Integer;".equals(field.desc)
					|| "[Ljava/lang/Long;".equals(field.desc)))
				return true;
		return false;
	}

	/**
	 * ZKM's index helper looks roughly like this <i>(There is a single long variant, general structure is same)</i>:
	 * <pre>{@code
	 * private static int indexHelper(long key0, long key1) {
	 *     // Combine the two call-site metadata values.
	 *     long mixed = key0 ^ ((key1 << 48) | key1);
	 *
	 *     // Extract the cache slot.
	 *     int slot = (int) (mixed >>> 46);
	 *
	 *     // Return immediately if the decoded string is already cached.
	 *     if (decodedStrings[slot] != null)
	 *     return slot;
	 *
	 *     Object encrypted = objectCache[slot];
	 *
	 *     // Nothing to decode for non-string entries.
	 *     if (!(encrypted instanceof String))
	 *     return slot;
	 *
	 *     // Derive a substitution value from six bits of the mixed key.
	 *     int selector = (int) ((mixed >>> 42) & 0x3f);
	 *     int substitution = substitutionTable[selector];
	 *
	 *     // Build a six-element XOR key schedule from seven-bit slices.
	 *     int[] key = new int[6];
	 *     for (int i = 0; i < 6; i++) {
	 *         int shift = 7 * (5 - i);
	 *         int part = (int) ((mixed >>> shift) & 0x7f);
	 *         key[i] = (part - substitution) & 0x7f;
	 *     }
	 *
	 *     // Decode the encrypted string using the repeating six-value key.
	 *     char[] chars = ((String) encrypted).toCharArray();
	 *     for (int i = 0; i < chars.length; i++)
	 *         chars[i] ^= key[i % 6];
	 *
	 *     // Cache the decoded value.
	 *     decodedStrings[slot] = new String(chars); // Does not use '.intern()' like the string decoder does.
	 *
	 *     return slot;
	 * }
	 * }</pre>
	 *
	 * @param classNode
	 * 		Class declaring the method and its cache fields.
	 * @param method
	 * 		Method to check.
	 *
	 * @return {@code true} when the method is in the shape of ZKM's index helper.
	 */
	private static boolean isIndexHelper(@Nonnull ClassNode classNode, @Nonnull MethodNode method) {
		InsnList instructions = method.instructions;
		if (instructions == null || (method.access & ACC_STATIC) == 0)
			return false;

		// Index helpers take one or two long arguments and return the decoded cache slot as an int.
		Type[] arguments;
		try {
			arguments = Type.getArgumentTypes(method.desc);
			if (!Type.INT_TYPE.equals(Type.getReturnType(method.desc)) || (arguments.length != 1 && arguments.length != 2))
				return false;
			for (Type argument : arguments)
				if (!Type.LONG_TYPE.equals(argument))
					return false;
		} catch (RuntimeException ignored) {
			return false;
		}

		// Heuristic fingerprint of the index helper's operations.
		boolean hasXor = false;
		boolean hasOr = false;
		boolean hasLeftShift = false;
		boolean hasSlotShift = false;
		boolean hasSelectorShift = false;
		boolean hasLongToInt = false;
		boolean hasObjectArray = false;
		boolean hasStringArray = false;
		for (AbstractInsnNode instruction : instructions) {
			int opcode = instruction.getOpcode();
			hasXor |= opcode == LXOR;
			hasOr |= opcode == LOR;
			hasLeftShift |= opcode == LSHL && hasNearbyConstant(instructions, instruction, 48);
			hasSlotShift |= opcode == LUSHR && hasNearbyConstant(instructions, instruction, 46);
			hasSelectorShift |= opcode == LUSHR && hasNearbyConstant(instructions, instruction, 42);
			hasLongToInt |= opcode == L2I;
			if (instruction instanceof FieldInsnNode field
					&& opcode == GETSTATIC
					&& classNode.name.equals(field.owner)) {
				hasObjectArray |= "[Ljava/lang/Object;".equals(field.desc);
				hasStringArray |= "[Ljava/lang/String;".equals(field.desc);
			}
		}

		// The original layout mixes two keys with a 48-bit rotate-like operation.
		if (arguments.length == 2)
			return hasXor && hasOr && hasLeftShift && hasSlotShift && hasLongToInt && hasObjectArray && hasStringArray;

		// The shared one-key layout derives both the slot and string selector directly from its key.
		return hasSlotShift && hasSelectorShift && hasLongToInt && hasObjectArray && hasStringArray;
	}

	/**
	 * @param instructions
	 * 		Instructions to search.
	 * @param target
	 * 		Target instruction to search around.
	 * @param expected
	 * 		Expected constant value.
	 *
	 * @return {@code true} when the target instruction is preceded by a constant with the expected value.
	 */
	private static boolean hasNearbyConstant(@Nonnull InsnList instructions,
	                                         @Nonnull AbstractInsnNode target,
	                                         int expected) {
		AbstractInsnNode current = target.getPrevious();
		while (current != null && current.getOpcode() < 0)
			current = current.getPrevious();
		return current instanceof IntInsnNode intInstruction && intInstruction.operand == expected
				|| current instanceof InsnNode && integerConstant(current) == expected
				|| current instanceof LdcInsnNode && integerConstant(current) == expected;
	}

	/**
	 * @param classNode
	 * 		Class being inspected.
	 * @param helperMethod
	 * 		The helper method to inspect for static array reads. See {@link #isIndexHelper(ClassNode, MethodNode)}.
	 * @param objectFields
	 * 		Set of object array fields to populate.
	 * @param descriptorFields
	 * 		Set of descriptor array fields to populate.
	 */
	private static void collectCacheFields(@Nonnull ClassNode classNode,
	                                       @Nonnull MethodNode helperMethod,
	                                       @Nonnull Set<FieldKey> objectFields,
	                                       @Nonnull Set<FieldKey> descriptorFields) {
		if (helperMethod.instructions == null)
			return;
		for (AbstractInsnNode instruction : helperMethod.instructions) {
			if (!(instruction instanceof FieldInsnNode field)
					|| field.getOpcode() != GETSTATIC
					|| !classNode.name.equals(field.owner))
				continue;
			FieldKey key = new FieldKey(field.owner, field.name, field.desc);
			if ("[Ljava/lang/Object;".equals(field.desc))
				objectFields.add(key);
			else if ("[Ljava/lang/String;".equals(field.desc))
				descriptorFields.add(key);
		}
	}

	/**
	 * ZKM's seed method looks roughly like this:
	 * <pre>{@code
	 * private static void seed() {
	 *     Object[] objectCache = OBJECT_CACHE;
	 *     String[] descriptorCache = DESCRIPTOR_CACHE;
	 *
	 *     // Populate the shared object pool with encrypted values or direct Class objects.
	 *     objectCache[0] = "<encrypted value>";
	 *     objectCache[1] = "<encrypted value>";
	 *     objectCache[2] = Void.TYPE;
	 *     objectCache[3] = "<encrypted value>";
	 *     objectCache[4] = "<encrypted value>";
	 *     // ...
	 *     objectCache[14] = "<encrypted value>";
	 *
	 *     // Some entries have a directly known descriptor/class name.
	 *     descriptorCache[2] = "java/lang/Void";
	 * }
	 * }</pre>
	 *
	 * @param classNode
	 * 		Class being inspected.
	 * @param objectField
	 * 		Field for the object array.
	 * @param descriptorField
	 * 		Field for the descriptor array.
	 *
	 * @return Method that populates the two arrays, or {@code null} when no such method is found.
	 */
	@Nullable
	private static MethodNode findSeedMethod(@Nonnull ClassNode classNode,
	                                         @Nonnull FieldKey objectField,
	                                         @Nonnull FieldKey descriptorField) {
		for (MethodNode method : classNode.methods) {
			if ("<clinit>".equals(method.name)
					|| (method.access & ACC_STATIC) == 0
					|| !"()V".equals(method.desc)
					|| method.instructions == null)
				continue;
			boolean objectRead = false;
			boolean descriptorRead = false;
			boolean arrayStore = false;
			for (AbstractInsnNode instruction : method.instructions) {
				if (instruction instanceof FieldInsnNode field && field.getOpcode() == GETSTATIC) {
					FieldKey key = new FieldKey(field.owner, field.name, field.desc);
					objectRead |= objectField.equals(key);
					descriptorRead |= descriptorField.equals(key);
				}
				arrayStore |= instruction.getOpcode() == AASTORE;
			}
			if (objectRead && descriptorRead && arrayStore)
				return method;
		}
		return null;
	}

	/**
	 * @param seed
	 * 		Seed method. See {@link #findSeedMethod(ClassNode, FieldKey, FieldKey)}.
	 *
	 * @return Map of array slot to class type for all entries that are directly known to be a class type.
	 */
	@Nonnull
	private static Map<Integer, Type> discoverClassTypes(@Nonnull MethodNode seed) {
		if (seed.instructions == null)
			return Map.of();
		AbstractInsnNode[] instructions = seed.instructions.toArray();
		Map<Integer, Type> result = new HashMap<>();
		for (int index = 0; index < instructions.length; index++) {
			AbstractInsnNode instruction = instructions[index];
			Type classType = null;
			if (instruction instanceof FieldInsnNode field && field.getOpcode() == GETSTATIC)
				classType = primitiveType(field);
			else if (instruction instanceof LdcInsnNode ldc && ldc.cst instanceof Type type)
				classType = type;
			if (classType == null)
				continue;
			Integer slot = findStoredIndex(instructions, index);
			if (slot != null)
				result.putIfAbsent(slot, classType);
		}
		return Map.copyOf(result);
	}

	@Nullable
	private static Type primitiveType(@Nonnull FieldInsnNode field) {
		if (!"java/lang/Boolean".equals(field.owner) && !"java/lang/Byte".equals(field.owner)
				&& !"java/lang/Character".equals(field.owner) && !"java/lang/Short".equals(field.owner)
				&& !"java/lang/Integer".equals(field.owner) && !"java/lang/Long".equals(field.owner)
				&& !"java/lang/Float".equals(field.owner) && !"java/lang/Double".equals(field.owner)
				&& !"java/lang/Void".equals(field.owner))
			return null;
		if (!"TYPE".equals(field.name) || !"Ljava/lang/Class;".equals(field.desc))
			return null;
		return switch (field.owner) {
			case "java/lang/Boolean" -> Type.BOOLEAN_TYPE;
			case "java/lang/Byte" -> Type.BYTE_TYPE;
			case "java/lang/Character" -> Type.CHAR_TYPE;
			case "java/lang/Short" -> Type.SHORT_TYPE;
			case "java/lang/Integer" -> Type.INT_TYPE;
			case "java/lang/Long" -> Type.LONG_TYPE;
			case "java/lang/Float" -> Type.FLOAT_TYPE;
			case "java/lang/Double" -> Type.DOUBLE_TYPE;
			case "java/lang/Void" -> Type.VOID_TYPE;
			default -> null;
		};
	}

	@Nullable
	private static Integer findStoredIndex(@Nonnull AbstractInsnNode[] instructions, int valueIndex) {
		// Seed writes place the slot literal before the class value and its AASTORE.
		boolean hasStore = false;
		for (int index = valueIndex + 1; index < instructions.length && index <= valueIndex + 8; index++) {
			int opcode = instructions[index].getOpcode();
			if (opcode == AASTORE) {
				hasStore = true;
				break;
			}
			if (opcode == GETSTATIC)
				break;
		}
		if (!hasStore)
			return null;
		for (int index = valueIndex - 1; index >= 0 && index >= valueIndex - 10; index--) {
			Integer constant = integerConstant(instructions[index]);
			if (constant != null)
				return constant;
			if (instructions[index].getOpcode() == AASTORE)
				break;
		}
		return null;
	}

	@Nullable
	private static Integer integerConstant(@Nonnull AbstractInsnNode instruction) {
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
	 * The ZKM resolver method looks roughly like this:
	 * <pre>{@code
	 * private static MethodHandle resolve(
	 *         MethodHandles.Lookup lookup,
	 *         MutableCallSite callSite,
	 *         String operationName,
	 *         MethodType methodType,
	 *         long key0,
	 *         long key1) {
	 *
	 *     int selector = operationName.charAt(0);
	 *
	 *     if (selector == GET_FIELD_CODE) {
	 *         return lookup.findGetter(ownerClass, memberName, fieldType);
	 *     }
	 *
	 *     if (selector == PUT_FIELD_CODE) {
	 *         return lookup.findSetter(ownerClass, memberName, fieldType);
	 *     }
	 *
	 *     if (selector == GET_STATIC_CODE) {
	 *         return lookup.findStaticGetter(ownerClass, memberName, fieldType);
	 *     }
	 *
	 *     if (selector == INVOKE_VIRTUAL_CODE) {
	 *         return lookup.findVirtual(ownerClass, memberName, methodType);
	 *     }
	 *
	 *     if (selector == INVOKE_STATIC_CODE) {
	 *         return lookup.findStatic(ownerClass, memberName, methodType);
	 *     }
	 *
	 *     // ZKM's generated default branches handle the remaining operations.
	 *     if (memberIsAField) {
	 *         return lookup.findStaticSetter(ownerClass, memberName, fieldType);
	 *     } else {
	 *         return lookup.findSpecial(ownerClass, memberName, methodType, callerClass);
	 *     }
	 * }
	 * }</pre>
	 *
	 * @param classNode
	 * 		Class to inspect for ZKM operation codes.
	 *
	 * @return Discovered operation codes, or {@link OperationCodes#EMPTY} when the class does not appear to be a ZKM-generated class.
	 */
	@Nonnull
	private static OperationCodes discoverOperationCodes(@Nonnull ClassNode classNode) {
		// Selector bytes vary per generated class, so derive them from lookup calls instead of hardcoding a table.
		for (MethodNode method : classNode.methods) {
			if (!isResolverDescriptor(method.desc))
				continue;
			int getField = findOperationCode(method, "findGetter");
			int putField = findOperationCode(method, "findSetter");
			int getStatic = findOperationCode(method, "findStaticGetter");
			int invokeVirtual = findOperationCode(method, "findVirtual");
			int invokeStatic = findOperationCode(method, "findStatic");

			// ZKM uses the unmatched field branch for findStaticSetter, so its selector is the remaining comparison code.
			Set<Integer> comparisonCodes = comparisonCodes(method);
			Set<Integer> fieldCodes = new HashSet<>(comparisonCodes);
			fieldCodes.remove(getField);
			fieldCodes.remove(putField);
			fieldCodes.remove(getStatic);
			fieldCodes.remove(invokeVirtual);
			fieldCodes.remove(invokeStatic);
			int putStatic = fieldCodes.size() == 1 ? fieldCodes.iterator().next() : -1;
			return new OperationCodes(getField, putField, getStatic, putStatic, invokeVirtual, invokeStatic);
		}
		return OperationCodes.EMPTY;
	}

	/**
	 * @param resolver
	 * 		Operation resolver method. See {@link #discoverOperationCodes(ClassNode)}.
	 * @param lookupName
	 * 		Method handle lookup method name to search for.
	 *
	 * @return The associated operation code, or {@code -1} when the lookup method is not found.
	 */
	private static int findOperationCode(@Nonnull MethodNode resolver, @Nonnull String lookupName) {
		if (resolver.instructions == null)
			return -1;
		AbstractInsnNode[] instructions = resolver.instructions.toArray();
		for (int index = 0; index < instructions.length; index++) {
			AbstractInsnNode instruction = instructions[index];
			if (!(instruction instanceof MethodInsnNode method)
					|| !"java/lang/invoke/MethodHandles$Lookup".equals(method.owner)
					|| !lookupName.equals(method.name))
				continue;
			for (int comparison = index - 1; comparison >= 0 && index - comparison <= 40; comparison--) {
				if (!isIntegerComparison(instructions[comparison].getOpcode()))
					continue;
				for (int constant = comparison - 1; constant >= 0 && comparison - constant <= 12; constant--) {
					Integer value = integerConstant(instructions[constant]);
					if (value != null)
						return value;
				}
			}
		}
		return -1;
	}

	/**
	 * @param resolver
	 * 		Operation resolver method. See {@link #discoverOperationCodes(ClassNode)}.
	 *
	 * @return Set of all comparison codes used in the resolver, which includes the operation codes for all branches.
	 */
	@Nonnull
	private static Set<Integer> comparisonCodes(@Nonnull MethodNode resolver) {
		if (resolver.instructions == null)
			return Set.of();
		AbstractInsnNode[] instructions = resolver.instructions.toArray();
		Set<Integer> result = new HashSet<>();
		for (int index = 0; index < instructions.length; index++) {
			if (!isIntegerComparison(instructions[index].getOpcode()))
				continue;
			for (int constant = index - 1; constant >= 0 && index - constant <= 12; constant--) {
				Integer value = integerConstant(instructions[constant]);
				if (value != null) {
					result.add(value);
					break;
				}
			}
		}
		return result;
	}

	private static boolean isIntegerComparison(int opcode) {
		return switch (opcode) {
			case Opcodes.IF_ICMPEQ,
			     Opcodes.IF_ICMPNE,
			     Opcodes.IF_ICMPLT,
			     Opcodes.IF_ICMPGE,
			     Opcodes.IF_ICMPGT,
			     Opcodes.IF_ICMPLE -> true;
			default -> false;
		};
	}

	/**
	 * @param workspace
	 * 		Workspace to resolve against.
	 * @param graph
	 * 		Inheritance graph of classes in the workspace.
	 * @param caller
	 * 		Name of the class that is calling the dynamic site.
	 * @param member
	 * 		Decoded member to resolve.
	 * @param operationCodes
	 * 		Operation codes for the ZKM class that generated the dynamic site.
	 * @param metadataKeyCount
	 * 		Number of trailing metadata arguments to remove from the call site.
	 *
	 * @return Resolved dynamic site, or {@code null} when the member cannot be resolved.
	 */
	@Nullable
	private static InvokeDynamicResolver.ResolvedInvokeDynamic resolveMember(@Nonnull Workspace workspace,
	                                                                         @Nonnull InheritanceGraph graph,
	                                                                         @Nonnull String caller,
	                                                                         @Nonnull DecodedMember member,
	                                                                         @Nonnull OperationCodes operationCodes,
	                                                                         int metadataKeyCount) {
		// Must be a known operation code, otherwise the bootstrap will throw an exception.
		int operation = operation(member.operationName);
		if (operation < 0 || !operationCodes.hasKnownOperation())
			return null;

		// ZKM's generated bootstrap has a separate branch for field operations, and a shared branch for all method operations.
		if (matches(operation, operationCodes.getField)
				|| matches(operation, operationCodes.putField)
				|| matches(operation, operationCodes.getStatic)
				|| matches(operation, operationCodes.putStatic)) {
			if (!member.parameterTypes.isEmpty())
				return null;

			// Look for the field in the workspace and ensure the caller has access to it.
			FieldMatch match = findField(workspace, member.owner, member.memberName, member.returnType, new HashSet<>());
			if (match == null || !isAccessible(caller, match.ownerInfo, match.member, graph))
				return null;

			// Validate that the field's static access flags match the operation code.
			boolean isStatic = match.member.hasStaticModifier();
			if (matches(operation, operationCodes.getStatic) || matches(operation, operationCodes.putStatic)) {
				if (!isStatic)
					return null;
			} else if (isStatic) {
				return null;
			}

			// Determine the handle tag based on the operation code and field staticness.
			int tag = matches(operation, operationCodes.getField) ? H_GETFIELD
					: matches(operation, operationCodes.putField) ? H_PUTFIELD
					: matches(operation, operationCodes.getStatic) ? H_GETSTATIC : H_PUTSTATIC;
			return new InvokeDynamicResolver.ResolvedInvokeDynamic(
					new Handle(tag, match.ownerName, match.member.getName(), match.member.getDescriptor(),
							match.ownerInfo.hasInterfaceModifier()), metadataKeyCount);
		}

		// Resolve method operations.
		if (matches(operation, operationCodes.invokeStatic))
			return resolveMethod(workspace, graph, caller, member, H_INVOKESTATIC, metadataKeyCount);
		if (matches(operation, operationCodes.invokeVirtual))
			return resolveMethod(workspace, graph, caller, member, -1, metadataKeyCount);

		// The generated bootstrap dispatches its remaining method selector through findSpecial without a separate
		// comparison, so an unmatched non-field/non-method selector has findSpecial semantics.
		return resolveMethod(workspace, graph, caller, member, H_INVOKESPECIAL, metadataKeyCount);
	}

	private static int operation(@Nonnull String name) {
		return name.isEmpty() ? -1 : name.charAt(0);
	}

	private static boolean matches(int operation, int code) {
		return code >= 0 && operation == code;
	}

	/**
	 * @param workspace
	 * 		Workspace to resolve against.
	 * @param graph
	 * 		Inheritance graph of classes in the workspace.
	 * @param caller
	 * 		Name of the class that is calling the dynamic site.
	 * @param member
	 * 		Decoded member to resolve.
	 * @param requestedTag
	 * 		The {@link Handle#getTag()} to use for the resolved handle,
	 * 		or {@code -1} to derive the tag from the resolved member.
	 * @param metadataKeyCount
	 * 		Number of trailing metadata arguments to remove from the call site.
	 *
	 * @return Resolved dynamic site, or {@code null} when the member cannot be resolved.
	 */
	@Nullable
	private static InvokeDynamicResolver.ResolvedInvokeDynamic resolveMethod(@Nonnull Workspace workspace,
	                                                                         @Nonnull InheritanceGraph graph,
	                                                                         @Nonnull String caller,
	                                                                         @Nonnull DecodedMember member,
	                                                                         int requestedTag,
	                                                                         int metadataKeyCount) {
		// Build the method descriptor from the decoded member's return type and parameter types.
		String descriptor;
		try {
			descriptor = Type.getMethodDescriptor(member.returnType, member.parameterTypes.toArray(Type[]::new));
		} catch (RuntimeException ignored) {
			return null;
		}

		// Find the method in the workspace and ensure the caller has access to it.
		// - Constructors are not allowed to be called through the bootstrap, so they are filtered out.
		MethodMatch match = findMethod(workspace, member.owner, member.memberName, descriptor, new HashSet<>());
		if (match == null
				|| "<init>".equals(match.member.getName())
				|| !isAccessible(caller, match.ownerInfo, match.member, graph))
			return null;

		// Validate that the method's static access flags match the requested tag.
		boolean isStatic = match.member.hasStaticModifier();
		if (requestedTag == H_INVOKESTATIC && !isStatic)
			return null;
		if (requestedTag != H_INVOKESTATIC && isStatic)
			return null;
		if (requestedTag == H_INVOKESPECIAL && !isSpecialLookupAllowed(caller, match.ownerName, match.ownerInfo, graph))
			return null;

		// Build the resolved handle with the appropriate tag, owner, name, and descriptor.
		int tag = requestedTag;
		if (tag < 0)
			tag = match.ownerInfo.hasInterfaceModifier() ? H_INVOKEINTERFACE : H_INVOKEVIRTUAL;
		return new InvokeDynamicResolver.ResolvedInvokeDynamic(
				new Handle(tag, match.ownerName, match.member.getName(), match.member.getDescriptor(),
						match.ownerInfo.hasInterfaceModifier()), metadataKeyCount);
	}

	private static boolean isSpecialLookupAllowed(@Nonnull String caller,
	                                              @Nonnull String owner,
	                                              @Nonnull JvmClassInfo ownerInfo,
	                                              @Nonnull InheritanceGraph graph) {
		if (caller.equals(owner))
			return true;
		return graph.isAssignableFrom(owner, caller) && !ownerInfo.hasAnnotationModifier();
	}

	private static boolean isAccessible(@Nonnull String caller,
	                                    @Nonnull JvmClassInfo owner,
	                                    @Nonnull ClassMember member,
	                                    @Nonnull InheritanceGraph graph) {
		if (!isClassAccessible(caller, owner))
			return false;
		if (member.hasPublicModifier())
			return true;
		if (member.hasPrivateModifier())
			return caller.equals(owner.getName());
		if (member.hasProtectedModifier())
			return samePackage(caller, owner.getName()) || graph.isAssignableFrom(owner.getName(), caller);
		return samePackage(caller, owner.getName());
	}

	private static boolean isClassAccessible(@Nonnull String caller, @Nonnull JvmClassInfo owner) {
		return owner.hasPublicModifier() || samePackage(caller, owner.getName());
	}

	private static boolean samePackage(@Nonnull String first, @Nonnull String second) {
		int firstSlash = first.lastIndexOf('/');
		int secondSlash = second.lastIndexOf('/');
		if (firstSlash < 0 || secondSlash < 0)
			return firstSlash < 0 && secondSlash < 0;
		return first.substring(0, firstSlash).equals(second.substring(0, secondSlash));
	}

	/**
	 * @param workspace
	 * 		Workspace to resolve against.
	 * @param owner
	 * 		Name of the class that declares the field.
	 * @param name
	 * 		Name of the field.
	 * @param type
	 * 		Type of the field.
	 * @param visited
	 * 		Set of already visited class names to avoid infinite recursion.
	 *
	 * @return Field match if found, or {@code null} if not found or inaccessible.
	 */
	@Nullable
	private static FieldMatch findField(@Nonnull Workspace workspace,
	                                    @Nonnull String owner,
	                                    @Nonnull String name,
	                                    @Nonnull Type type,
	                                    @Nonnull Set<String> visited) {
		// Skip already visited classes to avoid infinite recursion.
		if (!visited.add(owner))
			return null;

		// Skip method types since they cannot be fields.
		if (type.getSort() == Type.METHOD)
			return null;

		// Must be able to find the class in the workspace.
		JvmClassInfo info = findClass(workspace, owner);
		if (info == null)
			return null;

		// Check if the field is declared in the class.
		FieldMember field = info.getDeclaredField(name, type.getDescriptor());
		if (field != null)
			return new FieldMatch(owner, info, field);

		// Check parent types for the field.
		for (String interfaceName : info.getInterfaces()) {
			FieldMatch match = findField(workspace, interfaceName, name, type, visited);
			if (match != null)
				return match;
		}
		String superName = info.getSuperName();
		return superName == null ? null : findField(workspace, superName, name, type, visited);
	}

	/**
	 * @param workspace
	 * 		Workspace to resolve against.
	 * @param owner
	 * 		Name of the class that declares the method.
	 * @param name
	 * 		Name of the method.
	 * @param descriptor
	 * 		Method descriptor.
	 * @param visited
	 * 		Set of already visited class names to avoid infinite recursion.
	 *
	 * @return Method match if found, or {@code null} if not found or inaccessible.
	 */
	@Nullable
	private static MethodMatch findMethod(@Nonnull Workspace workspace,
	                                      @Nonnull String owner,
	                                      @Nonnull String name,
	                                      @Nonnull String descriptor,
	                                      @Nonnull Set<String> visited) {
		// Skip already visited classes to avoid infinite recursion.
		if (!visited.add(owner))
			return null;

		// Must be able to find the class in the workspace.
		JvmClassInfo info = findClass(workspace, owner);
		if (info == null)
			return null;

		// Check if the method is declared in the class.
		MethodMember method = info.getDeclaredMethod(name, descriptor);
		if (method != null)
			return new MethodMatch(owner, info, method);

		// Check parent types for the method.
		for (String interfaceName : info.getInterfaces()) {
			MethodMatch match = findMethod(workspace, interfaceName, name, descriptor, visited);
			if (match != null)
				return match;
		}
		String superName = info.getSuperName();
		return superName == null ? null : findMethod(workspace, superName, name, descriptor, visited);
	}

	@Nullable
	private static JvmClassInfo findClass(@Nonnull Workspace workspace, @Nonnull String name) {
		ClassPathNode path = workspace.findJvmClass(true, name);
		if (path == null)
			return null;
		return path.getValue().asJvmClass();
	}

	@Nullable
	private static Type parseClassName(@Nullable String value) {
		if (value == null || value.isEmpty())
			return null;
		return switch (value) {
			case "boolean" -> Type.BOOLEAN_TYPE;
			case "byte" -> Type.BYTE_TYPE;
			case "char" -> Type.CHAR_TYPE;
			case "short" -> Type.SHORT_TYPE;
			case "int" -> Type.INT_TYPE;
			case "long" -> Type.LONG_TYPE;
			case "float" -> Type.FLOAT_TYPE;
			case "double" -> Type.DOUBLE_TYPE;
			case "void" -> Type.VOID_TYPE;
			default -> parseNonPrimitiveClassName(value);
		};
	}

	@Nullable
	private static Type parseNonPrimitiveClassName(@Nonnull String value) {
		String normalized = value.replace('.', '/');
		try {
			if (normalized.charAt(0) == '[')
				return Type.getType(normalized);
			if (normalized.startsWith("L") && normalized.endsWith(";"))
				return Type.getType(normalized);
			if (normalized.endsWith("[]")) {
				String base = normalized;
				int dimensions = 0;
				while (base.endsWith("[]")) {
					base = base.substring(0, base.length() - 2);
					dimensions++;
				}
				Type baseType = parseClassName(base);
				if (baseType == null || baseType == Type.VOID_TYPE)
					return null;
				return Type.getType("[".repeat(dimensions) + baseType.getDescriptor());
			}
			return Type.getObjectType(normalized);
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	@Nullable
	private static MethodNode findMethod(@Nonnull ClassNode classNode, @Nonnull MethodKey key) {
		for (MethodNode method : classNode.methods)
			if (key.name.equals(method.name) && key.descriptor.equals(method.desc))
				return method;
		return null;
	}

	@Nullable
	private static FieldNode findField(@Nonnull ClassNode classNode, @Nonnull String name, @Nonnull String descriptor) {
		for (FieldNode field : classNode.fields)
			if (name.equals(field.name) && descriptor.equals(field.desc))
				return field;
		return null;
	}

	@Nonnull
	private static Map<LabelNode, LabelNode> cloneLabels(@Nonnull MethodNode source) {
		Map<LabelNode, LabelNode> labels = new HashMap<>();
		if (source.instructions != null)
			for (AbstractInsnNode instruction : source.instructions)
				if (instruction instanceof LabelNode label)
					labels.put(label, new LabelNode());
		return labels;
	}

	/**
	 * @param source
	 * 		Baseline {@code <clinit>} method to copy from.
	 * @param requiredFields
	 * 		Set of static fields that must be initialized in the prefix.
	 *
	 * @return A new {@code <clinit>} method that initializes the required fields,
	 * or {@code null} if the source does not initialize all required fields.
	 */
	@Nullable
	private static MethodNode createCachePrefix(@Nonnull MethodNode source, @Nonnull Set<FieldKey> requiredFields) {
		if (source.instructions == null)
			return null;

		// Need to collect the required fields that are initialized in the source method.
		Set<FieldKey> remaining = new HashSet<>(requiredFields);

		// As we collect them, we will copy the initializing instructions into a new method.
		MethodNode prefix = new MethodNode(ACC_STATIC, "<clinit>", "()I", null, null);
		Map<LabelNode, LabelNode> labels = cloneLabels(source);
		for (AbstractInsnNode instruction : source.instructions) {
			prefix.instructions.add(instruction.clone(labels));
			if (instruction instanceof FieldInsnNode field && instruction.getOpcode() == Opcodes.PUTSTATIC)
				remaining.remove(new FieldKey(field.owner, field.name, field.desc));
			if (remaining.isEmpty())
				break;
		}
		if (!remaining.isEmpty())
			return null;

		// Add a return value to the prefix method so it can be used as a bootstrap method.
		prefix.instructions.add(new InsnNode(Opcodes.ICONST_0));
		prefix.instructions.add(new InsnNode(Opcodes.IRETURN));
		prefix.maxStack = Math.max(source.maxStack, 4);
		prefix.maxLocals = source.maxLocals;
		return prefix;
	}

	/**
	 * @param source
	 * 		Baseline {@code <clinit>} method to copy from.
	 *
	 * @return A copy of the source method that returns an integer value instead of void,
	 * or {@code null} if the source method does not have a return instruction.
	 */
	@Nullable
	private static MethodNode createValueReturningMethod(@Nonnull MethodNode source) {
		if (source.instructions == null)
			return null;

		// Create a new method that copies the instructions from the source method, but replaces the return instruction with a return of 0.
		MethodNode entry = new MethodNode(ACC_STATIC, "<clinit>", "()I", null, null);
		Map<LabelNode, LabelNode> labels = cloneLabels(source);
		for (AbstractInsnNode instruction : source.instructions) {
			if (instruction.getOpcode() == Opcodes.RETURN) {
				entry.instructions.add(new InsnNode(Opcodes.ICONST_0));
				entry.instructions.add(new InsnNode(Opcodes.IRETURN));
				break;
			}
			entry.instructions.add(instruction.clone(labels));
		}
		if (entry.instructions.size() == 0 || entry.instructions.getLast().getOpcode() != Opcodes.IRETURN)
			return null;
		entry.maxStack = Math.max(source.maxStack, 4);
		entry.maxLocals = source.maxLocals;
		return entry;
	}

	private static final class ClassState {
		private final ClassNode classNode;
		private final Metadata metadata;
		@Nullable
		private final MethodNode indexMethod;
		private boolean initialized;
		private boolean usable;
		private FieldCacheManager fieldCacheManager;
		private Evaluator evaluator;

		private ClassState(@Nonnull ClassNode classNode, @Nonnull Metadata metadata) {
			this.classNode = classNode;
			this.metadata = metadata;
			indexMethod = findMethod(classNode, metadata.indexMethod);
		}

		@Nullable
		private synchronized DecodedMember decode(@Nonnull JvmTransformerContext context,
		                                          @Nonnull Workspace workspace,
		                                          @Nonnull InheritanceGraph graph,
		                                          int maxSteps,
		                                          @Nonnull String operationName,
		                                          @Nonnull long[] metadataKeys) {
			// Initialize the evaluator and cache metadata once before resolving any slot.
			if (!initialize(context, workspace, graph, maxSteps) || indexMethod == null
					|| metadataKeys.length != metadata.metadataKeyCount)
				return null;

			// Use the call-site keys to recover the shared metadata slot.
			Integer slot = evaluateIndex(indexMethod, metadataKeys);
			if (slot == null)
				return null;

			// Read the descriptor string associated with the slot.
			String descriptor = readString(metadata.descriptorField, slot);
			if (descriptor == null)
				return null;

			// Split the backspace-delimited descriptor and validate its owner/member portion.
			String[] parts = descriptor.split("\u0008", -1);
			if (parts.length < 3 || parts[0].isEmpty() || parts[1].isEmpty())
				return null;

			// Resolve the encoded owner token before interpreting the remaining descriptor fields.
			Type ownerType = resolveClassToken(parts[0]);
			if (ownerType == null || ownerType.getSort() != Type.OBJECT)
				return null;

			// A trailing empty component indicates a method descriptor.
			// Otherwise, the final component is the field type.
			boolean methodDescriptor = parts.length > 3 && parts[parts.length - 1].isEmpty();
			int returnIndex = methodDescriptor ? parts.length - 2 : parts.length - 1;
			if (returnIndex < 2)
				return null;

			// Resolve each encoded parameter type in descriptor order.
			List<Type> parameterTypes = new ArrayList<>();
			for (int index = 2; index < returnIndex; index++) {
				Type parameterType = resolveClassToken(parts[index]);
				if (parameterType == null)
					return null;
				parameterTypes.add(parameterType);
			}

			// Resolve the final field or method return type.
			Type returnType = resolveClassToken(parts[returnIndex]);
			if (returnType == null)
				return null;

			// Keep the decoded member immutable so later call sites cannot mutate shared metadata.
			return new DecodedMember(operationName, ownerType.getInternalName(), parts[1], returnType,
					List.copyOf(parameterTypes));
		}

		private boolean initialize(@Nonnull JvmTransformerContext context,
		                           @Nonnull Workspace workspace,
		                           @Nonnull InheritanceGraph graph,
		                           int maxSteps) {
			if (initialized)
				return usable;
			initialized = true;
			try {
				// Try the complete initializer first so ordinary classes retain normal initialization semantics.
				fieldCacheManager = new FieldCacheManager();
				Evaluator initializedEvaluator = new Evaluator(workspace, context.newInterpreter(graph), fieldCacheManager, maxSteps, false, true);
				EvaluationResult initializerResult = initializedEvaluator.evaluateClassInitializer(classNode);
				if (initializerResult instanceof EvaluationYieldResult) {
					// Prefer a no-initializer snapshot so key evaluations cannot rerun unrelated class initializers.
					FieldCacheManager initializedCache = fieldCacheManager;
					Evaluator initializedIndexEvaluator = new Evaluator(workspace, context.newInterpreter(graph), initializedCache, maxSteps, false, false);
					fieldCacheManager = new FieldCacheManager();
					Evaluator isolatedEvaluator = new Evaluator(workspace, context.newInterpreter(graph), fieldCacheManager, maxSteps, false, false);
					boolean isolatedUsable = seedGeneratedCaches(isolatedEvaluator, workspace);
					if (isolatedUsable) {
						evaluator = isolatedEvaluator;
					} else {
						fieldCacheManager = initializedCache;
						evaluator = initializedIndexEvaluator;
					}
					usable = true;
					return true;
				}

				// Opaque initializer tails should not hide cache entries proven by a bounded local seed.
				fieldCacheManager.reset();
				Evaluator seedEvaluator = new Evaluator(workspace, context.newInterpreter(graph), fieldCacheManager, maxSteps, false, false);
				usable = seedGeneratedCaches(seedEvaluator, workspace);
				if (usable)
					evaluator = seedEvaluator;
				return usable;
			} catch (RuntimeException ignored) {
				usable = false;
				return false;
			}
		}

		private boolean seedGeneratedCaches(@Nonnull Evaluator seedEvaluator, @Nonnull Workspace workspace) {
			// Locate the original initializer that allocates the generated caches.
			MethodNode initializer = findMethod(classNode, new MethodKey("<clinit>", "()V"));
			if (initializer == null)
				return false;

			// The bounded decoder must see JVM defaults even when application flags are assigned later.
			seedStaticDefaults(workspace);

			// Prefer the complete initializer so all normal cache writes and initialization order are preserved.
			MethodNode initializerEntry = createValueReturningMethod(initializer);
			if (initializerEntry != null) {
				EvaluationResult initializerResult = seedEvaluator.evaluate(classNode, initializerEntry, null, List.of());
				if (initializerResult instanceof EvaluationYieldResult)
					return true;
			}

			// If opaque code prevents full evaluation, reset the cache and isolate only the generated cache allocation.
			fieldCacheManager.reset();
			seedStaticDefaults(workspace);
			MethodNode prefix = createCachePrefix(initializer, Set.of(metadata.objectField, metadata.descriptorField));
			if (prefix == null)
				return false;
			EvaluationResult prefixResult = seedEvaluator.evaluate(classNode, prefix, null, List.of());
			if (!(prefixResult instanceof EvaluationYieldResult))
				return false;

			// With both arrays allocated, execute the separate seed method that fills their known entries.
			MethodNode seed = findMethod(classNode, metadata.seedMethod);
			MethodNode seedEntry = seed == null ? null : createValueReturningMethod(seed);
			if (seedEntry == null)
				return false;
			EvaluationResult seedResult = seedEvaluator.evaluate(classNode, seedEntry, null, List.of());
			return seedResult instanceof EvaluationYieldResult;
		}

		private void seedStaticDefaults(@Nonnull Workspace workspace) {
			// Collect primitive fields read by the generated helpers so opaque flags have JVM defaults available.
			Set<FieldKey> fields = new HashSet<>();
			for (MethodNode method : classNode.methods) {
				if (method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions)
					if (instruction instanceof FieldInsnNode field && instruction.getOpcode() == GETSTATIC
							&& isPrimitiveDescriptor(field.desc))
						fields.add(new FieldKey(field.owner, field.name, field.desc));
			}

			// Seed each field only once so a known default or initializer result is never overwritten.
			for (FieldKey field : fields) {
				FieldCache cache = fieldCacheManager.getStaticFieldCache(field.owner);
				if (cache.containsField(field.owner, field.name, field.descriptor))
					continue;

				// Prefer the field's constant value, falling back to the JVM type default when absent.
				JvmClassInfo owner = findClass(workspace, field.owner);
				FieldMember member = owner == null ? null : owner.getDeclaredField(field.name, field.descriptor);
				Object constant = member == null ? null : member.getDefaultValue();
				ReValue value;
				try {
					value = constant == null ?
							ReValue.ofTypeDefaultValue(Type.getType(field.descriptor)) :
							ReValue.ofConstant(constant);
				} catch (Exception ignored) {
					// Invalid constants should not block evaluation of the generated cache helpers.
					try {
						value = ReValue.ofTypeDefaultValue(Type.getType(field.descriptor));
					} catch (Exception impossible) {
						continue;
					}
				}
				cache.setField(field.owner, field.name, field.descriptor, value);
			}
		}

		private static boolean isPrimitiveDescriptor(@Nonnull String descriptor) {
			return descriptor.length() == 1
					&& descriptor.charAt(0) != 'V'
					&& descriptor.charAt(0) != 'L'
					&& descriptor.charAt(0) != '[';
		}

		@Nullable
		private Integer evaluateIndex(@Nonnull MethodNode indexMethod, @Nonnull long[] metadataKeys) {
			List<ReValue> arguments = metadataKeys.length == 1
					? List.of(LongValue.of(metadataKeys[0]))
					: List.of(LongValue.of(metadataKeys[0]), LongValue.of(metadataKeys[1]));
			EvaluationResult result = evaluator.evaluate(classNode, indexMethod, null, arguments);
			if (!(result instanceof EvaluationYieldResult(ReValue value)) || !(value instanceof IntValue intValue))
				return null;
			OptionalInt opt = intValue.value();
			return opt.isPresent() ? opt.getAsInt() : null;
		}

		@Nullable
		private String readString(@Nonnull FieldKey field, int slot) {
			// Read the array from the evaluator cache rather than executing the generated accessor.
			FieldCache staticCache = fieldCacheManager.getStaticFieldCache(field.owner);
			ReValue value = staticCache.getField(field.owner, field.name, field.descriptor);
			if (!(value instanceof ArrayValue array)
					|| array.isNull()
					|| slot < 0
					|| array.getFirstDimensionLength().isEmpty()
					|| slot >= array.getFirstDimensionLength().getAsInt())
				return null;

			// Extract the requested slot only when the evaluator can prove it is in bounds.
			ReValue entry = array.getValue(slot);

			// Convert host-backed strings into the evaluator's regular StringValue representation.
			if (entry instanceof InstancedObjectValue<?> instanced && instanced.getRealInstance() != null)
				entry = instanced.unmap();

			// Refuse non-string or unknown values instead of guessing a descriptor.
			if (!(entry instanceof StringValue stringValue))
				return null;
			return stringValue.getText().orElse(null);
		}

		@Nullable
		private Type resolveClassToken(@Nonnull String token) {
			long key;
			try {
				key = Long.parseLong(token, 36);
			} catch (NumberFormatException ignored) {
				return null;
			}
			if (indexMethod == null)
				return null;
			long[] classTokenKeys = metadata.metadataKeyCount == 1 ? new long[]{key} : new long[]{key, 0L};
			Integer slot = evaluateIndex(indexMethod, classTokenKeys);
			if (slot == null || slot < 0)
				return null;
			Type direct = metadata.classTypes.get(slot);
			return direct != null ? direct : parseClassName(readString(metadata.descriptorField, slot));
		}
	}

	/**
	 * Generated helper method reference.
	 *
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 */
	record MethodKey(@Nonnull String name, @Nonnull String descriptor) {}

	/**
	 * Generated cache field reference.
	 *
	 * @param owner
	 * 		Field owner.
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 */
	record FieldKey(@Nonnull String owner, @Nonnull String name, @Nonnull String descriptor) {}

	/**
	 * Structural members belonging to one generated ZKM helper component.
	 *
	 * @param helperMethods
	 * 		Generated static methods that are part of the helper component.
	 * @param helperFields
	 * 		Generated cache and storage fields owned by the helper component.
	 * @param stateFields
	 * 		Mutable primitive state fields used by helper control flow.
	 * @param objectField
	 * 		Primary object cache field, or {@code null} for residual metadata.
	 * @param descriptorField
	 * 		Primary descriptor cache field, or {@code null} for residual metadata.
	 */
	record CleanupMetadata(@Nonnull Set<MethodKey> helperMethods,
	                       @Nonnull Set<FieldKey> helperFields,
	                       @Nonnull Set<FieldKey> stateFields,
	                       @Nullable FieldKey objectField,
	                       @Nullable FieldKey descriptorField) {}

	/**
	 * Helper and metadata state prepared before either key source is read.
	 *
	 * @param helperOwner
	 * 		Bootstrap/helper owner name.
	 * @param helperNode
	 * 		Current helper owner node.
	 * @param metadata
	 * 		Metadata discovered from the helper node.
	 * @param metadataKeyCount
	 * 		Validated number of trailing metadata keys.
	 */
	private record PreparedSite(@Nonnull String helperOwner,
	                            @Nonnull ClassNode helperNode,
	                            @Nonnull Metadata metadata,
	                            int metadataKeyCount) {}

	/**
	 * Immutable metadata discovered from one generated class.
	 *
	 * @param indexMethod
	 * 		Index helper method.
	 * @param metadataKeyCount
	 * 		Number of long metadata keys accepted by the index helper.
	 * @param objectField
	 * 		Object cache field.
	 * @param descriptorField
	 * 		Descriptor cache field.
	 * @param seedMethod
	 * 		Generated cache-seed method.
	 * @param classTypes
	 * 		Direct primitive class constants keyed by cache slot.
	 * @param operationCodes
	 * 		Operation selector values from the generated bootstrap method.
	 */
	private record Metadata(@Nonnull MethodKey indexMethod,
	                        int metadataKeyCount,
	                        @Nonnull FieldKey objectField,
	                        @Nonnull FieldKey descriptorField,
	                        @Nonnull MethodKey seedMethod,
	                        @Nonnull Map<Integer, Type> classTypes,
	                        @Nonnull OperationCodes operationCodes) {}

	/**
	 * Operation selector values recovered from one generated bootstrap method.
	 *
	 * @param getField
	 * 		Operation selecting an instance field getter.
	 * @param putField
	 * 		Operation selecting an instance field setter.
	 * @param getStatic
	 * 		Operation selecting a static field getter.
	 * @param putStatic
	 * 		Operation selecting a static field setter.
	 * @param invokeVirtual
	 * 		Operation selecting a virtual method call.
	 * @param invokeStatic
	 * 		Operation selecting a static method call.
	 */
	private record OperationCodes(int getField, int putField, int getStatic, int putStatic,
	                              int invokeVirtual, int invokeStatic) {
		private static final OperationCodes EMPTY = new OperationCodes(-1, -1, -1, -1, -1, -1);

		private boolean hasKnownOperation() {
			return getField >= 0 || putField >= 0 || getStatic >= 0 || putStatic >= 0 || invokeVirtual >= 0 || invokeStatic >= 0;
		}
	}

	/**
	 * Descriptor decoded from one ZKM cache slot.
	 *
	 * @param operationName
	 * 		Encoded operation name.
	 * @param owner
	 * 		Resolved member owner.
	 * @param memberName
	 * 		Member name.
	 * @param returnType
	 * 		Field type or method return type.
	 * @param parameterTypes
	 * 		Method parameter types.
	 */
	private record DecodedMember(@Nonnull String operationName,
	                             @Nonnull String owner,
	                             @Nonnull String memberName,
	                             @Nonnull Type returnType,
	                             @Nonnull List<Type> parameterTypes) {}

	/**
	 * Resolved field and declaring metadata.
	 *
	 * @param ownerName
	 * 		Declaring owner name.
	 * @param ownerInfo
	 * 		Declaring owner metadata.
	 * @param member
	 * 		Resolved field.
	 */
	private record FieldMatch(@Nonnull String ownerName,
	                          @Nonnull JvmClassInfo ownerInfo,
	                          @Nonnull FieldMember member) {}

	/**
	 * Resolved method and declaring metadata.
	 *
	 * @param ownerName
	 * 		Declaring owner name.
	 * @param ownerInfo
	 * 		Declaring owner metadata.
	 * @param member
	 * 		Resolved method.
	 */
	private record MethodMatch(@Nonnull String ownerName,
	                           @Nonnull JvmClassInfo ownerInfo,
	                           @Nonnull MethodMember member) {}
}
