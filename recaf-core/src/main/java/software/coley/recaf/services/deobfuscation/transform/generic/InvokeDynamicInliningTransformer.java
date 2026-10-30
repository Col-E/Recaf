package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.services.transform.TransformationParameter;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Inlines obfuscator-specific {@code invokedynamic} sites after a resolver provides their direct member target.
 * <p>
 * Standard {@code LambdaMetafactory} and {@code StringConcatFactory} sites are intentionally left untouched.
 * A resolver returning no target, or a target that cannot be emitted without guessing, leaves the original site in
 * place for the JVM and the existing generic evaluator to handle.
 *
 * @author Matt Coley
 * @see InvokeDynamicResolver
 */
@Dependent
public class InvokeDynamicInliningTransformer implements JvmClassTransformer {
	private static final DebuggingLogger logger = Logging.get(InvokeDynamicInliningTransformer.class);

	public static final String IDENTIFIER = "peephole.data.indyinline";
	public static final String KEY_MAX_STEPS = IDENTIFIER + ".max-steps";

	private static final int DEFAULT_MAX_STEPS = 20_000;
	private static final TransformationParameter<Integer> MAX_STEPS_PARAMETER =
			new TransformationParameter<>(KEY_MAX_STEPS, int.class, DEFAULT_MAX_STEPS);

	private final List<InvokeDynamicResolver> resolvers;
	private final InheritanceGraphService graphService;
	private InheritanceGraph inheritanceGraph;

	@Inject
	public InvokeDynamicInliningTransformer(@Nonnull InheritanceGraphService graphService,
	                                        @Nonnull InvokeDynamicResolverManager resolverManagers) {
		this(graphService, resolverManagers.getResolvers());
	}

	public InvokeDynamicInliningTransformer(@Nonnull InheritanceGraphService graphService,
	                                        @Nonnull List<? extends InvokeDynamicResolver> resolvers) {
		this.graphService = graphService;
		this.resolvers = InvokeDynamicResolverManager.sort(resolvers);
	}

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) throws TransformationException {
		inheritanceGraph = graphService.getOrCreateInheritanceGraph(workspace);

		// Let adapters build immutable workspace metadata before classes are visited in parallel.
		for (InvokeDynamicResolver resolver : resolvers)
			resolver.setup(context, workspace);
	}

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		ClassNode classNode = context.getNode(bundle, initialClassState);
		boolean dirty = false;

		for (MethodNode method : classNode.methods) {
			if (method.instructions == null || method.instructions.size() == 0)
				continue;

			// Analyze once so every resolver sees the same pre-rewrite frame state.
			Frame<ReValue>[] frames;
			try {
				frames = context.analyze(inheritanceGraph, classNode, method);
			} catch (TransformationException ex) {
				// Can't analyze the method, so we can't inline any call sites.
				logger.debugging(l -> l.error("Failed to analyze method for invokedynamic inlining: {}.{}{}", classNode.name, method.name, method.desc, ex));
				continue;
			}

			// Iterate backwards so that we can rewrite the instruction list in-place without invalidating the indices of unvisited instructions.
			for (int index = method.instructions.size() - 1; index >= 0; index--) {
				// Skip non indy instructions.
				AbstractInsnNode insn = method.instructions.get(index);
				if (!(insn instanceof InvokeDynamicInsnNode indy))
					continue;

				// A missing frame is still offered to explicitly frame-less-capable resolvers.
				Frame<ReValue> frame = index < frames.length ? frames[index] : null;

				// See if any of the resolvers can provide and rewrite the call site to a direct member.
				for (InvokeDynamicResolver resolver : resolvers) {
					try {
						InvokeDynamicResolver.ResolvedInvokeDynamic resolution = frame != null
								? resolver.resolve(context, workspace, classNode, method, indy, frame)
								: resolver.resolveWithoutFrame(context, workspace, classNode, method, indy);
						if (resolution == null)
							continue;

						// If the resolver provided a target, attempt to rewrite the call site to direct member instructions.
						if (rewrite(method, indy, resolution)) {
							dirty = true;
							break;
						}
					} catch (Exception ex) {
						// Just try the next one.
						logger.debugging(l -> l.debug("Resolver {} failed to resolve invokedynamic site {}.{}{} in {}",
								resolver.getClass().getName(), classNode.name, method.name, method.desc, indy.name, ex));
					}
				}
			}
		}

		// The rewrite can introduce new local variables and stack shapes, so we need to recompute frames for the class.
		if (dirty) {
			context.setRecomputeFrames(classNode.name);
			context.setNode(bundle, initialClassState, classNode);
		}
	}

	/**
	 * Replaces a dynamic call site with direct member instructions.
	 *
	 * @param method
	 * 		Method containing the call site.
	 * @param instruction
	 * 		Dynamic call site to replace.
	 * @param resolution
	 * 		Resolved direct member and metadata suffix count.
	 *
	 * @return {@code true} when the replacement was emitted.
	 * {@code false} when the shape is unsupported.
	 */
	private static boolean rewrite(@Nonnull MethodNode method, @Nonnull InvokeDynamicInsnNode instruction,
	                               @Nonnull InvokeDynamicResolver.ResolvedInvokeDynamic resolution) {
		try {
			// Validate that the call site and target are compatible.
			Handle target = resolution.target();
			Type[] callSiteArguments = Type.getArgumentTypes(instruction.desc);
			Type callSiteReturn = Type.getReturnType(instruction.desc);
			int suffixCount = resolution.ignoredTrailingArgumentCount();
			if (suffixCount < 0 || suffixCount > callSiteArguments.length)
				return false;

			int representedArgumentCount = callSiteArguments.length - suffixCount;
			int targetArgumentCount = targetArgumentCount(target);
			if (representedArgumentCount != targetArgumentCount || !isSupportedTarget(target))
				return false;

			// Reserve fresh locals for every original argument so metadata can be discarded without disturbing
			// wide values or the order in which the JVM evaluates the call-site operands.
			int nextLocal = Math.max(0, method.maxLocals);
			int[] argumentLocals = new int[callSiteArguments.length];
			for (int i = 0; i < callSiteArguments.length; i++) {
				Type argument = callSiteArguments[i];
				if (argument.getSort() == Type.VOID || argument.getSort() == Type.METHOD)
					return false;
				argumentLocals[i] = nextLocal;
				nextLocal += argument.getSize();
			}

			// Build the replacement instruction list.
			InsnList replacement = new InsnList();
			for (int i = callSiteArguments.length - 1; i >= 0; i--) {
				Type argType = callSiteArguments[i];
				int argIndex = argumentLocals[i];
				replacement.add(AsmInsnUtil.createVarStore(argIndex, argType));
			}
			Type[] targetArguments = targetArguments(target);
			for (int i = 0; i < representedArgumentCount; i++) {
				Type source = callSiteArguments[i];
				Type destination = targetArguments[i];
				int ardIndex = argumentLocals[i];
				replacement.add(AsmInsnUtil.createVarLoad(ardIndex, source));
				if (!appendConversion(source, destination, replacement))
					return false;
			}
			replacement.add(targetInstruction(target));
			if (!appendReturnConversion(targetReturnType(target), callSiteReturn, replacement))
				return false;

			// Apply only after every conversion and instruction shape has been validated, preserving unsupported sites.
			method.instructions.insertBefore(instruction, replacement);
			method.instructions.remove(instruction);
			method.maxLocals = Math.max(method.maxLocals, nextLocal);
			return true;
		} catch (Throwable t) {
			// Malformed descriptors or handles must remain untouched rather than producing unverifiable bytecode.
			return false;
		}
	}

	/**
	 * Checks if the target handle can be emitted as a direct member instruction.
	 * Constructors are not supported because they require special handling to ensure the object is allocated before the call.
	 *
	 * @param target
	 * 		Target handle to check.
	 *
	 * @return {@code true} when the target can be emitted as a direct member instruction.
	 * {@code false} when the target is unsupported or would require guessing.
	 */
	private static boolean isSupportedTarget(@Nonnull Handle target) {
		int tag = target.getTag();
		return switch (tag) {
			case Opcodes.H_GETFIELD, Opcodes.H_PUTFIELD, Opcodes.H_GETSTATIC, Opcodes.H_PUTSTATIC,
			     Opcodes.H_INVOKEVIRTUAL, Opcodes.H_INVOKEINTERFACE, Opcodes.H_INVOKESTATIC,
			     Opcodes.H_INVOKESPECIAL -> !((tag == Opcodes.H_INVOKESPECIAL) && target.getName().equals("<init>"));
			default -> false;
		};
	}

	/**
	 * @param target
	 * 		Target handle to check.
	 *
	 * @return Number of arguments the target expects, including the implicit {@code this} for non-static calls.
	 */
	private static int targetArgumentCount(@Nonnull Handle target) {
		return switch (target.getTag()) {
			case Opcodes.H_GETFIELD -> 1;
			case Opcodes.H_GETSTATIC -> 0;
			case Opcodes.H_PUTFIELD -> 2;
			case Opcodes.H_PUTSTATIC -> 1;
			case Opcodes.H_INVOKEVIRTUAL, Opcodes.H_INVOKEINTERFACE, Opcodes.H_INVOKESPECIAL ->
					Type.getArgumentCount(target.getDesc()) + 1;
			case Opcodes.H_INVOKESTATIC -> Type.getArgumentCount(target.getDesc());
			default -> -1;
		};
	}

	/**
	 * @param target
	 * 		Target handle to check.
	 *
	 * @return Array of argument types the target expects, including the implicit {@code this} for non-static calls.
	 */
	@Nonnull
	private static Type[] targetArguments(@Nonnull Handle target) {
		return switch (target.getTag()) {
			case Opcodes.H_GETFIELD -> new Type[]{Type.getObjectType(target.getOwner())};
			case Opcodes.H_PUTFIELD ->
					new Type[]{Type.getObjectType(target.getOwner()), Type.getType(target.getDesc())};
			case Opcodes.H_GETSTATIC -> new Type[0];
			case Opcodes.H_PUTSTATIC -> new Type[]{Type.getType(target.getDesc())};
			case Opcodes.H_INVOKEVIRTUAL, Opcodes.H_INVOKEINTERFACE, Opcodes.H_INVOKESPECIAL -> {
				Type[] arguments = Type.getArgumentTypes(target.getDesc());
				Type[] result = new Type[arguments.length + 1];
				result[0] = Type.getObjectType(target.getOwner());
				System.arraycopy(arguments, 0, result, 1, arguments.length);
				yield result;
			}
			case Opcodes.H_INVOKESTATIC -> Type.getArgumentTypes(target.getDesc());
			default -> throw new IllegalArgumentException("Unsupported handle tag: " + target.getTag());
		};
	}

	/**
	 * @param target
	 * 		Target handle to check.
	 *
	 * @return Return type the target expects.
	 */
	@Nonnull
	private static Type targetReturnType(@Nonnull Handle target) {
		return switch (target.getTag()) {
			case Opcodes.H_GETFIELD, Opcodes.H_GETSTATIC -> Type.getType(target.getDesc());
			case Opcodes.H_PUTFIELD, Opcodes.H_PUTSTATIC -> Type.VOID_TYPE;
			case Opcodes.H_INVOKEVIRTUAL, Opcodes.H_INVOKEINTERFACE, Opcodes.H_INVOKESTATIC, Opcodes.H_INVOKESPECIAL ->
					Type.getReturnType(target.getDesc());
			default -> throw new IllegalArgumentException("Unsupported handle tag: " + target.getTag());
		};
	}

	/**
	 * @param target
	 * 		Target handle to check.
	 *
	 * @return Instruction for the direct field/method reference represented by the handle.
	 */
	@Nonnull
	private static AbstractInsnNode targetInstruction(@Nonnull Handle target) {
		String owner = target.getOwner();
		String name = target.getName();
		String desc = target.getDesc();
		boolean itf = target.isInterface();
		return switch (target.getTag()) {
			case Opcodes.H_GETFIELD -> new FieldInsnNode(Opcodes.GETFIELD, owner, name, desc);
			case Opcodes.H_PUTFIELD -> new FieldInsnNode(Opcodes.PUTFIELD, owner, name, desc);
			case Opcodes.H_GETSTATIC -> new FieldInsnNode(Opcodes.GETSTATIC, owner, name, desc);
			case Opcodes.H_PUTSTATIC -> new FieldInsnNode(Opcodes.PUTSTATIC, owner, name, desc);
			case Opcodes.H_INVOKEVIRTUAL -> new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, name, desc, itf);
			case Opcodes.H_INVOKEINTERFACE -> new MethodInsnNode(Opcodes.INVOKEINTERFACE, owner, name, desc, true);
			case Opcodes.H_INVOKESTATIC -> new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, itf);
			case Opcodes.H_INVOKESPECIAL -> new MethodInsnNode(Opcodes.INVOKESPECIAL, owner, name, desc, itf);
			default -> throw new IllegalArgumentException("Unsupported handle tag: " + target.getTag());
		};
	}

	/**
	 * @param source
	 * 		Source type to convert from.
	 * @param destination
	 * 		Destination type to convert to.
	 * @param output
	 * 		Instruction list to emit conversion instruction sequence into.
	 *
	 * @return {@code true} when the conversion was emitted.
	 * {@code false} when the conversion is unsupported.
	 */
	private static boolean appendConversion(@Nonnull Type source, @Nonnull Type destination, @Nonnull InsnList output) {
		// No conversion needed.
		if (source.equals(destination))
			return true;

		// Handle primitive conversions.
		if (isPrimitive(source) && isPrimitive(destination))
			return appendPrimitiveConversion(source, destination, output);

		// Handle reference conversions.
		if (isReference(source) && isReference(destination)) {
			appendCheckCast(destination, output);
			return true;
		}

		// Handle primitive <-> reference boxing conversions.
		if (isPrimitive(source) && isReference(destination)) {
			Type wrapper = primitiveWrapper(source);
			output.add(new MethodInsnNode(Opcodes.INVOKESTATIC, wrapper.getInternalName(), "valueOf", "(" + source.getDescriptor() + ")" + wrapper.getDescriptor(), false));
			if (!wrapper.equals(destination))
				appendCheckCast(destination, output);
			return true;
		}
		if (isReference(source) && isPrimitive(destination)) {
			// The reference may not be the exact wrapper expected by the target.
			// The generated bytecode would otherwise replace a runtime conversion failure with a different one.
			Type wrapper = primitiveWrapper(destination);
			if (!source.equals(wrapper))
				appendCheckCast(wrapper, output);
			output.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, wrapper.getInternalName(), primitiveValueMethod(destination), "()" + destination.getDescriptor(), false));
			return true;
		}
		return false;
	}

	/**
	 * @param source
	 * 		Source type to convert from.
	 * @param destination
	 * 		Destination type to convert to.
	 * @param output
	 * 		Instruction list to emit conversion instruction sequence into.
	 *
	 * @return {@code true} when the conversion was emitted.
	 * {@code false} when the conversion is unsupported.
	 */
	private static boolean appendReturnConversion(@Nonnull Type source, @Nonnull Type destination, @Nonnull InsnList output) {
		// Pop values if the call site expects a void return.
		if (Type.VOID_TYPE.equals(destination)) {
			if (!Type.VOID_TYPE.equals(source))
				output.add(new InsnNode(source.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
			return true;
		}

		// If the call site expects a value, but the target returns void, we need to push a default value for the expected type.
		if (Type.VOID_TYPE.equals(source))
			return appendDefaultValue(destination, output);

		// Otherwise, we need to convert the return value from the target to the expected type of the call site.
		return appendConversion(source, destination, output);
	}

	/**
	 * @param type
	 * 		Type to push a default value for.
	 * @param output
	 * 		Instruction list to emit the default value into.
	 *
	 * @return {@code true} when the default value was emitted.
	 * {@code false} when the type is unsupported.
	 */
	private static boolean appendDefaultValue(@Nonnull Type type, @Nonnull InsnList output) {
		switch (type.getSort()) {
			case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> output.add(new InsnNode(Opcodes.ICONST_0));
			case Type.FLOAT -> output.add(new InsnNode(Opcodes.FCONST_0));
			case Type.LONG -> output.add(new InsnNode(Opcodes.LCONST_0));
			case Type.DOUBLE -> output.add(new InsnNode(Opcodes.DCONST_0));
			case Type.ARRAY, Type.OBJECT -> output.add(new InsnNode(Opcodes.ACONST_NULL));
			default -> {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param source
	 * 		Source primitive type to convert from.
	 * @param destination
	 * 		Destination primitive type to convert to.
	 * @param output
	 * 		Instruction list to emit conversion instruction sequence into.
	 *
	 * @return {@code true} when the conversion was emitted.
	 * {@code false} when the conversion is unsupported.
	 */
	private static boolean appendPrimitiveConversion(@Nonnull Type source, @Nonnull Type destination, @Nonnull InsnList output) {
		int sourceSort = primitiveCategory(source);
		int destinationSort = primitiveCategory(destination);
		if (sourceSort == destinationSort) {
			appendIntegerNarrowing(destination, output);
			return true;
		}

		if (destinationSort == Type.INT) {
			switch (sourceSort) {
				case Type.LONG -> output.add(new InsnNode(Opcodes.L2I));
				case Type.FLOAT -> output.add(new InsnNode(Opcodes.F2I));
				case Type.DOUBLE -> output.add(new InsnNode(Opcodes.D2I));
				case Type.INT -> {
					// no-op
				}
				default -> {
					return false;
				}
			}
			appendIntegerNarrowing(destination, output);
			return true;
		}
		if (sourceSort == Type.INT) {
			switch (destinationSort) {
				case Type.LONG -> output.add(new InsnNode(Opcodes.I2L));
				case Type.FLOAT -> output.add(new InsnNode(Opcodes.I2F));
				case Type.DOUBLE -> output.add(new InsnNode(Opcodes.I2D));
				default -> {
					return false;
				}
			}
			return true;
		}
		if (sourceSort == Type.LONG) {
			switch (destinationSort) {
				case Type.FLOAT -> output.add(new InsnNode(Opcodes.L2F));
				case Type.DOUBLE -> output.add(new InsnNode(Opcodes.L2D));
				default -> {
					return false;
				}
			}
			return true;
		}
		if (sourceSort == Type.FLOAT) {
			switch (destinationSort) {
				case Type.LONG -> output.add(new InsnNode(Opcodes.F2L));
				case Type.DOUBLE -> output.add(new InsnNode(Opcodes.F2D));
				default -> {
					return false;
				}
			}
			return true;
		}
		if (sourceSort == Type.DOUBLE) {
			switch (destinationSort) {
				case Type.LONG -> output.add(new InsnNode(Opcodes.D2L));
				case Type.FLOAT -> output.add(new InsnNode(Opcodes.D2F));
				default -> {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * @param destination
	 * 		Destination primitive type to narrow to.
	 * @param output
	 * 		Instruction list to emit narrowing instruction sequence into.
	 */
	private static void appendIntegerNarrowing(@Nonnull Type destination, @Nonnull InsnList output) {
		switch (destination.getSort()) {
			case Type.BOOLEAN -> {
				// MethodHandles.explicitCastArguments uses the low bit when converting an integer to boolean.
				output.add(new InsnNode(Opcodes.ICONST_1));
				output.add(new InsnNode(Opcodes.IAND));
			}
			case Type.BYTE -> output.add(new InsnNode(Opcodes.I2B));
			case Type.CHAR -> output.add(new InsnNode(Opcodes.I2C));
			case Type.SHORT -> output.add(new InsnNode(Opcodes.I2S));
			default -> {}
		}
	}

	/**
	 * @param destination
	 * 		Destination reference type to cast to.
	 * @param output
	 * 		Instruction list to emit the cast instruction into.
	 */
	private static void appendCheckCast(@Nonnull Type destination, @Nonnull InsnList output) {
		String castType = destination.getSort() == Type.ARRAY ? destination.getDescriptor() : destination.getInternalName();
		output.add(new TypeInsnNode(Opcodes.CHECKCAST, castType));
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return Primitive category of the type, or -1 if not a primitive.
	 */
	private static int primitiveCategory(@Nonnull Type type) {
		return switch (type.getSort()) {
			case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> Type.INT;
			case Type.FLOAT -> Type.FLOAT;
			case Type.LONG -> Type.LONG;
			case Type.DOUBLE -> Type.DOUBLE;
			default -> -1; // Object, array, void, method, etc.
		};
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} when the type is a primitive.
	 */
	private static boolean isPrimitive(@Nonnull Type type) {
		return primitiveCategory(type) >= 0;
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} when the type is a reference (object or array).
	 */
	private static boolean isReference(@Nonnull Type type) {
		return type.getSort() == Type.ARRAY || type.getSort() == Type.OBJECT;
	}

	/**
	 * @param primitive
	 * 		Primitive type to get the wrapper for.
	 *
	 * @return Wrapper type for the primitive.
	 */
	@Nonnull
	private static Type primitiveWrapper(@Nonnull Type primitive) {
		return switch (primitive.getSort()) {
			case Type.BOOLEAN -> Type.getObjectType("java/lang/Boolean");
			case Type.BYTE -> Type.getObjectType("java/lang/Byte");
			case Type.CHAR -> Type.getObjectType("java/lang/Character");
			case Type.SHORT -> Type.getObjectType("java/lang/Short");
			case Type.INT -> Type.getObjectType("java/lang/Integer");
			case Type.FLOAT -> Type.getObjectType("java/lang/Float");
			case Type.LONG -> Type.getObjectType("java/lang/Long");
			case Type.DOUBLE -> Type.getObjectType("java/lang/Double");
			default -> throw new IllegalArgumentException("Not a primitive type: " + primitive);
		};
	}

	/**
	 * @param primitive
	 * 		Primitive type to get the value method for.
	 *
	 * @return Name of the method on the wrapper type that returns the primitive value.
	 */
	@Nonnull
	private static String primitiveValueMethod(@Nonnull Type primitive) {
		return switch (primitive.getSort()) {
			case Type.BOOLEAN -> "booleanValue";
			case Type.BYTE -> "byteValue";
			case Type.CHAR -> "charValue";
			case Type.SHORT -> "shortValue";
			case Type.INT -> "intValue";
			case Type.FLOAT -> "floatValue";
			case Type.LONG -> "longValue";
			case Type.DOUBLE -> "doubleValue";
			default -> throw new IllegalArgumentException("Not a primitive type: " + primitive);
		};
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> recommendedSuccessors() {
		// Direct rewrites introduce temporary local stores and casts that later value cleanup can simplify.
		return Collections.singleton(CallResultInliningTransformer.class);
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
				// Not super easy to represent as Java source since there is no direct way to represent
				// custom invokedynamic bootstrapping, but typically decompilers will emit something like this:
				String s = "Hello world";
				long l = 3239848925L;
				DynamicLoookup.a("\\u00df", string, (long)4118739427681035372L, (long)l);
				""";
	}

	@Nonnull
	@Override
	public String afterExample() {
		return """
				// The dynamic reference is decoded, and metadata arguments are discarded, leaving only the direct member call.
				println("Hello world");
				""";
	}

	@Nonnull
	@Override
	public List<TransformationParameter<?>> getParameterDefinitions() {
		return List.of(MAX_STEPS_PARAMETER);
	}
}
