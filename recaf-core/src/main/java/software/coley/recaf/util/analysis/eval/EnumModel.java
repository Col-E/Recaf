package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Models {@link java.lang.Enum} constructor and instance methods:
 * <ul>
 *     <li>{@code <init>(String name, int ordinal)}</li>
 *     <li>{@code name()}</li>
 *     <li>{@code ordinal()}</li>
 * </ul>
 *
 * @author Matt Coley
 */
final class EnumModel implements EvaluatorModel {
	private static final String ENUM = "java/lang/Enum";
	private final Evaluator evaluator;
	private final Map<ReValue, State> states = new IdentityHashMap<>();

	EnumModel(Evaluator evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public boolean supportsAllocation(@Nonnull String type) {
		return false;
	}

	@Override
	public boolean supportsConstructor(@Nonnull MethodInsnNode instruction) {
		return instruction.owner.equals(ENUM)
				&& instruction.name.equals("<init>")
				&& instruction.desc.equals("(Ljava/lang/String;I)V");
	}

	@Override
	public boolean supportsStatic(@Nonnull MethodInsnNode instruction) {
		return false;
	}

	@Override
	public boolean supportsInstance(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver) {
		return (instruction.name.equals("name") && instruction.desc.equals("()Ljava/lang/String;")
				|| instruction.name.equals("ordinal") && instruction.desc.equals("()I"))
				&& evaluator.isAssignableFrom(ENUM, instruction.owner);
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
		if (receiver.type() == null || !evaluator.isAssignableFrom(ENUM, receiver.type().getInternalName()))
			return ModelResult.NOT_HANDLED;
		states.put(receiver, new State(arguments.get(0), arguments.get(1)));
		return ModelResult.yielded(UninitializedValue.UNINITIALIZED_VALUE);
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
		State state = states.get(receiver);
		if (state == null)
			return ModelResult.NOT_HANDLED;
		return ModelResult.yielded(instruction.name.equals("name") ? state.name : state.ordinal);
	}

	private record State(@Nonnull ReValue name, @Nonnull ReValue ordinal) {}
}
