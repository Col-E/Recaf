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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
	 */
	public ClassStubGenerator(@Nonnull Workspace workspace,
	                          @Nonnull InheritanceGraph inheritanceGraph,
	                          int classAccess,
	                          @Nonnull String className,
	                          @Nullable String superName,
	                          @Nonnull List<String> implementing,
	                          @Nonnull List<FieldMember> fields,
	                          @Nonnull List<MethodMember> methods,
	                          @Nonnull List<InnerClassInfo> innerClasses) {
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
		if (implementing != null && !implementing.isEmpty())
			code.append(isInterface ? " extends " : " implements ").append(implementing.stream()
					.map(ClassStubGenerator::cleanType)
					.collect(Collectors.joining(", "))).append(' ');
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

			// Skip fields with types that aren't accessible in the workspace.
			if (isMissingType(field.getDescriptor()))
				continue;

			// Append the field. The only modifier that we care about here is if it is static or not.
			if (field.hasStaticModifier())
				code.append("static ");
			code.append(fieldNameType.className).append(' ').append(fieldNameType.name).append(";\n");
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
			Type localMethodType = Type.getMethodType(descriptor);
			if (doSkipMethod(name, localMethodType))
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
			for (Type parameterType : parameterTypes)
				if (!isSafeClassName(getInfo("p", parameterType.getDescriptor()).className))
					return;

			// Skip methods with return/parameter types that aren't accessible in the workspace.
			boolean hasMissingType = false;
			Type[] types = new Type[parameterTypes.length + 1];
			System.arraycopy(parameterTypes, 0, types, 0, types.length - 1);
			types[parameterTypes.length] = localMethodType.getReturnType();
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
			for (int i = 0; i < parameterCount; i++) {
				Type paramType = methodParameterTypes[i];

				// Skip this parameter if it is an inner class's outer "this" reference
				if (isCtor
						&& paramType.getSort() == Type.OBJECT
						&& className.startsWith(paramType.getInternalName() + '$'))
					continue;

				NameType paramInfo = getInfo("p" + i, paramType.getDescriptor());
				code.append(paramInfo.className).append(' ').append(paramInfo.name);
				if (i < parameterCount - 1)
					code.append(", ");
			}
			code.append(')');
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
				if (superPath != null) {
					// To make it easy, we'll find the simplest constructor in the parent class and pass dummy values.
					// Unlike regular methods we cannot just say 'throw new RuntimeException();' since calling
					// the 'super(...)' is required.
					Type parentConstructor = superPath.getValue().methodStream()
							.filter(m -> m.getName().equals("<init>"))
							.map(m -> Type.getMethodType(m.getDescriptor()))
							.min(Comparator.comparingInt(Type::getArgumentCount))
							.orElse(null);
					if (parentConstructor != null) {
						code.append("super(");

						// Filter out any leading parameters that are the outer "this" reference of an inner class,
						// since those are not actually passed by the caller in the source code.
						Type[] parentParameterTypes = parentConstructor.getArgumentTypes();
						int startIndex = 0;
						if (parentParameterTypes.length != 0) {
							Type firstParameterType = parentParameterTypes[0];
							if (firstParameterType.getSort() == Type.OBJECT &&
									className.startsWith(firstParameterType.getInternalName() + '$'))
								startIndex = 1;
						}

						parameterCount = parentParameterTypes.length;
						for (int i = startIndex; i < parameterCount; i++) {
							Type type = parentParameterTypes[i];
							if (type.getSort() == Type.OBJECT) {
								code.append("null");
							} else {
								char prim = type.getDescriptor().charAt(0);
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
		Set<String> visited = new HashSet<>();
		for (InnerClassInfo innerClass : innerClasses) {
			String innerClassName = innerClass.getInnerClassName();

			// Skip duplicate inner classes.
			if (!visited.add(innerClassName))
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
	 * @param descriptor
	 * 		Some non-method descriptor.
	 *
	 * @return {@code true} if the type in the descriptor is found in the {@link #workspace}.
	 */
	protected boolean isMissingType(@Nonnull String descriptor) {
		Type type = Type.getType(descriptor);
		return isMissingType(type);
	}

	/**
	 * @param type
	 * 		Some non-method type.
	 *
	 * @return {@code true} if the type in the descriptor is found in the {@link #workspace}.
	 */
	protected boolean isMissingType(@Nonnull Type type) {
		if (type.getSort() == Type.OBJECT && workspace.findClass(type.getInternalName()) == null)
			return true;
		else if (type.getSort() == Type.ARRAY) {
			Type elementType = type.getElementType();
			if (elementType.getSort() == Type.OBJECT)
				return workspace.findClass(elementType.getInternalName()) == null;
			else
				return false;
		} else return false;
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
