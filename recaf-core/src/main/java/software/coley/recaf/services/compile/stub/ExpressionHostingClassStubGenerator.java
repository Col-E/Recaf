package software.coley.recaf.services.compile.stub;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import regexodus.Matcher;
import regexodus.Pattern;
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
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.Types;
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
			if (hasEnclosingDeclarations() && hasSafeEnclosingSourceChain())
				appendDetachedEnclosingSource(code);
			appendClassStructure(code);
			appendEnumConsts(code);
			appendExpressionMethod(code, localExpression);
			appendFields(code);
			appendMethods(code);
			appendInnerClasses(code);
			appendClassEnd(code);
		}

		return code.toString();
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
		// Build the chain of outer classes that are in the workspace.
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

	private static boolean hasExplicitConstructorInvocation(@Nonnull String expression) {
		// Cursed regex that matches 'this(...)' or 'super(...)' invocations while ignoring comments.
		return RegexUtil.matches("^(?:(?:\\s+\\/\\/.*|\\/\\*.*?\\*\\/|[^*]\\/|\\/[^*]|[^*][^\\/])*?)\\K\\b(?:this|super)\\(", expression);
	}

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
}
