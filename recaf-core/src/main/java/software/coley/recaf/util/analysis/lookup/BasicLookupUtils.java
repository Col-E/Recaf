package software.coley.recaf.util.analysis.lookup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;

import java.util.List;

/**
 * Common utilities for lookup implementations to convert between JVM values and interpreter values.
 *
 * @author Matt Coley
 */
public class BasicLookupUtils {
	@SuppressWarnings("all")
	protected static byte b(@Nonnull IntValue value) {return (byte) value.value().getAsInt();}

	@SuppressWarnings("all")
	protected static boolean z(@Nonnull IntValue value) {return value.isNotEqualTo(0);}

	@SuppressWarnings("all")
	protected static short s(@Nonnull IntValue value) {return (short) value.value().getAsInt();}

	@SuppressWarnings("all")
	protected static char c(@Nonnull IntValue value) {return (char) value.value().getAsInt();}

	@SuppressWarnings("all")
	protected static int i(@Nonnull IntValue value) {return value.value().getAsInt();}

	@SuppressWarnings("all")
	protected static long j(@Nonnull LongValue value) {return value.value().getAsLong();}

	@SuppressWarnings("all")
	protected static float f(@Nonnull FloatValue value) {return (float) value.value().getAsDouble();}

	@SuppressWarnings("all")
	protected static double d(@Nonnull DoubleValue value) {return value.value().getAsDouble();}

	@SuppressWarnings("all")
	protected static String str(@Nonnull StringValue value) {return value.getText().get();}

	protected static char[] arr(@Nonnull ArrayValue value) {
		/* TODO: Uncomment when array-value tracks items
		OptionalInt dimLength = value.getFirstDimensionLength();
		int length = dimLength.getAsInt();
		char[] chars = new char[length];
		for (int i = 0; i < length; i++)
			chars[i] = c(value.getValueAt(i));
		return chars;
		 */
		throw new UnsupportedOperationException("Should not reach here");
	}

	@Nonnull
	protected static IntValue z(boolean value) {return IntValue.of(value ? 1 : 0);}

	@Nonnull
	protected static IntValue b(byte value) {return IntValue.of(value);}

	@Nonnull
	protected static IntValue c(char value) {return IntValue.of(value);}

	@Nonnull
	protected static IntValue s(short value) {return IntValue.of(value);}

	@Nonnull
	protected static IntValue i(int value) {return IntValue.of(value);}

	@Nonnull
	protected static LongValue j(long value) {return LongValue.of(value);}

	@Nonnull
	protected static FloatValue f(float value) {return FloatValue.of(value);}

	@Nonnull
	protected static DoubleValue d(double value) {return DoubleValue.of(value);}

	@Nonnull
	protected static StringValue str(@Nullable String value) {return ObjectValue.string(value);}

	@Nonnull
	protected static ArrayValue arr(@Nullable boolean[] value) {
		// TODO: Implement
		return ArrayValue.VAL_BOOLEANS;
	}

	@Nonnull
	protected static ArrayValue arr(@Nullable byte[] value) {
		// TODO: Implement
		return ArrayValue.VAL_BYTES;
	}

	@Nonnull
	protected static ArrayValue arr(@Nullable short[] value) {
		// TODO: Implement
		return ArrayValue.VAL_SHORTS;
	}

	@Nonnull
	protected static ArrayValue arr(@Nullable char[] value) {
		// TODO: Implement
		return ArrayValue.VAL_CHARS;
	}

	@Nonnull
	protected static ArrayValue arr(@Nullable int[] value) {
		// TODO: Implement
		return ArrayValue.VAL_INTS;
	}

	@Nonnull
	protected static ArrayValue arr(@Nullable long[] value) {
		// TODO: Implement
		return ArrayValue.VAL_LONGS;
	}

	@Nonnull
	protected static ArrayValue arr(@Nullable float[] value) {
		// TODO: Implement
		return ArrayValue.VAL_FLOATS;
	}

	@Nonnull
	protected static ArrayValue arr(@Nullable double[] value) {
		// TODO: Implement
		return ArrayValue.VAL_DOUBLES;
	}

	@Nonnull
	protected static ArrayValue arr(@Nullable String[] value) {
		// TODO: Implement
		return ArrayValue.VAL_STRINGS;
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
