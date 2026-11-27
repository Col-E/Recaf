package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.impl.ClassLiteralValueImpl;

import java.util.List;

/**
 * Models {@link Class} operations.
 *
 * @author Matt Coley
 */
final class ClassLiteralModel implements EvaluatorModel {
	private static final String CLASS = "java/lang/Class";
	private static final String STRING_DESC = "()Ljava/lang/String;";
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
		return instruction.owner.equals(CLASS)
				&& instruction.desc.equals(STRING_DESC)
				&& (instruction.name.equals("getName") || instruction.name.equals("getSimpleName"));
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
		if (instruction.name.equals("getName"))
			return ModelResult.yielded(ObjectValue.string(binaryName(type)));

		String simpleName = simpleName(type, context);
		if (simpleName == null)
			return ModelResult.failed("Simple name is not known to the evaluator for " + type.getDescriptor());

		return ModelResult.yielded(ObjectValue.string(simpleName));
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
