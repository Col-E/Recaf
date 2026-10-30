package software.coley.recaf.services.deobfuscation.transform.specific.zkm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.deobfuscation.transform.generic.InvokeDynamicInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StaticValueCollectionTransformer;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static software.coley.recaf.services.deobfuscation.transform.generic.OpaqueConstantFoldingTransformer.toInsn;
import static software.coley.recaf.util.AsmInsnUtil.getPreviousInsn;
import static software.coley.recaf.util.Types.isPrimitive;
import static software.coley.recaf.util.Types.isWide;

/**
 * Removes residual junk after ZKM {@code invokedynamic} sites have been inlined.
 *
 * @author Matt Coley
 */
@Dependent
public class ZkmDecryptionCleanupTransformer implements JvmClassTransformer {
	private static final Logger logger = Logging.get(ZkmDecryptionCleanupTransformer.class);

	public static final String IDENTIFIER = "specific.zkm.cleanup";

	private Map<String, ZkmInvokeDynamicResolver.CleanupMetadata> initialMetadata = Map.of();
	private Set<ZkmInvokeDynamicResolver.FieldKey> initialStateFields = Set.of();
	private boolean stateAccessesNormalized;
	private boolean warnedAboutMissingInliner;

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {
		// We cannot reliably clean things up if we are not sure the inliner also will run in this execution.
		// If it isn't present then we could remove things needed for the inliner to work properly, so we skip cleanup entirely.
		if (context.getOptionalTransformer(InvokeDynamicInliningTransformer.class) == null)
			return;

		// Collect all the reference encryption components for all classes in the workspace.
		// We need to do this for the entire workspace because the state fields are shared across classes,
		// so we need to know all of them before we can normalize state accesses.
		Map<String, ZkmInvokeDynamicResolver.CleanupMetadata> metadata = new HashMap<>();
		workspace.getPrimaryResource().jvmAllClassBundleStreamRecursive().forEach(bundle ->
				bundle.forEach(info -> {
					ClassNode node = context.getNode(bundle, info);
					ZkmInvokeDynamicResolver.CleanupMetadata value = ZkmInvokeDynamicResolver.discoverCleanupMetadata(node);
					if (value != null)
						metadata.put(info.getName(), value);
				}));
		initialMetadata = Map.copyOf(metadata);

		// Keep a copy of all the state fields across all classes.
		Set<ZkmInvokeDynamicResolver.FieldKey> stateFields = new HashSet<>();
		for (ZkmInvokeDynamicResolver.CleanupMetadata value : metadata.values())
			stateFields.addAll(value.stateFields());
		initialStateFields = Set.copyOf(stateFields);
		stateAccessesNormalized = false;
	}

	@Override
	public synchronized void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                                   @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                                   @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		// As stated above, if we don't know the inliner is running we cannot safely clean up anything, so we skip the entire pass.
		if (context.getOptionalTransformer(InvokeDynamicInliningTransformer.class) == null) {
			if (!warnedAboutMissingInliner) {
				warnedAboutMissingInliner = true;
				logger.warn("ZKM cleanup transformer is running without the inliner. "
						+ "This would leave behind dead helpers that are still referenced by unresolved ZKM call sites. " +
						"Skipping cleanup.");
			}
			return;
		}

		// Fold state-field reads to the mutable static fields and drop writes that feed nothing before any class is removed.
		// This applies to all classes in the workspace, not just the one being cleaned, because state fields are shared across classes.
		// The method is synchronized to avoid multiple threads trying to normalize state accesses at the same time.
		if (!stateAccessesNormalized) {
			normalizeStateAccesses(context, workspace, initialStateFields);
			stateAccessesNormalized = true;
		}

		ClassNode node = context.getNode(bundle, initialClassState);
		Map<String, ClassNode> nodes = currentNodes(context, resource);

		// The bootstrap adapter is the outermost dead layer once sites are inlined. Strip it first so the
		// remaining "does anything still reference this?" checks aren't fooled by a handle pointing at it.
		if (removeUnreferencedBootstrapMethods(node, nodes)) {
			context.setRecomputeFrames(node.name);
			context.setNode(bundle, initialClassState, node);
			nodes.put(node.name, node);
		}

		// Get the metadata for this class.
		// There shouldn't be any reason for it to be missing, but if it is then we can't clean up anything.
		ZkmInvokeDynamicResolver.CleanupMetadata metadata = initialMetadata.get(node.name);
		if (metadata == null)
			metadata = ZkmInvokeDynamicResolver.discoverCleanupMetadata(node);
		if (metadata == null || !hasValidCandidates(node, metadata))
			return;

		// The component is still live if any class keeps an unresolved ZKM site bootstrapped here.
		if (hasRemainingZkmSite(nodes, node.name))
			return;

		// When only part of the component is still referenced from outside, we can't delete the whole thing.
		// We can still dead-code-eliminate the helpers nothing reaches, shrinking the leftovers in place.
		if (hasExternalReference(nodes, node.name, metadata, initialMetadata)) {
			if (removeUnreachableHelperMethods(node, metadata, nodes)) {
				context.setRecomputeFrames(node.name);
				context.setNode(bundle, initialClassState, node);
			}
			return;
		}

		// State fields are shared across classes, so each one is only removable if no external code touches it.
		Set<ZkmInvokeDynamicResolver.FieldKey> removableStateFields = removableStateFields(nodes, metadata, initialMetadata);

		// Check if the static initializer can be cleaned up. We want to remove the ZKM bits not any application code.
		StaticValueCollectionTransformer collector = context.getOptionalTransformer(StaticValueCollectionTransformer.class);
		MethodNode initializer = findMethod(node, "<clinit>", "()V");
		List<FieldInitialization> retainedInitializers = new ArrayList<>();
		if (initializer != null) {
			if (!collectRetainedInitializers(nodes, node, initializer, metadata, removableStateFields, collector, retainedInitializers)
					|| !isSupportedInitializer(initializer, node.name, metadata))
				return;
		}

		// If the static initializer is now empty, remove it entirely. Otherwise, replace it with a new body that only
		// initializes the fields that are still referenced from outside. This keeps the class valid while removing the ZKM bits.
		if (initializer != null) {
			if (retainedInitializers.isEmpty()) {
				node.methods.remove(initializer);
			} else {
				// TODO: This model is fragile. It assumes any fields written to in the static initializer are simple
				//  enough to be represented with a single constant-pushing instruction. For anything more complex,
				//  the initializer isn't cleaned up and we leave the ZKM garbage in place.
				//  Only applicable for simple classes (which in practice most are).
				InsnList replacement = new InsnList();
				for (FieldInitialization field : retainedInitializers) {
					replacement.add(field.valueInstruction());
					replacement.add(new FieldInsnNode(Opcodes.PUTSTATIC, node.name, field.name(), field.descriptor()));
				}
				replacement.add(new InsnNode(Opcodes.RETURN));
				initializer.instructions = replacement;
				initializer.tryCatchBlocks.clear();
				initializer.localVariables.clear();
				initializer.maxStack = 2;
				initializer.maxLocals = 0;
			}
		}

		// By this point no external references to our ZKM component should remain.
		// We can now delete the helper methods and fields that were only used by the component,
		// as well as any state fields that are no longer referenced.
		Set<ZkmInvokeDynamicResolver.MethodKey> helperMethods = metadata.helperMethods();
		Set<ZkmInvokeDynamicResolver.FieldKey> helperFields = metadata.helperFields();
		node.methods.removeIf(method -> helperMethods.contains(new ZkmInvokeDynamicResolver.MethodKey(method.name, method.desc)));
		node.fields.removeIf(field -> {
			ZkmInvokeDynamicResolver.FieldKey key = new ZkmInvokeDynamicResolver.FieldKey(node.name, field.name, field.desc);
			boolean generatedString = isUnreferencedGeneratedStringField(nodes, node.name, field, helperMethods);
			boolean generatedPrimitive = isUnreferencedGeneratedPrimitiveField(nodes, node.name, field, helperMethods);
			return helperFields.contains(key) || removableStateFields.contains(key) || generatedString || generatedPrimitive;
		});

		// Removing this component may have orphaned state accesses in other classes that were previously kept
		// alive as external references. Sweep once more to fold the newly-dead reads and writes.
		if (!initialStateFields.isEmpty())
			normalizeStateAccesses(context, workspace, initialStateFields);
		context.setRecomputeFrames(node.name);
		context.setNode(bundle, initialClassState, node);
	}

	/**
	 * Replaces {@code getstatic} state reads with the constant last written to the field.
	 * If we know that nothing has been written to the field, we use a default value for the type.
	 * This turns ZKM's opaque predicates into ordinary constant comparisons.
	 *
	 * @param node
	 * 		Class whose methods are scanned for state accesses.
	 * @param stateFields
	 * 		Control-state fields to fold.
	 * @param allMetadata
	 * 		Metadata for every class, used to skip component methods.
	 *
	 * @return {@code true} when any instruction was rewritten.
	 */
	private static boolean rewriteStateAccesses(@Nonnull ClassNode node,
	                                            @Nonnull Set<ZkmInvokeDynamicResolver.FieldKey> stateFields,
	                                            @Nonnull Map<String, ZkmInvokeDynamicResolver.CleanupMetadata> allMetadata) {
		boolean dirty = false;
		for (MethodNode method : node.methods) {
			// Component methods own this state by design, so skip them and only fold reads in application code.
			// We'll delete the component methods later once we know nothing else references them.
			if (isComponentMethod(node.name, method, allMetadata) || method.instructions == null)
				continue;

			// Seed each field with its default value so a read before any write still folds correctly.
			Map<ZkmInvokeDynamicResolver.FieldKey, AbstractInsnNode> known = new HashMap<>();
			for (ZkmInvokeDynamicResolver.FieldKey state : stateFields) {
				AbstractInsnNode defaultValue = defaultStateInstruction(state.descriptor());
				if (defaultValue != null)
					known.put(state, defaultValue);
			}

			// Iterate through the instructions, tracking the last known value for each state field.
			for (AbstractInsnNode instruction : method.instructions.toArray()) {
				// A branch merge point has unknown state, so anything known before the jump is no longer safe.
				if (instruction instanceof JumpInsnNode
						|| instruction instanceof TableSwitchInsnNode
						|| instruction instanceof LookupSwitchInsnNode) {
					known.clear();
					continue;
				}

				// Skip any non-field.
				if (!(instruction instanceof FieldInsnNode field))
					continue;

				// Skip if the field isn't one of the state fields we're tracking.
				ZkmInvokeDynamicResolver.FieldKey key = new ZkmInvokeDynamicResolver.FieldKey(field.owner, field.name, field.desc);
				if (!stateFields.contains(key))
					continue;

				// Swap reads for the known constant.
				// Track writes to the field so future reads can be folded to the new value.
				if (field.getOpcode() == Opcodes.GETSTATIC) {
					AbstractInsnNode knownValue = known.get(key);
					if (knownValue == null)
						continue;
					method.instructions.set(field, knownValue.clone(Map.of()));
					dirty = true;
				} else if (field.getOpcode() == Opcodes.PUTSTATIC) {
					AbstractInsnNode producer = getPreviousInsn(field);
					AbstractInsnNode constant = constantStateInstruction(producer, field.desc);
					if (constant == null)
						known.remove(key);
					else
						known.put(key, constant);
				}
			}
		}
		return dirty;
	}

	/**
	 * Removes {@code putstatic} writes to state fields that are no longer referenced.
	 *
	 * @param node
	 * 		Class whose methods are scanned for dead state writes.
	 * @param stateFields
	 * 		Control-state fields whose writes may be dropped.
	 * @param survivingReads
	 * 		State fields that still have a live read somewhere.
	 * @param allMetadata
	 * 		Metadata for every class, used to skip component methods.
	 *
	 * @return {@code true} when any write was replaced.
	 */
	private static boolean removeUnobservedStateWrites(@Nonnull ClassNode node,
	                                                   @Nonnull Set<ZkmInvokeDynamicResolver.FieldKey> stateFields,
	                                                   @Nonnull Set<ZkmInvokeDynamicResolver.FieldKey> survivingReads,
	                                                   @Nonnull Map<String, ZkmInvokeDynamicResolver.CleanupMetadata> allMetadata) {
		boolean dirty = false;
		for (MethodNode method : node.methods) {
			// Same as above, skip these as we will delete them later if nothing else references them.
			if (isComponentMethod(node.name, method, allMetadata) || method.instructions == null)
				continue;

			// Remove any writes to state fields that no surviving read observes.
			// These writes are noise and can be dropped now that they aren't used.
			for (AbstractInsnNode instruction : method.instructions.toArray()) {
				if (instruction.getOpcode() != Opcodes.PUTSTATIC || !(instruction instanceof FieldInsnNode field))
					continue;
				ZkmInvokeDynamicResolver.FieldKey key = new ZkmInvokeDynamicResolver.FieldKey(field.owner, field.name, field.desc);
				if (stateFields.contains(key) && !survivingReads.contains(key)) {
					method.instructions.set(field, new InsnNode(isWide(field.desc) ? Opcodes.POP2 : Opcodes.POP));
					dirty = true;
				}
			}
		}
		return dirty;
	}

	/**
	 * Validates that a producer instruction is a literal that can be cloned into a folded read.
	 * <p>
	 * For example, an {@code int} state field accepts {@code iconst_*} through {@code sipush} plus an {@code ldc}
	 * of an {@link Integer}. Anything unrecognized returns {@code null}.
	 *
	 * @param producer
	 * 		Instruction that produces the value being stored, or {@code null} when there is none.
	 * @param descriptor
	 * 		Descriptor of the field being written.
	 *
	 * @return The constant-loading instruction, or {@code null} when the producer is not a valid literal for the given type.
	 */
	@Nullable
	private static AbstractInsnNode constantStateInstruction(@Nullable AbstractInsnNode producer, @Nonnull String descriptor) {
		if (producer == null)
			return null;

		int sort;
		try {
			sort = Type.getType(descriptor).getSort();
		} catch (Throwable ignored) {
			return null;
		}

		int opcode = producer.getOpcode();
		if (sort == Type.BOOLEAN || sort == Type.BYTE || sort == Type.CHAR || sort == Type.SHORT || sort == Type.INT) {
			if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5 || opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH)
				return producer;
			return producer instanceof LdcInsnNode ldc && ldc.cst instanceof Integer ? producer : null;
		}
		if (sort == Type.FLOAT) {
			if (opcode >= Opcodes.FCONST_0 && opcode <= Opcodes.FCONST_2)
				return producer;
			return producer instanceof LdcInsnNode ldc && ldc.cst instanceof Float ? producer : null;
		}
		if (sort == Type.LONG) {
			if (opcode == Opcodes.LCONST_0 || opcode == Opcodes.LCONST_1)
				return producer;
			return producer instanceof LdcInsnNode ldc && ldc.cst instanceof Long ? producer : null;
		}
		if (sort == Type.DOUBLE) {
			if (opcode == Opcodes.DCONST_0 || opcode == Opcodes.DCONST_1)
				return producer;
			return producer instanceof LdcInsnNode ldc && ldc.cst instanceof Double ? producer : null;
		}
		return null;
	}

	/**
	 * Produces the default value providing instruction for a primitive descriptor. Before the mutable state fields are
	 * written to they can still operate off the assumption of using this default value.
	 *
	 * @param descriptor
	 * 		Field descriptor to map to its default instruction.
	 *
	 * @return Instruction to push {@code 0} for the given type, or {@code null} when the descriptor has no supported default.
	 */
	@Nullable
	private static AbstractInsnNode defaultStateInstruction(@Nonnull String descriptor) {
		try {
			return switch (Type.getType(descriptor).getSort()) {
				case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> AsmInsnUtil.intToInsn(0);
				case Type.FLOAT -> AsmInsnUtil.floatToInsn(0F);
				case Type.LONG -> AsmInsnUtil.longToInsn(0L);
				case Type.DOUBLE -> AsmInsnUtil.doubleToInsn(0D);
				default -> null;
			};
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	/**
	 * Normalizes all state accesses across the workspace. This is a multi-pass process:
	 * <ol>
	 *     <li>Fold every read to a constant, tracking which classes changed.</li>
	 *     <li>Collect the surviving reads, which are the only ones that still observe the field.</li>
	 *     <li>Drop writes to fields with no surviving read, as they are pure noise.</li>
	 * </ol>
	 *
	 * @param context
	 * 		Transformer context that holds the transformed workspace classes.
	 * @param workspace
	 * 		Workspace to pull classes from not yet present in the context.
	 * @param stateFields
	 * 		State fields to normalize.
	 */
	private void normalizeStateAccesses(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                                    @Nonnull Set<ZkmInvokeDynamicResolver.FieldKey> stateFields) {
		if (stateFields.isEmpty())
			return;
		Map<String, ClassNode> nodes = currentNodes(context, workspace.getPrimaryResource());
		Map<String, Boolean> dirtyByClass = new HashMap<>();

		// Pass 1: fold every state read to a constant, tracking which classes changed.
		for (Map.Entry<String, ClassNode> entry : nodes.entrySet())
			dirtyByClass.put(entry.getKey(), rewriteStateAccesses(entry.getValue(), stateFields, initialMetadata));

		// Pass 2: the reads that survived folding are the only ones that still observe the field, so they are
		// the roots whose writes must be preserved.
		Set<ZkmInvokeDynamicResolver.FieldKey> survivingReads = new HashSet<>();
		for (Map.Entry<String, ClassNode> entry : nodes.entrySet()) {
			ClassNode node = entry.getValue();
			for (MethodNode method : node.methods) {
				if (isComponentMethod(node.name, method, initialMetadata) || method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions) {
					if (!(instruction instanceof FieldInsnNode field) || field.getOpcode() != Opcodes.GETSTATIC)
						continue;
					ZkmInvokeDynamicResolver.FieldKey key = new ZkmInvokeDynamicResolver.FieldKey(field.owner, field.name, field.desc);
					if (stateFields.contains(key))
						survivingReads.add(key);
				}
			}
		}

		// Pass 3: writes to fields with no surviving read are pure noise and can be dropped.
		for (Map.Entry<String, ClassNode> entry : nodes.entrySet()) {
			boolean writesDirty = removeUnobservedStateWrites(entry.getValue(), stateFields, survivingReads, initialMetadata);
			dirtyByClass.put(entry.getKey(), dirtyByClass.getOrDefault(entry.getKey(), false) || writesDirty);
		}

		// Record changed classes into the context.
		workspace.getPrimaryResource().jvmAllClassBundleStreamRecursive().forEach(bundle ->
				bundle.forEach(info -> {
					ClassNode node = nodes.get(info.getName());
					if (node == null || !Boolean.TRUE.equals(dirtyByClass.get(info.getName())))
						return;
					context.setRecomputeFrames(node.name);
					context.setNode(bundle, info, node);
				}));
	}

	/**
	 * @param context
	 * 		Transformer context that holds the transformed workspace classes.
	 * @param resource
	 * 		Workspace resource to pull classes from not yet present in the context.
	 *
	 * @return Map of class name to current {@link ClassNode}s.
	 */
	@Nonnull
	private static Map<String, ClassNode> currentNodes(@Nonnull JvmTransformerContext context,
	                                                   @Nonnull WorkspaceResource resource) {
		Map<String, ClassNode> nodes = new HashMap<>();
		resource.jvmAllClassBundleStreamRecursive()
				.forEach(bundle -> bundle.forEach(c -> nodes.put(c.getName(), context.getNode(bundle, c))));
		return nodes;
	}

	/**
	 * Deletes private-static bootstrap adapters nothing references anymore.
	 * These are the first thing to go after inlining. Removing them allows reference scans to properly identity other
	 * helper methods are no longer referenced.
	 *
	 * @param node
	 * 		Class being cleaned.
	 * @param nodes
	 * 		Current view of every class, used to check for references.
	 *
	 * @return {@code true} when a bootstrap method was removed.
	 */
	private static boolean removeUnreferencedBootstrapMethods(@Nonnull ClassNode node,
	                                                          @Nonnull Map<String, ClassNode> nodes) {
		boolean dirty = false;
		for (MethodNode method : List.copyOf(node.methods)) {
			if (!isBootstrapCandidate(method)
					|| hasMethodReference(nodes, node.name, method.name, method.desc))
				continue;
			node.methods.remove(method);
			dirty = true;
		}
		return dirty;
	}

	/**
	 * The ZKM bootstrap looks like this:
	 * <pre>{@code
	 * private static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type) {
	 *     // Decode the member, then link the call site.
	 * }
	 * }</pre>
	 *
	 * @param method
	 * 		Method to check.
	 *
	 * @return {@code true} when the method matches the bootstrap shape.
	 */
	private static boolean isBootstrapCandidate(@Nonnull MethodNode method) {
		return (method.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)
				&& ZkmInvokeDynamicResolver.BOOTSTRAP_DESCRIPTOR.equals(method.desc)
				&& !"<init>".equals(method.name) && !"<clinit>".equals(method.name);
	}

	/**
	 * Reports whether any class still references the given bootstrap method. Possible reference sources include:
	 * <ul>
	 *     <li>Direct method calls</li>
	 *     <li>{@code invokedynamic} bootstrap handles + args</li>
	 *     <li>{@code ldc} constants</li>
	 *     <li>Annotations</li>
	 * </ul>
	 *
	 * @param nodes
	 * 		Current view of every class.
	 * @param owner
	 * 		Class that declares the method.
	 * @param name
	 * 		Name of the method.
	 * @param descriptor
	 * 		Descriptor of the method.
	 *
	 * @return {@code true} when the method is still referenced.
	 */
	private static boolean hasMethodReference(@Nonnull Map<String, ClassNode> nodes, @Nonnull String owner,
	                                          @Nonnull String name, @Nonnull String descriptor) {
		Set<ZkmInvokeDynamicResolver.MethodKey> methods = Set.of(new ZkmInvokeDynamicResolver.MethodKey(name, descriptor));
		Set<ZkmInvokeDynamicResolver.FieldKey> fields = Set.of();
		for (ClassNode node : nodes.values()) {
			for (MethodNode method : node.methods) {
				// Check for annotation constants.
				if (containsComponentAnnotations(method, owner, methods, fields))
					return true;

				// Check for direct method calls, bootstrap handles, and constant references.
				if (method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions) {
					if (instruction instanceof MethodInsnNode call
							&& owner.equals(call.owner)
							&& name.equals(call.name)
							&& descriptor.equals(call.desc))
						return true;
					if (instruction instanceof InvokeDynamicInsnNode indy
							&& (containsComponentValue(indy.bsm, owner, methods, fields)
							|| containsComponentValues(indy.bsmArgs, owner, methods, fields)))
						return true;
					if (instruction instanceof LdcInsnNode ldc && containsComponentValue(ldc.cst, owner, methods, fields))
						return true;
				}
			}

			// Check for class-level annotations.
			if (containsComponentClassMetadata(node, owner, methods, fields))
				return true;
		}
		return false;
	}

	/**
	 * @param node
	 * 		Class being cleaned.
	 * @param metadata
	 * 		Component metadata to validate.
	 *
	 * @return {@code true} when at least one helper and field still look safely removable.
	 */
	private static boolean hasValidCandidates(@Nonnull ClassNode node,
	                                          @Nonnull ZkmInvokeDynamicResolver.CleanupMetadata metadata) {
		boolean found = false;
		for (ZkmInvokeDynamicResolver.MethodKey key : metadata.helperMethods()) {
			// Method must still exist.
			MethodNode method = findMethod(node, key.name(), key.descriptor());
			if (method == null)
				continue;

			// Generated shared-helper adapters can be public, but every helper must still be static and concrete.
			found = true;
			if ((method.access & Opcodes.ACC_STATIC) == 0
					|| (method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0
					|| "<init>".equals(method.name) || "<clinit>".equals(method.name))
				return false;
		}
		for (ZkmInvokeDynamicResolver.FieldKey key : metadata.helperFields()) {
			if (!node.name.equals(key.owner()))
				return false;

			// Field must still exist.
			FieldNode field = findField(node, key.name(), key.descriptor());
			if (field == null)
				continue;
			found = true;

			// Must be static, but can be any visibility.
			if ((field.access & Opcodes.ACC_STATIC) == 0)
				return false;
		}
		return found;
	}

	/**
	 * @param nodes
	 * 		Current view of every class.
	 * @param owner
	 * 		Class that declares the component.
	 *
	 * @return {@code true} when an unresolved site still targets this class's bootstrap.
	 */
	private static boolean hasRemainingZkmSite(@Nonnull Map<String, ClassNode> nodes, @Nonnull String owner) {
		for (ClassNode node : nodes.values()) {
			for (MethodNode method : node.methods) {
				if (method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions) {
					if (instruction instanceof InvokeDynamicInsnNode indy
							&& indy.bsm != null
							&& owner.equals(indy.bsm.getOwner())
							&& ZkmInvokeDynamicResolver.BOOTSTRAP_DESCRIPTOR.equals(indy.bsm.getDesc()))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param nodes
	 * 		Current view of every class.
	 * @param targetOwner
	 * 		Class whose component is being checked.
	 * @param metadata
	 * 		Component metadata for the target class.
	 * @param allMetadata
	 * 		Metadata for every class, used to skip component methods.
	 *
	 * @return {@code true} when external code still references the component.
	 */
	private static boolean hasExternalReference(@Nonnull Map<String, ClassNode> nodes, @Nonnull String targetOwner,
	                                            @Nonnull ZkmInvokeDynamicResolver.CleanupMetadata metadata,
	                                            @Nonnull Map<String, ZkmInvokeDynamicResolver.CleanupMetadata> allMetadata) {
		Set<ZkmInvokeDynamicResolver.MethodKey> helpers = metadata.helperMethods();
		Set<ZkmInvokeDynamicResolver.FieldKey> fields = new HashSet<>(metadata.helperFields());
		fields.addAll(metadata.stateFields());
		for (Map.Entry<String, ClassNode> entry : nodes.entrySet()) {
			String callerOwner = entry.getKey();
			ClassNode node = entry.getValue();
			for (MethodNode method : node.methods) {
				// Skip component methods, obviously they are going to reference each other...
				boolean helperCaller = isComponentMethod(callerOwner, method, allMetadata);
				if (helperCaller)
					continue;

				// Check for annotation constants.
				if (containsComponentAnnotations(method, targetOwner, helpers, fields))
					return true;

				// Check for direct method calls, bootstrap handles, and constant references.
				if (method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions) {
					if (instruction instanceof MethodInsnNode call && targetOwner.equals(call.owner)
							&& helpers.contains(new ZkmInvokeDynamicResolver.MethodKey(call.name, call.desc)))
						return true;
					if (instruction instanceof FieldInsnNode field && targetOwner.equals(field.owner)) {
						ZkmInvokeDynamicResolver.FieldKey key = new ZkmInvokeDynamicResolver.FieldKey(field.owner, field.name, field.desc);
						if (fields.contains(key)
								&& (field.getOpcode() != Opcodes.PUTSTATIC
								|| !metadata.stateFields().contains(key)))
							return true;
					}
					if (instruction instanceof InvokeDynamicInsnNode indy
							&& (containsComponentValue(indy.bsm, targetOwner, helpers, fields)
							|| containsComponentValues(indy.bsmArgs, targetOwner, helpers, fields)))
						return true;
					if (instruction instanceof LdcInsnNode ldc && containsComponentValue(ldc.cst, targetOwner, helpers, fields))
						return true;
				}
			}
			if (containsComponentClassMetadata(node, targetOwner, helpers, fields))
				return true;
		}
		return false;
	}

	/**
	 * @param owner
	 * 		Name of the class declaring the method.
	 * @param method
	 * 		Method to classify.
	 * @param allMetadata
	 * 		Metadata for every class.
	 *
	 * @return {@code true} when the method is part of its class's component.
	 */
	private static boolean isComponentMethod(@Nonnull String owner, @Nonnull MethodNode method,
	                                         @Nonnull Map<String, ZkmInvokeDynamicResolver.CleanupMetadata> allMetadata) {
		ZkmInvokeDynamicResolver.CleanupMetadata metadata = allMetadata.get(owner);
		if (metadata == null)
			return false;
		return "<clinit>".equals(method.name)
				|| metadata.helperMethods().contains(new ZkmInvokeDynamicResolver.MethodKey(method.name, method.desc));
	}

	/**
	 * Dead-code elimination for the partial-removal path. Scans for unreachable helpers and deletes them from the class.
	 *
	 * @param target
	 * 		Class whose helpers are pruned.
	 * @param metadata
	 * 		Component metadata for the target class.
	 * @param nodes
	 * 		Current view of every class.
	 *
	 * @return {@code true} when a helper method was removed.
	 */
	private static boolean removeUnreachableHelperMethods(@Nonnull ClassNode target,
	                                                      @Nonnull ZkmInvokeDynamicResolver.CleanupMetadata metadata,
	                                                      @Nonnull Map<String, ClassNode> nodes) {
		Set<ZkmInvokeDynamicResolver.MethodKey> helpers = metadata.helperMethods();
		Set<ZkmInvokeDynamicResolver.MethodKey> live = new HashSet<>();
		for (Map.Entry<String, ClassNode> entry : nodes.entrySet()) {
			String owner = entry.getKey();

			// Collect calls to helpers from application code and the static initializer.
			// Component methods are skipped because they are only reachable through other helpers.
			for (MethodNode method : entry.getValue().methods) {
				ZkmInvokeDynamicResolver.MethodKey key = new ZkmInvokeDynamicResolver.MethodKey(method.name, method.desc);
				boolean generated = target.name.equals(owner) && helpers.contains(key);
				if (generated && !"<clinit>".equals(method.name))
					continue;

				collectHelperCalls(method, target.name, helpers, live);
			}
		}

		// Iteratively scan the reachable set for calls to other helpers until no new ones are found.
		Set<ZkmInvokeDynamicResolver.MethodKey> reachable = new HashSet<>(live);
		boolean changed;
		do {
			changed = false;
			for (ZkmInvokeDynamicResolver.MethodKey key : List.copyOf(reachable)) {
				MethodNode method = findMethod(target, key.name(), key.descriptor());
				if (method == null)
					continue;

				int before = reachable.size();
				collectHelperCalls(method, target.name, helpers, reachable);
				changed |= before != reachable.size();
			}
		} while (changed);

		// Remove any helpers that are not reachable from the live roots.
		Set<ZkmInvokeDynamicResolver.MethodKey> removable = new HashSet<>(helpers);
		removable.removeAll(reachable);
		if (removable.isEmpty())
			return false;
		return target.methods.removeIf(method -> removable.contains(new ZkmInvokeDynamicResolver.MethodKey(method.name, method.desc)));
	}

	/**
	 * Adds helpers this method can reach to the output set.
	 *
	 * @param method
	 * 		Method whose instructions are scanned.
	 * @param owner
	 * 		Class that declares the helpers.
	 * @param helpers
	 * 		Helper methods that count as component members.
	 * @param output
	 * 		Set that receives the reachable helpers.
	 */
	private static void collectHelperCalls(@Nonnull MethodNode method, @Nonnull String owner,
	                                       @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> helpers,
	                                       @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> output) {
		if (method.instructions == null)
			return;
		for (AbstractInsnNode instruction : method.instructions) {
			if (instruction instanceof MethodInsnNode call && owner.equals(call.owner)) {
				// Follow direct calls to helpers.
				ZkmInvokeDynamicResolver.MethodKey key = new ZkmInvokeDynamicResolver.MethodKey(call.name, call.desc);
				if (helpers.contains(key))
					output.add(key);
			} else if (instruction instanceof InvokeDynamicInsnNode indy) {
				// Follow invokedynamic bootstrap handles and arguments to helpers.
				collectHelperHandle(indy.bsm, owner, helpers, output);
				collectHelperValues(indy.bsmArgs, owner, helpers, output);
			} else if (instruction instanceof LdcInsnNode ldc) {
				// Follow ldc handles to helpers.
				collectHelperValue(ldc.cst, owner, helpers, output);
			}
		}
	}

	/**
	 * Recursively walks nested constant structures such as handles, condy, annotations, arrays, and iterables, so a
	 * helper hidden behind any of them is still recorded as reachable.
	 *
	 * @param values
	 * 		Constant array of values to inspect, or {@code null}.
	 * @param owner
	 * 		Class that declares the helpers.
	 * @param helpers
	 * 		Helper methods that count as component members.
	 * @param output
	 * 		Set that receives the reachable helpers.
	 */
	private static void collectHelperValues(@Nullable Object[] values, @Nonnull String owner,
	                                        @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> helpers,
	                                        @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> output) {
		if (values == null)
			return;
		for (Object value : values)
			collectHelperValue(value, owner, helpers, output);
	}

	private static void collectHelperValue(@Nullable Object value, @Nonnull String owner,
	                                       @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> helpers,
	                                       @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> output) {
		if (value instanceof Handle handle)
			collectHelperHandle(handle, owner, helpers, output);
		else if (value instanceof AnnotationNode annotation)
			collectHelperValue(annotation.values, owner, helpers, output);
		else if (value instanceof ConstantDynamic dynamic) {
			collectHelperHandle(dynamic.getBootstrapMethod(), owner, helpers, output);
			for (int index = 0; index < dynamic.getBootstrapMethodArgumentCount(); index++)
				collectHelperValue(dynamic.getBootstrapMethodArgument(index), owner, helpers, output);
		} else if (value instanceof Iterable<?> values)
			for (Object nested : values)
				collectHelperValue(nested, owner, helpers, output);
		else if (value instanceof Object[] values)
			for (Object nested : values)
				collectHelperValue(nested, owner, helpers, output);
	}

	private static void collectHelperHandle(@Nullable Handle handle, @Nonnull String owner,
	                                        @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> helpers,
	                                        @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> output) {
		if (handle == null || !owner.equals(handle.getOwner()))
			return;
		ZkmInvokeDynamicResolver.MethodKey key = new ZkmInvokeDynamicResolver.MethodKey(handle.getName(), handle.getDesc());
		if (helpers.contains(key))
			output.add(key);
	}

	/**
	 *
	 * @param nodes
	 * 		Current view of every class.
	 * @param metadata
	 * 		Component metadata for the class being cleaned.
	 * @param allMetadata
	 * 		Metadata for every class, used to skip component methods.
	 *
	 * @return State fields that are no longer referenced and are safe to delete.
	 */
	@Nonnull
	private static Set<ZkmInvokeDynamicResolver.FieldKey> removableStateFields(@Nonnull Map<String, ClassNode> nodes,
	                                                                           @Nonnull ZkmInvokeDynamicResolver.CleanupMetadata metadata,
	                                                                           @Nonnull Map<String, ZkmInvokeDynamicResolver.CleanupMetadata> allMetadata) {
		Set<ZkmInvokeDynamicResolver.FieldKey> result = new HashSet<>();
		for (ZkmInvokeDynamicResolver.FieldKey state : metadata.stateFields())
			if (!hasExternalStateReference(nodes, state, allMetadata))
				result.add(state);
		return result;
	}

	/**
	 * @param nodes
	 * 		Current view of every class.
	 * @param target
	 * 		State field being checked.
	 * @param allMetadata
	 * 		Metadata for every class, used to skip component methods.
	 *
	 * @return {@code true} when external code still references the field.
	 */
	private static boolean hasExternalStateReference(@Nonnull Map<String, ClassNode> nodes,
	                                                 @Nonnull ZkmInvokeDynamicResolver.FieldKey target,
	                                                 @Nonnull Map<String, ZkmInvokeDynamicResolver.CleanupMetadata> allMetadata) {
		for (Map.Entry<String, ClassNode> entry : nodes.entrySet()) {
			String callerOwner = entry.getKey();
			ClassNode node = entry.getValue();
			for (MethodNode method : node.methods) {
				// Skip component methods, obviously they are going to reference each other...
				boolean helperCaller = isComponentMethod(callerOwner, method, allMetadata);
				if (helperCaller)
					continue;

				// Search for direct field accesses, bootstrap handles, and constant references.
				if (method.instructions != null)
					for (AbstractInsnNode instruction : method.instructions) {
						if (instruction instanceof FieldInsnNode field && target.equals(new ZkmInvokeDynamicResolver.FieldKey(field.owner, field.name, field.desc)))
							return true;
						if (instruction instanceof InvokeDynamicInsnNode indy
								&& (containsStateFieldHandle(indy.bsm, target)
								|| containsStateFieldValues(indy.bsmArgs, target)))
							return true;
						if (instruction instanceof LdcInsnNode ldc && containsStateFieldValue(ldc.cst, target))
							return true;
					}

				// Check for annotation constants.
				if (containsStateFieldMethodMetadata(method, target))
					return true;
			}
			if (containsStateFieldClassMetadata(node, target))
				return true;
		}
		return false;
	}

	/**
	 * @param nodes
	 * 		Current view of every class.
	 * @param targetOwner
	 * 		Class that declares the field.
	 * @param field
	 * 		Field to check.
	 * @param helpers
	 * 		Helper methods for the class, used to recognize component references.
	 *
	 * @return {@code true} when the field is a generated string only the component touches.
	 */
	private static boolean isUnreferencedGeneratedStringField(@Nonnull Map<String, ClassNode> nodes,
	                                                          @Nonnull String targetOwner, @Nonnull FieldNode field,
	                                                          @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> helpers) {
		// Must be a private static final String with no initial value.
		if (!"Ljava/lang/String;".equals(field.desc)
				|| (field.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL))
				!= (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
				|| field.value != null)
			return false;

		// Skip if we don't know about state fields for the class.
		ClassNode owner = nodes.get(targetOwner);
		if (owner == null || !hasNonPrivateState(owner))
			return false;

		// Check if the field is referenced by any method outside the component.
		// The <clinit> is a special case because it wires the component together,
		// so its own store to the field doesn't count as a real reference.
		ZkmInvokeDynamicResolver.FieldKey target = new ZkmInvokeDynamicResolver.FieldKey(targetOwner, field.name, field.desc);
		for (Map.Entry<String, ClassNode> entry : nodes.entrySet()) {
			String callerOwner = entry.getKey();
			for (MethodNode method : entry.getValue().methods) {
				if (method.instructions == null)
					continue;
				boolean helperCaller = targetOwner.equals(callerOwner)
						&& helpers.contains(new ZkmInvokeDynamicResolver.MethodKey(method.name, method.desc));
				for (AbstractInsnNode instruction : method.instructions) {
					if (!(instruction instanceof FieldInsnNode access)
							|| !target.equals(new ZkmInvokeDynamicResolver.FieldKey(access.owner, access.name, access.desc)))
						continue;

					// The <clinit>'s own store to this field is the wiring we're removing anyway, so it doesn't
					// count as a real reference keeping the field alive.
					if (targetOwner.equals(callerOwner)
							&& "<clinit>".equals(method.name)
							&& access.getOpcode() == Opcodes.PUTSTATIC)
						continue;

					// Otherwise any access to the field from outside the component is a real reference, so the field is not removable.
					if (!helperCaller)
						return false;
				}
				if (!helperCaller && containsStateFieldMethodMetadata(method, target))
					return false;
			}
			if (containsStateFieldClassMetadata(entry.getValue(), target))
				return false;
		}
		return true;
	}

	/**
	 * @param nodes
	 * 		Current view of every class.
	 * @param targetOwner
	 * 		Class that declares the field.
	 * @param field
	 * 		Field to check.
	 * @param helpers
	 * 		Helper methods for the class, used to recognize component references.
	 *
	 * @return {@code true} when the field is a generated mutable static primitive only the component touches.
	 */
	private static boolean isUnreferencedGeneratedPrimitiveField(@Nonnull Map<String, ClassNode> nodes,
	                                                             @Nonnull String targetOwner, @Nonnull FieldNode field,
	                                                             @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> helpers) {
		// Field must be a static primitive with no initial value.
		if ((field.access & Opcodes.ACC_STATIC) == 0 || !isPrimitive(field.desc) || field.value != null)
			return false;

		// Check if the field is referenced by any method outside the component.
		// Same special case <clinit> as above.
		ZkmInvokeDynamicResolver.FieldKey target = new ZkmInvokeDynamicResolver.FieldKey(targetOwner, field.name, field.desc);
		for (Map.Entry<String, ClassNode> entry : nodes.entrySet()) {
			String callerOwner = entry.getKey();
			for (MethodNode method : entry.getValue().methods) {
				if (method.instructions == null)
					continue;
				boolean helperCaller = targetOwner.equals(callerOwner)
						&& ("<clinit>".equals(method.name)
						|| helpers.contains(new ZkmInvokeDynamicResolver.MethodKey(method.name, method.desc)));
				for (AbstractInsnNode instruction : method.instructions)
					if (instruction instanceof FieldInsnNode access
							&& target.equals(new ZkmInvokeDynamicResolver.FieldKey(access.owner, access.name, access.desc))
							&& !helperCaller)
						return false;
				if (!helperCaller && containsStateFieldMethodMetadata(method, target))
					return false;
			}
			if (containsStateFieldClassMetadata(entry.getValue(), target))
				return false;
		}
		return true;
	}

	/**
	 * @param node
	 * 		Class to check.
	 *
	 * @return {@code true} when the class declares a non-private static primitive field.
	 */
	private static boolean hasNonPrivateState(@Nonnull ClassNode node) {
		// There are plenty of cases where a class will have accessible static primitives that are not part of the component.
		// This is just a heuristic to avoid scanning classes that are clearly not components, so we don't have to be perfect.
		for (FieldNode field : node.fields)
			if ((field.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == Opcodes.ACC_STATIC
					&& isPrimitive(field.desc))
				return true;
		return false;
	}

	/**
	 * Splits the {@code <clinit>} into component wiring, which is dropped, and real static-field initialization,
	 * which is kept. Returns {@code false} to abort the whole cleanup when any surviving store can't be
	 * reconstructed from a statically-collected value. Emitting a subtly wrong initializer is worse than leaving
	 * the class alone.
	 *
	 * @param nodes
	 * 		Current view of every class.
	 * @param node
	 * 		Class being cleaned.
	 * @param initializer
	 * 		The class's {@code <clinit>}.
	 * @param metadata
	 * 		Component metadata for the class.
	 * @param removableStateFields
	 * 		State fields safe to delete.
	 * @param collector
	 * 		Static value collector, or {@code null} when unavailable.
	 * @param retained
	 * 		List that receives the stores to keep.
	 *
	 * @return {@code true} when the initializer can be split safely.
	 */
	private static boolean collectRetainedInitializers(@Nonnull Map<String, ClassNode> nodes,
	                                                   @Nonnull ClassNode node, @Nonnull MethodNode initializer,
	                                                   @Nonnull ZkmInvokeDynamicResolver.CleanupMetadata metadata,
	                                                   @Nonnull Set<ZkmInvokeDynamicResolver.FieldKey> removableStateFields,
	                                                   @Nullable StaticValueCollectionTransformer collector,
	                                                   @Nonnull List<FieldInitialization> retained) {
		if (initializer.instructions == null)
			return true;

		Set<ZkmInvokeDynamicResolver.MethodKey> helperMethods = metadata.helperMethods();
		Set<ZkmInvokeDynamicResolver.FieldKey> helperFields = metadata.helperFields();
		Set<ZkmInvokeDynamicResolver.FieldKey> seen = new HashSet<>();
		for (AbstractInsnNode instruction : initializer.instructions) {
			if (!(instruction instanceof FieldInsnNode field))
				continue;
			if (!node.name.equals(field.owner))
				return false;
			ZkmInvokeDynamicResolver.FieldKey key = new ZkmInvokeDynamicResolver.FieldKey(field.owner, field.name, field.desc);
			if (field.getOpcode() == Opcodes.PUTSTATIC) {
				// Skip any stores to helpers or removable state fields, they are part of the component wiring.
				if (helperFields.contains(key) || removableStateFields.contains(key))
					continue;

				// Skip any stores to unobserved generated string fields, they are also part of the component wiring.
				if (canDropUnobservedInitializerStore(nodes, node.name, key, helperMethods))
					continue;

				// Any other store is *probably* real static initialization code from the original application logic.
				// We can only keep it if we can statically collect the value being stored, otherwise we have to abort
				// the cleanup because we can't safely reconstruct the initializer.
				if (!seen.add(key) || !isStatic(node, field.name, field.desc) || collector == null)
					return false;
				ReValue value = collector.getStaticValue(field.owner, field.name, field.desc);
				AbstractInsnNode valueInstruction = toInsn(value);
				if (valueInstruction == null)
					return false;
				retained.add(new FieldInitialization(field.name, field.desc, valueInstruction));
			} else if (field.getOpcode() != Opcodes.GETSTATIC || (!helperFields.contains(key) && !removableStateFields.contains(key)))
				return false;
		}
		return true;
	}

	/**
	 * @param nodes
	 * 		Current view of every class.
	 * @param targetOwner
	 * 		Class that declares the field.
	 * @param target
	 * 		Field whose store may be dropped.
	 * @param helpers
	 * 		Helper methods for the class, used to recognize component references.
	 *
	 * @return {@code true} when the field is never observed outside the component.
	 */
	private static boolean canDropUnobservedInitializerStore(@Nonnull Map<String, ClassNode> nodes,
	                                                         @Nonnull String targetOwner,
	                                                         @Nonnull ZkmInvokeDynamicResolver.FieldKey target,
	                                                         @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> helpers) {
		ClassNode owner = nodes.get(targetOwner);
		if (owner == null)
			return false;

		// Field must be a private static final String with no initial value, otherwise it is not a generated wiring field.
		FieldNode declaration = findField(owner, target.name(), target.descriptor());
		if (declaration == null
				|| (declaration.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) != (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
				|| !"Ljava/lang/String;".equals(target.descriptor()))
			return false;

		for (Map.Entry<String, ClassNode> entry : nodes.entrySet()) {
			String callerOwner = entry.getKey();
			for (MethodNode method : entry.getValue().methods) {
				if (method.instructions == null)
					continue;

				// Skip any method that is part of the component, they are obviously going to reference each other...
				boolean helperCaller = targetOwner.equals(callerOwner)
						&& ("<clinit>".equals(method.name)
						|| helpers.contains(new ZkmInvokeDynamicResolver.MethodKey(method.name, method.desc)));
				if (helperCaller)
					continue;

				// Check for any instruction that references the field. If it is referenced anywhere outside the component, we can't drop the store.
				for (AbstractInsnNode instruction : method.instructions) {
					if (instruction instanceof FieldInsnNode field && target.equals(new ZkmInvokeDynamicResolver.FieldKey(field.owner, field.name, field.desc)))
						return false;
					if (instruction instanceof InvokeDynamicInsnNode dynamic
							&& (containsStateFieldHandle(dynamic.bsm, target)
							|| containsStateFieldValues(dynamic.bsmArgs, target)))
						return false;
					if (instruction instanceof LdcInsnNode ldc && containsStateFieldValue(ldc.cst, target))
						return false;
				}
				if (containsStateFieldMethodMetadata(method, target))
					return false;
			}
			if (containsStateFieldClassMetadata(entry.getValue(), target))
				return false;
		}
		return true;
	}

	/**
	 * Validates that the initializer contains only reproducible constructs. An example of a valid initializer is:
	 * <pre>{@code
	 * static {
	 *     someField = "...".substring(0, 4); // foldable String calls only
	 * }
	 * }</pre>
	 * Anything non-trivial cannot be easily modeled so we decline to rewrite it.
	 *
	 * @param initializer
	 * 		The class's {@code <clinit>}.
	 * @param owner
	 * 		Name of the class being cleaned.
	 * @param metadata
	 * 		Component metadata for the class.
	 *
	 * @return {@code true} when the initializer uses only reproducible constructs.
	 */
	private static boolean isSupportedInitializer(@Nonnull MethodNode initializer, @Nonnull String owner,
	                                              @Nonnull ZkmInvokeDynamicResolver.CleanupMetadata metadata) {
		Set<ZkmInvokeDynamicResolver.MethodKey> helpers = metadata.helperMethods();
		if (initializer.instructions == null)
			return true;
		for (AbstractInsnNode instruction : initializer.instructions) {
			// Check for synchronization and control flow we cannot easily model.
			int opcode = instruction.getOpcode();
			if (opcode == Opcodes.MONITORENTER
					|| opcode == Opcodes.MONITOREXIT
					|| opcode == Opcodes.JSR
					|| opcode == Opcodes.RET
					|| opcode == Opcodes.ATHROW)
				return false;

			// Check for invokedynamic, which is not easily reproducible either.
			if (instruction instanceof InvokeDynamicInsnNode)
				return false;

			// Only allow calls to helpers and a narrow set of supported inlinable calls.
			if (instruction instanceof MethodInsnNode call) {
				if (owner.equals(call.owner) && helpers.contains(new ZkmInvokeDynamicResolver.MethodKey(call.name, call.desc)))
					continue;
				if (!isWhitelistedPoolCall(call))
					return false;
			}

			// Only allow string construction for now, as other types of object construction are not easily reproducible.
			if (instruction instanceof TypeInsnNode type
					&& opcode == Opcodes.NEW
					&& !"java/lang/String".equals(type.desc))
				return false;
		}
		return true;
	}

	/**
	 * @param call
	 * 		Method call to check.
	 *
	 * @return {@code true} when the call is safe to reproduce.
	 */
	private static boolean isWhitelistedPoolCall(@Nonnull MethodInsnNode call) {
		// TODO: This is REALLY stupid and fragile.
		//  - We could use the evaluator / folding transformers to track simple constant values and rebuild from there.
		//    - Would also allow arrays and some simple structures. But at that point we'd have a LOT of reconstruction logic.
		//      It would almost be to the point where we have a dedicated utility/service for "produce insn sequence for Value<T>"
		//    - Doesn't work for anything that isn't explicitly handled by the evaluator. So a custom model type wouldn't be supported.
		//  - Best case would be to find a provable "this section is ZKM, this section is not" and then just rewrite to keep the non-ZKM section.
		//    - The one ZKM 26 sample we have doesn't have any non-trivial example of this so it's hard to know what the right approach is
		//      unless we can get more samples.
		//    - Maybe something like the constant folding transformer's "walk expressions backwards" approach could be modified
		//      to find the base of any intentional application code... Again, really need more samples to actually see if this would work.
		if ("java/lang/String".equals(call.owner))
			return call.name.equals("<init>")
					|| call.name.equals("length")
					|| call.name.equals("substring")
					|| call.name.equals("charAt")
					|| call.name.equals("toCharArray")
					|| call.name.equals("getBytes")
					|| call.name.equals("intern");
		return call.name.equals("valueOf") && call.owner.startsWith("java/lang/")
				&& Type.getReturnType(call.desc).getSort() == Type.OBJECT;
	}

	/**
	 * @param node
	 * 		Class that declares the field.
	 * @param name
	 * 		Name of the field.
	 * @param descriptor
	 * 		Descriptor of the field.
	 *
	 * @return {@code true} when the field exists and is static.
	 */
	private static boolean isStatic(@Nonnull ClassNode node, @Nonnull String name, @Nonnull String descriptor) {
		FieldNode field = findField(node, name, descriptor);
		return field != null && (field.access & Opcodes.ACC_STATIC) != 0;
	}

	@Nullable
	private static MethodNode findMethod(@Nonnull ClassNode node, @Nonnull String name, @Nonnull String descriptor) {
		for (MethodNode method : node.methods)
			if (name.equals(method.name) && descriptor.equals(method.desc))
				return method;
		return null;
	}

	@Nullable
	private static FieldNode findField(@Nonnull ClassNode node, @Nonnull String name, @Nonnull String descriptor) {
		for (FieldNode field : node.fields)
			if (name.equals(field.name) && descriptor.equals(field.desc))
				return field;
		return null;
	}

	/**
	 * @param method
	 * 		Method whose metadata is scanned.
	 * @param owner
	 * 		Class that declares the component.
	 * @param methods
	 * 		Helper methods that count as component members.
	 * @param fields
	 * 		Helper fields that count as component members.
	 *
	 * @return {@code true} when the method's metadata references the component.
	 */
	private static boolean containsComponentAnnotations(@Nonnull MethodNode method, @Nonnull String owner,
	                                                    @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> methods,
	                                                    @Nonnull Set<ZkmInvokeDynamicResolver.FieldKey> fields) {
		return containsComponentAnnotations(method.visibleAnnotations, owner, methods, fields)
				|| containsComponentAnnotations(method.invisibleAnnotations, owner, methods, fields)
				|| containsComponentAnnotations(method.visibleTypeAnnotations, owner, methods, fields)
				|| containsComponentAnnotations(method.invisibleTypeAnnotations, owner, methods, fields)
				|| containsComponentValue(method.annotationDefault, owner, methods, fields)
				|| containsComponentValue(method.visibleParameterAnnotations, owner, methods, fields)
				|| containsComponentValue(method.invisibleParameterAnnotations, owner, methods, fields)
				|| containsComponentValue(method.visibleLocalVariableAnnotations, owner, methods, fields)
				|| containsComponentValue(method.invisibleLocalVariableAnnotations, owner, methods, fields);
	}

	/**
	 * @param node
	 * 		Class whose metadata is scanned.
	 * @param owner
	 * 		Class that declares the component.
	 * @param methods
	 * 		Helper methods that count as component members.
	 * @param fields
	 * 		Helper fields that count as component members.
	 *
	 * @return {@code true} when the class's metadata references the component.
	 */
	private static boolean containsComponentClassMetadata(@Nonnull ClassNode node, @Nonnull String owner,
	                                                      @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> methods,
	                                                      @Nonnull Set<ZkmInvokeDynamicResolver.FieldKey> fields) {
		if (containsComponentAnnotations(node.visibleAnnotations, owner, methods, fields)
				|| containsComponentAnnotations(node.invisibleAnnotations, owner, methods, fields)
				|| containsComponentAnnotations(node.visibleTypeAnnotations, owner, methods, fields)
				|| containsComponentAnnotations(node.invisibleTypeAnnotations, owner, methods, fields))
			return true;
		for (FieldNode field : node.fields)
			if (containsComponentValue(field.value, owner, methods, fields)
					|| containsComponentAnnotations(field.visibleAnnotations, owner, methods, fields)
					|| containsComponentAnnotations(field.invisibleAnnotations, owner, methods, fields)
					|| containsComponentAnnotations(field.visibleTypeAnnotations, owner, methods, fields)
					|| containsComponentAnnotations(field.invisibleTypeAnnotations, owner, methods, fields))
				return true;
		if (node.recordComponents != null)
			for (RecordComponentNode component : node.recordComponents)
				if (containsComponentAnnotations(component.visibleAnnotations, owner, methods, fields)
						|| containsComponentAnnotations(component.invisibleAnnotations, owner, methods, fields)
						|| containsComponentAnnotations(component.visibleTypeAnnotations, owner, methods, fields)
						|| containsComponentAnnotations(component.invisibleTypeAnnotations, owner, methods, fields))
					return true;
		return false;
	}

	private static boolean containsComponentAnnotations(@Nullable List<? extends AnnotationNode> annotations,
	                                                    @Nonnull String owner,
	                                                    @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> methods,
	                                                    @Nonnull Set<ZkmInvokeDynamicResolver.FieldKey> fields) {
		if (annotations == null)
			return false;
		for (AnnotationNode annotation : annotations)
			if (containsComponentValue(annotation.values, owner, methods, fields))
				return true;
		return false;
	}

	private static boolean containsComponentValues(@Nullable Object[] values, @Nonnull String owner,
	                                               @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> methods,
	                                               @Nonnull Set<ZkmInvokeDynamicResolver.FieldKey> fields) {
		return containsComponentValue(values, owner, methods, fields);
	}

	/**
	 * @param value
	 * 		Constant value to inspect, or {@code null}.
	 * @param owner
	 * 		Class that declares the component.
	 * @param methods
	 * 		Helper methods that count as component members.
	 * @param fields
	 * 		Helper fields that count as component members.
	 *
	 * @return {@code true} when the constant references the component.
	 */
	private static boolean containsComponentValue(@Nullable Object value, @Nonnull String owner,
	                                              @Nonnull Set<ZkmInvokeDynamicResolver.MethodKey> methods,
	                                              @Nonnull Set<ZkmInvokeDynamicResolver.FieldKey> fields) {
		if (value instanceof Handle handle) {
			if (owner.equals(handle.getOwner())
					&& methods.contains(new ZkmInvokeDynamicResolver.MethodKey(handle.getName(), handle.getDesc())))
				return true;
			return (handle.getTag() == Opcodes.H_GETSTATIC
					|| handle.getTag() == Opcodes.H_PUTSTATIC)
					&& fields.contains(new ZkmInvokeDynamicResolver.FieldKey(handle.getOwner(), handle.getName(), handle.getDesc()));
		}
		if (value instanceof ConstantDynamic dynamic) {
			if (containsComponentValue(dynamic.getBootstrapMethod(), owner, methods, fields))
				return true;
			for (int index = 0; index < dynamic.getBootstrapMethodArgumentCount(); index++)
				if (containsComponentValue(dynamic.getBootstrapMethodArgument(index), owner, methods, fields))
					return true;
			return false;
		}
		if (value instanceof AnnotationNode annotation)
			return containsComponentValue(annotation.values, owner, methods, fields);
		if (value instanceof Iterable<?> values)
			for (Object nested : values)
				if (containsComponentValue(nested, owner, methods, fields))
					return true;
		if (value instanceof Object[] values)
			for (Object nested : values)
				if (containsComponentValue(nested, owner, methods, fields))
					return true;
		return false;
	}

	/**
	 * @param method
	 * 		Method whose metadata is scanned.
	 * @param target
	 * 		State field being checked.
	 *
	 * @return {@code true} when the method's metadata references the field.
	 */
	private static boolean containsStateFieldMethodMetadata(@Nonnull MethodNode method,
	                                                        @Nonnull ZkmInvokeDynamicResolver.FieldKey target) {
		return containsStateFieldAnnotations(method.visibleAnnotations, target)
				|| containsStateFieldAnnotations(method.invisibleAnnotations, target)
				|| containsStateFieldAnnotations(method.visibleTypeAnnotations, target)
				|| containsStateFieldAnnotations(method.invisibleTypeAnnotations, target)
				|| containsStateFieldAnnotations(method.visibleLocalVariableAnnotations, target)
				|| containsStateFieldAnnotations(method.invisibleLocalVariableAnnotations, target)
				|| containsStateFieldValues(method.visibleParameterAnnotations, target)
				|| containsStateFieldValues(method.invisibleParameterAnnotations, target)
				|| containsStateFieldValue(method.annotationDefault, target);
	}

	/**
	 * @param node
	 * 		Class whose metadata is scanned.
	 * @param target
	 * 		State field being checked.
	 *
	 * @return {@code true} when the class's metadata references the field.
	 */
	private static boolean containsStateFieldClassMetadata(@Nonnull ClassNode node,
	                                                       @Nonnull ZkmInvokeDynamicResolver.FieldKey target) {
		if (containsStateFieldAnnotations(node.visibleAnnotations, target)
				|| containsStateFieldAnnotations(node.invisibleAnnotations, target)
				|| containsStateFieldAnnotations(node.visibleTypeAnnotations, target)
				|| containsStateFieldAnnotations(node.invisibleTypeAnnotations, target))
			return true;
		for (FieldNode field : node.fields)
			if (containsStateFieldValue(field.value, target)
					|| containsStateFieldAnnotations(field.visibleAnnotations, target)
					|| containsStateFieldAnnotations(field.invisibleAnnotations, target)
					|| containsStateFieldAnnotations(field.visibleTypeAnnotations, target)
					|| containsStateFieldAnnotations(field.invisibleTypeAnnotations, target))
				return true;
		if (node.recordComponents != null)
			for (RecordComponentNode component : node.recordComponents)
				if (containsStateFieldAnnotations(component.visibleAnnotations, target)
						|| containsStateFieldAnnotations(component.invisibleAnnotations, target)
						|| containsStateFieldAnnotations(component.visibleTypeAnnotations, target)
						|| containsStateFieldAnnotations(component.invisibleTypeAnnotations, target))
					return true;
		return false;
	}

	private static boolean containsStateFieldAnnotations(@Nullable List<? extends AnnotationNode> annotations,
	                                                     @Nonnull ZkmInvokeDynamicResolver.FieldKey target) {
		if (annotations == null)
			return false;
		for (AnnotationNode annotation : annotations)
			if (containsStateFieldValue(annotation.values, target))
				return true;
		return false;
	}

	private static boolean containsStateFieldValues(@Nullable Object value,
	                                                @Nonnull ZkmInvokeDynamicResolver.FieldKey target) {
		if (value instanceof Iterable<?> values)
			for (Object nested : values)
				if (containsStateFieldValue(nested, target))
					return true;
		if (value instanceof Object[] values)
			for (Object nested : values)
				if (containsStateFieldValue(nested, target))
					return true;
		return containsStateFieldValue(value, target);
	}

	/**
	 * @param value
	 * 		Constant value to inspect, or {@code null}.
	 * @param target
	 * 		State field being checked.
	 *
	 * @return {@code true} when the constant references the field.
	 */
	private static boolean containsStateFieldValue(@Nullable Object value,
	                                               @Nonnull ZkmInvokeDynamicResolver.FieldKey target) {
		if (value instanceof Handle handle)
			return (handle.getTag() == Opcodes.H_GETSTATIC
					|| handle.getTag() == Opcodes.H_PUTSTATIC)
					&& target.equals(new ZkmInvokeDynamicResolver.FieldKey(handle.getOwner(), handle.getName(), handle.getDesc()));
		if (value instanceof AnnotationNode annotation)
			return containsStateFieldValue(annotation.values, target);
		if (value instanceof ConstantDynamic dynamic) {
			if (containsStateFieldValue(dynamic.getBootstrapMethod(), target))
				return true;
			for (int index = 0; index < dynamic.getBootstrapMethodArgumentCount(); index++)
				if (containsStateFieldValue(dynamic.getBootstrapMethodArgument(index), target))
					return true;
			return false;
		}
		if (value instanceof Iterable<?> values)
			for (Object nested : values)
				if (containsStateFieldValue(nested, target))
					return true;
		if (value instanceof Object[] values)
			for (Object nested : values)
				if (containsStateFieldValue(nested, target))
					return true;
		return false;
	}

	private static boolean containsStateFieldHandle(@Nullable Handle handle,
	                                                @Nonnull ZkmInvokeDynamicResolver.FieldKey target) {
		return handle != null && (handle.getTag() == Opcodes.H_GETSTATIC || handle.getTag() == Opcodes.H_PUTSTATIC)
				&& target.equals(new ZkmInvokeDynamicResolver.FieldKey(handle.getOwner(), handle.getName(), handle.getDesc()));
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
				// ZKM generates a large blob of code in the static initializer and inserts a few helper methods.
				// We just delete them once the inliner has turned the dynamic sites into direct calls.
				static {
				    // Component wiring code...
				}
				static String decode(int key) { ... }
				static int mapKey(int foo) { ... }
				""";
	}

	@Nonnull
	@Override
	public String afterExample() {
		return """
				// Nothing, ideally.
				""";
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> recommendedPredecessors() {
		// Cleanup is only meaningful once the inliner has turned ZKM's dynamic sites into direct calls. This
		// ordering is also why the inliner's presence is treated as a hard precondition throughout.
		return Set.of(InvokeDynamicInliningTransformer.class);
	}

	/**
	 * Modeled static field initialization that can be reconstructed in a new {@code <clinit>} after the component is removed.
	 *
	 * @param name
	 * 		Name of the field being initialized.
	 * @param descriptor
	 * 		Descriptor of the field being initialized.
	 * @param valueInstruction
	 * 		Instruction that pushes the field's folded initial value.
	 */
	private record FieldInitialization(@Nonnull String name, @Nonnull String descriptor,
	                                   @Nonnull AbstractInsnNode valueInstruction) {}
}
