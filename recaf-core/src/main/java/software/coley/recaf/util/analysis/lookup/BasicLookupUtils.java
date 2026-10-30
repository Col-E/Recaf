package software.coley.recaf.util.analysis.lookup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.analysis.gen.InstanceMapperGenerator;
import software.coley.recaf.util.analysis.gen.InstanceMethodInvokeHandlerGenerator;
import software.coley.recaf.util.analysis.gen.LookupGenerator;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.util.analysis.value.Values;

import java.util.List;

/**
 * Common utilities for lookup implementations to convert between JVM values and interpreter values.
 * <p>
 * The shorthand names exist here for brevity in auto-generated code.
 * See:
 * <ul>
 *     <li>{@link InstanceMapperGenerator}</li>
 *     <li>{@link InstanceMethodInvokeHandlerGenerator}</li>
 *     <li>{@link LookupGenerator}</li>
 * </ul>
 *
 * @author Matt Coley
 */
public class BasicLookupUtils {
	protected static byte b(@Nonnull IntValue value) {
		return Values.asByte(value);
	}

	protected static boolean z(@Nonnull IntValue value) {
		return Values.asBoolean(value);
	}

	protected static short s(@Nonnull IntValue value) {
		return Values.asShort(value);
	}

	protected static char c(@Nonnull IntValue value) {
		return Values.asChar(value);
	}

	protected static int i(@Nonnull IntValue value) {
		return Values.asInt(value);
	}

	protected static long j(@Nonnull LongValue value) {
		return Values.asLong(value);
	}

	protected static float f(@Nonnull FloatValue value) {
		return Values.asFloat(value);
	}

	protected static double d(@Nonnull DoubleValue value) {
		return Values.asDouble(value);
	}

	protected static String str(@Nonnull ObjectValue value) {
		return Values.asString(value);
	}

	protected static Object objl(@Nonnull ObjectValue value) {
		return Values.asObjectLiteral(value);
	}

	protected static <T> T obj(@Nonnull ObjectValue value) {
		return Values.asObject(value);
	}

	@Nonnull
	protected static IntValue z(boolean value) {
		return Values.toBooleanValue(value);
	}

	@Nonnull
	protected static IntValue b(byte value) {
		return Values.toByteValue(value);
	}

	@Nonnull
	protected static IntValue c(char value) {
		return Values.toCharValue(value);
	}

	@Nonnull
	protected static IntValue s(short value) {
		return Values.toShortValue(value);
	}

	@Nonnull
	protected static IntValue i(int value) {
		return Values.toIntValue(value);
	}

	@Nonnull
	protected static LongValue j(long value) {
		return Values.toLongValue(value);
	}

	@Nonnull
	protected static FloatValue f(float value) {
		return Values.toFloatValue(value);
	}

	@Nonnull
	protected static DoubleValue d(double value) {
		return Values.toDoubleValue(value);
	}

	@Nonnull
	protected static StringValue str(@Nullable String value) {
		return Values.toStringValue(value);
	}

	@Nonnull
	protected static StringValue str(@Nullable CharSequence value) {
		return Values.toStringValue(value);
	}

	@Nonnull
	protected static ObjectValue obj(@Nullable Object value) {
		return Values.toObjectValue(value);
	}

	protected static boolean[] arrz(@Nonnull ArrayValue value) {
		return Values.asBooleanArray(value);
	}

	protected static byte[] arrb(@Nonnull ArrayValue value) {
		return Values.asByteArray(value);
	}

	protected static short[] arrs(@Nonnull ArrayValue value) {
		return Values.asShortArray(value);
	}

	protected static char[] arrc(@Nonnull ArrayValue value) {
		return Values.asCharArray(value);
	}

	protected static int[] arri(@Nonnull ArrayValue value) {
		return Values.asIntArray(value);
	}

	protected static float[] arrf(@Nonnull ArrayValue value) {
		return Values.asFloatArray(value);
	}

	protected static double[] arrd(@Nonnull ArrayValue value) {
		return Values.asDoubleArray(value);
	}

	protected static long[] arrj(@Nonnull ArrayValue value) {
		return Values.asLongArray(value);
	}

	protected static String[] arrstr(@Nonnull ArrayValue value) {
		return Values.asStringArray(value);
	}

	protected static Object[] arrobj(@Nonnull ArrayValue value) {
		return Values.asObjectArray(value);
	}

	protected static boolean[] arrz(@Nonnull ReValue value) {
		return Values.asBooleanArray(value);
	}

	protected static byte[] arrb(@Nonnull ReValue value) {
		return Values.asByteArray(value);
	}

	protected static short[] arrs(@Nonnull ReValue value) {
		return Values.asShortArray(value);
	}

	protected static char[] arrc(@Nonnull ReValue value) {
		return Values.asCharArray(value);
	}

	protected static int[] arri(@Nonnull ReValue value) {
		return Values.asIntArray(value);
	}

	protected static float[] arrf(@Nonnull ReValue value) {
		return Values.asFloatArray(value);
	}

	protected static double[] arrd(@Nonnull ReValue value) {
		return Values.asDoubleArray(value);
	}

	protected static long[] arrj(@Nonnull ReValue value) {
		return Values.asLongArray(value);
	}

	protected static String[] arrstr(@Nonnull ReValue value) {
		return Values.asStringArray(value);
	}

	protected static Object[] arrobj(@Nonnull ReValue value) {
		return Values.asObjectArray(value);
	}

	@Nonnull
	protected static ArrayValue arrz(@Nullable boolean[] value) {
		return Values.toBooleanArrayValue(value);
	}

	@Nonnull
	protected static ArrayValue arrb(@Nullable byte[] value) {
		return Values.toByteArrayValue(value);
	}

	@Nonnull
	protected static ArrayValue arrs(@Nullable short[] value) {
		return Values.toShortArrayValue(value);
	}

	@Nonnull
	protected static ArrayValue arrc(@Nullable char[] value) {
		return Values.toCharArrayValue(value);
	}

	@Nonnull
	protected static ArrayValue arri(@Nullable int[] value) {
		return Values.toIntArrayValue(value);
	}

	@Nonnull
	protected static ArrayValue arrj(@Nullable long[] value) {
		return Values.toLongArrayValue(value);
	}

	@Nonnull
	protected static ArrayValue arrf(@Nullable float[] value) {
		return Values.toFloatArrayValue(value);
	}

	@Nonnull
	protected static ArrayValue arrd(@Nullable double[] value) {
		return Values.toDoubleArrayValue(value);
	}

	@Nonnull
	protected static ArrayValue arrstr(@Nullable CharSequence[] value) {
		return Values.toCharSequenceArrayValue(value);
	}

	@Nonnull
	protected static ArrayValue arrstr(@Nullable String[] value) {
		return Values.toStringArrayValue(value);
	}

	@Nonnull
	protected static ArrayValue arrobj(@Nullable Object[] value) {
		return Values.toObjectArrayValue(value);
	}

	@SuppressWarnings("unchecked")
	protected interface Func_7<A extends ReValue, B extends ReValue, C extends ReValue, D extends ReValue, E extends ReValue, F extends ReValue, G extends ReValue> extends Func {
		@Nullable
		@Override
		default ReValue apply(@Nonnull List<? extends ReValue> values) {
			return apply((A) values.get(0), (B) values.get(1), (C) values.get(2), (D) values.get(3), (E) values.get(4), (F) values.get(5), (G) values.get(6));
		}

		@Nullable
		ReValue apply(A a, B b, C c, D d, E e, F f, G g);
	}

	@SuppressWarnings("unchecked")
	protected interface Func_6<A extends ReValue, B extends ReValue, C extends ReValue, D extends ReValue, E extends ReValue, F extends ReValue> extends Func {
		@Nullable
		@Override
		default ReValue apply(@Nonnull List<? extends ReValue> values) {
			return apply((A) values.get(0), (B) values.get(1), (C) values.get(2), (D) values.get(3), (E) values.get(4), (F) values.get(5));
		}

		@Nullable
		ReValue apply(A a, B b, C c, D d, E e, F f);
	}

	@SuppressWarnings("unchecked")
	protected interface Func_5<A extends ReValue, B extends ReValue, C extends ReValue, D extends ReValue, E extends ReValue> extends Func {
		@Nullable
		@Override
		default ReValue apply(@Nonnull List<? extends ReValue> values) {
			return apply((A) values.get(0), (B) values.get(1), (C) values.get(2), (D) values.get(3), (E) values.get(4));
		}

		@Nullable
		ReValue apply(A a, B b, C c, D d, E e);
	}

	@SuppressWarnings("unchecked")
	protected interface Func_4<A extends ReValue, B extends ReValue, C extends ReValue, D extends ReValue> extends Func {
		@Nullable
		@Override
		default ReValue apply(@Nonnull List<? extends ReValue> values) {
			return apply((A) values.get(0), (B) values.get(1), (C) values.get(2), (D) values.get(3));
		}

		@Nullable
		ReValue apply(A a, B b, C c, D d);
	}

	@SuppressWarnings("unchecked")
	protected interface Func_3<A extends ReValue, B extends ReValue, C extends ReValue> extends Func {
		@Nullable
		@Override
		default ReValue apply(@Nonnull List<? extends ReValue> values) {
			return apply((A) values.get(0), (B) values.get(1), (C) values.get(2));
		}

		@Nullable
		ReValue apply(A a, B b, C c);
	}

	@SuppressWarnings("unchecked")
	protected interface Func_2<A extends ReValue, B extends ReValue> extends Func {
		@Nullable
		@Override
		default ReValue apply(@Nonnull List<? extends ReValue> values) {
			return apply((A) values.get(0), (B) values.get(1));
		}

		@Nullable
		ReValue apply(A a, B b);
	}

	@SuppressWarnings("unchecked")
	protected interface Func_1<A extends ReValue> extends Func {
		@Nullable
		@Override
		default ReValue apply(@Nonnull List<? extends ReValue> values) {
			return apply((A) values.getFirst());
		}

		@Nullable
		ReValue apply(A a);
	}

	protected interface Func_0 extends Func {
		@Nullable
		@Override
		default ReValue apply(@Nonnull List<? extends ReValue> values) {
			return apply();
		}

		@Nullable
		ReValue apply();
	}

	protected interface Func {
		@Nullable
		ReValue apply(@Nonnull List<? extends ReValue> values);
	}
}
