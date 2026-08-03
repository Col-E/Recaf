package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.ReFrame;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.ThrowableValue;
import software.coley.recaf.util.analysis.value.impl.ArrayValueImpl;
import software.coley.recaf.util.analysis.value.impl.ThrowableValueImpl;

import java.util.List;
import java.util.function.Supplier;

/**
 * Handles modeled throwable creation, implicit fault prediction, and exception routing for an executing frame.
 *
 * @author Matt Coley
 * @see Evaluator
 */
public class ExceptionHandler {
	private final ReInterpreter interpreter;
	@Nullable
	private final MethodNode method;
	private final Supplier<List<StackTraceElement>> stackTraceSupplier;

	/**
	 * Creates a handler for one executing frame.
	 *
	 * @param interpreter
	 * 		Interpreter used for type assignability checks.
	 * @param method
	 * 		Method whose try/catch blocks receive routed exceptions, or {@code null} for block evaluation.
	 * @param stackTraceSupplier
	 * 		Supplier for the live evaluator stack trace used when creating throwable values.
	 */
	public ExceptionHandler(@Nonnull ReInterpreter interpreter, @Nullable MethodNode method,
	                        @Nonnull Supplier<List<StackTraceElement>> stackTraceSupplier) {
		this.interpreter = interpreter;
		this.method = method;
		this.stackTraceSupplier = stackTraceSupplier;
	}

	/**
	 * Checks whether a type is assignable to {@code java/lang/Throwable}.
	 *
	 * @param internalName
	 * 		Internal name of the type to check.
	 *
	 * @return {@code true} if the type is throwable.
	 */
	public boolean isThrowableType(@Nonnull String internalName) {
		return interpreter.isAssignableFrom("java/lang/Throwable", internalName);
	}

	private boolean isAssignableFrom(@Nonnull String parent, @Nonnull String child) {
		return interpreter.isAssignableFrom(parent, child);
	}

	/**
	 * Checks whether an invocation initializes a throwable.
	 *
	 * @param instruction
	 * 		Invocation instruction to inspect.
	 *
	 * @return {@code true} if the instruction invokes a throwable constructor.
	 */
	public boolean isThrowableConstructor(@Nonnull MethodInsnNode instruction) {
		return instruction.name.equals("<init>") && isThrowableType(instruction.owner);
	}

	/**
	 * Checks whether an invocation reads a throwable's synthesized stack trace.
	 *
	 * @param instruction
	 * 		Invocation instruction to inspect.
	 *
	 * @return {@code true} if the instruction invokes {@code getStackTrace()} on a throwable.
	 */
	public boolean isThrowableGetStackTrace(@Nonnull MethodInsnNode instruction) {
		return instruction.name.equals("getStackTrace")
				&& instruction.desc.equals("()[Ljava/lang/StackTraceElement;")
				&& isThrowableType(instruction.owner);
	}

	/**
	 * Predicts an implicit exception when the current frame state makes one certain.
	 *
	 * @param instruction
	 * 		Instruction whose operands are checked.
	 * @param frame
	 * 		Current frame containing the instruction operands.
	 *
	 * @return Predicted throwable value, or {@code null} if no fault is known.
	 */
	@Nullable
	public ReValue knownFault(@Nonnull AbstractInsnNode instruction, @Nonnull ReFrame frame) {
		return switch (instruction.getOpcode()) {
			case Opcodes.IDIV, Opcodes.IREM, Opcodes.LDIV, Opcodes.LREM ->
					isZero(peek(frame)) ? newThrowable("java/lang/ArithmeticException", null) : null;
			case Opcodes.GETFIELD, Opcodes.ARRAYLENGTH, Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD,
			     Opcodes.DALOAD, Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD ->
					knownArrayOrFieldFault(instruction, frame);
			case Opcodes.PUTFIELD -> isNull(peekFromTop(frame, 1)) ?
					newThrowable("java/lang/NullPointerException", null) : null;
			case Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE,
			     Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE -> knownArrayStoreFault(instruction, frame);
			case Opcodes.NEWARRAY, Opcodes.ANEWARRAY ->
					isNegative(peek(frame)) ? newThrowable("java/lang/NegativeArraySizeException", null) : null;
			case Opcodes.MULTIANEWARRAY -> knownMultiArrayFault(instruction, frame);
			case Opcodes.CHECKCAST -> knownCastFault(instruction, frame);
			case Opcodes.MONITORENTER, Opcodes.MONITOREXIT ->
					isNull(peek(frame)) ? newThrowable("java/lang/NullPointerException", null) : null;
			case Opcodes.INVOKESPECIAL, Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE ->
					instruction instanceof MethodInsnNode methodInsn && isNull(peekMethodReceiver(methodInsn, frame)) ?
							newThrowable("java/lang/NullPointerException", null) : null;
			default -> null;
		};
	}

	/**
	 * Routes a thrown value to the first matching handler in the current method.
	 *
	 * @param frame
	 * 		Frame whose operand stack is replaced for a matching handler.
	 * @param exception
	 * 		Throwable value to route.
	 * @param source
	 * 		Instruction that produced the exception.
	 *
	 * @return Handler instruction that receives the exception.
	 *
	 * @throws AnalyzerException
	 * 		When the value is not a valid throwable.
	 * @throws ThrownException
	 * 		When no handler can receive the exception.
	 */
	@Nonnull
	public AbstractInsnNode routeException(@Nonnull ReFrame frame, @Nonnull ReValue exception,
	                                       @Nonnull AbstractInsnNode source) throws AnalyzerException, ThrownException {
		// Must be a throwable/exception type reference value.
		if (!(exception instanceof ObjectValue object)
				|| object.isNull()
				|| !isThrowableType(object.type().getInternalName()))
			throw new AnalyzerException(source, "Invalid throwable value: " + exception);

		// Check in the current method for a try/catch block that can handle this exception.
		if (method != null && method.tryCatchBlocks != null) {
			int sourceIndex = method.instructions.indexOf(source);
			for (TryCatchBlockNode block : method.tryCatchBlocks) {
				int start = method.instructions.indexOf(block.start);
				int end = method.instructions.indexOf(block.end);
				if (sourceIndex < start || sourceIndex >= end)
					continue;
				if (block.type == null || isAssignableFrom(block.type, object.type().getInternalName())) {
					frame.clearStack();
					frame.push(exception);
					return block.handler;
				}
			}
		}

		// Propagate the exception to the caller if no try/catch block can handle it.
		throw new ThrownException(exception);
	}

	/**
	 * Creates an evaluator throwable value for a modeled or host exception.
	 *
	 * @param internalName
	 * 		Internal name of the throwable type to create.
	 * @param backingException
	 * 		Host exception associated with the value, or {@code null} for a modeled fault.
	 *
	 * @return Throwable value with the current evaluator stack trace.
	 */
	@Nonnull
	public ThrowableValue newThrowable(@Nonnull String internalName, @Nullable Throwable backingException) {
		return new ThrowableValueImpl(Type.getObjectType(internalName), stackTraceSupplier.get(), backingException);
	}

	/**
	 * Creates an evaluator throwable value for a host exception.
	 *
	 * @param throwable
	 * 		Host exception to represent.
	 *
	 * @return Throwable value with the current evaluator stack trace.
	 */
	@Nonnull
	public ThrowableValue newThrowable(@Nonnull Throwable throwable) {
		return newThrowable(throwable.getClass().getName().replace('.', '/'), throwable);
	}

	/**
	 * Creates the evaluator array returned by throwable {@code getStackTrace()}.
	 *
	 * @param throwable
	 * 		Throwable whose captured trace is converted.
	 *
	 * @return Array value containing the throwable's captured stack trace.
	 */
	@Nonnull
	public ArrayValue createStackTrace(@Nonnull ThrowableValue throwable) {
		Type stackTraceType = Type.getType("[Ljava/lang/StackTraceElement;");
		List<StackTraceElement> stackTrace = throwable.getStackTrace();
		return new ArrayValueImpl(stackTraceType, Nullness.NOT_NULL, stackTrace.size(),
				i -> new InstancedObjectValue<>(stackTrace.get(i)));
	}

	@Nullable
	private ReValue knownArrayOrFieldFault(@Nonnull AbstractInsnNode instruction, @Nonnull ReFrame frame) {
		// Check for null receiver/array for field access or array length.
		int opcode = instruction.getOpcode();
		ReValue arrayOrReceiver = opcode == Opcodes.GETFIELD || opcode == Opcodes.ARRAYLENGTH ?
				peek(frame) : peekFromTop(frame, 1);
		if (isNull(arrayOrReceiver))
			return newThrowable("java/lang/NullPointerException", null);

		// Check for out-of-bounds array access.
		if (!(arrayOrReceiver instanceof ArrayValue array))
			return null;
		ReValue index = peek(frame);
		if (!(index instanceof IntValue intIndex) || intIndex.value().isEmpty())
			return null;
		int value = intIndex.value().getAsInt();
		if (array.getFirstDimensionLength().isPresent() &&
				(value < 0 || value >= array.getFirstDimensionLength().getAsInt()))
			return newThrowable("java/lang/ArrayIndexOutOfBoundsException", null);
		return null;
	}

	@Nullable
	private ReValue knownArrayStoreFault(@Nonnull AbstractInsnNode instruction, @Nonnull ReFrame frame) {
		// Check for null array for array store.
		ReValue arrayValue = peekFromTop(frame, 2);
		if (isNull(arrayValue))
			return newThrowable("java/lang/NullPointerException", null);
		if (!(arrayValue instanceof ArrayValue array))
			return null;

		// Check for out-of-bounds array store.
		ReValue index = peekFromTop(frame, 1);
		if (index instanceof IntValue intIndex
				&& intIndex.value().isPresent()
				&& array.getFirstDimensionLength().isPresent()) {
			int value = intIndex.value().getAsInt();
			if (value < 0 || value >= array.getFirstDimensionLength().getAsInt())
				return newThrowable("java/lang/ArrayIndexOutOfBoundsException", null);
		}
		if (instruction.getOpcode() == Opcodes.AASTORE && array.elementType().getSort() == Type.OBJECT) {
			ReValue stored = peek(frame);
			if (stored instanceof ObjectValue object
					&& !object.isNull()
					&& !isAssignableFrom(array.elementType().getInternalName(), stored.type().getInternalName()))
				return newThrowable("java/lang/ArrayStoreException", null);
		}
		return null;
	}

	@Nullable
	private ReValue knownMultiArrayFault(@Nonnull AbstractInsnNode instruction, @Nonnull ReFrame frame) {
		if (!(instruction instanceof MultiANewArrayInsnNode multi))
			return null;
		// Check that all dimensions are non-negative.
		for (int i = 0; i < multi.dims; i++)
			if (isNegative(peekFromTop(frame, i)))
				return newThrowable("java/lang/NegativeArraySizeException", null);
		return null;
	}

	@Nullable
	private ReValue knownCastFault(@Nonnull AbstractInsnNode instruction, @Nonnull ReFrame frame) {
		if (!(instruction instanceof TypeInsnNode typeInsn))
			return null;
		// If the value is null, then it can be cast to any type.
		// Otherwise, the value must be assignable to the target type.
		ReValue value = peek(frame);
		if (value instanceof ObjectValue object
				&& !object.isNull()
				&& !isAssignableFrom(typeInsn.desc, value.type().getInternalName()))
			return newThrowable("java/lang/ClassCastException", null);
		return null;
	}

	@Nonnull
	private static ReValue peek(@Nonnull ReFrame frame) {
		return peekFromTop(frame, 0);
	}

	@Nonnull
	private static ReValue peekMethodReceiver(@Nonnull MethodInsnNode instruction, @Nonnull ReFrame frame) {
		return peekFromTop(frame, Type.getArgumentTypes(instruction.desc).length);
	}

	@Nonnull
	private static ReValue peekFromTop(@Nonnull ReFrame frame, int offset) {
		int index = frame.getStackSize() - 1;
		for (int i = 0; i < offset; i++) {
			ReValue value = frame.getStack(index);
			index -= Math.max(1, value.getSize());
		}
		return frame.getStack(index);
	}

	private static boolean isNull(@Nullable ReValue value) {
		return value instanceof ObjectValue object && object.isNull();
	}

	private static boolean isZero(@Nullable ReValue value) {
		return value instanceof IntValue intValue && intValue.value().isPresent() && intValue.value().getAsInt() == 0 ||
				value instanceof LongValue longValue && longValue.value().isPresent() && longValue.value().getAsLong() == 0L;
	}

	private static boolean isNegative(@Nullable ReValue value) {
		return value instanceof IntValue intValue && intValue.isLessThan(0);
	}

	/**
	 * Signals that an exception must propagate beyond the current frame.
	 *
	 * @see ExceptionHandler#routeException(ReFrame, ReValue, AbstractInsnNode)
	 */
	public static final class ThrownException extends Exception {
		private final ReValue exception;

		private ThrownException(@Nonnull ReValue exception) {
			this.exception = exception;
		}

		/**
		 * @return Value of the exception that was thrown and could not be handled in the current frame.
		 */
		@Nonnull
		public ReValue getExceptionValue() {
			return exception;
		}

		@Override
		public synchronized Throwable fillInStackTrace() {
			// Don't care.
			return this;
		}
	}
}
