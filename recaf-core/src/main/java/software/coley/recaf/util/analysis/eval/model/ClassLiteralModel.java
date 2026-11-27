package software.coley.recaf.util.analysis.eval.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.eval.EvaluationContext;
import software.coley.recaf.util.analysis.eval.Evaluator;
import software.coley.recaf.util.analysis.eval.InstancedObjectValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.util.analysis.value.impl.ClassLiteralValueImpl;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

/**
 * Models {@link Class} operations.
 *
 * @author Matt Coley
 */
public final class ClassLiteralModel implements EvaluatorModel {
	private static final String CLASS = "java/lang/Class";
	private static final String STRING_DESC = "()Ljava/lang/String;";
	private static final String RESOURCE_STREAM = "getResourceAsStream";
	private static final String RESOURCE_STREAM_DESC = "(Ljava/lang/String;)Ljava/io/InputStream;";
	private static final Type INPUT_STREAM_TYPE = Type.getObjectType("java/io/InputStream");
	private static final int MAX_RESOURCE_BYTES = 16 * 1024 * 1024;
	private static final List<String> JDK_PREFIXES = List.of("java/", "javax/", "jdk/", "sun/", "com/sun/");
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
		return false;
	}

	@Override
	public boolean supportsInstance(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver) {
		// TODO: Remaining Class methods that can be realistically supported
		//  - Also clean this up to use the switch like other models do.
		//  - Be consistent with usages of constants for method names and descriptors.
		if (!instruction.owner.equals(CLASS))
			return false;
		if (instruction.desc.equals(STRING_DESC))
			return instruction.name.equals("getName") || instruction.name.equals("getSimpleName");
		return instruction.desc.equals(RESOURCE_STREAM_DESC) && instruction.name.equals(RESOURCE_STREAM);
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
		return ModelResult.NOT_HANDLED;
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
		if (instruction.name.equals(RESOURCE_STREAM))
			return resourceAsStream(type, arguments);
		if (instruction.name.equals("getName"))
			return ModelResult.yielded(ObjectValue.string(binaryName(type)));

		String simpleName = simpleName(type, context);
		if (simpleName == null)
			return ModelResult.failed("Simple name is not known to the evaluator for " + type.getDescriptor());

		return ModelResult.yielded(ObjectValue.string(simpleName));
	}

	/**
	 * Answers {@code Class.getResourceAsStream(name)} from the workspace's non-internal resources, in the order
	 * the class loader searches them. Only answers the evaluator can determine exactly are returned.
	 *
	 * @param type
	 * 		Type of the class literal.
	 * @param arguments
	 * 		Call arguments. The resource name.
	 *
	 * @return Stream of the first matching resource, or {@code null} when no resource matches and the JVM cannot
	 * resolve the name from the JDK. Otherwise the lookup fails.
	 */
	@Nonnull
	private ModelResult resourceAsStream(@Nonnull Type type, @Nonnull List<ReValue> arguments) {
		// Array classes search the package of their element type, which is not modeled.
		if (type.getSort() != Type.OBJECT)
			return ModelResult.failed("Resource lookup on an array class literal is not modeled: " + type.getDescriptor());

		// A JDK class searches its module, not the classpath.
		String owner = type.getInternalName();
		if (isJdkOwned(owner))
			return ModelResult.failed("Resource lookup on a JDK class is not modeled: " + owner);

		Optional<String> nameText = arguments.get(0) instanceof StringValue nameValue ? nameValue.getText() : Optional.empty();
		if (nameText.isEmpty())
			return ModelResult.failed("Resource name is not known to the evaluator");
		String name = nameText.get();

		String key = resourceKey(owner, name);
		if (key == null)
			return ModelResult.failed("Resource name is not a canonical file name: " + name);

		for (WorkspaceResource resource : evaluator.getWorkspace().getAllResources(false)) {
			FileInfo file = resource.getFileBundle().get(key);
			if (file == null)
				continue;
			byte[] content = file.getRawContent();
			if (content == null || content.length > MAX_RESOURCE_BYTES)
				return ModelResult.failed("Resource cannot be copied by the evaluator: " + key);

			// The stream holds its own copy, so later changes to the workspace do not reach it.
			return ModelResult.yielded(new InstancedObjectValue<>(new ByteArrayInputStream(content.clone())));
		}

		// The JVM may resolve a JDK name from the JDK instead of finding nothing here.
		if (isJdkOwned(key))
			return ModelResult.failed("Resource may resolve from the JDK, which is not modeled: " + key);
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
	 * @param name
	 * 		Internal class name or resource name.
	 *
	 * @return {@code true} when the JDK provides the name, or the name is under a package the JDK provides.
	 */
	private static boolean isJdkOwned(@Nonnull String name) {
		for (String prefix : JDK_PREFIXES) {
			if (name.startsWith(prefix))
				return true;
		}
		return false;
	}

	/**
	 * @param type
	 * 		Type of the class literal. Object or array.
	 *
	 * @return Binary name of the type, with dots. Arrays use their descriptor.
	 */
	@Nonnull
	private static String binaryName(@Nonnull Type type) {
		String name = type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName();
		return name.replace('/', '.');
	}

	/**
	 * @param type
	 * 		Type of the class literal.
	 * @param context
	 * 		Evaluation context, for class overrides.
	 *
	 * @return Simple name of the type, following {@link Class#getSimpleName()}.
	 * {@code null} when the evaluator cannot see the class, or its {@code InnerClasses} data is inconsistent.
	 */
	@Nullable
	private String simpleName(@Nonnull Type type, @Nonnull EvaluationContext context) {
		if (type.getSort() == Type.ARRAY) {
			String element = simpleName(type.getElementType(), context);
			return element == null ? null : element + "[]".repeat(type.getDimensions());
		}
		if (type.getSort() != Type.OBJECT)
			return type.getClassName(); // Primitive element type, such as 'int' in 'int[]'.

		String internalName = type.getInternalName();
		ClassNode node = evaluator.getNode(internalName, context);
		if (node == null)
			return null;

		InnerClassNode self = null;
		for (InnerClassNode inner : node.innerClasses) {
			if (inner.name.equals(internalName)) {
				self = inner;
				break;
			}
		}

		// Top-level classes are neither members nor enclosed by a method.
		// Their name is the text after the package.
		if (node.outerClass == null && (self == null || self.outerName == null))
			return internalName.substring(internalName.lastIndexOf('/') + 1);

		// Enclosed classes are named by their own entry.
		// Anonymous classes have no inner name.
		if (self == null)
			return null;
		return self.innerName == null ? "" : self.innerName;
	}
}
