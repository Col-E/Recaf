package software.coley.recaf.services.compile.stub;

import dev.xdark.blw.type.ArrayType;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.type.PrimitiveType;
import dev.xdark.blw.type.Type;
import dev.xdark.blw.type.Types;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.assembler.ExpressionCompileException;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.Keywords;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base stub generator for classes.
 *
 * @author Matt Coley
 */
public abstract class ClassStubGenerator {
	protected final Workspace workspace;
	protected final int classAccess;
	protected final String className;
	protected final String superName;
	protected final List<String> implementing;
	protected final List<FieldMember> fields;
	protected final List<MethodMember> methods;
	protected final List<InnerClassInfo> innerClasses;

	/**
	 * @param workspace
	 * 		Workspace to pull class information from.
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
	 */
	public ClassStubGenerator(@Nonnull Workspace workspace,
	                          int classAccess,
	                          @Nonnull String className,
	                          @Nullable String superName,
	                          @Nonnull List<String> implementing,
	                          @Nonnull List<FieldMember> fields,
	                          @Nonnull List<MethodMember> methods,
	                          @Nonnull List<InnerClassInfo> innerClasses) {
		this.workspace = workspace;
		this.classAccess = classAccess;
		this.className = isSafeInternalClassName(className) ? className : "obfuscated_class";
		this.superName = superName != null && isSafeInternalClassName(superName) ? superName : null;
		this.implementing = implementing.stream()
				.filter(ClassStubGenerator::isSafeInternalClassName)
				.toList();
		this.fields = fields;
		this.methods = methods;
		this.innerClasses = innerClasses;
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
		code.append(AccessFlag.isEnum(classAccess) ? "enum " : getLocalModifier() + " class ").append(getLocalName());
		if (superName != null && !superName.equals("java/lang/Object") && !superName.equals("java/lang/Enum"))
			code.append(" extends ").append(superName.replace('/', '.'));
		if (implementing != null && !implementing.isEmpty())
			code.append(" implements ").append(implementing.stream().map(s -> s.replace('/', '.')).collect(Collectors.joining(", "))).append(' ');
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
				if (field.getDescriptor().length() == 1)
					continue;
				InstanceType fieldDesc = Types.instanceTypeFromDescriptor(field.getDescriptor());
				if (fieldDesc.internalName().equals(className) && field.hasFinalModifier() && field.hasStaticModifier()) {
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
	 *
	 * @throws ExpressionCompileException
	 * 		When the fields could not be stubbed out.
	 */
	protected void appendFields(@Nonnull StringBuilder code) throws ExpressionCompileException {
		// Stub out fields / methods
		for (FieldMember field : fields) {
			// Skip stubbing compiler-generated fields.
			if (field.hasBridgeModifier() || field.hasSyntheticModifier())
				continue;

			// Skip stubbing of illegally named fields.
			String name = field.getName();
			if (!isSafeName(name))
				continue;
			NameType fieldNameType = getInfo(name, field.getDescriptor());
			if (!isSafeClassName(fieldNameType.className))
				continue;

			// Skip enum constants, we added those earlier.
			if (AccessFlag.isEnum(classAccess)
					&& fieldNameType.className.equals(className.replace('/', '.'))
					&& field.hasFinalModifier()
					&& field.hasStaticModifier())
				continue;

			// Skip fields with types that aren't accessible in the workspace.
			if (isMissingType(field.getDescriptor())) continue;

			// Append the field. The only modifier that we care about here is if it is static or not.
			if (field.hasStaticModifier())
				code.append("static ");
			code.append(fieldNameType.className).append(' ').append(fieldNameType.name).append(";\n");
		}
	}

	/**
	 * Appends all method stubs to the class.
	 * Some methods can be skipped by implementing {@link #doSkipMethod(String, MethodType)}.
	 *
	 * @param code
	 * 		Class code to append the methods to.
	 *
	 * @throws ExpressionCompileException
	 * 		When the methods could not be stubbed out.
	 */
	protected void appendMethods(@Nonnull StringBuilder code) throws ExpressionCompileException {
		boolean isEnum = AccessFlag.isEnum(classAccess);
		for (MethodMember method : methods) {
			// Skip stubbing compiler-generated methods.
			if (method.hasBridgeModifier() || method.hasSyntheticModifier())
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
			MethodType localMethodType = Types.methodType(descriptor);
			if (doSkipMethod(name, localMethodType))
				continue;

			// Skip enum's 'valueOf'
			if (isEnum &&
					name.equals("valueOf") &&
					descriptor.equals("(Ljava/lang/String;)L" + className + ";"))
				continue;

			// Skip stubbing of methods with bad return types / bad parameter types.
			NameType returnInfo = getInfo(name, localMethodType.returnType().descriptor());
			if (!isSafeClassName(returnInfo.className))
				continue;
			List<ClassType> parameterTypes = localMethodType.parameterTypes();
			if (!parameterTypes.stream().map(p -> {
				try {
					return getInfo("p", p.descriptor()).className();
				} catch (Throwable t) {
					return "\0"; // Bogus which will throw off the safe name check.
				}
			}).allMatch(ClassStubGenerator::isSafeClassName))
				continue;

			// Skip methods with return/parameter types that aren't accessible in the workspace.
			boolean hasMissingType = false;
			Type[] types = new Type[parameterTypes.size() + 1];
			for (int i = 0; i < types.length - 1; i++)
				types[i] = parameterTypes.get(i);
			types[parameterTypes.size()] = localMethodType.returnType();
			for (Type type : types) {
				hasMissingType = isMissingType(type);
				if (hasMissingType)
					break;
			}
			if (hasMissingType) continue;

			// Stub the method. Start with the access modifiers.
			if (method.hasPublicModifier())
				code.append("public ");
			else if (method.hasProtectedModifier())
				code.append("protected ");
			else if (method.hasPrivateModifier())
				code.append("private ");
			if (method.hasStaticModifier())
				code.append("static ");

			// Method name. Consider edge case for constructors.
			if (isCtor)
				code.append(getLocalName()).append('(');
			else
				code.append(returnInfo.className()).append(' ').append(returnInfo.name).append('(');

			// Add the parameters. We only care about the types, names don't really matter.
			List<ClassType> methodParameterTypes = parameterTypes;
			int parameterCount = methodParameterTypes.size();
			for (int i = 0; i < parameterCount; i++) {
				ClassType paramType = methodParameterTypes.get(i);

				// Skip this parameter if it is an inner class's outer "this" reference
				if (isCtor
						&& paramType instanceof ObjectType paramObjectType
						&& className.startsWith(paramObjectType.internalName() + '$'))
					continue;

				NameType paramInfo = getInfo("p" + i, paramType.descriptor());
				code.append(paramInfo.className).append(' ').append(paramInfo.name);
				if (i < parameterCount - 1) code.append(", ");
			}
			code.append(") { ");
			if (isCtor) {
				// If we know the parent type, we need to properly implement the constructor.
				// If we don't know the parent type, we cannot generate a valid constructor.
				ClassPathNode superPath = superName == null ? null : workspace.findJvmClass(superName);
				if (superPath == null && superName != null)
					throw new ExpressionCompileException("Cannot generate 'super(...)' for constructor, " +
							"missing type information for: " + superName);
				if (superPath != null) {
					// To make it easy, we'll find the simplest constructor in the parent class and pass dummy values.
					// Unlike regular methods we cannot just say 'throw new RuntimeException();' since calling
					// the 'super(...)' is required.
					MethodType parentConstructor = superPath.getValue().methodStream()
							.filter(m -> m.getName().equals("<init>"))
							.map(m -> Types.methodType(m.getDescriptor()))
							.min(Comparator.comparingInt(a -> a.parameterTypes().size()))
							.orElse(null);
					if (parentConstructor != null) {
						code.append("super(");
						parameterCount = parentConstructor.parameterTypes().size();
						for (int i = 0; i < parameterCount; i++) {
							ClassType type = parentConstructor.parameterTypes().get(i);
							if (type instanceof ObjectType) {
								code.append("null");
							} else {
								char prim = type.descriptor().charAt(0);
								if (prim == 'Z')
									code.append("false");
								else
									code.append('0');
							}
							if (i < parameterCount - 1) code.append(", ");
						}
						code.append(");");
					}
				}
			} else {
				code.append("throw new RuntimeException();");
			}
			code.append(" }\n");
		}
	}


	/**
	 * @param code
	 * 		Class code to append the inner classes to.
	 *
	 * @throws ExpressionCompileException
	 * 		When the inner classes could not be stubbed out.
	 */
	protected void appendInnerClasses(@Nonnull StringBuilder code) throws ExpressionCompileException {
		for (InnerClassInfo innerClass : innerClasses) {
			String innerClassName = innerClass.getInnerClassName();
			if (!innerClassName.startsWith(className))
				continue;
			if (innerClassName.length() <= className.length())
				continue;
			ClassPathNode innerClassPath = workspace.findClass(innerClassName);
			if (innerClassPath != null) {
				ClassInfo innerClassInfo = innerClassPath.getValue();
				ClassStubGenerator generator = new InnerClassStubGenerator(workspace,
						innerClassInfo.getAccess(),
						innerClassInfo.getName(),
						innerClassInfo.getSuperName(),
						innerClassInfo.getInterfaces(),
						innerClassInfo.getFields(),
						innerClassInfo.getMethods(),
						innerClassInfo.getInnerClasses()
				);
				String inner = generator.generate();
				code.append('\n').append(inner).append('\n');
			}
		}
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
	protected abstract boolean doSkipMethod(@Nonnull String name, @Nonnull MethodType type);

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
	 * @param descriptor
	 * 		Some non-method descriptor.
	 *
	 * @return {@code true} if the type in the descriptor is found in the {@link #workspace}.
	 */
	protected boolean isMissingType(@Nonnull String descriptor) {
		Type type = Types.typeFromDescriptor(descriptor);
		return isMissingType(type);
	}

	/**
	 * @param type
	 * 		Some non-method type.
	 *
	 * @return {@code true} if the type in the descriptor is found in the {@link #workspace}.
	 */
	protected boolean isMissingType(@Nonnull Type type) {
		if (type instanceof InstanceType instanceType && workspace.findClass(instanceType.internalName()) == null)
			return true;
		else
			return type instanceof ArrayType arrayType
					&& arrayType.rootComponentType() instanceof InstanceType instanceType
					&& workspace.findClass(instanceType.internalName()) == null;
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
		return !Keywords.getKeywords().contains(name);
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
	 *
	 * @throws ExpressionCompileException
	 * 		When the variable descriptor is malformed.
	 */
	@Nonnull
	protected static NameType getInfo(@Nonnull String name, @Nonnull String descriptor) throws ExpressionCompileException {
		int size;
		String className;
		if (Types.isPrimitive(descriptor)) {
			PrimitiveType primitiveType = Types.primitiveFromDesc(descriptor);
			size = Types.category(primitiveType);
			className = primitiveType.name();
		} else if (descriptor.charAt(0) == '[') {
			ArrayType arrayParameterType = Types.arrayTypeFromDescriptor(descriptor);
			ClassType componentReturnType = arrayParameterType.componentType();
			if (componentReturnType instanceof PrimitiveType primitiveParameter) {
				className = primitiveParameter.name();
			} else if (componentReturnType instanceof InstanceType instanceType) {
				className = instanceType.internalName().replace('/', '.').replace('$', '.');
			} else {
				throw new ExpressionCompileException("Illegal component type: " + componentReturnType);
			}
			className += "[]".repeat(arrayParameterType.dimensions());
			size = 1;
		} else {
			size = 1;
			className = Types.instanceTypeFromDescriptor(descriptor).internalName().replace('/', '.').replace('$', '.');
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
	protected record NameType(int size, @Nonnull String name, @Nonnull String className) {
	}
}
