package software.coley.recaf.services.compile.stub;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.assembler.ExpressionCompileException;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.Keywords;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.visitors.SkippingClassVisitor;
import software.coley.recaf.workspace.model.Workspace;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base stub generator for classes.
 *
 * @author Matt Coley
 */
public abstract class ClassStubGenerator {
	protected final Workspace workspace;
	protected final InheritanceGraph inheritanceGraph;
	protected final int classAccess;
	protected final String className;
	protected final String superName;
	protected final List<String> implementing;
	protected final List<FieldMember> fields;
	protected final List<MethodMember> methods;
	protected final List<InnerClassInfo> innerClasses;
	protected final String classSignature;

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
	 * @param classSignature
	 * 		Host class generic signature, if any.
	 */
	public ClassStubGenerator(@Nonnull Workspace workspace,
	                          @Nonnull InheritanceGraph inheritanceGraph,
	                          int classAccess,
	                          @Nonnull String className,
	                          @Nullable String superName,
	                          @Nonnull List<String> implementing,
	                          @Nonnull List<FieldMember> fields,
	                          @Nonnull List<MethodMember> methods,
	                          @Nonnull List<InnerClassInfo> innerClasses,
	                          @Nullable String classSignature) {
		this.workspace = workspace;
		this.inheritanceGraph = inheritanceGraph;
		this.classAccess = classAccess;
		this.className = isSafeInternalClassName(className) ? className : "obfuscated_class";
		this.superName = isSafeReferencableName(superName) ? superName : null;
		this.implementing = implementing.stream()
				.filter(this::isSafeReferencableName)
				.toList();
		this.fields = fields;
		this.methods = methods;
		this.innerClasses = innerClasses;
		this.classSignature = classSignature;
	}

	/**
	 * @return Generated stub for the target class.
	 *
	 * @throws ExpressionCompileException
	 * 		When the class could not be fully stubbed out.
	 */
	public abstract String generate() throws ExpressionCompileException;

	/**
	 * Appends a package declaration if the {@link #className} is not in the default package.
	 *
	 * @param code
	 * 		Class code to append package declaration to.
	 */
	protected void appendPackage(@Nonnull StringBuilder code) {
		if (className.indexOf('/') > 0) {
			String packageName = className.replace('/', '.').substring(0, className.lastIndexOf('/'));
			code.append("package ").append(packageName).append(";\n");
		}
	}

	/**
	 * Appends the class's access modifiers, type (class, interface, enum), name, extended type, and any implemented interfaces.
	 *
	 * @param code
	 * 		Class code to append the class type structure to.
	 */
	protected void appendClassStructure(@Nonnull StringBuilder code) {
		// Class structure
		boolean isEnum = AccessFlag.isEnum(classAccess);
		boolean isInterface = AccessFlag.isInterface(classAccess);
		InheritanceVertex classVertex = inheritanceGraph.getVertex(className);
		if (classVertex != null && classVertex.getParents().stream().anyMatch(this::isSealedType))
			code.append("non-sealed ");
		if (isEnum) {
			code.append("enum ").append(getLocalName());
		} else {
			String modifier = getLocalModifier();
			if (!modifier.isBlank())
				code.append(modifier).append(' ');
			code.append(isInterface ? "interface " : "class ").append(getLocalName());
		}
		if (!isInterface && superName != null && !superName.equals("java/lang/Object") && !superName.equals("java/lang/Enum"))
			code.append(" extends ").append(cleanType(superName));
		if (implementing != null && !implementing.isEmpty()) {
			code.append(isInterface ? " extends " : " implements ");
			for (int i = 0; i < implementing.size(); i++) {
				if (i > 0) code.append(", ");
				code.append(sourceInterfaceType(i, implementing.get(i)));
			}
			code.append(' ');
		}
		code.append("{\n");
	}

	/**
	 * Appends enum constants defined in {@link #fields} to the class.
	 * Must be called before {@link #appendFields(StringBuilder)}.
	 *
	 * @param code
	 * 		Class code to append enum constants to.
	 */
	protected void appendEnumConsts(@Nonnull StringBuilder code) {
		// Enum constants must come first if the class is an enum.
		if (AccessFlag.isEnum(classAccess)) {
			int enumConsts = 0;
			for (FieldMember field : fields) {
				if (isEnumConst(field)) {
					if (enumConsts > 0)
						code.append(", ");
					code.append(field.getName());
					enumConsts++;
				}
			}
			code.append(';');
		}
	}

	/**
	 * Appends all non-enum constant fields to the class.
	 *
	 * @param code
	 * 		Class code to append the fields to.
	 */
	protected void appendFields(@Nonnull StringBuilder code) {
		boolean isInterface = AccessFlag.isInterface(classAccess);
		// Stub out fields / methods
		for (FieldMember field : fields) {
			// Skip stubbing compiler-generated fields.
			if (field.hasBridgeModifier() || field.hasSyntheticModifier())
				continue;

			// Skip enum constants, we added those earlier.
			if (isEnumConst(field))
				continue;

			// Skip stubbing of illegally named fields.
			String name = field.getName();
			if (!isSafeName(name))
				continue;
			NameType fieldNameType = getInfo(name, field.getDescriptor());
			if (!isSafeClassName(fieldNameType.className))
				continue;

			// Append the field. The only modifier that we care about here is if it is static or not.
			if (field.hasStaticModifier())
				code.append("static ");
			code.append(fieldNameType.className).append(' ').append(fieldNameType.name);
			if (isInterface) {
				code.append(" = ");
				appendSourceDefaultValue(code, Type.getType(field.getDescriptor()));
			}
			code.append(";\n");
		}
	}

	/**
	 * Appends all method stubs to the class.
	 * Some methods can be skipped by implementing {@link #doSkipMethod(String, Type)}.
	 *
	 * @param code
	 * 		Class code to append the methods to.
	 *
	 * @throws ExpressionCompileException
	 * 		When the methods could not be stubbed out.
	 */
	protected void appendMethods(@Nonnull StringBuilder code) throws ExpressionCompileException {
		boolean isEnum = AccessFlag.isEnum(classAccess);
		boolean isInterface = AccessFlag.isInterface(classAccess);
		for (MethodMember method : methods) {
			// Skip stubbing compiler-generated methods.
			if (method.hasSyntheticModifier() && !method.hasBridgeModifier())
				continue;
			if (method.hasBridgeModifier() && hasNonBridgeSourceSibling(method))
				continue;

			// Skip stubbing of illegally named methods.
			String name = method.getName();
			boolean isCtor = false;
			if (name.equals("<init>")) {
				// Skip constructors for enum classes since we always drop enum const parameters.
				if (isEnum)
					continue;
				isCtor = true;
			} else if (!isSafeName(name))
				continue;

			// Skip stubbing the method if it is the one we're assembling the expression within.
			String descriptor = method.getDescriptor();
			Type localMethodType = Type.getMethodType(descriptor);
			if (doSkipMethod(name, localMethodType))
				continue;
			if (hasIncompatibleInheritedMethod(method))
				continue;

			// Skip enum's 'valueOf' + 'values'
			if (isEnum &&
					name.equals("valueOf") &&
					descriptor.equals("(Ljava/lang/String;)L" + className + ";"))
				continue;
			if (isEnum &&
					name.equals("values") &&
					descriptor.equals("()[L" + className + ";"))
				continue;

			// Skip stubbing of methods with bad return types / bad parameter types.
			NameType returnInfo = getInfo(name, localMethodType.getReturnType().getDescriptor());
			if (!isSafeClassName(returnInfo.className))
				continue;
			Type[] parameterTypes = localMethodType.getArgumentTypes();
			boolean validParameters = true;
			for (Type parameterType : parameterTypes) {
				if (!isSafeClassName(getInfo("p", parameterType.getDescriptor()).className)) {
					validParameters = false;
					break;
				}
			}
			if (!validParameters)
				continue;

			// Stub the method. Start with the access modifiers.
			if (method.hasPublicModifier())
				code.append("public ");
			else if (method.hasProtectedModifier())
				code.append("protected ");
			else if (method.hasPrivateModifier())
				code.append("private ");
			if (method.hasStaticModifier())
				code.append("static ");
			else if (isInterface && !method.hasAbstractModifier() && !method.hasPrivateModifier())
				code.append("default ");

			// Method name. Consider edge case for constructors.
			if (isCtor)
				code.append(getLocalName()).append('(');
			else
				code.append(returnInfo.className()).append(' ').append(returnInfo.name).append('(');

			// Add the parameters. We only care about the types, names don't really matter.
			Type[] methodParameterTypes = Arrays.copyOf(parameterTypes, parameterTypes.length);
			int parameterCount = methodParameterTypes.length;
			int emittedParameters = 0;
			boolean nonStaticInner = isNonStaticInnerClass();
			for (int i = 0; i < parameterCount; i++) {
				Type paramType = methodParameterTypes[i];

				// Skip this parameter if it is an inner class's outer "this" reference
				if (isCtor && nonStaticInner
						&& paramType.getSort() == Type.OBJECT
						&& className.startsWith(paramType.getInternalName() + '$'))
					continue;

				NameType paramInfo = getInfo("p" + i, paramType.getDescriptor());
				if (emittedParameters++ > 0)
					code.append(", ");
				code.append(paramInfo.className).append(' ').append(paramInfo.name);
			}
			code.append(')');
			if (isCtor)
				code.append(" throws Throwable");
			if (isInterface && method.hasAbstractModifier() && !method.hasStaticModifier() && !method.hasPrivateModifier()) {
				code.append(";\n");
				continue;
			}
			code.append(" { ");
			if (isCtor) {
				// If we know the parent type, we need to properly implement the constructor.
				// If we don't know the parent type, we cannot generate a valid constructor.
				ClassPathNode superPath = superName == null ? null : workspace.findJvmClass(superName);
				if (superPath == null && superName != null)
					// Generally this shouldn't happen since we filter the super-name in the constructor.
					// But just in case we'll keep this error handling here.
					throw new ExpressionCompileException("Cannot generate 'super(...)' for constructor, " +
							"missing type information for: " + superName);

				// If the parent type is known, we can hopefully generate a valid constructor.
				if (superPath != null)
					appendParentConstructorInvocation(code, nonStaticInner);
			} else {
				code.append("throw new RuntimeException();");
			}
			code.append(" }\n");
		}
	}

	/**
	 * @param constructor
	 * 		Method type for a constructor.
	 *
	 * @return {@code true} if all parameter types are representable in source.
	 */
	private boolean isRepresentableConstructor(@Nonnull Type constructor) {
		for (Type parameter : constructor.getArgumentTypes())
			if (!isSafeClassName(parameter.getClassName()))
				return false;
		return true;
	}

	/**
	 * Appends a call to the simplest representable constructor of the parent type.
	 *
	 * @param code
	 * 		Class code to append to.
	 * @param nonStaticInner
	 * 		Whether the generated class is a non-static inner class.
	 *
	 * @throws ExpressionCompileException
	 * 		When the parent type is known but cannot be resolved.
	 */
	protected void appendParentConstructorInvocation(@Nonnull StringBuilder code,
	                                                 boolean nonStaticInner) throws ExpressionCompileException {
		// Get the parent type, throwing if it is missing in the workspace.
		ClassPathNode superPath = superName == null ? null : workspace.findJvmClass(superName);
		if (superPath == null && superName != null)
			throw new ExpressionCompileException("Cannot generate 'super(...)' for constructor, missing type information for: " + superName);
		if (superPath == null)
			return;

		// To make it easy, we'll find the simplest constructor in the parent class and pass dummy values.
		// Unlike regular methods we cannot just say 'throw new RuntimeException();' since calling
		// the 'super(...)' is required.
		Type parentConstructor = superPath.getValue().methodStream()
				.filter(m -> m.getName().equals("<init>"))
				.filter(m -> !m.hasPrivateModifier())
				.map(m -> Type.getMethodType(m.getDescriptor()))
				.filter(this::isRepresentableConstructor)
				.min(Comparator.comparingInt(Type::getArgumentCount))
				.orElse(null);

		// If there are no representable constructors, we cannot generate a valid constructor.
		if (parentConstructor == null)
			return;

		// Filter out any leading parameters that are the outer "this" reference of an inner class,
		// since those are not actually passed by the caller in the source code.
		Type[] parentParameterTypes = parentConstructor.getArgumentTypes();
		int startIndex = 0;
		if (parentParameterTypes.length != 0 && nonStaticInner) {
			Type firstParameterType = parentParameterTypes[0];
			if (firstParameterType.getSort() == Type.OBJECT && isParentSyntheticOuterParameter(firstParameterType))
				startIndex = 1;
		}

		// Emit the super call with dummy values for the parameters.
		code.append("super(");
		int emittedParentParameters = 0;
		for (int i = startIndex; i < parentParameterTypes.length; i++) {
			Type type = parentParameterTypes[i];
			if (emittedParentParameters++ > 0)
				code.append(", ");
			if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
				code.append('(').append(sourceType(type)).append(") null");
			} else {
				char prim = type.getDescriptor().charAt(0);
				if (prim == 'Z')
					code.append("false");
				else
					code.append('0');
			}
		}
		code.append(");");
	}

	/**
	 * Checks whether a parent constructor parameter is the compiler-supplied outer instance.
	 * This also covers a child nested in a subtype of the parent's enclosing class.
	 *
	 * @param parameterType
	 * 		Type of the parameter in a constructor to check.
	 */
	private boolean isParentSyntheticOuterParameter(@Nonnull Type parameterType) {
		if (parameterType.getSort() != Type.OBJECT)
			return false;

		// Check if the class context is a nested class of the parameter type.
		// This indicates that the parameter is the synthetic outer "this" reference.
		if (className.startsWith(parameterType.getInternalName() + '$'))
			return true;

		// If we cut off the last inner class separator, we can check if the parameter type is an enclosing class of the current class.
		int split = className.lastIndexOf('$');
		if (split < 0)
			return false;
		String enclosingName = className.substring(0, split);
		return inheritanceGraph.isAssignableFrom(parameterType.getInternalName(), enclosingName);
	}

	/**
	 * Appends direct inner classes, skipping any that are emitted separately by the caller.
	 *
	 * @param code
	 * 		Class code to append the inner classes to.
	 *
	 * @throws ExpressionCompileException
	 * 		When the inner classes could not be stubbed out.
	 */
	protected void appendInnerClasses(@Nonnull StringBuilder code) throws ExpressionCompileException {
		appendInnerClasses(code, Set.of());
	}

	/**
	 * Appends direct inner classes, optionally excluding selected declarations.
	 *
	 * @param code
	 * 		Class code to append the inner classes to.
	 * @param excludedNames
	 * 		Inner class names which are emitted separately by the caller.
	 *
	 * @throws ExpressionCompileException
	 * 		When the inner classes could not be stubbed out.
	 */
	protected void appendInnerClasses(@Nonnull StringBuilder code,
	                                  @Nonnull Set<String> excludedNames) throws ExpressionCompileException {
		Set<String> visited = new HashSet<>();
		for (InnerClassInfo innerClass : innerClasses) {
			String innerClassName = innerClass.getInnerClassName();

			// Skip duplicate inner classes.
			if (!visited.add(innerClassName))
				continue;

			// Skip inner classes that are emitted separately by the caller.
			if (excludedNames.contains(innerClassName))
				continue;

			// If the inner class's outer class name is not an exact match, skip it.
			// We will recursively visit nested inner classes, so if we have:
			//  A$B$C
			// Then we don't want to put C as a direct inner of A.
			// We want to make B an inner of A, and C an inner of B.
			if (!className.equals(innerClass.getOuterClassName()))
				continue;

			// Skip stubbing of inner classes with illegal names.
			if (!isSafeClassName(cleanType(innerClassName)))
				continue;
			ClassPathNode innerClassPath = workspace.findClass(innerClassName);
			if (innerClassPath != null) {
				ClassInfo innerClassInfo = innerClassPath.getValue();
				ClassStubGenerator generator = new InnerClassStubGenerator(workspace, inheritanceGraph,
						// Bitwise or the flags together since we need to know if the inner class is static.
						// The inner class attribute will say whether it is or not, but the actual class will not.
						innerClassInfo.getAccess() | (innerClass.getInnerAccess() & Modifier.STATIC),
						innerClassInfo.getName(),
						innerClassInfo.getSuperName(),
						innerClassInfo.getInterfaces(),
						innerClassInfo.getFields(),
						innerClassInfo.getMethods(),
						innerClassInfo.getInnerClasses(),
						innerClassInfo.getSignature()
				);
				String inner = generator.generate();
				code.append('\n').append(inner).append('\n');
			}
		}
	}

	/**
	 * Appends all members and closes the class body. This allows nested expression
	 * hosts to reuse the normal member stubbing while supplying their own topology.
	 *
	 * @param code
	 * 		Class source to append to.
	 * @param includeInnerClasses
	 * 		Whether direct nested classes should also be emitted.
	 */
	protected void appendClassContents(@Nonnull StringBuilder code, boolean includeInnerClasses) throws ExpressionCompileException {
		appendEnumConsts(code);
		appendClassMembers(code, includeInnerClasses);
		appendClassEnd(code);
	}

	/**
	 * Appends members without closing the class body.
	 *
	 * @param code
	 * 		Class source to append to.
	 * @param includeInnerClasses
	 * 		Whether direct nested classes should also be emitted.
	 */
	protected void appendClassMembers(@Nonnull StringBuilder code, boolean includeInnerClasses) throws ExpressionCompileException {
		appendFields(code);
		appendMethods(code);
		if (includeInnerClasses)
			appendInnerClasses(code);
	}

	/**
	 * Ends the class definition.
	 *
	 * @param code
	 * 		Class code to append end to.
	 */
	protected void appendClassEnd(@Nonnull StringBuilder code) {
		// Done with the class
		code.append("}\n");
	}

	/**
	 * Controls which methods are included in {@link #appendMethods(StringBuilder)}.
	 *
	 * @param name
	 * 		Method name.
	 * @param type
	 * 		Method type.
	 *
	 * @return {@code true} to skip. {@code false} to include in output stubbing.
	 */
	protected abstract boolean doSkipMethod(@Nonnull String name, @Nonnull Type type);

	/**
	 * @return Modifier to prefix {@code Foo} in {@code class Foo {}}.
	 */
	@Nonnull
	public String getLocalModifier() {
		return "abstract";
	}

	/**
	 * @return Name string to where {@code Foo} is in {@code class Foo {}}.
	 */
	@Nonnull
	protected String getLocalName() {
		return StringUtil.shortenPath(className);
	}

	/**
	 * @param type
	 * 		Some internal type name.
	 *
	 * @return Cleaned name for use in source.
	 */
	@Nonnull
	protected static String cleanType(String type) {
		return type.replace('/', '.').replace('$', '.');
	}

	/**
	 * @param index
	 * 		Interface index.
	 * @param internalName
	 * 		Interface internal name.
	 *
	 * @return Interface type for source output.
	 */
	@Nonnull
	protected String sourceInterfaceType(int index, @Nonnull String internalName) {
		// If we have a generic signature, use it to get the interface type.
		List<String> genericTypes = GenericSignatureRenderer.renderInterfaces(classSignature);
		if (index < genericTypes.size()) {
			String candidate = genericTypes.get(index);
			int genericStart = candidate.indexOf('<');
			if (genericStart > 0 && candidate.substring(0, genericStart).equals(cleanType(internalName)))
				return candidate;
		}

		// If we don't have a generic signature, or the generic signature doesn't match the interface type,
		// just return the cleaned type.
		return cleanType(internalName);
	}

	/**
	 * @param type
	 * 		Descriptor type.
	 *
	 * @return Java source spelling of the type.
	 */
	@Nonnull
	protected static String sourceType(@Nonnull Type type) {
		if (type.getSort() == Type.ARRAY)
			return sourceType(type.getElementType()) + "[]".repeat(type.getDimensions());
		if (type.getSort() == Type.OBJECT)
			return cleanType(type.getInternalName());
		return type.getClassName();
	}

	/**
	 * Appends a source-compatible default value for an interface field.
	 *
	 * @param code
	 * 		Source to append to.
	 * @param type
	 * 		Field type.
	 */
	protected static void appendSourceDefaultValue(@Nonnull StringBuilder code, @Nonnull Type type) {
		switch (type.getSort()) {
			case Type.BOOLEAN -> code.append("false");
			case Type.LONG -> code.append("0L");
			case Type.FLOAT -> code.append("0.0f");
			case Type.DOUBLE -> code.append("0.0d");
			case Type.BYTE, Type.SHORT, Type.CHAR, Type.INT -> code.append('0');
			default -> code.append("null");
		}
	}

	/**
	 * @param method
	 * 		Method to check.
	 *
	 * @return {@code true} when the method cannot be represented as a Java source override of an inherited method.
	 */
	protected boolean hasIncompatibleInheritedMethod(@Nonnull MethodMember method) {
		// Get the class vertex for the current class.
		// If it doesn't exist, then we cannot check for inherited methods (we assume they are compatible).
		InheritanceVertex classVertex = inheritanceGraph.getVertex(className);
		if (classVertex == null)
			return false;

		// Check if any parent class has a method with the same source signature but an incompatible return type.
		String signature = sourceSignature(method.getName(), method.getDescriptor());
		Type methodType = Type.getMethodType(method.getDescriptor());
		return classVertex.allParents().anyMatch(parent -> parent.getValue().getMethods().stream()
				.anyMatch(parentMethod -> signature.equals(sourceSignature(parentMethod.getName(), parentMethod.getDescriptor())) &&
						!isSourceReturnCompatible(Type.getMethodType(parentMethod.getDescriptor()).getReturnType(), methodType.getReturnType())));
	}

	/**
	 * @param parentType
	 * 		Parent method return type.
	 * @param childType
	 * 		Child method return type.
	 *
	 * @return {@code true} if the child method return type is compatible with the parent method return type.
	 */
	protected boolean isSourceReturnCompatible(@Nonnull Type parentType, @Nonnull Type childType) {
		// Check for exact match first, since that is the most common case.
		if (parentType.equals(childType))
			return true;

		// Both types must be object types.
		if (parentType.getSort() != Type.OBJECT || childType.getSort() != Type.OBJECT)
			return false;

		// Check if the child type is assignable to the parent type in the inheritance graph.
		return inheritanceGraph.isAssignableFrom(parentType.getInternalName(), childType.getInternalName());
	}

	/**
	 * @return {@code true} when this class is a non-static nested class.
	 */
	protected boolean isNonStaticInnerClass() {
		if (className.indexOf('$') < 0)
			return false;
		InnerClassInfo nestedInfo = getNestedInnerInfo();
		return nestedInfo == null ? !AccessFlag.isStatic(classAccess) : !AccessFlag.isStatic(nestedInfo.getInnerAccess());
	}

	/**
	 * @return Inner-class metadata for this class as recorded by its enclosing class.
	 */
	@Nullable
	protected InnerClassInfo getNestedInnerInfo() {
		int split = className.lastIndexOf('$');
		if (split < 0)
			return null;
		ClassPathNode outerPath = workspace.findClass(className.substring(0, split));
		if (outerPath == null)
			return null;
		return outerPath.getValue().getInnerClasses().stream()
				.filter(inner -> className.equals(inner.getInnerClassName()))
				.findFirst().orElse(null);
	}

	/**
	 * Check if the given method has a non-bridge sibling with the same source signature.
	 * <pre>{@code
	 * class StringSupplier implements java.util.function.Supplier<String> {
	 *     // Source method you wrote.
	 *     // - Compiled signature is: ()Ljava/lang/String;
	 *     @Override
	 *     public String get() { return "Hello"; }
	 *
	 *     // Bridge method that satisfies the Supplier interface.
	 *     // - Compiled signature is: ()Ljava/lang/Object;
	 *     // - Delegates to the source-level method.
	 *     public bridge Object get() { return get(); }
	 *  }
	 * }</pre>
	 *
	 * @param method
	 * 		Method to check. Assumed to be a bridge method.
	 *
	 * @return {@code true} if there is a non-bridge method with the same source signature as the given method.
	 *
	 * @see #hasNonBridgeSourceSibling(MethodMember)
	 */
	protected boolean hasNonBridgeSourceSibling(@Nonnull MethodMember method) {
		// If the method is not a bridge, then it cannot have a non-bridge sibling.
		if (!method.hasBridgeModifier())
			return false;

		String sourceSignature = sourceSignature(method.getName(), method.getDescriptor());
		for (MethodMember other : methods) {
			// Skip self method.
			if (other == method)
				continue;

			// Skip methods with different names.
			if (!method.getName().equals(other.getName()))
				continue;

			// Skip bridge/synthetic methods, we only care about non-bridge source siblings.
			if (other.hasBridgeModifier() || other.hasSyntheticModifier())
				continue;

			// Check if the candidate method has the same erased parameter shape.
			String otherSignature = sourceSignature(other.getName(), other.getDescriptor());
			if (sourceSignature.equals(otherSignature) || isBridgeTargetWithErasedParameterShape(method, other))
				return true;
		}
		return false;
	}

	/**
	 * Check if the given bridge method has a source sibling.
	 *
	 * @param bridgeName
	 * 		Name of the bridge method.
	 * @param bridgeType
	 * 		Method type of the bridge method.
	 * @param bridgeFlags
	 * 		Access flags of the bridge method.
	 *
	 * @return {@code true} if there is a non-bridge method with the same source signature as the given method.
	 *
	 * @see #hasNonBridgeSourceSibling(MethodMember)
	 */
	protected boolean hasBridgeSourceSibling(@Nonnull String bridgeName, @Nonnull Type bridgeType, int bridgeFlags) {
		// The method context isn't a bridge, so it cannot be a bridge sibling.
		if (!AccessFlag.isBridge(bridgeFlags))
			return false;

		// The method context is a bridge, so we need to check if there is a
		// sibling source method that has the same name and erased parameter shape.
		for (MethodMember method : methods) {
			// Skip methods with different names.
			if (!bridgeName.equals(method.getName()))
				continue;

			// Skip bridge/synthetic methods. The candidate must be a source method.
			if (method.hasBridgeModifier() || method.hasSyntheticModifier())
				continue;

			// Check if the candidate method has the same erased parameter shape.
			Type candidateType = Type.getMethodType(method.getDescriptor());
			if (isBridgeTargetWithErasedParameterShape(bridgeName, bridgeType, candidateType))
				return true;
		}
		return false;
	}

	/**
	 * Check if the given bridge method has a source sibling <i>(the candidate method)</i>.
	 *
	 * @param bridge
	 * 		Bridge method.
	 * @param candidate
	 * 		Candidate source form associated with the bridge method.
	 *
	 * @see #hasNonBridgeSourceSibling(MethodMember)
	 */
	protected boolean isBridgeTargetWithErasedParameterShape(@Nonnull MethodMember bridge,
	                                                         @Nonnull MethodMember candidate) {
		// Names must match, otherwise they are not related.
		String methodName = bridge.getName();
		if (!methodName.equals(candidate.getName()))
			return false;

		// Parameter counts must match.
		// Bridge return types are allowed to differ from the source method's return type because covariant returns and generic substitutions both produce return-type bridges.
		Type bridgeType = Type.getMethodType(bridge.getDescriptor());
		Type candidateType = Type.getMethodType(candidate.getDescriptor());
		if (bridgeType.getArgumentTypes().length != candidateType.getArgumentTypes().length)
			return false;

		// The source signature (name + parameter types) must differ.
		return isBridgeTargetWithErasedParameterShape(methodName, bridgeType, candidateType);
	}

	/**
	 * Check if the given bridge method has a source sibling <i>(the candidate method)</i>.
	 *
	 * @param methodName
	 * 		Name of the bridged method.
	 * @param bridgeType
	 * 		Method type of the bridge method.
	 * @param candidateType
	 * 		Method type of the candidate method.
	 *
	 * @see #hasNonBridgeSourceSibling(MethodMember)
	 */
	protected boolean isBridgeTargetWithErasedParameterShape(@Nonnull String methodName,
	                                                         @Nonnull Type bridgeType,
	                                                         @Nonnull Type candidateType) {
		// If the bridge and candidate are the same method, then they are not siblings.
		if (bridgeType.equals(candidateType))
			return false;

		// Bridge and candidate must have the same number of parameters.
		Type[] bridgeParameters = bridgeType.getArgumentTypes();
		Type[] candidateParameters = candidateType.getArgumentTypes();
		if (bridgeParameters.length != candidateParameters.length)
			return false;

		// All parameters must be assignable to each other, or the same type.
		for (int i = 0; i < bridgeParameters.length; i++) {
			Type bridgeParameter = bridgeParameters[i];
			Type candidateParameter = candidateParameters[i];
			if (bridgeParameter.equals(candidateParameter))
				continue;
			if (bridgeParameter.getSort() != Type.OBJECT
					|| candidateParameter.getSort() != Type.OBJECT
					|| !inheritanceGraph.isAssignableFrom(bridgeParameter.getInternalName(), candidateParameter.getInternalName()))
				return false;
		}

		// The source signature (name + parameter types) must differ.
		String bridgeSignature = sourceSignature(methodName, bridgeType.getDescriptor());
		String candidateSignature = sourceSignature(methodName, candidateType.getDescriptor());
		return !Objects.equals(bridgeSignature, candidateSignature);
	}

	/**
	 * In Java source, you can have multiple methods with the same name if the parameters are distinct.
	 * However, the return type is not part of the method signature in source. So for duplication detection
	 * we want to ignore the return type when comparing methods.
	 *
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 *
	 * @return Name + method descriptor, without return type.
	 *
	 * @see #hasNonBridgeSourceSibling(MethodMember)
	 */
	@Nonnull
	protected static String sourceSignature(@Nonnull String name, @Nonnull String descriptor) {
		int end = descriptor.indexOf(')');
		return name + descriptor.substring(0, end + 1);
	}

	/**
	 * @param type
	 * 		Some internal type name.
	 *
	 * @return {@code true} if the type is an inner class of the current class.
	 */
	protected boolean isInnerClassType(@Nonnull String type) {
		return innerClasses.stream().anyMatch(c -> c.getInnerClassName().equals(type));
	}

	/**
	 * @param field
	 * 		Field to check.
	 *
	 * @return {@code true} when it represents an enum constant.
	 */
	protected boolean isEnumConst(@Nonnull FieldMember field) {
		// This class must be an enum.
		if (!AccessFlag.isEnum(classAccess))
			return false;

		// The field must be 'public static final'
		if (!field.hasFinalModifier() || !field.hasStaticModifier() || !field.hasPublicModifier())
			return false;

		// The descriptor must be: L + className + ;
		if (field.getDescriptor().length() != className.length() + 2)
			return false;
		Type fieldDesc = Type.getObjectType(field.getDescriptor());
		return fieldDesc.getInternalName().equals(className);
	}

	/**
	 * @param vertex
	 * 		Inheritance vertex to check.
	 *
	 * @return {@code true} if the type is sealed <i>(Defines any permitted subclass)</i>.
	 */
	private boolean isSealedType(@Nonnull InheritanceVertex vertex) {
		if (vertex.getValue() instanceof JvmClassInfo cls) {
			AtomicBoolean result = new AtomicBoolean(false);
			cls.getClassReader().accept(new SkippingClassVisitor() {
				@Override
				public void visitPermittedSubclass(String permittedSubclass) {
					result.set(true);
				}
			}, ClassReader.SKIP_DEBUG);
			return result.get();
		}
		return false;
	}

	/**
	 * @param name
	 * 		Class name to check.
	 *
	 * @return The class name if it is safe to reference, otherwise {@code null}.
	 */
	private boolean isSafeReferencableName(@Nullable String name) {
		if (name == null)
			return false;

		// Must be well-formed
		if (!isSafeInternalClassName(name))
			return false;

		// Must be found in the workspace
		return workspace.findClass(name) != null;
	}

	/**
	 * @param name
	 * 		Name to check.
	 *
	 * @return {@code true} when it can be used as a variable name safely.
	 */
	protected static boolean isSafeName(@Nonnull String name) {
		// Name must not be empty.
		if (name.isEmpty())
			return false;

		// Must be comprised of valid identifier characters.
		char first = name.charAt(0);
		if (!Character.isJavaIdentifierStart(first))
			return false;
		char[] chars = name.toCharArray();
		for (int i = 1; i < chars.length; i++) {
			if (!Character.isJavaIdentifierPart(chars[i]))
				return false;
		}

		// Cannot be a reserved keyword.
		return !Keywords.getKeywordsWithoutVarSafe().contains(name);
	}

	/**
	 * @param internalName
	 * 		Name to check. Expected to be in the internal format. IE {@code java/lang/String}.
	 *
	 * @return {@code true} when it can be used as a class name safely.
	 */
	protected static boolean isSafeInternalClassName(@Nonnull String internalName) {
		// Sanity check input
		if (internalName.indexOf('.') >= 0)
			throw new IllegalStateException("Saw source name format, expected internal name format");

		// Extending record directly is not allowed
		if ("java/lang/Record".equals(internalName))
			return false;

		// All package name portions and the class name must be valid names.
		return StringUtil.fastSplit(internalName, true, '/').stream()
				.allMatch(ClassStubGenerator::isSafeName);
	}

	/**
	 * @param name
	 * 		Name to check. Expected to be in the source format. IE {@code java.lang.String}.
	 *
	 * @return {@code true} when it can be used as a class name safely.
	 */
	protected static boolean isSafeClassName(@Nonnull String name) {
		// Sanity check input
		if (name.indexOf('/') >= 0)
			throw new IllegalStateException("Saw internal name format, expected source name format");

		// Strip array dimensions
		if (name.endsWith("[]"))
			name = name.substring(0, name.indexOf('['));

		// Allow primitives
		if (software.coley.recaf.util.Types.isPrimitiveClassName(name))
			return true;

		// All package name portions and the class name must be valid names.
		return StringUtil.fastSplit(name, true, '.').stream()
				.allMatch(ClassStubGenerator::isSafeName);
	}

	/**
	 * @param name
	 * 		Variable name.
	 * @param descriptor
	 * 		Variable descriptor.
	 *
	 * @return Variable info wrapper.
	 */
	@Nonnull
	protected static NameType getInfo(@Nonnull String name, @Nonnull String descriptor) {
		int size;
		String className;
		if (Types.isPrimitive(descriptor)) {
			Type primitiveType = Type.getType(descriptor);
			size = Types.isWide(primitiveType) ? 2 : 1;
			className = primitiveType.getClassName();
		} else if (descriptor.charAt(0) == '[') {
			Type arrayParameterType = Type.getType(descriptor);
			Type componentReturnType = arrayParameterType.getElementType();
			if (Types.isPrimitive(componentReturnType)) {
				className = componentReturnType.getClassName();
			} else {
				className = cleanType(componentReturnType.getInternalName());
			}
			className += "[]".repeat(arrayParameterType.getDimensions());
			size = 1;
		} else {
			size = 1;
			className = cleanType(Type.getType(descriptor).getInternalName());
		}
		return new NameType(size, name, className);
	}

	/**
	 * Wrapper for field/variable info.
	 *
	 * @param size
	 * 		Variable slot size.
	 * @param name
	 * 		Variable name.
	 * @param className
	 * 		Variable class type name.
	 */
	protected record NameType(int size, @Nonnull String name, @Nonnull String className) {}
}
