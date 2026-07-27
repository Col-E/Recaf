package software.coley.recaf.services.compile.stub;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.slf4j.Logger;
import regexodus.Matcher;
import regexodus.Pattern;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.member.BasicLocalVariable;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.assembler.ExpressionCompileException;
import software.coley.recaf.services.assembler.ExpressionCompiler;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.visitors.WorkspaceClassWriter;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class stub generator which implements a specific method with a user-defined expression.
 *
 * @author Matt Coley
 * @see ExpressionCompiler#compile(String)
 */
public class ExpressionHostingClassStubGenerator extends ClassStubGenerator {
	private static final Logger logger = Logging.get(ExpressionHostingClassStubGenerator.class);
	private static final Pattern IMPORT_EXTRACT_PATTERN = RegexUtil.pattern("^\\s*(import \\w.+;)");
	private static final String NAME_CONSTRUCTOR = "instance_ctor";
	private static final String NAME_STATIC_INIT = "static_ctor";
	private static final String NAME_OBFUSCATED = "obfuscated_method";
	private static final String NAME_ENUM_RESERVED = "enum_method";
	private final int methodFlags;
	private final String originalMethodName;
	private final String methodName;
	private final Type methodType;
	private final List<LocalVariable> methodVariables;
	private final String expression;
	private final List<DetachedOuterMethod> detachedOuterMethods = new ArrayList<>();
	private final List<DetachedOuterField> detachedOuterFields = new ArrayList<>();

	/**
	 * @param workspace
	 * 		Workspace to pull class information from.
	 * @param inheritanceGraph
	 * 		Inheritance graph of the workspace.
	 * @param classAccess
	 * 		Host class access modifiers.
	 * @param className
	 * 		Host class name.
	 * @param superName
	 * 		Host class super name.
	 * @param implementing
	 * 		Host class interfaces implemented.
	 * @param fields
	 * 		Host class declared fields.
	 * @param methods
	 * 		Host class declared methods.
	 * @param innerClasses
	 * 		Host class declared inner classes.
	 * @param methodFlags
	 * 		Expression hosting method's access modifiers.
	 * @param methodName
	 * 		Expression hosting method's name.
	 * @param methodType
	 * 		Expression hosting method arguments + return type.
	 * @param methodVariables
	 * 		Expression hosting method's local variables.
	 * @param expression
	 * 		The expression to insert into the target hosting method.
	 * @param classSignature
	 * 		Host class generic signature, if any.
	 */
	public ExpressionHostingClassStubGenerator(@Nonnull Workspace workspace,
	                                           @Nonnull InheritanceGraph inheritanceGraph,
	                                           int classAccess,
	                                           @Nonnull String className,
	                                           @Nullable String superName,
	                                           @Nonnull List<String> implementing,
	                                           @Nonnull List<FieldMember> fields,
	                                           @Nonnull List<MethodMember> methods,
	                                           @Nonnull List<InnerClassInfo> innerClasses,
	                                           int methodFlags,
	                                           @Nonnull String methodName,
	                                           @Nonnull Type methodType,
	                                           @Nonnull List<LocalVariable> methodVariables,
	                                           @Nonnull String expression,
	                                           @Nullable String classSignature) {
		super(workspace, inheritanceGraph, classAccess, className, superName, implementing, fields, methods, innerClasses, classSignature);

		// Map edge cases for disallowed names.
		this.originalMethodName = methodName;
		if (methodName.equals("<init>"))
			methodName = NAME_CONSTRUCTOR;
		else if (methodName.equals("<clinit>"))
			methodName = NAME_STATIC_INIT;
		else if (!isSafeName(methodName))
			methodName = NAME_OBFUSCATED;
		else if (AccessFlag.isEnum(classAccess) && isReservedEnumMethodName(methodName))
			methodName = NAME_ENUM_RESERVED;
		if (isSourceMethodConflict(methodName, methodType, methodFlags))
			methodName = createSourceMethodAlias(methodName);

		// Assign expression host method details
		this.methodFlags = methodFlags;
		this.methodName = methodName;
		this.methodType = methodType;
		this.methodVariables = methodVariables;
		this.expression = expression;
	}

	@Override
	public String generate() throws ExpressionCompileException {
		String localExpression = expression;

		StringBuilder code = new StringBuilder();
		appendPackage(code);
		localExpression = appendExpressionImports(code, localExpression);
		if (hasEnclosingSource()) {
			appendNestedSource(code, localExpression);
		} else {
			if (hasEnclosingDeclarations()) {
				if (hasSafeEnclosingSourceChain())
					appendDetachedEnclosingSource(code);
				else
					appendDetachedTopLevelSource(code);
			}
			appendClassStructure(code);
			appendDetachedOuterField(code);
			appendDetachedOuterFields(code);
			appendDetachedOuterMethods(code);
			appendDetachedEnumAliases(code);
			appendEnumConsts(code);
			appendExpressionMethod(code, localExpression);
			appendFields(code);
			appendMethods(code);
			appendInnerClasses(code);
			appendClassEnd(code);
		}

		return code.toString();
	}

	/**
	 * Rewrites calls to the source-only outer-member forwarding methods into proper accessed calls.
	 * The forwarding methods make the generated source easy to write against, but they do
	 * not exist in the actual class being edited. To keep the expression output consistent with
	 * the original class structure we rewrite access to the forwarding methods to real outer-member access.
	 * <p>
	 * Consider this example:
	 * <pre>{@code
	 * class HamburgerFactory {
	 *     BreadFactory bread = new BreadFactory() {
	 *         YeastFactory yeast = new YeastFactory() {
	 *             // Method where user writes their expression.
	 *             void context() {
	 *                 // ...
	 *             }
	 *         }
	 *
	 *         void bake() {
	 *             something();
	 *         }
	 *     }
	 * }
	 * }</pre>
	 * In this example the bread/yeast factories are implemented as anonymous inner classes.
	 * They would become {@code HamburgerFactory$1} and {@code HamburgerFactory$1$1} once compiled.
	 * With our expression compiler we cannot create an isolated class called {@code 1} since that
	 * is not a valid identifier, so we have a system of creating <i>"detached"</i> classes.
	 * This allows us to write expressions in anonymous classes but breaks inner-to-outer class relations.
	 * <p>
	 * The solution to this breakage is to create forwarding methods in the detached class.
	 * <pre>{@code
	 * // Detached class for the BreadFactory.
	 * class HamburgerFactory$1 {
	 *     void bake() { something(); }
	 * }
	 *
	 * // Detached class for the YeastFactory
	 * class HamburgerFactory$1$1 {
	 *     HamburgerFactory$1 this$1; // Synthetic outer field to BreadFactory.
	 *
	 *     // Method where user writes their expression.
	 *     // The 'bake()' call targets this class's forwarding 'bake()' and not the outer class's 'bake()'.
	 *     void context() { bake(); }
	 *
	 *     // Forwarding method to the outer class's bake() method.
	 *     // Generated by the expression compiler to simulate outer-class access.
	 *     void bake() { this$1.bake(); }
	 * }
	 * }</pre>
	 * Compiling the expression within the {@code context()} method will produce bytecode that looks like this:
	 * <pre>{@code
	 * aload this
	 * invokevirtual HamburgerFactory$1$1.bake()V
	 * }</pre>
	 * We take that and rewrite it into:
	 * <pre>{@code
	 * aload this
	 * getfield HamburgerFactory$1$1.this$1 LHamburgerFactory$1;
	 * invokevirtual HamburgerFactory$1.bake()V
	 * }</pre>
	 *
	 * @param bytecode
	 * 		Compiled expression-host class.
	 *
	 * @return Compiled class with detached outer-member references rewritten to original members.
	 *
	 * @throws ExpressionCompileException
	 * 		When the expression method descriptor cannot be resolved.
	 */
	public byte[] rewriteDetachedOuterRefs(@Nonnull byte[] bytecode) throws ExpressionCompileException {
		// Nothing to rewrite if there are no detached outer members references to rewrite.
		if (detachedOuterMethods.isEmpty() && detachedOuterFields.isEmpty())
			return bytecode;

		// Find the outer method context and rewrite calls to the outer methods into the original JVM access sequence.
		String descriptor = methodDescriptorWithVariables();
		ClassNode classNode = new ClassNode(RecafConstants.getAsmVersion());
		new ClassReader(bytecode).accept(classNode, 0);
		DetachedOuterBinding binding = getDetachedOuterBinding();

		// Static fields do not need an outer instance. Instance methods and fields do.
		if (binding == null
				&& detachedOuterMethods.stream().anyMatch(method -> !method.isStatic())
				&& detachedOuterFields.stream().anyMatch(field -> !field.isStatic()))
			return bytecode;

		for (MethodNode method : classNode.methods) {
			// Skip if the method is not the one we are rewriting.
			String compiledMethodName = originalMethodName.equals("<init>") ? "<init>" : methodName;
			if (!method.name.equals(compiledMethodName) || !method.desc.equals(descriptor))
				continue;

			// If the outer binding is from a constructor parameter, then we need to remove the initialization of the outer field from the constructor.
			if (binding != null && binding.fromConstructorParameter())
				removeDetachedOuterInitialization(method, binding);

			// Remove any writes to final source-only field aliases.
			removeDetachedOuterFieldInitializations(method);

			int nextLocal = method.maxLocals;
			for (AbstractInsnNode instruction : method.instructions.toArray()) {
				// Skip if the instruction is not a field access to an outer field.
				if (instruction instanceof FieldInsnNode field && className.equals(field.owner)) {
					// Find the outer field that matches the access.
					DetachedOuterField outerField = detachedOuterFields.stream()
							.filter(candidate -> candidate.name().equals(field.name) && candidate.descriptor().equals(field.desc))
							.findFirst().orElse(null);

					// If found, rewrite the access to the outer field into the original JVM access sequence.
					if (outerField != null)
						rewriteDetachedOuterField(method, field, outerField, binding, nextLocal);

					// If the access is a write, then we need to bump the next local variable index to account for the value being written.
					if (outerField != null && field.getOpcode() == Opcodes.PUTFIELD)
						nextLocal += Type.getType(field.desc).getSize();

					continue;
				}

				// Skip if the instruction is not a method invocation to an outer method.
				if (!(instruction instanceof MethodInsnNode invocation) || !className.equals(invocation.owner))
					continue;
				if (binding == null)
					continue;

				// Find the outer method that matches the invocation.
				// If we can't find it then it is not an outer method, and we can skip it.
				DetachedOuterMethod outerMethod = detachedOuterMethods.stream()
						.filter(candidate -> candidate.name().equals(invocation.name) && candidate.descriptor().equals(invocation.desc))
						.findFirst().orElse(null);
				if (outerMethod == null)
					continue;

				// Collect the argument types and their local variable indices.
				Type[] argumentTypes = Type.getArgumentTypes(invocation.desc);
				int[] argumentLocals = new int[argumentTypes.length];
				int local = nextLocal;
				InsnList replacement = new InsnList();
				for (int i = argumentTypes.length - 1; i >= 0; i--) {
					Type argumentType = argumentTypes[i];
					argumentLocals[i] = local;
					replacement.add(AsmInsnUtil.createVarLoad(local, argumentType));
					local += argumentType.getSize();
				}
				nextLocal = local;

				// Collect the receiver of the outer method call, and remove it if it is a load of the synthetic outer field.
				AbstractInsnNode receiver = instruction.getPrevious();
				boolean receiverRemoved = receiver instanceof VarInsnNode variable && variable.getOpcode() == Opcodes.ALOAD && variable.var == 0;
				if (receiverRemoved)
					method.instructions.remove(receiver);
				else
					replacement.add(new InsnNode(Opcodes.POP));

				// If the outer method is not static, then we need to load the receiver of the outer method call.
				if (!outerMethod.isStatic()) {
					if (binding.fromConstructorParameter()) {
						replacement.add(new VarInsnNode(Opcodes.ALOAD, 1));
					} else {
						replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
						replacement.add(new FieldInsnNode(Opcodes.GETFIELD, className, binding.name(), binding.type().getDescriptor()));
					}
				}

				// Load the arguments.
				for (int i = 0; i < argumentTypes.length; i++)
					replacement.add(AsmInsnUtil.createVarLoad(local, argumentLocals[i]));

				// Emit the appropriate invocation instruction for the outer method.
				int opcode = outerMethod.isStatic() ? Opcodes.INVOKESTATIC :
						outerMethod.isInterface() ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL;
				replacement.add(new MethodInsnNode(opcode, outerMethod.owner(), outerMethod.name(), outerMethod.descriptor(), outerMethod.isInterface()));

				// Replace the invocation instruction with the new instructions.
				method.instructions.insertBefore(instruction, replacement);
				method.instructions.remove(instruction);
			}
			method.maxLocals = nextLocal;
		}

		// Rebuild the class with the rewritten method.
		ClassWriter writer = new WorkspaceClassWriter(inheritanceGraph, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * Rewrites a detached field access mirror to the original outer field.
	 *
	 * @param method
	 * 		Method containing the instruction to rewrite.
	 * @param instruction
	 * 		Field access instruction to rewrite.
	 * @param outerField
	 * 		Outer field to target.
	 * @param binding
	 * 		Outer class access binding, if any.
	 * @param nextLocal
	 * 		Next local variable index to use for temporary storage.
	 *
	 * @see #rewriteDetachedOuterRefs(byte[])
	 */
	private void rewriteDetachedOuterField(@Nonnull MethodNode method, @Nonnull FieldInsnNode instruction,
	                                       @Nonnull DetachedOuterField outerField, @Nullable DetachedOuterBinding binding,
	                                       int nextLocal) {
		// Static fields just need to change the owner.
		if (outerField.isStatic()) {
			instruction.owner = outerField.owner();
			return;
		}

		// Instance fields need access to the synthetic outer field.
		// If it isn't available we cannot rewrite the access, so we just leave it as-is.
		// This generally shouldn't occur anyways.
		if (binding == null)
			return;

		Type fieldType = Type.getType(outerField.descriptor());
		InsnList replacement = new InsnList();
		if (instruction.getOpcode() == Opcodes.GETFIELD) {
			// Input:
			//  aload this
			//  getfield Outer$N.someField      <-- Mirror inside N
			//
			// Output:
			//  aload this
			//  getfield Outer$N.this$N LOuter; <-- Synthetic outer class access field
			//  getfield Outer.someField        <-- Real field
			AbstractInsnNode receiver = instruction.getPrevious();
			if (receiver instanceof VarInsnNode variable && variable.getOpcode() == Opcodes.ALOAD && variable.var == 0)
				method.instructions.remove(receiver);
			else
				replacement.add(new InsnNode(Opcodes.POP));
			appendDetachedOuterReceiver(replacement, binding);
			replacement.add(new FieldInsnNode(Opcodes.GETFIELD, outerField.owner(), outerField.name(), outerField.descriptor()));
		} else if (instruction.getOpcode() == Opcodes.PUTFIELD) {
			// Input:
			//  aload this
			//  aload value
			//  putfield Outer$N.someField      <-- Mirror inside N
			//
			// Output:
			//  aload this
			//  getfield Outer$N.this$N LOuter; <-- Synthetic outer class access field
			//  aload value
			//  putfield Outer.someField        <-- Real field
			int valueLocal = nextLocal;
			method.instructions.insertBefore(instruction, AsmInsnUtil.createVarStore(valueLocal, fieldType));
			AbstractInsnNode receiver = instruction.getPrevious();
			if (receiver instanceof VarInsnNode variable && variable.getOpcode() == Opcodes.ALOAD && variable.var == 0)
				method.instructions.remove(receiver);
			else
				method.instructions.insertBefore(instruction, new InsnNode(Opcodes.POP));
			appendDetachedOuterReceiver(replacement, binding);
			replacement.add(AsmInsnUtil.createVarLoad(valueLocal, fieldType));
			replacement.add(new FieldInsnNode(Opcodes.PUTFIELD, outerField.owner(), outerField.name(), outerField.descriptor()));
			method.instructions.insertBefore(instruction, replacement);
			method.instructions.remove(instruction);
			return;
		}
		method.instructions.insertBefore(instruction, replacement);
		method.instructions.remove(instruction);
	}

	/**
	 * Appends the appropriate instructions to load the synthetic outer field receiver.
	 *
	 * @param instructions
	 * 		Instructions to append to.
	 * @param binding
	 * 		Synthetic outer field binding to load the receiver for.
	 */
	private void appendDetachedOuterReceiver(@Nonnull InsnList instructions, @Nonnull DetachedOuterBinding binding) {
		if (binding.fromConstructorParameter()) {
			// If we're in the constructor and the outer class access is the first parameter, then we can just load it from the parameter.
			instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
		} else {
			// Otherwise we need to load the synthetic outer field from the current instance.
			instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
			instructions.add(new FieldInsnNode(Opcodes.GETFIELD, className, binding.name(), binding.type().getDescriptor()));
		}
	}

	/**
	 * Removes the initialization of the synthetic outer field from the constructor, if it is present.
	 *
	 * @param method
	 * 		Constructor method to remove the initialization from.
	 * @param binding
	 * 		Synthetic outer field binding to remove the initialization for.
	 */
	private void removeDetachedOuterInitialization(@Nonnull MethodNode method, @Nonnull DetachedOuterBinding binding) {
		for (AbstractInsnNode instruction : method.instructions.toArray()) {
			// Skip if the instruction is not a field write to the synthetic outer field.
			if (!(instruction instanceof FieldInsnNode field)
					|| !className.equals(field.owner)
					|| field.getOpcode() != Opcodes.PUTFIELD
					|| !binding.name().equals(field.name)
					|| !binding.type().getDescriptor().equals(field.desc))
				continue;

			// Remove the field write and the two preceding instructions that load the receiver and value.
			AbstractInsnNode value = instruction.getPrevious();
			AbstractInsnNode receiver = value == null ? null : value.getPrevious();
			if (value instanceof VarInsnNode valueLoad
					&& valueLoad.getOpcode() == Opcodes.ALOAD
					&& valueLoad.var == 1
					&& receiver instanceof VarInsnNode receiverLoad
					&& receiverLoad.getOpcode() == Opcodes.ALOAD
					&& receiverLoad.var == 0) {
				method.instructions.remove(receiver);
				method.instructions.remove(value);
				method.instructions.remove(instruction);
			}
		}
	}

	/**
	 * Removes final source-only field mirror initialization statements.
	 * These would otherwise pollute the JASM output of the expression compiler.
	 * <p>
	 * Consider this example class:
	 * <pre>{@code
	 * public class Example {
	 *     final int outerField = new java.util.Random().nextInt();
	 *
	 *     Runnable action = new Runnable() {
	 *         @Override
	 *         public void run() { System.out.println(outerField); }
	 *     };
	 * }
	 * }</pre>
	 * When we want to create an expression in the {@code run()} method,
	 * we need to create a detached class for the anonymous inner class.
	 * The output of the detached class will look like this:
	 * <pre>{@code
	 * class Example{
	 *     int outerField;
	 *     java.lang.Runnable action;
	 *     public Example() throws Throwable { super(); }
	 * }
	 * abstract class Example$1 implements java.lang.Runnable {
	 *     // Synthetic outer class access
	 *     private final Example this$0;
	 *
	 *     // Mirror fields
	 *     final int outerField = detachedOuterFieldDefault$0();
	 *     java.lang.Runnable action;
	 *
	 *     // Dummy mirror initialization function
	 *     private static int detachedOuterFieldDefault$0() { return 0; }
	 *
	 *     Example$1(Example this$0) throws Throwable {
	 *         super();
	 *
	 *         // Mirror field initialization gets placed here when compiled.
	 *
	 *         this.this$0 = this$0;
	 *
	 *         // User-provided expression starts here
	 *     }
	 *
	 *     public void run() { throw new RuntimeException(); }
	 * }
	 * }</pre>
	 *
	 * @param method
	 * 		Method to remove the initialization from. Generally {@code <init>}.
	 */
	private void removeDetachedOuterFieldInitializations(@Nonnull MethodNode method) {
		for (DetachedOuterField field : detachedOuterFields) {
			// We don't have initialization for non-final fields, so skip those.
			if (!field.isFinal() || field.initializerName() == null)
				continue;

			for (AbstractInsnNode instruction : method.instructions.toArray()) {
				// Field access must be a write to the mirror field.
				if (!(instruction instanceof FieldInsnNode target)
						|| !className.equals(target.owner)
						|| !field.name().equals(target.name)
						|| !field.descriptor().equals(target.desc))
					continue;

				// Sanity check that the field access is the correct type.
				boolean staticField = target.getOpcode() == Opcodes.PUTSTATIC && field.isStatic();
				boolean instanceField = target.getOpcode() == Opcodes.PUTFIELD && !field.isStatic();
				if (!staticField && !instanceField)
					continue;

				// Remove helper initialization method calls for the mirror field.
				AbstractInsnNode helperCall = target.getPrevious();
				if (!(helperCall instanceof MethodInsnNode invocation)
						|| invocation.getOpcode() != Opcodes.INVOKESTATIC
						|| !className.equals(invocation.owner)
						|| !field.initializerName().equals(invocation.name)
						|| !("()" + field.descriptor()).equals(invocation.desc))
					continue;

				// For instance fields we also need to remove the receiver load of the mirror field.
				if (instanceField) {
					AbstractInsnNode receiver = helperCall.getPrevious();
					if (!(receiver instanceof VarInsnNode variable) || variable.getOpcode() != Opcodes.ALOAD || variable.var != 0)
						continue;
					method.instructions.remove(receiver);
				}
				method.instructions.remove(helperCall);
				method.instructions.remove(target);
			}
		}
	}

	@Override
	protected boolean doSkipMethod(@Nonnull String name, @Nonnull Type type) {
		// We want to skip generating a stub of the method our expression will reside within.
		// Anything that doesn't have the same name can be generated normally, so don't skip it.
		if (!originalMethodName.equals(name))
			return false;

		// Local-variable debug information is appended to the expression method's source parameter list.
		// Compare against that expanded signature as well, since it may collide with a different JVM overload.
		try {
			if (sourceSignature(name, methodDescriptorWithVariables()).equals(sourceSignature(name, type.getDescriptor())))
				return true;
		} catch (ExpressionCompileException ignored) {
			// Thrown when parameter variable information cannot be found.
			// Shouldn't happen, but if it does, we can just fall back to the original method signature check below.
		}

		// If the method signature is the same, then we can skip generating a stub of it.
		if (sourceSignature(name, methodType.getDescriptor()).equals(sourceSignature(name, type.getDescriptor())))
			return true;

		// javac synthesizes generic erasure bridges itself. Keeping the class-file
		// bridge alongside the generic source method creates a source-level clash.
		return AccessFlag.isBridge(methodFlags) && hasBridgeSourceSibling(originalMethodName, methodType, methodFlags);
	}

	@Nonnull
	@Override
	protected String getLocalName() {
		// When we have an enclosing source class, we want to use the inner class name as the local name.
		if (hasEnclosingSource())
			return className.substring(className.lastIndexOf('$') + 1);
		return super.getLocalName();
	}

	@Nonnull
	@Override
	public String getLocalModifier() {
		if (!hasEnclosingSource())
			return super.getLocalModifier();
		InnerClassInfo nestedInfo = getNestedInnerInfo();
		boolean isStatic = nestedInfo == null ?
				AccessFlag.isStatic(classAccess) :
				AccessFlag.isStatic(nestedInfo.getInnerAccess());
		return isStatic ? "static abstract" : "abstract";
	}

	@Override
	protected boolean isNonStaticInnerClass() {
		// A detached class such as Outer$1 is emitted as a top-level declaration. Its '$' is part of
		// the source identifier, so its synthetic outer constructor parameter must remain explicit.
		return hasEnclosingSource() && super.isNonStaticInnerClass();
	}

	@Override
	protected void appendConstructorBody(@Nonnull StringBuilder code, @Nonnull Type[] parameterTypes) {
		DetachedOuterBinding binding = getDetachedOuterBinding();
		if (binding == null)
			return;

		// If the synthetic outer field is from a constructor parameter, then we need to assign it from the parameter.
		for (int i = 0; i < parameterTypes.length; i++) {
			Type parameterType = parameterTypes[i];
			if (binding.type().equals(parameterType)) {
				code.append("this.").append(binding.name()).append(" = p").append(i).append("; ");
				return;
			}
		}
	}

	@Nonnull
	@Override
	protected String sourceInterfaceType(int index, @Nonnull String internalName) {
		if (AccessFlag.isBridge(methodFlags))
			return cleanType(internalName);
		return super.sourceInterfaceType(index, internalName);
	}

	/**
	 * @return {@code true} when all enclosing classes are available in the workspace, {@code false} otherwise.
	 */
	private boolean hasEnclosingSource() {
		int split = className.lastIndexOf('$');
		if (split < 0)
			return false;

		// If the local name is not a safe name, then we cannot generate enclosing source for it.
		String localName = className.substring(split + 1);
		if (!isSafeName(localName))
			return false;

		// Every enclosing declaration must exist.
		//
		// If the chain is broken, we fall back to representing this inner class as a top-level class using the original
		// binary name rather than fabricating the missing outer declaration.
		return hasEnclosingDeclarations();
	}

	/**
	 * @return {@code true} when every enclosing declaration exists in the workspace.
	 */
	private boolean hasEnclosingDeclarations() {
		int split = className.lastIndexOf('$');
		if (split < 0)
			return false;

		String current = className;
		while ((split = current.lastIndexOf('$')) >= 0) {
			current = current.substring(0, split);
			if (workspace.findClass(current) == null)
				return false;
		}

		return true;
	}

	/**
	 * @return {@code true} when every nested source name in the enclosing chain can be declared in Java source.
	 */
	private boolean hasSafeEnclosingSourceChain() {
		// If there is no enclosing source, then there is no chain to check.
		int firstSplit = className.lastIndexOf('$');
		if (firstSplit < 0)
			return true;

		// Check if every enclosing declaration's local name is a safe name.
		// If any of them are not, then we cannot generate source for the enclosing chain.
		String current = className.substring(0, firstSplit);
		int split;
		while ((split = current.lastIndexOf('$')) >= 0) {
			if (!isSafeName(current.substring(split + 1)))
				return false;
			current = current.substring(0, split);
		}
		return true;
	}

	/**
	 * Exposes constants from a detached anonymous class's enclosing enum so expressions can use
	 * the same simple names they had in the original nested source context.
	 *
	 * @param code
	 * 		Class source to append the aliases to.
	 */
	private void appendDetachedEnumAliases(@Nonnull StringBuilder code) {
		if (hasEnclosingSource())
			return;

		// Look for an enclosing enum class, and if found, expose its constants as static final fields.
		String current = className;
		int split;
		while ((split = current.lastIndexOf('$')) >= 0) {
			current = current.substring(0, split);
			ClassPathNode path = workspace.findClass(current);
			String currentDesc = "L" + current + ";";

			// If the enclosing class is an enum, then expose its constants as static final fields.
			if (path != null && AccessFlag.isEnum(path.getValue().getAccess())) {
				String enumType = cleanType(current);
				for (FieldMember field : path.getValue().getFields()) {
					// Skip non-enum constants
					if (!field.hasFinalModifier()
							|| !field.hasStaticModifier()
							|| !field.hasPublicModifier()
							|| !field.getDescriptor().equals(currentDesc)
							|| !isSafeName(field.getName()))
						continue;
					code.append("static final ").append(enumType).append(' ').append(field.getName())
							.append(" = ").append(enumType).append('.').append(field.getName()).append(";\n");
				}
				return;
			}
		}
	}

	/**
	 * Emits available enclosing declarations as ordinary top-level source. This is used for anonymous or otherwise
	 * invalid source names where the target must remain a top-level declaration with its original binary name.
	 *
	 * @param code
	 * 		Current code to append the enclosing source to.
	 */
	private void appendDetachedEnclosingSource(@Nonnull StringBuilder code) throws ExpressionCompileException {
		// Build the chain of outer classes.
		List<String> chain = new ArrayList<>();
		String current = className;
		while (current != null) {
			chain.add(current);
			int split = current.lastIndexOf('$');
			if (split < 0)
				break;
			current = current.substring(0, split);
		}
		chain.removeFirst();
		Collections.reverse(chain);

		// Walk the chain and append the outer classes, and then the inner class that will host the expression.
		for (int i = 0; i < chain.size(); i++) {
			String name = chain.get(i);

			// Class must exist in the workspace, otherwise we cannot generate a stub of it.
			ClassPathNode path = workspace.findClass(name);
			if (path == null)
				return;

			// Create an inner class stub generator for the outer class and append its structure and members.
			ClassInfo info = path.getValue();
			int access = getAccess(chain, i, info);
			InnerClassStubGenerator generator = new InnerClassStubGenerator(workspace, inheritanceGraph, access,
					info.getName(), info.getSuperName(), info.getInterfaces(), info.getFields(), info.getMethods(), info.getInnerClasses(),
					info.getSignature());
			generator.appendClassStructure(code);
			generator.appendEnumConsts(code);
			generator.appendClassMembers(code, false);
			String childName = i + 1 < chain.size() ? chain.get(i + 1) : className;
			generator.appendInnerClasses(code, Set.of(childName));
		}
		for (int i = 0; i < chain.size(); i++)
			appendClassEnd(code);
	}

	/**
	 * Emits broken anonymous chains as a series of top-level declarations using their valid JVM binary names.
	 *
	 * @param code
	 * 		Class source to append the enclosing source to.
	 */
	private void appendDetachedTopLevelSource(@Nonnull StringBuilder code) throws ExpressionCompileException {
		// Build the chain of outer classes.
		List<String> chain = new ArrayList<>();
		String current = className;
		while (current != null) {
			chain.add(current);
			int split = current.lastIndexOf('$');
			if (split < 0)
				break;
			current = current.substring(0, split);
		}
		chain.removeFirst(); // Drop the top-level class, since it is already being generated as a stub.
		Collections.reverse(chain);

		// For each class in the chain, emit a detached stub using its original binary name.
		for (String name : chain) {
			ClassPathNode path = workspace.findClass(name);
			if (path == null)
				return;
			ClassInfo info = path.getValue();
			DetachedClassStubGenerator generator = new DetachedClassStubGenerator(workspace, inheritanceGraph, info);
			code.append(generator.generate()).append('\n');
		}
	}

	/**
	 * Emits the synthetic outer field omitted from ordinary member stubbing when a class is detached.
	 *
	 * @param code
	 * 		Class source to append the outer field to.
	 */
	private void appendDetachedOuterField(@Nonnull StringBuilder code) {
		DetachedOuterBinding binding = getDetachedOuterBinding();
		if (binding == null)
			return;

		// Emit: private final Outer this$N
		code.append("private final ").append(sourceType(binding.type())).append(' ')
				.append(binding.name()).append(";\n");
	}

	/**
	 * @return Binding for the synthetic outer field, if one exists, or {@code null} if there is no synthetic outer field.
	 */
	@Nullable
	private DetachedOuterBinding getDetachedOuterBinding() {
		if (hasEnclosingSource())
			return null;

		// If the synthetic outer field is present, then we can use it to determine the outer type and name.
		FieldMember field = getDetachedOuterField();
		if (field != null)
			return new DetachedOuterBinding(field.getName(), Type.getType(field.getDescriptor()), false);

		// Otherwise, if the method is a non-static constructor with at least one parameter, then the first parameter is the outer type.
		if (!originalMethodName.equals("<init>") || methodType.getArgumentCount() == 0)
			return null;

		// Take the first parameter as the outer type, but only if it is an object type and its internal name matches the expected outer class.
		Type outerType = methodType.getArgumentTypes()[0];
		if (outerType.getSort() != Type.OBJECT || !className.startsWith(outerType.getInternalName() + '$'))
			return null;
		return new DetachedOuterBinding("detachedOuter", outerType, true);
	}

	/**
	 * Emit mirrors of outer class methods to our detached class so that expressions
	 * can continue to call those methods they had in the original nested source context.
	 * Access to these local mirrors will be rewritten later to the original outer method access sequence.
	 *
	 * @param code
	 * 		Class source to append the mirrors to.
	 *
	 * @see #rewriteDetachedOuterRefs(byte[])
	 */
	private void appendDetachedOuterMethods(@Nonnull StringBuilder code) {
		DetachedOuterBinding outerBinding = getDetachedOuterBinding();
		if (outerBinding == null)
			return;

		// Need to find the outer class in the workspace, otherwise we cannot generate stubs of its methods.
		String outerName = outerBinding.type().getInternalName();
		ClassPathNode outerPath = workspace.findClass(outerName);
		if (outerPath == null)
			return;

		String fieldName = outerBinding.name();
		for (MethodMember method : outerPath.getValue().getMethods()) {
			// Skip synthetic methods.
			if (method.hasSyntheticModifier() || method.hasBridgeModifier())
				continue;

			// Skip constructors and methods with names that cannot be declared in source.
			String name = method.getName();
			if (name.equals("<init>") || !isSafeName(name))
				continue;

			// Skip methods that have already been declared in the source of the target class.
			Type methodType = Type.getMethodType(method.getDescriptor());
			if (hasDeclaredSourceSignature(name, methodType.getDescriptor()))
				continue;

			// Skip methods that have return types or parameter types that cannot be declared in source.
			NameType returnInfo = getInfo(name, methodType.getReturnType().getDescriptor());
			if (!isSafeClassName(returnInfo.className()))
				continue;

			// Skip methods that have parameter types that cannot be declared in source.
			Type[] parameterTypes = methodType.getArgumentTypes();
			boolean validParameters = true;
			for (Type parameterType : parameterTypes) {
				if (!isSafeClassName(getInfo("p", parameterType.getDescriptor()).className())) {
					validParameters = false;
					break;
				}
			}
			if (!validParameters)
				continue;

			// Emit the method signature and body.
			code.append(returnInfo.className()).append(' ').append(name).append('(');
			for (int i = 0; i < parameterTypes.length; i++) {
				if (i > 0)
					code.append(", ");
				code.append(getInfo("p" + i, parameterTypes[i].getDescriptor()).className())
						.append(" p").append(i);
			}
			code.append(')');
			List<String> thrownTypes = method.getThrownTypes();
			if (!thrownTypes.isEmpty()) {
				code.append(" throws ");
				for (int i = 0; i < thrownTypes.size(); i++) {
					if (i > 0)
						code.append(", ");
					code.append(cleanType(thrownTypes.get(i)));
				}
			}
			code.append(" { ");
			if (methodType.getReturnType().getSort() != Type.VOID)
				code.append("return ");
			code.append("this.").append(fieldName).append('.').append(name).append('(');
			for (int i = 0; i < parameterTypes.length; i++) {
				if (i > 0)
					code.append(", ");
				code.append("p").append(i);
			}
			code.append("); }\n");
			detachedOuterMethods.add(new DetachedOuterMethod(outerName, name, methodType.getDescriptor(),
					method.hasStaticModifier(), AccessFlag.isInterface(outerPath.getValue().getAccess())));
		}
	}

	/**
	 * Emit mirrors of outer class fields to our detached class so that expressions
	 * can continue to use the same simple names they had in the original nested source context.
	 * Access to these local mirrors will be rewritten later to the original outer field access sequence.
	 *
	 * @param code
	 * 		Class source to append the mirrors to.
	 *
	 * @see #rewriteDetachedOuterField(MethodNode, FieldInsnNode, DetachedOuterField, DetachedOuterBinding, int)
	 */
	private void appendDetachedOuterFields(@Nonnull StringBuilder code) {
		// Nothing to emit if there is no enclosing source.
		if (hasEnclosingSource())
			return;

		// Need access to the outer class.
		String outerName = getDetachedOuterName();
		if (outerName == null)
			return;
		ClassPathNode outerPath = workspace.findClass(outerName);
		if (outerPath == null)
			return;

		// Collect the names of fields that are already declared in the target class so we don't emit mirrors for them.
		DetachedOuterBinding binding = getDetachedOuterBinding();
		Set<String> declaredNames = new HashSet<>();
		for (FieldMember field : fields) {
			if (isSafeName(field.getName()))
				declaredNames.add(field.getName());
		}

		// Emit mirrors of the outer class fields that are not already declared in the target class.
		boolean outerEnum = AccessFlag.isEnum(outerPath.getValue().getAccess());
		String enumDescriptor = "L" + outerName + ";";
		List<DetachedOuterField> fieldsWithInitializers = new ArrayList<>();
		for (FieldMember field : outerPath.getValue().getFields()) {
			// Skip synthetic fields, those aren't intended to be accessed from source.
			if (field.hasSyntheticModifier() || field.hasBridgeModifier())
				continue;

			// Skip if the field name isn't source safe, or already declared in the target class.
			String name = field.getName();
			if (!isSafeName(name) || declaredNames.contains(name))
				continue;

			// Skip enum constants, those are already handled by appendDetachedEnumAliases().
			if (outerEnum
					&& field.hasFinalModifier()
					&& field.hasStaticModifier()
					&& field.hasPublicModifier()
					&& enumDescriptor.equals(field.getDescriptor()))
				continue;

			// Skip if the field type isn't source safe.
			Type type = Type.getType(field.getDescriptor());
			if (!isSafeClassName(getInfo(name, field.getDescriptor()).className()))
				continue;

			// If the field is instanced, and we don't have outer class access, then we cannot emit a mirror for it.
			boolean isStatic = field.hasStaticModifier();
			if (!isStatic && binding == null)
				continue;

			// Generate initializer method name.
			// Final fields will get assigned to these bogus initializers, which will be removed later.
			String initializerName = null;
			if (field.hasFinalModifier())
				initializerName = "detachedOuterFieldDefault$" + detachedOuterFields.size();

			// Emit 'field = initializer();' for final fields, or just 'field;' for non-final fields.
			code.append(isStatic ? "static " : "")
					.append(field.hasFinalModifier() ? "final " : "")
					.append(getInfo(name, field.getDescriptor()).className()).append(' ').append(name);
			if (field.hasFinalModifier())
				code.append(" = ").append(initializerName).append("()");
			code.append(";\n");

			// Add the field to the list of detached outer fields for later rewriting.
			DetachedOuterField detachedField = new DetachedOuterField(outerName, name, field.getDescriptor(), isStatic,
					field.hasFinalModifier(), initializerName);
			detachedOuterFields.add(detachedField);
			if (initializerName != null)
				fieldsWithInitializers.add(detachedField);
		}

		// For final fields with initializers we need to emit those bogus methods now.
		for (DetachedOuterField field : fieldsWithInitializers) {
			Type type = Type.getType(field.descriptor());
			code.append("private static ").append(sourceType(type)).append(' ').append(field.initializerName())
					.append("() { return ");
			appendSourceDefaultValue(code, type);
			code.append("; }\n");
		}
	}

	/**
	 * @return Outer class name for the detached class, or {@code null} if it cannot be determined.
	 */
	@Nullable
	private String getDetachedOuterName() {
		// If we have a synthetic outer field, then we can use its type to determine the outer class name.
		DetachedOuterBinding binding = getDetachedOuterBinding();
		if (binding != null)
			return binding.type().getInternalName();

		// Otherwise we can fall back to the class name, assuming the standard 'Outer$N' naming convention.
		int split = className.lastIndexOf('$');
		return split < 0 ? null : className.substring(0, split);
	}

	/**
	 * Checks if the target class has a method with the same source signature as the given name and descriptor.
	 *
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 *
	 * @return {@code true} when the target class has a method with the same source signature, {@code false} otherwise.
	 */
	private boolean hasDeclaredSourceSignature(@Nonnull String name, @Nonnull String descriptor) {
		String signature = sourceSignature(name, descriptor);
		if (signature.equals(sourceSignature(originalMethodName, methodType.getDescriptor())))
			return true;
		for (MethodMember method : methods) {
			if (signature.equals(sourceSignature(method.getName(), method.getDescriptor())))
				return true;
		}
		return false;
	}

	/**
	 * @return Synthetic outer field of the target class, if one exists,
	 * or {@code null} if there is no synthetic outer field.
	 */
	@Nullable
	private FieldMember getDetachedOuterField() {
		// No enclosing source, no synthetic outer field.
		if (hasEnclosingSource())
			return null;

		// Class name must imply nesting.
		int split = className.lastIndexOf('$');
		if (split < 0)
			return null;

		// The synthetic outer field is always a reference to the immediate outer class.
		String outerName = className.substring(0, split);
		return fields.stream()
				.filter(FieldMember::hasSyntheticModifier)
				.filter(field -> Type.getType(field.getDescriptor()).getSort() == Type.OBJECT)
				.filter(field -> outerName.equals(Type.getType(field.getDescriptor()).getInternalName()))
				.findFirst().orElse(null);
	}

	/**
	 * Appends the source of the outer class and any other outer classes that are in the workspace.
	 *
	 * @param code
	 * 		Code to append the outer class source to.
	 * @param localExpression
	 * 		Expression to insert into the target method.
	 *
	 * @throws ExpressionCompileException
	 * 		When the expression hosting method could not be fully generated.
	 */
	private void appendNestedSource(@Nonnull StringBuilder code, @Nonnull String localExpression) throws ExpressionCompileException {
		// Build the chain of outer classes.
		List<String> chain = new ArrayList<>();
		String current = className;
		while (current != null) {
			chain.add(current);
			int split = current.lastIndexOf('$');
			if (split < 0)
				break;
			current = current.substring(0, split);
		}
		Collections.reverse(chain);

		// Append the outer classes, and then the inner class that will host the expression.
		for (int i = 0; i < chain.size(); i++) {
			String name = chain.get(i);
			boolean target = i == chain.size() - 1;
			if (target) {
				appendClassStructure(code);
				appendEnumConsts(code);
				appendExpressionMethod(code, localExpression);
				appendClassMembers(code, true);
				continue;
			}

			ClassPathNode path = workspace.findClass(name);
			if (path == null)
				throw new ExpressionCompileException("Enclosing source chain changed while generating stub: " + name);

			// Create an inner class stub generator for the outer class and append its structure and members.
			ClassInfo info = path.getValue();
			int access = getAccess(chain, i, info);
			InnerClassStubGenerator generator = new InnerClassStubGenerator(workspace, inheritanceGraph, access,
					info.getName(), info.getSuperName(), info.getInterfaces(), info.getFields(), info.getMethods(), info.getInnerClasses(),
					info.getSignature());
			generator.appendClassStructure(code);
			generator.appendEnumConsts(code);
			generator.appendClassMembers(code, false);
			generator.appendInnerClasses(code, Set.of(chain.get(i + 1)));
		}

		// Close off the outer classes.
		for (int i = 0; i < chain.size(); i++)
			appendClassEnd(code);
	}

	/**
	 * @param chain
	 * 		Chain of enclosing class names, from outermost to innermost.
	 * @param i
	 * 		Current index in the chain.
	 * @param info
	 * 		Class info of the current class.
	 *
	 * @return Access flags for the current class in the chain,
	 * including any {@code static} modifier from the parent class's {@code InnerClassInfo}.
	 */
	private int getAccess(@Nonnull List<String> chain, int i, @Nonnull ClassInfo info) {
		// The first declaration (i = 0) is the top-level class.
		// The last declaration (i = chain.size() - 1) is the inner class that will host the expression.
		//
		// public class Outer {
		//    public static class Middle {
		//        public class Inner {
		//            public void context() {} <--- Our context is here
		//        }
		//    }
		// }
		//
		// The 'static' modifier of 'Middle' is stored in the InnerClassInfo of 'Inner', not in the ClassInfo of 'Middle'.
		// So we need to look at the parent class's InnerClassInfo to determine if the inner class is static or not.
		String name = chain.get(i);
		int access = info.getAccess();
		if (i > 0) {
			ClassPathNode parentPath = workspace.findClass(chain.get(i - 1));
			if (parentPath != null) {
				for (InnerClassInfo inner : parentPath.getValue().getInnerClasses()) {
					if (name.equals(inner.getInnerClassName())) {
						access |= inner.getInnerAccess() & java.lang.reflect.Modifier.STATIC;
						break;
					}
				}
			}
		}
		return access;
	}

	/**
	 * @return Adapted method name for compiler-safe use.
	 */
	@Nonnull
	public String getAdaptedMethodName() {
		return methodName;
	}

	/**
	 * @return Original method name from the class file.
	 */
	@Nonnull
	public String getOriginalMethodName() {
		return originalMethodName;
	}

	/**
	 * @return {@code true} when the original JVM method was a constructor.
	 */
	private boolean isConstructorContext() {
		return originalMethodName.equals("<init>");
	}

	/**
	 * Expressions can contain imports at the top so that the end-user can work without needing fully qualified names.
	 * We want to take those out and append them to the class we're generating, and update the expression to remove
	 * the imports so that we can slap it into the method body later without syntax issues coming from imports being
	 * used in a method body.
	 *
	 * @param code
	 * 		Class code to append imports to.
	 * @param expression
	 * 		Expression to extract imports from.
	 *
	 * @return Modified expression <i>(without imports)</i>
	 */
	@Nonnull
	private String appendExpressionImports(@Nonnull StringBuilder code, @Nonnull String expression) {
		// Add imports from the user defined expression.
		// Remove the imports from the expression once copied to the output code.
		StringBuilder expressionBuffer = new StringBuilder();
		expression.lines().forEach(l -> {
			Matcher matcher = IMPORT_EXTRACT_PATTERN.matcher(l);
			if (matcher.find()) {
				code.append(matcher.group(1)).append('\n');
			} else {
				expressionBuffer.append(l).append('\n');
			}
		});
		return expressionBuffer.toString();
	}

	/**
	 * @param code
	 * 		Class code to append method definition to.
	 * @param expression
	 * 		User-defined expression.
	 *
	 * @throws ExpressionCompileException
	 * 		When the expression hosting method could not be fully generated.
	 */
	private void appendExpressionMethod(@Nonnull StringBuilder code, @Nonnull String expression) throws ExpressionCompileException {
		// Need to build the method structure to house the expression.
		// We'll start off with the access level.
		boolean constructorContext = isConstructorContext();
		int parameterVarIndex = 0;
		if (AccessFlag.isPublic(methodFlags))
			code.append("public ");
		else if (AccessFlag.isProtected(methodFlags))
			code.append("protected ");
		else if (AccessFlag.isPrivate(methodFlags))
			code.append("private ");
		if (AccessFlag.isStatic(methodFlags))
			code.append("static ");
		else if (!constructorContext && AccessFlag.isInterface(classAccess) && !AccessFlag.isPrivate(methodFlags))
			code.append("default ");
		else
			parameterVarIndex++;
		if (!AccessFlag.isStatic(methodFlags))
			parameterVarIndex = 1;

		if (constructorContext) {
			// Constructor contexts must be emitted as real constructors so an
			// expression can contain an explicit super(...) invocation.
			code.append(getLocalName()).append('(');
		} else {
			// Add the return type.
			Type returnType = methodType.getReturnType();
			if (Types.isPrimitive(returnType)) {
				code.append(returnType.getClassName()).append(' ');
			} else if (returnType.getSort() == Type.OBJECT) {
				code.append(cleanType(returnType.getInternalName())).append(' ');
			} else if (returnType.getSort() == Type.ARRAY) {
				Type componentReturnType = returnType.getElementType();
				if (Types.isPrimitive(componentReturnType)) {
					code.append(componentReturnType.getClassName());
				} else {
					code.append(cleanType(componentReturnType.getInternalName()));
				}
				code.append("[]".repeat(returnType.getDimensions()));
			}

			// Now the method name.
			code.append(' ').append(methodName).append('(');
		}

		// And now the parameters.
		int parameterCount = methodType.getArgumentCount();
		Type[] parameterTypes = methodType.getArgumentTypes();
		int enumConstructorParameters = constructorContext && AccessFlag.isEnum(classAccess) ? 2 : 0;
		Set<String> usedVariables = new HashSet<>();
		int emittedParameters = 0;
		for (int i = 0; i < parameterCount; i++) {
			// Lookup the parameter variable
			LocalVariable parameterVariable = getParameterVariable(parameterVarIndex, i);
			String parameterName = parameterVariable.getName();

			// Record the parameter as being used
			usedVariables.add(parameterName);

			// Skip the parameter if it is a synthetic outer class reference for a non-static inner class.
			boolean syntheticEnumParameter = i < enumConstructorParameters;
			boolean syntheticOuter = constructorContext && enumConstructorParameters == 0 && isNonStaticInnerClass() && i == 0
					&& parameterTypes[i].getSort() == Type.OBJECT
					&& className.startsWith(parameterTypes[i].getInternalName() + '$');
			parameterVarIndex += parameterTypes[i].getSize();
			if (syntheticEnumParameter || syntheticOuter)
				continue;

			// Skip if the parameter is illegally named.
			if (!isSafeName(parameterName))
				continue;

			// Append the parameter.
			String descriptor = parameterVariable.getDescriptor();
			NameType varInfo = getInfo(parameterName, descriptor);
			if (emittedParameters++ > 0)
				code.append(", ");
			code.append(varInfo.className()).append(' ').append(varInfo.name());
		}
		for (LocalVariable variable : methodVariables) {
			String name = variable.getName();

			// Skip illegal named variables and the implicit 'this'
			if (!isSafeName(name) || name.equals("this"))
				continue;

			// Skip if we already included the parameter in the loop above.
			if (!usedVariables.add(name))
				continue;

			// Append the parameter.
			String descriptor = variable.getDescriptor();
			NameType varInfo = getInfo(name, descriptor);
			if (emittedParameters++ > 0)
				code.append(", ");
			code.append(varInfo.className()).append(' ').append(varInfo.name());
		}

		// If we skipped the last parameter for some reason we need to remove the trailing ', ' before closing
		// off the parameters section.
		if (code.substring(code.length() - 2).endsWith(", "))
			code.setLength(code.length() - 2);

		// Close off declaration and add a 'throws Throwable' so the user doesn't need to specify try-catch.
		// If the method is a library method (something we cannot control, like Object.toString()) then
		// unfortunately we cannot add the 'throws'.
		//
		// Note: enum types cannot call 'super()' in their constructor.
		if (constructorContext) {
			code.append(") throws Throwable { ");
			if (!AccessFlag.isEnum(classAccess) && !hasExplicitConstructorInvocation(expression))
				appendParentConstructorInvocation(code, isNonStaticInnerClass());
			appendDetachedOuterFieldAssignment(code);
			code.append(ExpressionCompiler.EXPR_MARKER).append(" \n");
		} else {
			InheritanceVertex classVertex = inheritanceGraph.getVertex(className);
			if (classVertex != null && isOverridden(classVertex))
				code.append(") { " + ExpressionCompiler.EXPR_MARKER + " \n");
			else
				code.append(") throws Throwable { " + ExpressionCompiler.EXPR_MARKER + " \n");
		}
		code.append(expression);
		code.append("}\n");
	}

	/**
	 * Emits the assignment of the synthetic outer field from the first parameter of a detached constructor.
	 *
	 * @param code
	 * 		Code to append the assignment to.
	 */
	private void appendDetachedOuterFieldAssignment(@Nonnull StringBuilder code) {
		DetachedOuterBinding binding = getDetachedOuterBinding();
		if (binding == null)
			return;
		String parameterName = getParameterVariable(AccessFlag.isStatic(methodFlags) ? 0 : 1, 0).getName();
		if (isSafeName(parameterName))
			code.append("this.").append(binding.name()).append(" = ").append(parameterName).append("; ");
	}

	/**
	 * @param classVertex
	 * 		Class vertex to check for overridden methods in.
	 *
	 * @return {@code true} if the expression hosting method is an override of a parent class's method, {@code false} otherwise.
	 */
	private boolean isOverridden(@Nonnull InheritanceVertex classVertex) {
		String sourceSignature = sourceSignature(originalMethodName, methodType.getDescriptor());
		return classVertex.allParents().anyMatch(parent -> parent.getValue().getMethods().stream()
				.anyMatch(parentMethod -> {
					if (sourceSignature.equals(sourceSignature(parentMethod.getName(), parentMethod.getDescriptor())) &&
							isSourceReturnCompatible(Type.getMethodType(parentMethod.getDescriptor()).getReturnType(), methodType.getReturnType()))
						return true;

					// A generic source override is represented in bytecode by a compiler-generated bridge.
					// Match that erased bridge against the inherited declaration so the source method gets legal exceptions.
					return originalMethodName.equals(parentMethod.getName()) && methods.stream()
							.anyMatch(method -> method.hasBridgeModifier() &&
									method.getName().equals(parentMethod.getName()) &&
									method.getDescriptor().equals(parentMethod.getDescriptor()));
				}));
	}

	/**
	 * Checks if the given method name and type would conflict with a source-level method in a parent class.
	 *
	 * @param adaptedName
	 * 		Current method name that may have been adapted to avoid conflicts with reserved/illegal names.
	 * @param type
	 * 		Current method type to check for conflicts with.
	 * @param flags
	 * 		Current method access flags.
	 *
	 * @return {@code true} when the method would conflict with a source-level method in a parent class, {@code false} otherwise.
	 */
	private boolean isSourceMethodConflict(@Nonnull String adaptedName, @Nonnull Type type, int flags) {
		// If the method name was adapted, then it is no longer conflicting with a source-level method.
		if (!adaptedName.equals(originalMethodName))
			return false;

		// If the current method is a bridge, then it is a compiler-generated method that is not present in source code.
		// If its source sibling is present, then our compiler-generated method being defined at the source-level will conflict.
		if (AccessFlag.isBridge(flags) && hasBridgeSourceSibling(adaptedName, type, flags))
			return true;

		// Try and look up the class in the inheritance graph.
		// If it is not present, then we cannot check for conflicts (we fall back and assume there is no conflict).
		InheritanceVertex classVertex = inheritanceGraph.getVertex(className);
		if (classVertex == null)
			return false;

		// Check if any parent class has a method with the same name and descriptor, but with an incompatible return type.
		String signature = sourceSignature(originalMethodName, type.getDescriptor());
		return classVertex.allParents().anyMatch(parent -> parent.getValue().getMethods().stream()
				.anyMatch(parentMethod -> signature.equals(sourceSignature(parentMethod.getName(), parentMethod.getDescriptor())) &&
						!isSourceReturnCompatible(Type.getMethodType(parentMethod.getDescriptor()).getReturnType(), type.getReturnType())));
	}

	/**
	 * @param baseName
	 * 		Original method name that is conflicting with a source method.
	 *
	 * @return Unique alias for the source method to avoid a source-level name clash.
	 */
	@Nonnull
	private String createSourceMethodAlias(@Nonnull String baseName) {
		String base = "alias$" + baseName;
		String candidate = base;
		int suffix = 1;
		while (hasDeclaredMethodName(candidate))
			candidate = base + '$' + suffix++;
		return candidate;
	}

	/**
	 * @param name
	 * 		Method name to check for.
	 *
	 * @return {@code true} when the host class has a declared method with the given name, {@code false} otherwise.
	 */
	private boolean hasDeclaredMethodName(@Nonnull String name) {
		return methods.stream().anyMatch(method -> name.equals(method.getName()));
	}

	/**
	 * <b>Note</b>: The logic for appending parameters to the desc within this method must align with {@link #generate()}.
	 *
	 * @return The method descriptor with additional parameters from the {@link #methodVariables} appended at the end.
	 *
	 * @throws ExpressionCompileException
	 * 		When parameter variable information cannot be found.
	 */
	@Nonnull
	public String methodDescriptorWithVariables() throws ExpressionCompileException {
		StringBuilder sb = new StringBuilder("(");
		int parameterVarIndex = AccessFlag.isStatic(methodFlags) ? 0 : 1;
		int parameterCount = methodType.getArgumentCount();
		Set<String> usedVariables = new HashSet<>();
		for (int i = 0; i < parameterCount; i++) {
			LocalVariable parameterVariable = getParameterVariable(parameterVarIndex, i);
			String parameterName = parameterVariable.getName();
			usedVariables.add(parameterName);
			if (!isSafeName(parameterName))
				continue;
			String descriptor = parameterVariable.getDescriptor();
			NameType varInfo = getInfo(parameterName, descriptor);
			parameterVarIndex += varInfo.size();
			sb.append(descriptor);
		}
		for (LocalVariable variable : methodVariables) {
			String name = variable.getName();
			if (!isSafeName(name) || name.equals("this"))
				continue;
			if (!usedVariables.add(name))
				continue;
			String descriptor = variable.getDescriptor();
			sb.append(descriptor);
		}
		sb.append(')').append(methodType.getReturnType().getDescriptor());
		return sb.toString();
	}

	/**
	 * @param index
	 * 		Local variable index.
	 *
	 * @return Variable entry from the target method, or {@code null} if not known.
	 */
	@Nullable
	private LocalVariable findVar(int index) {
		if (methodVariables == null) return null;
		return methodVariables.stream()
				.filter(l -> l.getIndex() == index)
				.findFirst().orElse(null);
	}

	/**
	 * @param parameterVarIndex
	 * 		Local variable index of the parameter.
	 * @param parameterIndex
	 * 		Parameter index.
	 *
	 * @return Local variable info of the parameter.
	 */
	@Nonnull
	private LocalVariable getParameterVariable(int parameterVarIndex, int parameterIndex) {
		LocalVariable parameterVariable = findVar(parameterVarIndex);
		if (parameterVariable == null) {
			Type[] parameterTypes = methodType.getArgumentTypes();
			Type parameterType;
			if (parameterIndex < parameterTypes.length) {
				parameterType = parameterTypes[parameterIndex];
			} else {
				logger.warn("Could not resolve parameter variable (pVar={}, pIndex={}) in {}", parameterVarIndex, parameterIndex, methodName);
				parameterType = Types.OBJECT_TYPE;
			}
			parameterVariable = new BasicLocalVariable(parameterVarIndex, "p" + parameterIndex, parameterType.getDescriptor(), null);

		}
		return parameterVariable;
	}

	/**
	 * @param expression
	 * 		User defined expression to check for explicit constructor invocations.
	 *
	 * @return {@code true} if the expression contains an explicit constructor invocation, {@code false} otherwise.
	 */
	private static boolean hasExplicitConstructorInvocation(@Nonnull String expression) {
		// Cursed regex that matches 'this(...)' or 'super(...)' invocations while ignoring comments.
		return RegexUtil.matches("^(?:(?:\\s+\\/\\/.*|\\/\\*.*?\\*\\/|[^*]\\/|\\/[^*]|[^*][^\\/])*?)\\K\\b(?:this|super)\\(", expression);
	}

	/**
	 * @param methodName
	 * 		Name to check.
	 *
	 * @return {@code true} if the method name is reserved in an enum, {@code false} otherwise.
	 */
	private static boolean isReservedEnumMethodName(@Nonnull String methodName) {
		return methodName.equals("values")
				|| methodName.equals("valueOf")
				|| methodName.equals("ordinal")
				|| methodName.equals("name")
				|| methodName.equals("describeConstable")
				|| methodName.equals("compareTo")
				|| methodName.equals("equals")
				|| methodName.equals("hashCode");
	}

	/**
	 * Access to the outer class via a synthetic field or constructor parameter.
	 *
	 * @param name
	 * 		Field name or parameter name.
	 * @param type
	 * 		Type of the outer class.
	 * @param fromConstructorParameter
	 *        {@code true} if the outer class is accessed via a constructor parameter,
	 *        {@code false} if it is accessed via a synthetic field.
	 */
	private record DetachedOuterBinding(@Nonnull String name, @Nonnull Type type, boolean fromConstructorParameter) {}

	/**
	 * Access to an outer class method that has been mirrored in the detached class.
	 *
	 * @param owner
	 * 		Outer class internal name.
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 * @param isStatic
	 * 		Method static access flag.
	 * @param isInterface
	 * 		Method interface access flag.
	 */
	private record DetachedOuterMethod(@Nonnull String owner, @Nonnull String name, @Nonnull String descriptor,
	                                   boolean isStatic, boolean isInterface) {}

	/**
	 * Access to an outer class field that has been mirrored in the detached class.
	 *
	 * @param owner
	 * 		Outer class internal name.
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 * @param isStatic
	 * 		Field static access flag.
	 * @param isFinal
	 * 		Field final access flag.
	 * @param initializerName
	 * 		Name of the bogus initializer method for final fields, or {@code null} if the field is not final.
	 */
	private record DetachedOuterField(@Nonnull String owner, @Nonnull String name, @Nonnull String descriptor,
	                                  boolean isStatic, boolean isFinal, @Nullable String initializerName) {}
}
