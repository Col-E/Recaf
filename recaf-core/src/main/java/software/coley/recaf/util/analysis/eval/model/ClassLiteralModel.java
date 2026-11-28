package software.coley.recaf.util.analysis.eval.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.eval.EvaluationContext;
import software.coley.recaf.util.analysis.eval.EvaluationResult;
import software.coley.recaf.util.analysis.eval.Evaluator;
import software.coley.recaf.util.analysis.eval.InstancedObjectValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.util.analysis.value.impl.ArrayValueImpl;
import software.coley.recaf.util.analysis.value.impl.ClassLiteralValueImpl;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Models {@link Class} operations.
 *
 * @author Matt Coley
 */
public final class ClassLiteralModel implements EvaluatorModel {
	private static final String CLASS = "java/lang/Class";
	private static final String OBJECT = "java/lang/Object";
	private static final String ENUM = "java/lang/Enum";
	private static final String RECORD = "java/lang/Record";
	private static final String CLONEABLE = "java/lang/Cloneable";
	private static final String SERIALIZABLE = "java/io/Serializable";

	private static final String STRING_DESC = "()Ljava/lang/String;";
	private static final String BOOLEAN_DESC = "()Z";
	private static final String INT_DESC = "()I";
	private static final String CLASS_DESC = "()Ljava/lang/Class;";
	private static final String CLASS_ARRAY_DESC = "()[Ljava/lang/Class;";
	private static final String EQUALS_DESC = "(Ljava/lang/Object;)Z";
	private static final String IS_ASSIGNABLE_FROM_DESC = "(Ljava/lang/Class;)Z";
	private static final String RESOURCE_STREAM_DESC = "(Ljava/lang/String;)Ljava/io/InputStream;";
	private static final String FOR_NAME_DESC = "(Ljava/lang/String;)Ljava/lang/Class;";

	private static final String GET_NAME = "getName";
	private static final String GET_SIMPLE_NAME = "getSimpleName";
	private static final String GET_TYPE_NAME = "getTypeName";
	private static final String GET_PACKAGE_NAME = "getPackageName";
	private static final String GET_CANONICAL_NAME = "getCanonicalName";
	private static final String TO_STRING = "toString";
	private static final String EQUALS = "equals";
	private static final String IS_ARRAY = "isArray";
	private static final String IS_PRIMITIVE = "isPrimitive";
	private static final String IS_INTERFACE = "isInterface";
	private static final String IS_ENUM = "isEnum";
	private static final String IS_ANNOTATION = "isAnnotation";
	private static final String IS_RECORD = "isRecord";
	private static final String IS_SEALED = "isSealed";
	private static final String IS_SYNTHETIC = "isSynthetic";
	private static final String IS_MEMBER_CLASS = "isMemberClass";
	private static final String IS_LOCAL_CLASS = "isLocalClass";
	private static final String IS_ANONYMOUS_CLASS = "isAnonymousClass";
	private static final String IS_ASSIGNABLE_FROM = "isAssignableFrom";
	private static final String GET_MODIFIERS = "getModifiers";
	private static final String GET_SUPERCLASS = "getSuperclass";
	private static final String GET_INTERFACES = "getInterfaces";
	private static final String GET_ENCLOSING_CLASS = "getEnclosingClass";
	private static final String GET_DECLARING_CLASS = "getDeclaringClass";
	private static final String GET_NEST_HOST = "getNestHost";
	private static final String GET_COMPONENT_TYPE = "getComponentType";
	private static final String COMPONENT_TYPE = "componentType";
	private static final String ARRAY_TYPE = "arrayType";
	private static final String RESOURCE_STREAM = "getResourceAsStream";
	private static final String FOR_NAME = "forName";

	private static final Set<String> ARRAY_SUPERTYPES = Set.of(OBJECT, CLONEABLE, SERIALIZABLE);
	private static final Type CLASS_ARRAY_TYPE = Type.getType("[Ljava/lang/Class;");
	private static final Type INPUT_STREAM_TYPE = Type.getObjectType("java/io/InputStream");
	private static final int MAX_RESOURCE_BYTES = 16 * 1024 * 1024;

	private static final int MODIFIER_MASK = 0x7FFF & ~Opcodes.ACC_SUPER;
	private static final int ACCESS_MODIFIERS = Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED;


	private final Map<String, ClassNode> runtimeNodes = new HashMap<>();
	private final Evaluator evaluator;

	ClassLiteralModel(@Nonnull Evaluator evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public boolean supportsAllocation(@Nonnull String type) {
		return false;
	}

	@Override
	public boolean supportsConstructor(@Nonnull MethodInsnNode instruction) {
		return false;
	}

	@Override
	public boolean supportsStatic(@Nonnull MethodInsnNode instruction) {
		return instruction.owner.equals(CLASS)
				&& instruction.name.equals(FOR_NAME)
				&& instruction.desc.equals(FOR_NAME_DESC);
	}

	@Override
	public boolean supportsInstance(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver) {
		if (!instruction.owner.equals(CLASS))
			return false;
		return switch (instruction.name) {
			case GET_NAME,
			     GET_SIMPLE_NAME,
			     GET_TYPE_NAME,
			     GET_PACKAGE_NAME,
			     GET_CANONICAL_NAME,
			     TO_STRING ->
					instruction.desc.equals(STRING_DESC);
			case IS_ARRAY,
			     IS_PRIMITIVE,
			     IS_INTERFACE,
			     IS_ENUM,
			     IS_ANNOTATION,
			     IS_RECORD,
			     IS_SEALED,
			     IS_SYNTHETIC,
			     IS_MEMBER_CLASS,
			     IS_LOCAL_CLASS,
			     IS_ANONYMOUS_CLASS ->
					instruction.desc.equals(BOOLEAN_DESC);
			case GET_MODIFIERS -> instruction.desc.equals(INT_DESC);
			case GET_SUPERCLASS,
			     GET_ENCLOSING_CLASS,
			     GET_DECLARING_CLASS,
			     GET_NEST_HOST,
			     GET_COMPONENT_TYPE,
			     COMPONENT_TYPE,
			     ARRAY_TYPE -> instruction.desc.equals(CLASS_DESC);
			case GET_INTERFACES -> instruction.desc.equals(CLASS_ARRAY_DESC);
			case EQUALS -> instruction.desc.equals(EQUALS_DESC);
			case IS_ASSIGNABLE_FROM -> instruction.desc.equals(IS_ASSIGNABLE_FROM_DESC);
			case RESOURCE_STREAM -> instruction.desc.equals(RESOURCE_STREAM_DESC);
			default -> false;
		};
	}

	@Override
	public ObjectValue allocate(@Nonnull String type, @Nonnull EvaluationContext context) {
		return null;
	}

	@Nonnull
	@Override
	public ModelResult invokeConstructor(@Nonnull MethodInsnNode instruction,
	                                     @Nonnull ReValue receiver,
	                                     @Nonnull List<ReValue> arguments,
	                                     @Nonnull EvaluationContext context) {
		return ModelResult.NOT_HANDLED;
	}

	@Nonnull
	@Override
	public ModelResult invokeStatic(@Nonnull MethodInsnNode instruction,
	                                @Nonnull List<ReValue> arguments,
	                                @Nonnull EvaluationContext context) {
		try {
			return switch (instruction.name) {
				case FOR_NAME -> forName(arguments, context);
				default -> ModelResult.NOT_HANDLED;
			};
		} catch (UnresolvedClassException ex) {
			return ModelResult.failed(ex.getMessage());
		}
	}

	@Nonnull
	@Override
	public ModelResult invokeInstance(@Nonnull MethodInsnNode instruction,
	                                  @Nonnull ReValue receiver,
	                                  @Nonnull List<ReValue> arguments,
	                                  @Nonnull EvaluationContext context) {
		if (!(receiver instanceof ClassLiteralValueImpl literal))
			return ModelResult.failed("Class value has no known name, cannot evaluate " + instruction.name);

		Type type = literal.literal();
		try {
			return switch (instruction.name) {
				case GET_NAME -> ModelResult.yielded(ObjectValue.string(Types.getBinaryName(type)));
				case GET_SIMPLE_NAME -> ModelResult.yielded(ObjectValue.string(simpleName(type, context)));
				case GET_TYPE_NAME -> ModelResult.yielded(ObjectValue.string(Types.getTypeName(type)));
				case GET_PACKAGE_NAME -> ModelResult.yielded(ObjectValue.string(Types.getPackageName(type)));
				case GET_CANONICAL_NAME -> ModelResult.yielded(ObjectValue.string(canonicalName(type, context)));
				case TO_STRING -> ModelResult.yielded(ObjectValue.string(classString(type, context)));
				case IS_ARRAY -> bool(type.getSort() == Type.ARRAY);
				case IS_PRIMITIVE -> bool(!isReference(type));
				case IS_INTERFACE -> bool(isInterface(type, context));
				case IS_ENUM -> bool(isEnum(type, context));
				case IS_ANNOTATION -> bool(hasModifier(type, Opcodes.ACC_ANNOTATION, context));
				case IS_RECORD -> bool(isRecord(type, context));
				case IS_SEALED -> bool(isSealed(type, context));
				case IS_SYNTHETIC -> bool(hasModifier(type, Opcodes.ACC_SYNTHETIC, context));
				case IS_MEMBER_CLASS -> bool(isInnerClass(type, context));
				case IS_LOCAL_CLASS -> bool(isMethodInnerClass(type, context));
				case IS_ANONYMOUS_CLASS -> bool(isAnonymousClass(type, context));
				case GET_MODIFIERS -> ModelResult.yielded(IntValue.of(modifiers(type, context)));
				case GET_SUPERCLASS -> ModelResult.yielded(classValue(superclass(type, context)));
				case GET_INTERFACES -> ModelResult.yielded(interfaces(type, context));
				case GET_ENCLOSING_CLASS -> ModelResult.yielded(classValue(enclosingClass(type, context)));
				case GET_DECLARING_CLASS -> ModelResult.yielded(classValue(declaringClass(type, context)));
				case GET_NEST_HOST -> ModelResult.yielded(classValue(nestHost(type, context)));
				case GET_COMPONENT_TYPE, COMPONENT_TYPE -> ModelResult.yielded(classValue(Types.getComponentType(type)));
				case ARRAY_TYPE -> ModelResult.yielded(classValue(Type.getType("[" + type.getDescriptor())));
				case EQUALS -> bool(equalsClass(type, arguments.getFirst()));
				case IS_ASSIGNABLE_FROM -> bool(isAssignable(type, classArgument(arguments.getFirst()), context));
				case RESOURCE_STREAM -> resourceAsStream(type, arguments);
				default -> ModelResult.NOT_HANDLED;
			};
		} catch (UnresolvedClassException ex) {
			return ModelResult.failed(ex.getMessage());
		}
	}

	/**
	 * Resolves {@code Class.forName(name)} from the workspace's known classes, or the runtime JDK classes.
	 * The JVM initializes the class as part of the lookup, which the evaluator does when class initializers are enabled.
	 *
	 * @param arguments
	 * 		Call arguments. The class name.
	 * @param context
	 * 		Current evaluation context.
	 *
	 * @return Class literal of the named class, or a failure when the name is not known or not resolvable.
	 */
	@Nonnull
	private ModelResult forName(@Nonnull List<ReValue> arguments, @Nonnull EvaluationContext context) {
		Optional<String> nameText = arguments.getFirst() instanceof StringValue nameValue ? nameValue.getText() : Optional.empty();
		if (nameText.isEmpty())
			return ModelResult.failed("Class name is not known to the evaluator");

		String name = nameText.get();
		Type type = forNameType(name);
		if (type == null)
			return ModelResult.failed("Class name is not a valid binary name: " + name);

		Type element = type.getSort() == Type.ARRAY ? type.getElementType() : type;
		if (isObject(element))
			requireNode(element, context);

		if (isObject(type)) {
			EvaluationResult initialization = evaluator.initializeClassIfNeeded(type.getInternalName(), context);
			if (initialization != null)
				return new ModelResult(initialization);
		}
		return ModelResult.yielded(ObjectValue.classLiteral(type));
	}

	/**
	 * Resolves {@code Class.getResourceAsStream(name)} from the workspace's known resources, or the runtime JDK classes.
	 *
	 * @param type
	 * 		Type of the class literal.
	 * @param arguments
	 * 		Call arguments. The resource name.
	 *
	 * @return Stream of the first matching resource, or {@code null} when no resource matches and the JVM cannot
	 * resolve the name from the JDK. Otherwise, the lookup fails.
	 */
	@Nonnull
	private ModelResult resourceAsStream(@Nonnull Type type, @Nonnull List<ReValue> arguments) {
		// Arrays and primitives resolve names through the innermost element type's module and package.
		if (type.getSort() != Type.OBJECT) {
			Type element = type.getElementType();
			return resourceAsStream(element, arguments);
		}

		// A JDK class searches its module, not the classpath/workspace supplied files.
		String owner = type.getInternalName();
		if (RuntimeWorkspaceResource.getInstance().getJvmClassBundle().get(owner) != null)
			return ModelResult.yielded(ObjectValue.object(INPUT_STREAM_TYPE, Nullness.NULL));

		Optional<String> nameText = arguments.getFirst() instanceof StringValue nameValue ? nameValue.getText() : Optional.empty();
		if (nameText.isEmpty())
			return ModelResult.failed("Cannot evaluate Class.getResourceAsStream: the resource name is not a known string constant");

		String name = nameText.get();
		String key = resourceKey(owner, name);
		if (key == null)
			return ModelResult.failed("Cannot evaluate Class.getResourceAsStream: the resource name has an empty, '.', '..', or backslash segment: " + name);

		// Check all workspace resources for a matching resource.
		for (WorkspaceResource resource : evaluator.getWorkspace().getAllResources(false)) {
			FileInfo file = resource.getFileBundle().get(key);
			if (file == null)
				continue;

			byte[] content = file.getRawContent();
			if (content.length > MAX_RESOURCE_BYTES)
				return ModelResult.failed("Cannot copy resource " + key + ": it is larger than the evaluator's limit of " + MAX_RESOURCE_BYTES + " bytes");

			return ModelResult.yielded(new InstancedObjectValue<>(new ByteArrayInputStream(content)));
		}

		return ModelResult.yielded(ObjectValue.object(INPUT_STREAM_TYPE, Nullness.NULL));
	}

	/**
	 * @param owner
	 * 		Internal name of the class the lookup is relative to.
	 * @param name
	 * 		Resource name passed to the lookup.
	 *
	 * @return Name the class loader searches for, following {@link Class#getResourceAsStream(String)}.
	 * {@code null} when the name is not canonical, since the class loader does not normalize such names.
	 */
	@Nullable
	private static String resourceKey(@Nonnull String owner, @Nonnull String name) {
		String key;
		if (name.startsWith("/")) {
			key = name.substring(1);
		} else {
			int slash = owner.lastIndexOf('/');
			key = slash < 0 ? name : owner.substring(0, slash + 1) + name;
		}
		for (String segment : key.split("/", -1)) {
			if (segment.isEmpty() || segment.equals(".") || segment.equals("..") || segment.indexOf('\\') >= 0)
				return null;
		}
		return key;
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return Simple name of the type, following {@link Class#getSimpleName()}.
	 *
	 * @throws UnresolvedClassException
	 * 		When the class, or its {@code InnerClasses} data, is not known to the evaluator.
	 */
	@Nonnull
	private String simpleName(@Nonnull Type type, @Nonnull EvaluationContext context) {
		if (type.getSort() == Type.ARRAY)
			return simpleName(type.getElementType(), context) + "[]".repeat(type.getDimensions());
		if (!isObject(type))
			return type.getClassName(); // Primitive, such as 'int'.

		String internalName = type.getInternalName();
		ClassNode node = requireNode(type, context);
		InnerClassNode self = selfEntry(node);

		// Last segment after last package separator.
		if (node.outerClass == null && (self == null || self.outerName == null))
			return internalName.substring(internalName.lastIndexOf('/') + 1);

		// Enclosed classes are named by their own entry.
		// Anonymous classes have no inner name.
		if (self == null)
			throw new UnresolvedClassException("Enclosed class has no InnerClasses entry: " + internalName);

		return self.innerName == null ? "" : self.innerName;
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return Canonical name of the type, following {@link Class#getCanonicalName()}.
	 * {@code null} for local and anonymous classes, and for members of those.
	 */
	@Nullable
	private String canonicalName(@Nonnull Type type, @Nonnull EvaluationContext context) {
		if (type.getSort() == Type.ARRAY) {
			String element = canonicalName(type.getElementType(), context);
			return element == null ? null : element + "[]".repeat(type.getDimensions());
		}
		if (!isObject(type))
			return type.getClassName();

		ClassNode node = requireNode(type, context);
		if (node.outerClass != null)
			return null;

		// Top-level classes are named by their binary name.
		String declaring = declaringClassName(node);
		if (declaring == null)
			return Types.getBinaryName(type);

		// Member classes are named by their enclosing class, followed by their simple name.
		String enclosingName = canonicalName(Type.getObjectType(declaring), context);
		return enclosingName == null ? null : enclosingName + "." + simpleName(type, context);
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return String form of the class, following {@link Class#toString()}.
	 */
	@Nonnull
	private String classString(@Nonnull Type type, @Nonnull EvaluationContext context) {
		String prefix = isInterface(type, context) ? "interface " : isReference(type) ? "class " : "";
		return prefix + Types.getBinaryName(type);
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return Modifiers of the class, following {@link Class#getModifiers()}.
	 */
	private int modifiers(@Nonnull Type type, @Nonnull EvaluationContext context) {
		// Primitive classes are public, abstract, and final.
		if (!isReference(type))
			return Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_FINAL;

		// Array classes have the access of their innermost element, and are always abstract and final.
		if (type.getSort() == Type.ARRAY)
			return (modifiers(type.getElementType(), context) & ACCESS_MODIFIERS) | Opcodes.ACC_ABSTRACT | Opcodes.ACC_FINAL;

		// Classes with an InnerClasses entry for themselves report the flags of that entry, as the JVM does.
		ClassNode node = requireNode(type, context);
		InnerClassNode self = selfEntry(node);
		int access = self != null ? self.access : node.access;
		return access & MODIFIER_MASK;
	}

	private boolean hasModifier(@Nonnull Type type, int flag, @Nonnull EvaluationContext context) {
		return isObject(type) && (modifiers(type, context) & flag) != 0;
	}

	private boolean isInterface(@Nonnull Type type, @Nonnull EvaluationContext context) {
		return isObject(type) && (requireNode(type, context).access & Opcodes.ACC_INTERFACE) != 0;
	}

	private boolean isEnum(@Nonnull Type type, @Nonnull EvaluationContext context) {
		return hasModifier(type, Opcodes.ACC_ENUM, context)
				&& ENUM.equals(requireNode(type, context).superName);
	}

	private boolean isRecord(@Nonnull Type type, @Nonnull EvaluationContext context) {
		if (!isObject(type))
			return false;

		ClassNode node = requireNode(type, context);
		return RECORD.equals(node.superName);
	}

	private boolean isSealed(@Nonnull Type type, @Nonnull EvaluationContext context) {
		return isObject(type) && requireNode(type, context).permittedSubclasses != null;
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return {@code true} when the type is an inner class.
	 */
	private boolean isInnerClass(@Nonnull Type type, @Nonnull EvaluationContext context) {
		return isObject(type) && declaringClassName(requireNode(type, context)) != null;
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return {@code true} when the type is an anonymous class.
	 */
	private boolean isMethodInnerClass(@Nonnull Type type, @Nonnull EvaluationContext context) {
		if (!isObject(type))
			return false;

		ClassNode node = requireNode(type, context);
		InnerClassNode self = selfEntry(node);
		return node.outerClass != null && self != null && self.innerName != null;
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return {@code true} when the type is an anonymous class.
	 */
	private boolean isAnonymousClass(@Nonnull Type type, @Nonnull EvaluationContext context) {
		if (!isObject(type))
			return false;

		ClassNode node = requireNode(type, context);
		InnerClassNode self = selfEntry(node);
		return node.outerClass != null && (self == null || self.innerName == null);
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return Superclass of the type, or {@code null} for interfaces, {@code Object}, and primitives.
	 */
	@Nullable
	private Type superclass(@Nonnull Type type, @Nonnull EvaluationContext context) {
		if (type.getSort() == Type.ARRAY)
			return Type.getObjectType(OBJECT);
		if (!isObject(type))
			return null;

		ClassNode node = requireNode(type, context);
		if ((node.access & Opcodes.ACC_INTERFACE) != 0 || node.superName == null)
			return null;
		return Type.getObjectType(node.superName);
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return Array of the directly implemented interfaces. Arrays implement {@code Cloneable} and {@code Serializable}.
	 */
	@Nonnull
	private ReValue interfaces(@Nonnull Type type, @Nonnull EvaluationContext context) {
		List<Type> interfaces = switch (type.getSort()) {
			case Type.ARRAY -> List.of(Type.getObjectType(CLONEABLE), Type.getObjectType(SERIALIZABLE));
			case Type.OBJECT -> requireNode(type, context).interfaces.stream().map(Type::getObjectType).toList();
			default -> List.of(); // Primitives implement no interfaces.
		};
		return new ArrayValueImpl(CLASS_ARRAY_TYPE, Nullness.NOT_NULL, interfaces.size(),
				i -> classValue(interfaces.get(i)));
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return Class that immediately encloses the type, or {@code null} for top-level classes.
	 */
	@Nullable
	private Type enclosingClass(@Nonnull Type type, @Nonnull EvaluationContext context) {
		if (!isObject(type))
			return null;

		ClassNode node = requireNode(type, context);
		String enclosing = node.outerClass != null ? node.outerClass : declaringClassName(node);
		return enclosing == null ? null : Type.getObjectType(enclosing);
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return Class that declares the type as a member, or {@code null} for all other classes.
	 */
	@Nullable
	private Type declaringClass(@Nonnull Type type, @Nonnull EvaluationContext context) {
		if (!isObject(type))
			return null;

		String declaring = declaringClassName(requireNode(type, context));
		return declaring == null ? null : Type.getObjectType(declaring);
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return Nest host of the type. Types without a {@code NestHost} attribute, arrays, and primitives are their own host.
	 */
	@Nonnull
	private Type nestHost(@Nonnull Type type, @Nonnull EvaluationContext context) {
		if (!isObject(type))
			return type;

		String host = requireNode(type, context).nestHostClass;
		return host == null ? type : Type.getObjectType(host);
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param other
	 * 		Value compared with the class literal.
	 *
	 * @return {@code true} when the other value is the same class.
	 *
	 * @throws UnresolvedClassException
	 * 		When the other value is neither null nor a known class literal.
	 */
	private static boolean equalsClass(@Nonnull Type type, @Nonnull ReValue other) {
		if (other instanceof ObjectValue object && object.isNull())
			return false;
		return type.equals(classArgument(other));
	}

	/**
	 * @param type
	 * 		Type of the class literal this class is assignable to, which is the receiver of {@code isAssignableFrom}.
	 * @param from
	 * 		Type of the class literal that is checked for assignability.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return {@code true} when a value of type {@code from} can be assigned to {@code type}.
	 */
	private boolean isAssignable(@Nonnull Type type, @Nonnull Type from, @Nonnull EvaluationContext context) {
		if (type.equals(from))
			return true;

		// Primitives are only assignable to themselves.
		if (!isReference(type) || !isReference(from))
			return false;

		// Every reference type is assignable to Object.
		if (isObject(type) && OBJECT.equals(type.getInternalName()))
			return true;

		if (from.getSort() == Type.ARRAY) {
			// Arrays are only assignable to the array supertypes, unless the target is also an array.
			if (isObject(type))
				return ARRAY_SUPERTYPES.contains(type.getInternalName());

			int typeDimensions = type.getDimensions();
			int fromDimensions = from.getDimensions();
			if (typeDimensions > fromDimensions)
				return false;
			Type typeElement = type.getElementType();
			Type fromElement = from.getElementType();

			// The source has more dimensions, so its component is an array. That component is only assignable to
			// the array supertypes.
			if (typeDimensions < fromDimensions)
				return isObject(typeElement) && ARRAY_SUPERTYPES.contains(typeElement.getInternalName());

			// With matching dimensions, the element types must be assignable. Primitive elements only match themselves.
			return isObject(typeElement) && isObject(fromElement) && isAssignable(typeElement, fromElement, context);
		}

		// A class type is never assignable from an array, other than the array supertypes which were handled above.
		if (type.getSort() == Type.ARRAY)
			return false;

		requireNode(type, context);
		requireNode(from, context);
		return evaluator.isAssignableFrom(type.getInternalName(), from.getInternalName());
	}

	/**
	 * @param value
	 * 		Argument expected to be a class literal.
	 *
	 * @return Type of the class literal.
	 *
	 * @throws UnresolvedClassException
	 * 		When the argument is not a known class literal.
	 */
	@Nonnull
	private static Type classArgument(@Nonnull ReValue value) {
		if (value instanceof ClassLiteralValueImpl literal)
			return literal.literal();
		throw new UnresolvedClassException("Class argument is not a known class literal");
	}

	/**
	 * @param name
	 * 		Binary class name, such as {@code a.b.C}, or an array name such as {@code [I} or {@code [La.b.C;}.
	 *
	 * @return Type named by the given name. {@code null} when the name is not a valid binary name.
	 */
	@Nullable
	private static Type forNameType(@Nonnull String name) {
		// Binary names use dots. Slashes are never valid, and the JVM rejects them.
		if (name.isEmpty() || name.indexOf('/') >= 0)
			return null;
		String internalName = name.replace('.', '/');
		if (internalName.charAt(0) != '[')
			return Type.getObjectType(internalName);

		// Array names are descriptors, such as "[I" or "[Ljava/lang/String;".
		int dimensions = 0;
		while (dimensions < internalName.length() && internalName.charAt(dimensions) == '[')
			dimensions++;
		if (dimensions > 255)
			return null;
		String element = internalName.substring(dimensions);
		if (element.length() == 1 && "ZBCSIJFD".indexOf(element.charAt(0)) >= 0)
			return Type.getType(internalName);
		if (element.length() > 2 && element.charAt(0) == 'L' && element.endsWith(";")) {
			String elementName = element.substring(1, element.length() - 1);
			if (elementName.indexOf(';') < 0 && elementName.indexOf('[') < 0)
				return Type.getType(internalName);
		}
		return null;
	}

	/**
	 * @param node
	 * 		Class node to read.
	 *
	 * @return Name of the class declaring the node as a member, or {@code null} for all other classes.
	 */
	@Nullable
	private static String declaringClassName(@Nonnull ClassNode node) {
		// Local and anonymous classes have an enclosing method, and no declaring class.
		if (node.outerClass != null)
			return null;
		InnerClassNode self = selfEntry(node);
		return self == null ? null : self.outerName;
	}

	/**
	 * @param node
	 * 		Class node to search.
	 *
	 * @return The {@code InnerClasses} entry describing the class itself, or {@code null} if there is none.
	 */
	@Nullable
	private static InnerClassNode selfEntry(@Nonnull ClassNode node) {
		for (InnerClassNode inner : node.innerClasses) {
			if (inner.name.equals(node.name))
				return inner;
		}
		return null;
	}

	/**
	 * @param name
	 * 		Internal class name.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return Class node of the workspace class, honoring the evaluation's overrides. Otherwise the runtime JDK class.
	 * {@code null} when neither provides the class.
	 */
	@Nullable
	private ClassNode node(@Nonnull String name, @Nonnull EvaluationContext context) {
		ClassNode node = evaluator.getNode(name, context);
		return node != null ? node : runtimeNodes.computeIfAbsent(name, this::runtimeNode);
	}

	/**
	 * @param type
	 * 		Object type of a class.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return Class node of the type.
	 *
	 * @throws UnresolvedClassException
	 * 		When the class is not known to the evaluator.
	 */
	@Nonnull
	private ClassNode requireNode(@Nonnull Type type, @Nonnull EvaluationContext context) {
		ClassNode node = node(type.getInternalName(), context);
		if (node == null)
			throw new UnresolvedClassException("Class is not known to the evaluator: " + type.getInternalName());
		return node;
	}

	/**
	 * @param name
	 * 		Internal class name of a runtime JDK class.
	 *
	 * @return Class node parsed from the runtime JDK, or {@code null} when the runtime does not provide a JVM class.
	 */
	@Nullable
	private ClassNode runtimeNode(@Nonnull String name) {
		ClassPathNode path = evaluator.getWorkspace().findClass(true, name);
		if (path == null || !path.getValue().isJvmClass())
			return null;
		ClassNode node = new ClassNode();
		path.getValue().asJvmClass().getClassReader()
				.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		return node;
	}

	private static boolean isObject(@Nonnull Type type) {
		return type.getSort() == Type.OBJECT;
	}

	private static boolean isReference(@Nonnull Type type) {
		return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
	}

	@Nonnull
	private static ObjectValue classValue(@Nullable Type type) {
		return type == null ? ObjectValue.clazz(Nullness.NULL) : ObjectValue.classLiteral(type);
	}

	@Nonnull
	private static ModelResult bool(boolean value) {
		return ModelResult.yielded(IntValue.of(value ? 1 : 0));
	}

	/**
	 * Thrown when a class literal is not known to the evaluator, or a class literal argument is not known.
	 */
	private static final class UnresolvedClassException extends RuntimeException {
		private UnresolvedClassException(@Nonnull String message) {
			super(message, null, false, false);
		}
	}
}
