package software.coley.recaf.util.analysis.lookup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.util.analysis.value.impl.ArrayValueImpl;

import java.util.List;
import java.util.OptionalInt;

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
	protected static StringValue str(@Nullable CharSequence value) {return str(value == null ? null : value.toString());}

	protected static boolean[] arrz(@Nonnull ArrayValue value) {
		OptionalInt dimLength = value.getFirstDimensionLength();
		if (dimLength.isEmpty() || !value.hasKnownValue())
			throw new IllegalArgumentException();
		int length = dimLength.getAsInt();
		boolean[] booleans = new boolean[length];
		for (int i = 0; i < length; i++) {
			ReValue iv = value.getValue(i);
			if (iv instanceof IntValue iiv && iiv.hasKnownValue())
				booleans[i] = z(iiv);
		}
		return booleans;
	}

	protected static byte[] arrb(@Nonnull ArrayValue value) {
		OptionalInt dimLength = value.getFirstDimensionLength();
		if (dimLength.isEmpty() || !value.hasKnownValue())
			throw new IllegalArgumentException();
		int length = dimLength.getAsInt();
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			ReValue iv = value.getValue(i);
			if (iv instanceof IntValue iiv && iiv.hasKnownValue())
				bytes[i] = b(iiv);
		}
		return bytes;
	}

	protected static short[] arrs(@Nonnull ArrayValue value) {
		OptionalInt dimLength = value.getFirstDimensionLength();
		if (dimLength.isEmpty() || !value.hasKnownValue())
			throw new IllegalArgumentException();
		int length = dimLength.getAsInt();
		short[] shorts = new short[length];
		for (int i = 0; i < length; i++) {
			ReValue iv = value.getValue(i);
			if (iv instanceof IntValue iiv && iiv.hasKnownValue())
				shorts[i] = s(iiv);
		}
		return shorts;
	}

	protected static char[] arrc(@Nonnull ArrayValue value) {
		OptionalInt dimLength = value.getFirstDimensionLength();
		if (dimLength.isEmpty() || !value.hasKnownValue())
			throw new IllegalArgumentException();
		int length = dimLength.getAsInt();
		char[] chars = new char[length];
		for (int i = 0; i < length; i++) {
			ReValue iv = value.getValue(i);
			if (iv instanceof IntValue iiv && iiv.hasKnownValue())
				chars[i] = c(iiv);
		}
		return chars;
	}

	protected static int[] arri(@Nonnull ArrayValue value) {
		OptionalInt dimLength = value.getFirstDimensionLength();
		if (dimLength.isEmpty() || !value.hasKnownValue())
			throw new IllegalArgumentException();
		int length = dimLength.getAsInt();
		int[] ints = new int[length];
		for (int i = 0; i < length; i++) {
			ReValue iv = value.getValue(i);
			if (iv instanceof IntValue iiv && iiv.hasKnownValue())
				ints[i] = i(iiv);
		}
		return ints;
	}

	protected static float[] arrf(@Nonnull ArrayValue value) {
		OptionalInt dimLength = value.getFirstDimensionLength();
		if (dimLength.isEmpty() || !value.hasKnownValue())
			throw new IllegalArgumentException();
		int length = dimLength.getAsInt();
		float[] floats = new float[length];
		for (int i = 0; i < length; i++) {
			ReValue iv = value.getValue(i);
			if (iv instanceof FloatValue fv && fv.hasKnownValue())
				floats[i] = f(fv);
		}
		return floats;
	}

	protected static double[] arrd(@Nonnull ArrayValue value) {
		OptionalInt dimLength = value.getFirstDimensionLength();
		if (dimLength.isEmpty() || !value.hasKnownValue())
			throw new IllegalArgumentException();
		int length = dimLength.getAsInt();
		double[] doubles = new double[length];
		for (int i = 0; i < length; i++) {
			ReValue iv = value.getValue(i);
			if (iv instanceof DoubleValue dv && dv.hasKnownValue())
				doubles[i] = d(dv);
		}
		return doubles;
	}

	protected static long[] arrj(@Nonnull ArrayValue value) {
		OptionalInt dimLength = value.getFirstDimensionLength();
		if (dimLength.isEmpty() || !value.hasKnownValue())
			throw new IllegalArgumentException();
		int length = dimLength.getAsInt();
		long[] longs = new long[length];
		for (int i = 0; i < length; i++) {
			ReValue iv = value.getValue(i);
			if (iv instanceof LongValue lv && lv.hasKnownValue())
				longs[i] = j(lv);
		}
		return longs;
	}

	protected static String[] arrstr(@Nonnull ArrayValue value) {
		OptionalInt dimLength = value.getFirstDimensionLength();
		if (dimLength.isEmpty() || !value.hasKnownValue())
			throw new IllegalArgumentException();
		int length = dimLength.getAsInt();
		String[] strings = new String[length];
		for (int i = 0; i < length; i++) {
			ReValue iv = value.getValue(i);
			if (iv instanceof StringValue sv && sv.hasKnownValue())
				strings[i] = str(sv);
		}
		return strings;
	}

	@Nonnull
	protected static ArrayValue arrz(@Nullable boolean[] value) {
		if (value == null)
			return ArrayValue.VAL_BOOLEANS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_BOOLEAN, Nullness.NOT_NULL, value.length, index -> IntValue.of(value[index] ? 1 : 0));
	}

	@Nonnull
	protected static ArrayValue arrb(@Nullable byte[] value) {
		if (value == null)
			return ArrayValue.VAL_BYTES_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_BYTE, Nullness.NOT_NULL, value.length, index -> IntValue.of(value[index]));
	}

	@Nonnull
	protected static ArrayValue arrs(@Nullable short[] value) {
		if (value == null)
			return ArrayValue.VAL_SHORTS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_SHORT, Nullness.NOT_NULL, value.length, index -> IntValue.of(value[index]));
	}

	@Nonnull
	protected static ArrayValue arrc(@Nullable char[] value) {
		if (value == null)
			return ArrayValue.VAL_CHARS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_CHAR, Nullness.NOT_NULL, value.length, index -> IntValue.of(value[index]));
	}

	@Nonnull
	protected static ArrayValue arri(@Nullable int[] value) {
		if (value == null)
			return ArrayValue.VAL_INTS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_INT, Nullness.NOT_NULL, value.length, index -> IntValue.of(value[index]));
	}

	@Nonnull
	protected static ArrayValue arrj(@Nullable long[] value) {
		if (value == null)
			return ArrayValue.VAL_LONGS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_LONG, Nullness.NOT_NULL, value.length, index -> LongValue.of(value[index]));
	}

	@Nonnull
	protected static ArrayValue arrf(@Nullable float[] value) {
		if (value == null)
			return ArrayValue.VAL_FLOATS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_FLOAT, Nullness.NOT_NULL, value.length, index -> FloatValue.of(value[index]));
	}

	@Nonnull
	protected static ArrayValue arrd(@Nullable double[] value) {
		if (value == null)
			return ArrayValue.VAL_DOUBLES_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_DOUBLE, Nullness.NOT_NULL, value.length, index -> DoubleValue.of(value[index]));
	}

	@Nonnull
	protected static ArrayValue arrstr(@Nullable String[] value) {
		if (value == null)
			return ArrayValue.VAL_STRINGS_NULL;
		return new ArrayValueImpl(Types.ARRAY_1D_STRING, Nullness.NOT_NULL, value.length, index -> ObjectValue.string(value[index]));
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
