package software.coley.recaf.util.analysis.lookup;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Basic implementation of {@link InvokeStaticLookup} for common static fields.
 * <br>
 * Mostly auto-generated, see {@link BasicLookupGenerator#main(String[])}
 *
 * @author Matt Coley
 */
public class BasicInvokeStaticLookup extends BasicLookupUtils implements InvokeStaticLookup {
	private static final Map<String, Func> METHODS = new HashMap<>();
	private static final DebuggingLogger logger = Logging.get(BasicInvokeStaticLookup.class);

	@Nonnull
	@Override
	public ReValue get(@Nonnull MethodInsnNode method, @Nonnull List<? extends ReValue> values) {
		String key = getKey(method);
		Func func = METHODS.get(key);
		ReValue value = null;
		if (func != null)
			try {
				value = func.apply(values);
			} catch (Throwable t) {
				// Some methods may inherently throw, like 'Math.floorDiv(0, 0)' so these error
				// log calls are only active while debugging.
				logger.debugging(l -> l.error("Computation threw an exception for: " + key, t));
			}
		if (value == null) {
			try {
				value = ReValue.ofType(Type.getReturnType(method.desc), Nullness.UNKNOWN);
			} catch (IllegalValueException ex) {
				logger.error("Failed default value computation for: " + key, ex);
			}
		}
		return Objects.requireNonNull(value);
	}

	@Override
	public boolean hasLookup(@Nonnull MethodInsnNode method) {
		return METHODS.containsKey(getKey(method));
	}

	@Nonnull
	private static String getKey(@Nonnull MethodInsnNode method) {
		return method.owner + "." + method.name + method.desc;
	}

	static {
		// Utilities & common types
		math();
		arrays();
		system();
		strings();
		objects();

		// Primitives
		booleans();
		bytes();
		chars();
		shorts();
		ints();
		longs();
		floats();
		doubles();
	}

	private static void system() {
		METHODS.put("java/lang/System.lineSeparator()Ljava/lang/String;", (Func_0) () -> str(System.lineSeparator()));
		//METHODS.put("java/lang/System.getProperty(Ljava/lang/String;)Ljava/lang/String;", (Func_1<StringValue>) (a) -> str(System.getProperty(str(a))));
		//METHODS.put("java/lang/System.getProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (Func_2<StringValue, StringValue>) (a, b) -> str(System.getProperty(str(a), str(b))));
		//METHODS.put("java/lang/System.getenv(Ljava/lang/String;)Ljava/lang/String;", (Func_1<StringValue>) (a) -> str(System.getenv(str(a))));
	}

	private static void strings() {
		METHODS.put("java/lang/String.valueOf(J)Ljava/lang/String;", (Func_1<LongValue>) (a) -> str(String.valueOf(j(a))));
		METHODS.put("java/lang/String.valueOf([C)Ljava/lang/String;", (Func_1<ArrayValue>) (a) -> str(String.valueOf(arrc(a))));
		METHODS.put("java/lang/String.valueOf(Ljava/lang/Object;)Ljava/lang/String;", (Func_1<ObjectValue>) (a) -> str(String.valueOf(obj(a))));
		METHODS.put("java/lang/String.valueOf([CII)Ljava/lang/String;", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> str(String.valueOf(arrc(a), i(b), i(c))));
		METHODS.put("java/lang/String.valueOf(F)Ljava/lang/String;", (Func_1<FloatValue>) (a) -> str(String.valueOf(f(a))));
		METHODS.put("java/lang/String.valueOf(D)Ljava/lang/String;", (Func_1<DoubleValue>) (a) -> str(String.valueOf(d(a))));
		METHODS.put("java/lang/String.valueOf(C)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(String.valueOf(c(a))));
		METHODS.put("java/lang/String.valueOf(Z)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(String.valueOf(z(a))));
		METHODS.put("java/lang/String.valueOf(I)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(String.valueOf(i(a))));
		METHODS.put("java/lang/String.join(Ljava/lang/CharSequence;[Ljava/lang/CharSequence;)Ljava/lang/String;", (Func_2<StringValue, ArrayValue>) (a, b) -> str(String.join(str(a), arrstr(b))));
		METHODS.put("java/lang/String.format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", (Func_2<StringValue, ArrayValue>) (a, b) -> str(String.format(str(a), arrobj(b))));
		METHODS.put("java/lang/String.copyValueOf([C)Ljava/lang/String;", (Func_1<ArrayValue>) (a) -> str(String.copyValueOf(arrc(a))));
		METHODS.put("java/lang/String.copyValueOf([CII)Ljava/lang/String;", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> str(String.copyValueOf(arrc(a), i(b), i(c))));
		METHODS.put("java/lang/CharSequence.compare(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)I", (Func_2<StringValue, StringValue>) (a, b) -> i(CharSequence.compare(str(a), str(b))));
	}

	private static void objects() {
		METHODS.put("java/util/Objects.equals(Ljava/lang/Object;Ljava/lang/Object;)Z", (Func_2<ObjectValue, ObjectValue>) (a, b) -> z(Objects.equals(obj(a), obj(b))));
		METHODS.put("java/util/Objects.toString(Ljava/lang/Object;)Ljava/lang/String;", (Func_1<ObjectValue>) (a) -> str(Objects.toString(obj(a))));
		METHODS.put("java/util/Objects.toString(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;", (Func_2<ObjectValue, StringValue>) (a, b) -> str(Objects.toString(obj(a), str(b))));
		METHODS.put("java/util/Objects.checkIndex(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Objects.checkIndex(i(a), i(b))));
		METHODS.put("java/util/Objects.checkIndex(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Objects.checkIndex(j(a), j(b))));
		METHODS.put("java/util/Objects.hashCode(Ljava/lang/Object;)I", (Func_1<ObjectValue>) (a) -> i(Objects.hashCode(obj(a))));
		METHODS.put("java/util/Objects.hash([Ljava/lang/Object;)I", (Func_1<ArrayValue>) (a) -> i(Objects.hash(arrobj(a))));
		METHODS.put("java/util/Objects.requireNonNull(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", (Func_2<ObjectValue, StringValue>) (a, b) -> obj(Objects.requireNonNull(obj(a), str(b))));
		METHODS.put("java/util/Objects.requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;", (Func_1<ObjectValue>) (a) -> obj(Objects.requireNonNull(obj(a))));
		METHODS.put("java/util/Objects.checkFromToIndex(JJJ)J", (Func_3<LongValue, LongValue, LongValue>) (a, b, c) -> j(Objects.checkFromToIndex(j(a), j(b), j(c))));
		METHODS.put("java/util/Objects.checkFromToIndex(III)I", (Func_3<IntValue, IntValue, IntValue>) (a, b, c) -> i(Objects.checkFromToIndex(i(a), i(b), i(c))));
		METHODS.put("java/util/Objects.checkFromIndexSize(III)I", (Func_3<IntValue, IntValue, IntValue>) (a, b, c) -> i(Objects.checkFromIndexSize(i(a), i(b), i(c))));
		METHODS.put("java/util/Objects.checkFromIndexSize(JJJ)J", (Func_3<LongValue, LongValue, LongValue>) (a, b, c) -> j(Objects.checkFromIndexSize(j(a), j(b), j(c))));
		METHODS.put("java/util/Objects.deepEquals(Ljava/lang/Object;Ljava/lang/Object;)Z", (Func_2<ObjectValue, ObjectValue>) (a, b) -> z(Objects.deepEquals(obj(a), obj(b))));
		METHODS.put("java/util/Objects.toIdentityString(Ljava/lang/Object;)Ljava/lang/String;", (Func_1<ObjectValue>) (a) -> str(Objects.toIdentityString(obj(a))));
		METHODS.put("java/util/Objects.isNull(Ljava/lang/Object;)Z", (Func_1<ObjectValue>) (a) -> z(Objects.isNull(obj(a))));
		METHODS.put("java/util/Objects.nonNull(Ljava/lang/Object;)Z", (Func_1<ObjectValue>) (a) -> z(Objects.nonNull(obj(a))));
		METHODS.put("java/util/Objects.requireNonNullElse(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", (Func_2<ObjectValue, ObjectValue>) (a, b) -> obj(Objects.requireNonNullElse(obj(a), obj(b))));
	}

	private static void arrays() {
		METHODS.put("java/util/Arrays.equals([D[D)Z", (Func_2<ArrayValue, ArrayValue>) (a, b) -> z(Arrays.equals(arrd(a), arrd(b))));
		METHODS.put("java/util/Arrays.equals([ZII[ZII)Z", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> z(Arrays.equals(arrz(a), i(b), i(c), arrz(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.equals([Z[Z)Z", (Func_2<ArrayValue, ArrayValue>) (a, b) -> z(Arrays.equals(arrz(a), arrz(b))));
		METHODS.put("java/util/Arrays.equals([BII[BII)Z", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> z(Arrays.equals(arrb(a), i(b), i(c), arrb(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.equals([B[B)Z", (Func_2<ArrayValue, ArrayValue>) (a, b) -> z(Arrays.equals(arrb(a), arrb(b))));
		METHODS.put("java/util/Arrays.equals([Ljava/lang/Object;II[Ljava/lang/Object;II)Z", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> z(Arrays.equals(arrobj(a), i(b), i(c), arrobj(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.equals([Ljava/lang/Object;[Ljava/lang/Object;)Z", (Func_2<ArrayValue, ArrayValue>) (a, b) -> z(Arrays.equals(arrobj(a), arrobj(b))));
		METHODS.put("java/util/Arrays.equals([FII[FII)Z", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> z(Arrays.equals(arrf(a), i(b), i(c), arrf(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.equals([F[F)Z", (Func_2<ArrayValue, ArrayValue>) (a, b) -> z(Arrays.equals(arrf(a), arrf(b))));
		METHODS.put("java/util/Arrays.equals([DII[DII)Z", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> z(Arrays.equals(arrd(a), i(b), i(c), arrd(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.equals([III[III)Z", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> z(Arrays.equals(arri(a), i(b), i(c), arri(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.equals([CII[CII)Z", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> z(Arrays.equals(arrc(a), i(b), i(c), arrc(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.equals([JII[JII)Z", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> z(Arrays.equals(arrj(a), i(b), i(c), arrj(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.equals([J[J)Z", (Func_2<ArrayValue, ArrayValue>) (a, b) -> z(Arrays.equals(arrj(a), arrj(b))));
		METHODS.put("java/util/Arrays.equals([S[S)Z", (Func_2<ArrayValue, ArrayValue>) (a, b) -> z(Arrays.equals(arrs(a), arrs(b))));
		METHODS.put("java/util/Arrays.equals([SII[SII)Z", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> z(Arrays.equals(arrs(a), i(b), i(c), arrs(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.equals([C[C)Z", (Func_2<ArrayValue, ArrayValue>) (a, b) -> z(Arrays.equals(arrc(a), arrc(b))));
		METHODS.put("java/util/Arrays.equals([I[I)Z", (Func_2<ArrayValue, ArrayValue>) (a, b) -> z(Arrays.equals(arri(a), arri(b))));
		METHODS.put("java/util/Arrays.toString([C)Ljava/lang/String;", (Func_1<ArrayValue>) (a) -> str(Arrays.toString(arrc(a))));
		METHODS.put("java/util/Arrays.toString([F)Ljava/lang/String;", (Func_1<ArrayValue>) (a) -> str(Arrays.toString(arrf(a))));
		METHODS.put("java/util/Arrays.toString([D)Ljava/lang/String;", (Func_1<ArrayValue>) (a) -> str(Arrays.toString(arrd(a))));
		METHODS.put("java/util/Arrays.toString([Ljava/lang/Object;)Ljava/lang/String;", (Func_1<ArrayValue>) (a) -> str(Arrays.toString(arrobj(a))));
		METHODS.put("java/util/Arrays.toString([B)Ljava/lang/String;", (Func_1<ArrayValue>) (a) -> str(Arrays.toString(arrb(a))));
		METHODS.put("java/util/Arrays.toString([Z)Ljava/lang/String;", (Func_1<ArrayValue>) (a) -> str(Arrays.toString(arrz(a))));
		METHODS.put("java/util/Arrays.toString([J)Ljava/lang/String;", (Func_1<ArrayValue>) (a) -> str(Arrays.toString(arrj(a))));
		METHODS.put("java/util/Arrays.toString([I)Ljava/lang/String;", (Func_1<ArrayValue>) (a) -> str(Arrays.toString(arri(a))));
		METHODS.put("java/util/Arrays.toString([S)Ljava/lang/String;", (Func_1<ArrayValue>) (a) -> str(Arrays.toString(arrs(a))));
		METHODS.put("java/util/Arrays.hashCode([F)I", (Func_1<ArrayValue>) (a) -> i(Arrays.hashCode(arrf(a))));
		METHODS.put("java/util/Arrays.hashCode([B)I", (Func_1<ArrayValue>) (a) -> i(Arrays.hashCode(arrb(a))));
		METHODS.put("java/util/Arrays.hashCode([Z)I", (Func_1<ArrayValue>) (a) -> i(Arrays.hashCode(arrz(a))));
		METHODS.put("java/util/Arrays.hashCode([D)I", (Func_1<ArrayValue>) (a) -> i(Arrays.hashCode(arrd(a))));
		METHODS.put("java/util/Arrays.hashCode([Ljava/lang/Object;)I", (Func_1<ArrayValue>) (a) -> i(Arrays.hashCode(arrobj(a))));
		METHODS.put("java/util/Arrays.hashCode([J)I", (Func_1<ArrayValue>) (a) -> i(Arrays.hashCode(arrj(a))));
		METHODS.put("java/util/Arrays.hashCode([I)I", (Func_1<ArrayValue>) (a) -> i(Arrays.hashCode(arri(a))));
		METHODS.put("java/util/Arrays.hashCode([S)I", (Func_1<ArrayValue>) (a) -> i(Arrays.hashCode(arrs(a))));
		METHODS.put("java/util/Arrays.hashCode([C)I", (Func_1<ArrayValue>) (a) -> i(Arrays.hashCode(arrc(a))));
		METHODS.put("java/util/Arrays.compareUnsigned([B[B)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.compareUnsigned(arrb(a), arrb(b))));
		METHODS.put("java/util/Arrays.compareUnsigned([S[S)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.compareUnsigned(arrs(a), arrs(b))));
		METHODS.put("java/util/Arrays.compareUnsigned([BII[BII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.compareUnsigned(arrb(a), i(b), i(c), arrb(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.compareUnsigned([JII[JII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.compareUnsigned(arrj(a), i(b), i(c), arrj(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.compareUnsigned([SII[SII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.compareUnsigned(arrs(a), i(b), i(c), arrs(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.compareUnsigned([J[J)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.compareUnsigned(arrj(a), arrj(b))));
		METHODS.put("java/util/Arrays.compareUnsigned([III[III)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.compareUnsigned(arri(a), i(b), i(c), arri(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.compareUnsigned([I[I)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.compareUnsigned(arri(a), arri(b))));
		METHODS.put("java/util/Arrays.copyOf([DI)[D", (Func_2<ArrayValue, IntValue>) (a, b) -> arrd(Arrays.copyOf(arrd(a), i(b))));
		METHODS.put("java/util/Arrays.copyOf([II)[I", (Func_2<ArrayValue, IntValue>) (a, b) -> arri(Arrays.copyOf(arri(a), i(b))));
		METHODS.put("java/util/Arrays.copyOf([BI)[B", (Func_2<ArrayValue, IntValue>) (a, b) -> arrb(Arrays.copyOf(arrb(a), i(b))));
		METHODS.put("java/util/Arrays.copyOf([Ljava/lang/Object;I)[Ljava/lang/Object;", (Func_2<ArrayValue, IntValue>) (a, b) -> arrobj(Arrays.copyOf(arrobj(a), i(b))));
		METHODS.put("java/util/Arrays.copyOf([ZI)[Z", (Func_2<ArrayValue, IntValue>) (a, b) -> arrz(Arrays.copyOf(arrz(a), i(b))));
		METHODS.put("java/util/Arrays.copyOf([FI)[F", (Func_2<ArrayValue, IntValue>) (a, b) -> arrf(Arrays.copyOf(arrf(a), i(b))));
		METHODS.put("java/util/Arrays.copyOf([CI)[C", (Func_2<ArrayValue, IntValue>) (a, b) -> arrc(Arrays.copyOf(arrc(a), i(b))));
		METHODS.put("java/util/Arrays.copyOf([JI)[J", (Func_2<ArrayValue, IntValue>) (a, b) -> arrj(Arrays.copyOf(arrj(a), i(b))));
		METHODS.put("java/util/Arrays.copyOf([SI)[S", (Func_2<ArrayValue, IntValue>) (a, b) -> arrs(Arrays.copyOf(arrs(a), i(b))));
		METHODS.put("java/util/Arrays.copyOfRange([ZII)[Z", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> arrz(Arrays.copyOfRange(arrz(a), i(b), i(c))));
		METHODS.put("java/util/Arrays.copyOfRange([CII)[C", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> arrc(Arrays.copyOfRange(arrc(a), i(b), i(c))));
		METHODS.put("java/util/Arrays.copyOfRange([JII)[J", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> arrj(Arrays.copyOfRange(arrj(a), i(b), i(c))));
		METHODS.put("java/util/Arrays.copyOfRange([FII)[F", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> arrf(Arrays.copyOfRange(arrf(a), i(b), i(c))));
		METHODS.put("java/util/Arrays.copyOfRange([DII)[D", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> arrd(Arrays.copyOfRange(arrd(a), i(b), i(c))));
		METHODS.put("java/util/Arrays.copyOfRange([Ljava/lang/Object;II)[Ljava/lang/Object;", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> arrobj(Arrays.copyOfRange(arrobj(a), i(b), i(c))));
		METHODS.put("java/util/Arrays.copyOfRange([BII)[B", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> arrb(Arrays.copyOfRange(arrb(a), i(b), i(c))));
		METHODS.put("java/util/Arrays.copyOfRange([SII)[S", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> arrs(Arrays.copyOfRange(arrs(a), i(b), i(c))));
		METHODS.put("java/util/Arrays.copyOfRange([III)[I", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> arri(Arrays.copyOfRange(arri(a), i(b), i(c))));
		METHODS.put("java/util/Arrays.compare([CII[CII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.compare(arrc(a), i(b), i(c), arrc(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.compare([I[I)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.compare(arri(a), arri(b))));
		METHODS.put("java/util/Arrays.compare([C[C)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.compare(arrc(a), arrc(b))));
		METHODS.put("java/util/Arrays.compare([III[III)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.compare(arri(a), i(b), i(c), arri(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.compare([J[J)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.compare(arrj(a), arrj(b))));
		METHODS.put("java/util/Arrays.compare([JII[JII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.compare(arrj(a), i(b), i(c), arrj(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.compare([D[D)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.compare(arrd(a), arrd(b))));
		METHODS.put("java/util/Arrays.compare([FII[FII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.compare(arrf(a), i(b), i(c), arrf(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.compare([S[S)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.compare(arrs(a), arrs(b))));
		METHODS.put("java/util/Arrays.compare([SII[SII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.compare(arrs(a), i(b), i(c), arrs(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.compare([Z[Z)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.compare(arrz(a), arrz(b))));
		METHODS.put("java/util/Arrays.compare([ZII[ZII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.compare(arrz(a), i(b), i(c), arrz(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.compare([B[B)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.compare(arrb(a), arrb(b))));
		METHODS.put("java/util/Arrays.compare([BII[BII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.compare(arrb(a), i(b), i(c), arrb(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.compare([DII[DII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.compare(arrd(a), i(b), i(c), arrd(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.compare([F[F)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.compare(arrf(a), arrf(b))));
		METHODS.put("java/util/Arrays.mismatch([J[J)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.mismatch(arrj(a), arrj(b))));
		METHODS.put("java/util/Arrays.mismatch([F[F)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.mismatch(arrf(a), arrf(b))));
		METHODS.put("java/util/Arrays.mismatch([JII[JII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.mismatch(arrj(a), i(b), i(c), arrj(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.mismatch([Ljava/lang/Object;[Ljava/lang/Object;)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.mismatch(arrobj(a), arrobj(b))));
		METHODS.put("java/util/Arrays.mismatch([Ljava/lang/Object;II[Ljava/lang/Object;II)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.mismatch(arrobj(a), i(b), i(c), arrobj(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.mismatch([DII[DII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.mismatch(arrd(a), i(b), i(c), arrd(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.mismatch([D[D)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.mismatch(arrd(a), arrd(b))));
		METHODS.put("java/util/Arrays.mismatch([FII[FII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.mismatch(arrf(a), i(b), i(c), arrf(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.mismatch([BII[BII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.mismatch(arrb(a), i(b), i(c), arrb(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.mismatch([C[C)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.mismatch(arrc(a), arrc(b))));
		METHODS.put("java/util/Arrays.mismatch([CII[CII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.mismatch(arrc(a), i(b), i(c), arrc(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.mismatch([I[I)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.mismatch(arri(a), arri(b))));
		METHODS.put("java/util/Arrays.mismatch([SII[SII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.mismatch(arrs(a), i(b), i(c), arrs(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.mismatch([S[S)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.mismatch(arrs(a), arrs(b))));
		METHODS.put("java/util/Arrays.mismatch([III[III)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.mismatch(arri(a), i(b), i(c), arri(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.mismatch([Z[Z)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.mismatch(arrz(a), arrz(b))));
		METHODS.put("java/util/Arrays.mismatch([ZII[ZII)I", (Func_6<ArrayValue, IntValue, IntValue, ArrayValue, IntValue, IntValue>) (a, b, c, d, e, f) -> i(Arrays.mismatch(arrz(a), i(b), i(c), arrz(d), i(e), i(f))));
		METHODS.put("java/util/Arrays.mismatch([B[B)I", (Func_2<ArrayValue, ArrayValue>) (a, b) -> i(Arrays.mismatch(arrb(a), arrb(b))));
		METHODS.put("java/util/Arrays.deepEquals([Ljava/lang/Object;[Ljava/lang/Object;)Z", (Func_2<ArrayValue, ArrayValue>) (a, b) -> z(Arrays.deepEquals(arrobj(a), arrobj(b))));
		METHODS.put("java/util/Arrays.deepHashCode([Ljava/lang/Object;)I", (Func_1<ArrayValue>) (a) -> i(Arrays.deepHashCode(arrobj(a))));
		METHODS.put("java/util/Arrays.deepToString([Ljava/lang/Object;)Ljava/lang/String;", (Func_1<ArrayValue>) (a) -> str(Arrays.deepToString(arrobj(a))));
		METHODS.put("java/util/Arrays.binarySearch([FIIF)I", (Func_4<ArrayValue, IntValue, IntValue, FloatValue>) (a, b, c, d) -> i(Arrays.binarySearch(arrf(a), i(b), i(c), f(d))));
		METHODS.put("java/util/Arrays.binarySearch([FF)I", (Func_2<ArrayValue, FloatValue>) (a, b) -> i(Arrays.binarySearch(arrf(a), f(b))));
		METHODS.put("java/util/Arrays.binarySearch([SS)I", (Func_2<ArrayValue, IntValue>) (a, b) -> i(Arrays.binarySearch(arrs(a), s(b))));
		METHODS.put("java/util/Arrays.binarySearch([SIIS)I", (Func_4<ArrayValue, IntValue, IntValue, IntValue>) (a, b, c, d) -> i(Arrays.binarySearch(arrs(a), i(b), i(c), s(d))));
		METHODS.put("java/util/Arrays.binarySearch([Ljava/lang/Object;Ljava/lang/Object;)I", (Func_2<ArrayValue, ObjectValue>) (a, b) -> i(Arrays.binarySearch(arrobj(a), obj(b))));
		METHODS.put("java/util/Arrays.binarySearch([Ljava/lang/Object;IILjava/lang/Object;)I", (Func_4<ArrayValue, IntValue, IntValue, ObjectValue>) (a, b, c, d) -> i(Arrays.binarySearch(arrobj(a), i(b), i(c), obj(d))));
		METHODS.put("java/util/Arrays.binarySearch([DIID)I", (Func_4<ArrayValue, IntValue, IntValue, DoubleValue>) (a, b, c, d) -> i(Arrays.binarySearch(arrd(a), i(b), i(c), d(d))));
		METHODS.put("java/util/Arrays.binarySearch([DD)I", (Func_2<ArrayValue, DoubleValue>) (a, b) -> i(Arrays.binarySearch(arrd(a), d(b))));
		METHODS.put("java/util/Arrays.binarySearch([BIIB)I", (Func_4<ArrayValue, IntValue, IntValue, IntValue>) (a, b, c, d) -> i(Arrays.binarySearch(arrb(a), i(b), i(c), b(d))));
		METHODS.put("java/util/Arrays.binarySearch([BB)I", (Func_2<ArrayValue, IntValue>) (a, b) -> i(Arrays.binarySearch(arrb(a), b(b))));
		METHODS.put("java/util/Arrays.binarySearch([CIIC)I", (Func_4<ArrayValue, IntValue, IntValue, IntValue>) (a, b, c, d) -> i(Arrays.binarySearch(arrc(a), i(b), i(c), c(d))));
		METHODS.put("java/util/Arrays.binarySearch([CC)I", (Func_2<ArrayValue, IntValue>) (a, b) -> i(Arrays.binarySearch(arrc(a), c(b))));
		METHODS.put("java/util/Arrays.binarySearch([IIII)I", (Func_4<ArrayValue, IntValue, IntValue, IntValue>) (a, b, c, d) -> i(Arrays.binarySearch(arri(a), i(b), i(c), i(d))));
		METHODS.put("java/util/Arrays.binarySearch([JJ)I", (Func_2<ArrayValue, LongValue>) (a, b) -> i(Arrays.binarySearch(arrj(a), j(b))));
		METHODS.put("java/util/Arrays.binarySearch([JIIJ)I", (Func_4<ArrayValue, IntValue, IntValue, LongValue>) (a, b, c, d) -> i(Arrays.binarySearch(arrj(a), i(b), i(c), j(d))));
		METHODS.put("java/util/Arrays.binarySearch([II)I", (Func_2<ArrayValue, IntValue>) (a, b) -> i(Arrays.binarySearch(arri(a), i(b))));
	}

	private static void booleans() {
		METHODS.put("java/lang/Boolean.toString(Z)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(Boolean.toString(z(a))));
		METHODS.put("java/lang/Boolean.hashCode(Z)I", (Func_1<IntValue>) (a) -> i(Boolean.hashCode(z(a))));
		METHODS.put("java/lang/Boolean.getBoolean(Ljava/lang/String;)Z", (Func_1<StringValue>) (a) -> z(Boolean.getBoolean(str(a))));
		METHODS.put("java/lang/Boolean.compare(ZZ)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Boolean.compare(z(a), z(b))));
		METHODS.put("java/lang/Boolean.valueOf(Ljava/lang/String;)Ljava/lang/Boolean;", (Func_1<StringValue>) (a) -> obj(Boolean.valueOf(str(a))));
		METHODS.put("java/lang/Boolean.valueOf(Z)Ljava/lang/Boolean;", (Func_1<IntValue>) (a) -> obj(Boolean.valueOf(z(a))));
		METHODS.put("java/lang/Boolean.parseBoolean(Ljava/lang/String;)Z", (Func_1<StringValue>) (a) -> z(Boolean.parseBoolean(str(a))));
		METHODS.put("java/lang/Boolean.logicalAnd(ZZ)Z", (Func_2<IntValue, IntValue>) (a, b) -> z(Boolean.logicalAnd(z(a), z(b))));
		METHODS.put("java/lang/Boolean.logicalOr(ZZ)Z", (Func_2<IntValue, IntValue>) (a, b) -> z(Boolean.logicalOr(z(a), z(b))));
		METHODS.put("java/lang/Boolean.logicalXor(ZZ)Z", (Func_2<IntValue, IntValue>) (a, b) -> z(Boolean.logicalXor(z(a), z(b))));
	}

	private static void bytes() {
		METHODS.put("java/lang/Byte.toString(B)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(Byte.toString(b(a))));
		METHODS.put("java/lang/Byte.hashCode(B)I", (Func_1<IntValue>) (a) -> i(Byte.hashCode(b(a))));
		METHODS.put("java/lang/Byte.compareUnsigned(BB)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Byte.compareUnsigned(b(a), b(b))));
		METHODS.put("java/lang/Byte.compare(BB)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Byte.compare(b(a), b(b))));
		METHODS.put("java/lang/Byte.valueOf(Ljava/lang/String;)Ljava/lang/Byte;", (Func_1<StringValue>) (a) -> obj(Byte.valueOf(str(a))));
		METHODS.put("java/lang/Byte.valueOf(Ljava/lang/String;I)Ljava/lang/Byte;", (Func_2<StringValue, IntValue>) (a, b) -> obj(Byte.valueOf(str(a), i(b))));
		METHODS.put("java/lang/Byte.valueOf(B)Ljava/lang/Byte;", (Func_1<IntValue>) (a) -> obj(Byte.valueOf(b(a))));
		METHODS.put("java/lang/Byte.decode(Ljava/lang/String;)Ljava/lang/Byte;", (Func_1<StringValue>) (a) -> obj(Byte.decode(str(a))));
		METHODS.put("java/lang/Byte.toUnsignedLong(B)J", (Func_1<IntValue>) (a) -> j(Byte.toUnsignedLong(b(a))));
		METHODS.put("java/lang/Byte.toUnsignedInt(B)I", (Func_1<IntValue>) (a) -> i(Byte.toUnsignedInt(b(a))));
		METHODS.put("java/lang/Byte.parseByte(Ljava/lang/String;)B", (Func_1<StringValue>) (a) -> b(Byte.parseByte(str(a))));
		METHODS.put("java/lang/Byte.parseByte(Ljava/lang/String;I)B", (Func_2<StringValue, IntValue>) (a, b) -> b(Byte.parseByte(str(a), i(b))));
	}

	@SuppressWarnings("deprecation")
	private static void chars() {
		METHODS.put("java/lang/Character.getName(I)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(Character.getName(i(a))));
		METHODS.put("java/lang/Character.isJavaIdentifierStart(C)Z", (Func_1<IntValue>) (a) -> z(Character.isJavaIdentifierStart(c(a))));
		METHODS.put("java/lang/Character.isJavaIdentifierStart(I)Z", (Func_1<IntValue>) (a) -> z(Character.isJavaIdentifierStart(i(a))));
		METHODS.put("java/lang/Character.isJavaIdentifierPart(C)Z", (Func_1<IntValue>) (a) -> z(Character.isJavaIdentifierPart(c(a))));
		METHODS.put("java/lang/Character.isJavaIdentifierPart(I)Z", (Func_1<IntValue>) (a) -> z(Character.isJavaIdentifierPart(i(a))));
		METHODS.put("java/lang/Character.toString(I)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(Character.toString(i(a))));
		METHODS.put("java/lang/Character.toString(C)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(Character.toString(c(a))));
		METHODS.put("java/lang/Character.hashCode(C)I", (Func_1<IntValue>) (a) -> i(Character.hashCode(c(a))));
		METHODS.put("java/lang/Character.reverseBytes(C)C", (Func_1<IntValue>) (a) -> c(Character.reverseBytes(c(a))));
		METHODS.put("java/lang/Character.isDigit(C)Z", (Func_1<IntValue>) (a) -> z(Character.isDigit(c(a))));
		METHODS.put("java/lang/Character.isDigit(I)Z", (Func_1<IntValue>) (a) -> z(Character.isDigit(i(a))));
		METHODS.put("java/lang/Character.isLowerCase(C)Z", (Func_1<IntValue>) (a) -> z(Character.isLowerCase(c(a))));
		METHODS.put("java/lang/Character.isLowerCase(I)Z", (Func_1<IntValue>) (a) -> z(Character.isLowerCase(i(a))));
		METHODS.put("java/lang/Character.isUpperCase(I)Z", (Func_1<IntValue>) (a) -> z(Character.isUpperCase(i(a))));
		METHODS.put("java/lang/Character.isUpperCase(C)Z", (Func_1<IntValue>) (a) -> z(Character.isUpperCase(c(a))));
		METHODS.put("java/lang/Character.isWhitespace(C)Z", (Func_1<IntValue>) (a) -> z(Character.isWhitespace(c(a))));
		METHODS.put("java/lang/Character.isWhitespace(I)Z", (Func_1<IntValue>) (a) -> z(Character.isWhitespace(i(a))));
		METHODS.put("java/lang/Character.compare(CC)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Character.compare(c(a), c(b))));
		METHODS.put("java/lang/Character.valueOf(C)Ljava/lang/Character;", (Func_1<IntValue>) (a) -> obj(Character.valueOf(c(a))));
		METHODS.put("java/lang/Character.toChars(I)[C", (Func_1<IntValue>) (a) -> arrc(Character.toChars(i(a))));
		METHODS.put("java/lang/Character.toChars(I[CI)I", (Func_3<IntValue, ArrayValue, IntValue>) (a, b, c) -> i(Character.toChars(i(a), arrc(b), i(c))));
		METHODS.put("java/lang/Character.isHighSurrogate(C)Z", (Func_1<IntValue>) (a) -> z(Character.isHighSurrogate(c(a))));
		METHODS.put("java/lang/Character.isLowSurrogate(C)Z", (Func_1<IntValue>) (a) -> z(Character.isLowSurrogate(c(a))));
		METHODS.put("java/lang/Character.isSurrogate(C)Z", (Func_1<IntValue>) (a) -> z(Character.isSurrogate(c(a))));
		METHODS.put("java/lang/Character.isSupplementaryCodePoint(I)Z", (Func_1<IntValue>) (a) -> z(Character.isSupplementaryCodePoint(i(a))));
		METHODS.put("java/lang/Character.highSurrogate(I)C", (Func_1<IntValue>) (a) -> c(Character.highSurrogate(i(a))));
		METHODS.put("java/lang/Character.lowSurrogate(I)C", (Func_1<IntValue>) (a) -> c(Character.lowSurrogate(i(a))));
		METHODS.put("java/lang/Character.toCodePoint(CC)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Character.toCodePoint(c(a), c(b))));
		METHODS.put("java/lang/Character.codePointAt(Ljava/lang/CharSequence;I)I", (Func_2<StringValue, IntValue>) (a, b) -> i(Character.codePointAt(str(a), i(b))));
		METHODS.put("java/lang/Character.codePointAt([CII)I", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> i(Character.codePointAt(arrc(a), i(b), i(c))));
		METHODS.put("java/lang/Character.codePointAt([CI)I", (Func_2<ArrayValue, IntValue>) (a, b) -> i(Character.codePointAt(arrc(a), i(b))));
		METHODS.put("java/lang/Character.codePointBefore(Ljava/lang/CharSequence;I)I", (Func_2<StringValue, IntValue>) (a, b) -> i(Character.codePointBefore(str(a), i(b))));
		METHODS.put("java/lang/Character.codePointBefore([CI)I", (Func_2<ArrayValue, IntValue>) (a, b) -> i(Character.codePointBefore(arrc(a), i(b))));
		METHODS.put("java/lang/Character.codePointBefore([CII)I", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> i(Character.codePointBefore(arrc(a), i(b), i(c))));
		METHODS.put("java/lang/Character.codePointCount(Ljava/lang/CharSequence;II)I", (Func_3<StringValue, IntValue, IntValue>) (a, b, c) -> i(Character.codePointCount(str(a), i(b), i(c))));
		METHODS.put("java/lang/Character.codePointCount([CII)I", (Func_3<ArrayValue, IntValue, IntValue>) (a, b, c) -> i(Character.codePointCount(arrc(a), i(b), i(c))));
		METHODS.put("java/lang/Character.offsetByCodePoints([CIIII)I", (Func_5<ArrayValue, IntValue, IntValue, IntValue, IntValue>) (a, b, c, d, e) -> i(Character.offsetByCodePoints(arrc(a), i(b), i(c), i(d), i(e))));
		METHODS.put("java/lang/Character.offsetByCodePoints(Ljava/lang/CharSequence;II)I", (Func_3<StringValue, IntValue, IntValue>) (a, b, c) -> i(Character.offsetByCodePoints(str(a), i(b), i(c))));
		METHODS.put("java/lang/Character.toLowerCase(C)C", (Func_1<IntValue>) (a) -> c(Character.toLowerCase(c(a))));
		METHODS.put("java/lang/Character.toLowerCase(I)I", (Func_1<IntValue>) (a) -> i(Character.toLowerCase(i(a))));
		METHODS.put("java/lang/Character.toUpperCase(I)I", (Func_1<IntValue>) (a) -> i(Character.toUpperCase(i(a))));
		METHODS.put("java/lang/Character.toUpperCase(C)C", (Func_1<IntValue>) (a) -> c(Character.toUpperCase(c(a))));
		METHODS.put("java/lang/Character.isBmpCodePoint(I)Z", (Func_1<IntValue>) (a) -> z(Character.isBmpCodePoint(i(a))));
		METHODS.put("java/lang/Character.getType(C)I", (Func_1<IntValue>) (a) -> i(Character.getType(c(a))));
		METHODS.put("java/lang/Character.getType(I)I", (Func_1<IntValue>) (a) -> i(Character.getType(i(a))));
		METHODS.put("java/lang/Character.isLetter(C)Z", (Func_1<IntValue>) (a) -> z(Character.isLetter(c(a))));
		METHODS.put("java/lang/Character.isLetter(I)Z", (Func_1<IntValue>) (a) -> z(Character.isLetter(i(a))));
		METHODS.put("java/lang/Character.isLetterOrDigit(I)Z", (Func_1<IntValue>) (a) -> z(Character.isLetterOrDigit(i(a))));
		METHODS.put("java/lang/Character.isLetterOrDigit(C)Z", (Func_1<IntValue>) (a) -> z(Character.isLetterOrDigit(c(a))));
		METHODS.put("java/lang/Character.isValidCodePoint(I)Z", (Func_1<IntValue>) (a) -> z(Character.isValidCodePoint(i(a))));
		METHODS.put("java/lang/Character.isTitleCase(I)Z", (Func_1<IntValue>) (a) -> z(Character.isTitleCase(i(a))));
		METHODS.put("java/lang/Character.isTitleCase(C)Z", (Func_1<IntValue>) (a) -> z(Character.isTitleCase(c(a))));
		METHODS.put("java/lang/Character.isDefined(C)Z", (Func_1<IntValue>) (a) -> z(Character.isDefined(c(a))));
		METHODS.put("java/lang/Character.isDefined(I)Z", (Func_1<IntValue>) (a) -> z(Character.isDefined(i(a))));
		METHODS.put("java/lang/Character.isIdeographic(I)Z", (Func_1<IntValue>) (a) -> z(Character.isIdeographic(i(a))));
		METHODS.put("java/lang/Character.isUnicodeIdentifierStart(I)Z", (Func_1<IntValue>) (a) -> z(Character.isUnicodeIdentifierStart(i(a))));
		METHODS.put("java/lang/Character.isUnicodeIdentifierStart(C)Z", (Func_1<IntValue>) (a) -> z(Character.isUnicodeIdentifierStart(c(a))));
		METHODS.put("java/lang/Character.isUnicodeIdentifierPart(I)Z", (Func_1<IntValue>) (a) -> z(Character.isUnicodeIdentifierPart(i(a))));
		METHODS.put("java/lang/Character.isUnicodeIdentifierPart(C)Z", (Func_1<IntValue>) (a) -> z(Character.isUnicodeIdentifierPart(c(a))));
		METHODS.put("java/lang/Character.isIdentifierIgnorable(I)Z", (Func_1<IntValue>) (a) -> z(Character.isIdentifierIgnorable(i(a))));
		METHODS.put("java/lang/Character.isIdentifierIgnorable(C)Z", (Func_1<IntValue>) (a) -> z(Character.isIdentifierIgnorable(c(a))));
		METHODS.put("java/lang/Character.isEmoji(I)Z", (Func_1<IntValue>) (a) -> z(Character.isEmoji(i(a))));
		METHODS.put("java/lang/Character.isEmojiPresentation(I)Z", (Func_1<IntValue>) (a) -> z(Character.isEmojiPresentation(i(a))));
		METHODS.put("java/lang/Character.isEmojiModifier(I)Z", (Func_1<IntValue>) (a) -> z(Character.isEmojiModifier(i(a))));
		METHODS.put("java/lang/Character.isEmojiModifierBase(I)Z", (Func_1<IntValue>) (a) -> z(Character.isEmojiModifierBase(i(a))));
		METHODS.put("java/lang/Character.isEmojiComponent(I)Z", (Func_1<IntValue>) (a) -> z(Character.isEmojiComponent(i(a))));
		METHODS.put("java/lang/Character.isExtendedPictographic(I)Z", (Func_1<IntValue>) (a) -> z(Character.isExtendedPictographic(i(a))));
		METHODS.put("java/lang/Character.toTitleCase(C)C", (Func_1<IntValue>) (a) -> c(Character.toTitleCase(c(a))));
		METHODS.put("java/lang/Character.toTitleCase(I)I", (Func_1<IntValue>) (a) -> i(Character.toTitleCase(i(a))));
		METHODS.put("java/lang/Character.digit(CI)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Character.digit(c(a), i(b))));
		METHODS.put("java/lang/Character.digit(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Character.digit(i(a), i(b))));
		METHODS.put("java/lang/Character.getNumericValue(I)I", (Func_1<IntValue>) (a) -> i(Character.getNumericValue(i(a))));
		METHODS.put("java/lang/Character.getNumericValue(C)I", (Func_1<IntValue>) (a) -> i(Character.getNumericValue(c(a))));
		METHODS.put("java/lang/Character.isSpaceChar(C)Z", (Func_1<IntValue>) (a) -> z(Character.isSpaceChar(c(a))));
		METHODS.put("java/lang/Character.isSpaceChar(I)Z", (Func_1<IntValue>) (a) -> z(Character.isSpaceChar(i(a))));
		METHODS.put("java/lang/Character.isISOControl(I)Z", (Func_1<IntValue>) (a) -> z(Character.isISOControl(i(a))));
		METHODS.put("java/lang/Character.isISOControl(C)Z", (Func_1<IntValue>) (a) -> z(Character.isISOControl(c(a))));
		METHODS.put("java/lang/Character.getDirectionality(C)B", (Func_1<IntValue>) (a) -> b(Character.getDirectionality(c(a))));
		METHODS.put("java/lang/Character.getDirectionality(I)B", (Func_1<IntValue>) (a) -> b(Character.getDirectionality(i(a))));
		METHODS.put("java/lang/Character.isMirrored(C)Z", (Func_1<IntValue>) (a) -> z(Character.isMirrored(c(a))));
		METHODS.put("java/lang/Character.isMirrored(I)Z", (Func_1<IntValue>) (a) -> z(Character.isMirrored(i(a))));
		METHODS.put("java/lang/Character.isSurrogatePair(CC)Z", (Func_2<IntValue, IntValue>) (a, b) -> z(Character.isSurrogatePair(c(a), c(b))));
		METHODS.put("java/lang/Character.charCount(I)I", (Func_1<IntValue>) (a) -> i(Character.charCount(i(a))));
		METHODS.put("java/lang/Character.isJavaLetter(C)Z", (Func_1<IntValue>) (a) -> z(Character.isJavaLetter(c(a))));
		METHODS.put("java/lang/Character.isJavaLetterOrDigit(C)Z", (Func_1<IntValue>) (a) -> z(Character.isJavaLetterOrDigit(c(a))));
		METHODS.put("java/lang/Character.isAlphabetic(I)Z", (Func_1<IntValue>) (a) -> z(Character.isAlphabetic(i(a))));
		METHODS.put("java/lang/Character.isSpace(C)Z", (Func_1<IntValue>) (a) -> z(Character.isSpace(c(a))));
		METHODS.put("java/lang/Character.forDigit(II)C", (Func_2<IntValue, IntValue>) (a, b) -> c(Character.forDigit(i(a), i(b))));
		METHODS.put("java/lang/Character.codePointOf(Ljava/lang/String;)I", (Func_1<StringValue>) (a) -> i(Character.codePointOf(str(a))));
	}

	private static void shorts() {
		METHODS.put("java/lang/Short.toString(S)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(Short.toString(s(a))));
		METHODS.put("java/lang/Short.hashCode(S)I", (Func_1<IntValue>) (a) -> i(Short.hashCode(s(a))));
		METHODS.put("java/lang/Short.compareUnsigned(SS)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Short.compareUnsigned(s(a), s(b))));
		METHODS.put("java/lang/Short.reverseBytes(S)S", (Func_1<IntValue>) (a) -> s(Short.reverseBytes(s(a))));
		METHODS.put("java/lang/Short.compare(SS)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Short.compare(s(a), s(b))));
		METHODS.put("java/lang/Short.valueOf(Ljava/lang/String;I)Ljava/lang/Short;", (Func_2<StringValue, IntValue>) (a, b) -> obj(Short.valueOf(str(a), i(b))));
		METHODS.put("java/lang/Short.valueOf(Ljava/lang/String;)Ljava/lang/Short;", (Func_1<StringValue>) (a) -> obj(Short.valueOf(str(a))));
		METHODS.put("java/lang/Short.valueOf(S)Ljava/lang/Short;", (Func_1<IntValue>) (a) -> obj(Short.valueOf(s(a))));
		METHODS.put("java/lang/Short.decode(Ljava/lang/String;)Ljava/lang/Short;", (Func_1<StringValue>) (a) -> obj(Short.decode(str(a))));
		METHODS.put("java/lang/Short.toUnsignedLong(S)J", (Func_1<IntValue>) (a) -> j(Short.toUnsignedLong(s(a))));
		METHODS.put("java/lang/Short.toUnsignedInt(S)I", (Func_1<IntValue>) (a) -> i(Short.toUnsignedInt(s(a))));
		METHODS.put("java/lang/Short.parseShort(Ljava/lang/String;)S", (Func_1<StringValue>) (a) -> s(Short.parseShort(str(a))));
		METHODS.put("java/lang/Short.parseShort(Ljava/lang/String;I)S", (Func_2<StringValue, IntValue>) (a, b) -> s(Short.parseShort(str(a), i(b))));
	}

	private static void ints() {
		METHODS.put("java/lang/Integer.numberOfLeadingZeros(I)I", (Func_1<IntValue>) (a) -> i(Integer.numberOfLeadingZeros(i(a))));
		METHODS.put("java/lang/Integer.numberOfTrailingZeros(I)I", (Func_1<IntValue>) (a) -> i(Integer.numberOfTrailingZeros(i(a))));
		METHODS.put("java/lang/Integer.bitCount(I)I", (Func_1<IntValue>) (a) -> i(Integer.bitCount(i(a))));
		METHODS.put("java/lang/Integer.toString(I)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(Integer.toString(i(a))));
		METHODS.put("java/lang/Integer.toString(II)Ljava/lang/String;", (Func_2<IntValue, IntValue>) (a, b) -> str(Integer.toString(i(a), i(b))));
		METHODS.put("java/lang/Integer.hashCode(I)I", (Func_1<IntValue>) (a) -> i(Integer.hashCode(i(a))));
		METHODS.put("java/lang/Integer.min(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Integer.min(i(a), i(b))));
		METHODS.put("java/lang/Integer.max(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Integer.max(i(a), i(b))));
		METHODS.put("java/lang/Integer.signum(I)I", (Func_1<IntValue>) (a) -> i(Integer.signum(i(a))));
		METHODS.put("java/lang/Integer.expand(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Integer.expand(i(a), i(b))));
		METHODS.put("java/lang/Integer.compareUnsigned(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Integer.compareUnsigned(i(a), i(b))));
		METHODS.put("java/lang/Integer.divideUnsigned(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Integer.divideUnsigned(i(a), i(b))));
		METHODS.put("java/lang/Integer.remainderUnsigned(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Integer.remainderUnsigned(i(a), i(b))));
		METHODS.put("java/lang/Integer.reverse(I)I", (Func_1<IntValue>) (a) -> i(Integer.reverse(i(a))));
		METHODS.put("java/lang/Integer.reverseBytes(I)I", (Func_1<IntValue>) (a) -> i(Integer.reverseBytes(i(a))));
		METHODS.put("java/lang/Integer.compress(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Integer.compress(i(a), i(b))));
		METHODS.put("java/lang/Integer.compare(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Integer.compare(i(a), i(b))));
		METHODS.put("java/lang/Integer.valueOf(Ljava/lang/String;)Ljava/lang/Integer;", (Func_1<StringValue>) (a) -> obj(Integer.valueOf(str(a))));
		METHODS.put("java/lang/Integer.valueOf(Ljava/lang/String;I)Ljava/lang/Integer;", (Func_2<StringValue, IntValue>) (a, b) -> obj(Integer.valueOf(str(a), i(b))));
		METHODS.put("java/lang/Integer.valueOf(I)Ljava/lang/Integer;", (Func_1<IntValue>) (a) -> obj(Integer.valueOf(i(a))));
		METHODS.put("java/lang/Integer.toHexString(I)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(Integer.toHexString(i(a))));
		METHODS.put("java/lang/Integer.decode(Ljava/lang/String;)Ljava/lang/Integer;", (Func_1<StringValue>) (a) -> obj(Integer.decode(str(a))));
		METHODS.put("java/lang/Integer.parseInt(Ljava/lang/String;)I", (Func_1<StringValue>) (a) -> i(Integer.parseInt(str(a))));
		METHODS.put("java/lang/Integer.parseInt(Ljava/lang/CharSequence;III)I", (Func_4<StringValue, IntValue, IntValue, IntValue>) (a, b, c, d) -> i(Integer.parseInt(str(a), i(b), i(c), i(d))));
		METHODS.put("java/lang/Integer.parseInt(Ljava/lang/String;I)I", (Func_2<StringValue, IntValue>) (a, b) -> i(Integer.parseInt(str(a), i(b))));
		METHODS.put("java/lang/Integer.toUnsignedLong(I)J", (Func_1<IntValue>) (a) -> j(Integer.toUnsignedLong(i(a))));
		METHODS.put("java/lang/Integer.sum(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Integer.sum(i(a), i(b))));
		METHODS.put("java/lang/Integer.toUnsignedString(II)Ljava/lang/String;", (Func_2<IntValue, IntValue>) (a, b) -> str(Integer.toUnsignedString(i(a), i(b))));
		METHODS.put("java/lang/Integer.toUnsignedString(I)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(Integer.toUnsignedString(i(a))));
		METHODS.put("java/lang/Integer.parseUnsignedInt(Ljava/lang/String;)I", (Func_1<StringValue>) (a) -> i(Integer.parseUnsignedInt(str(a))));
		METHODS.put("java/lang/Integer.parseUnsignedInt(Ljava/lang/String;I)I", (Func_2<StringValue, IntValue>) (a, b) -> i(Integer.parseUnsignedInt(str(a), i(b))));
		METHODS.put("java/lang/Integer.parseUnsignedInt(Ljava/lang/CharSequence;III)I", (Func_4<StringValue, IntValue, IntValue, IntValue>) (a, b, c, d) -> i(Integer.parseUnsignedInt(str(a), i(b), i(c), i(d))));
		METHODS.put("java/lang/Integer.getInteger(Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/Integer;", (Func_2<StringValue, ObjectValue>) (a, b) -> obj(Integer.getInteger(str(a), BasicLookupUtils.<Integer>obj(b))));
		METHODS.put("java/lang/Integer.getInteger(Ljava/lang/String;I)Ljava/lang/Integer;", (Func_2<StringValue, IntValue>) (a, b) -> obj(Integer.getInteger(str(a), i(b))));
		METHODS.put("java/lang/Integer.getInteger(Ljava/lang/String;)Ljava/lang/Integer;", (Func_1<StringValue>) (a) -> obj(Integer.getInteger(str(a))));
		METHODS.put("java/lang/Integer.toOctalString(I)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(Integer.toOctalString(i(a))));
		METHODS.put("java/lang/Integer.toBinaryString(I)Ljava/lang/String;", (Func_1<IntValue>) (a) -> str(Integer.toBinaryString(i(a))));
		METHODS.put("java/lang/Integer.highestOneBit(I)I", (Func_1<IntValue>) (a) -> i(Integer.highestOneBit(i(a))));
		METHODS.put("java/lang/Integer.lowestOneBit(I)I", (Func_1<IntValue>) (a) -> i(Integer.lowestOneBit(i(a))));
		METHODS.put("java/lang/Integer.rotateLeft(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Integer.rotateLeft(i(a), i(b))));
		METHODS.put("java/lang/Integer.rotateRight(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Integer.rotateRight(i(a), i(b))));
	}

	private static void longs() {
		METHODS.put("java/lang/Long.numberOfLeadingZeros(J)I", (Func_1<LongValue>) (a) -> i(Long.numberOfLeadingZeros(j(a))));
		METHODS.put("java/lang/Long.numberOfTrailingZeros(J)I", (Func_1<LongValue>) (a) -> i(Long.numberOfTrailingZeros(j(a))));
		METHODS.put("java/lang/Long.bitCount(J)I", (Func_1<LongValue>) (a) -> i(Long.bitCount(j(a))));
		METHODS.put("java/lang/Long.toString(J)Ljava/lang/String;", (Func_1<LongValue>) (a) -> str(Long.toString(j(a))));
		METHODS.put("java/lang/Long.toString(JI)Ljava/lang/String;", (Func_2<LongValue, IntValue>) (a, b) -> str(Long.toString(j(a), i(b))));
		METHODS.put("java/lang/Long.hashCode(J)I", (Func_1<LongValue>) (a) -> i(Long.hashCode(j(a))));
		METHODS.put("java/lang/Long.min(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Long.min(j(a), j(b))));
		METHODS.put("java/lang/Long.max(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Long.max(j(a), j(b))));
		METHODS.put("java/lang/Long.signum(J)I", (Func_1<LongValue>) (a) -> i(Long.signum(j(a))));
		METHODS.put("java/lang/Long.expand(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Long.expand(j(a), j(b))));
		METHODS.put("java/lang/Long.compareUnsigned(JJ)I", (Func_2<LongValue, LongValue>) (a, b) -> i(Long.compareUnsigned(j(a), j(b))));
		METHODS.put("java/lang/Long.divideUnsigned(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Long.divideUnsigned(j(a), j(b))));
		METHODS.put("java/lang/Long.remainderUnsigned(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Long.remainderUnsigned(j(a), j(b))));
		METHODS.put("java/lang/Long.reverse(J)J", (Func_1<LongValue>) (a) -> j(Long.reverse(j(a))));
		METHODS.put("java/lang/Long.reverseBytes(J)J", (Func_1<LongValue>) (a) -> j(Long.reverseBytes(j(a))));
		METHODS.put("java/lang/Long.compress(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Long.compress(j(a), j(b))));
		METHODS.put("java/lang/Long.getLong(Ljava/lang/String;Ljava/lang/Long;)Ljava/lang/Long;", (Func_2<StringValue, ObjectValue>) (a, b) -> obj(Long.getLong(str(a), BasicLookupUtils.<Long>obj(b))));
		METHODS.put("java/lang/Long.getLong(Ljava/lang/String;)Ljava/lang/Long;", (Func_1<StringValue>) (a) -> obj(Long.getLong(str(a))));
		METHODS.put("java/lang/Long.getLong(Ljava/lang/String;J)Ljava/lang/Long;", (Func_2<StringValue, LongValue>) (a, b) -> obj(Long.getLong(str(a), j(b))));
		METHODS.put("java/lang/Long.compare(JJ)I", (Func_2<LongValue, LongValue>) (a, b) -> i(Long.compare(j(a), j(b))));
		METHODS.put("java/lang/Long.valueOf(Ljava/lang/String;)Ljava/lang/Long;", (Func_1<StringValue>) (a) -> obj(Long.valueOf(str(a))));
		METHODS.put("java/lang/Long.valueOf(J)Ljava/lang/Long;", (Func_1<LongValue>) (a) -> obj(Long.valueOf(j(a))));
		METHODS.put("java/lang/Long.valueOf(Ljava/lang/String;I)Ljava/lang/Long;", (Func_2<StringValue, IntValue>) (a, b) -> obj(Long.valueOf(str(a), i(b))));
		METHODS.put("java/lang/Long.toHexString(J)Ljava/lang/String;", (Func_1<LongValue>) (a) -> str(Long.toHexString(j(a))));
		METHODS.put("java/lang/Long.decode(Ljava/lang/String;)Ljava/lang/Long;", (Func_1<StringValue>) (a) -> obj(Long.decode(str(a))));
		METHODS.put("java/lang/Long.sum(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Long.sum(j(a), j(b))));
		METHODS.put("java/lang/Long.toUnsignedString(J)Ljava/lang/String;", (Func_1<LongValue>) (a) -> str(Long.toUnsignedString(j(a))));
		METHODS.put("java/lang/Long.toUnsignedString(JI)Ljava/lang/String;", (Func_2<LongValue, IntValue>) (a, b) -> str(Long.toUnsignedString(j(a), i(b))));
		METHODS.put("java/lang/Long.toOctalString(J)Ljava/lang/String;", (Func_1<LongValue>) (a) -> str(Long.toOctalString(j(a))));
		METHODS.put("java/lang/Long.toBinaryString(J)Ljava/lang/String;", (Func_1<LongValue>) (a) -> str(Long.toBinaryString(j(a))));
		METHODS.put("java/lang/Long.highestOneBit(J)J", (Func_1<LongValue>) (a) -> j(Long.highestOneBit(j(a))));
		METHODS.put("java/lang/Long.lowestOneBit(J)J", (Func_1<LongValue>) (a) -> j(Long.lowestOneBit(j(a))));
		METHODS.put("java/lang/Long.rotateLeft(JI)J", (Func_2<LongValue, IntValue>) (a, b) -> j(Long.rotateLeft(j(a), i(b))));
		METHODS.put("java/lang/Long.rotateRight(JI)J", (Func_2<LongValue, IntValue>) (a, b) -> j(Long.rotateRight(j(a), i(b))));
		METHODS.put("java/lang/Long.parseLong(Ljava/lang/CharSequence;III)J", (Func_4<StringValue, IntValue, IntValue, IntValue>) (a, b, c, d) -> j(Long.parseLong(str(a), i(b), i(c), i(d))));
		METHODS.put("java/lang/Long.parseLong(Ljava/lang/String;I)J", (Func_2<StringValue, IntValue>) (a, b) -> j(Long.parseLong(str(a), i(b))));
		METHODS.put("java/lang/Long.parseLong(Ljava/lang/String;)J", (Func_1<StringValue>) (a) -> j(Long.parseLong(str(a))));
		METHODS.put("java/lang/Long.parseUnsignedLong(Ljava/lang/CharSequence;III)J", (Func_4<StringValue, IntValue, IntValue, IntValue>) (a, b, c, d) -> j(Long.parseUnsignedLong(str(a), i(b), i(c), i(d))));
		METHODS.put("java/lang/Long.parseUnsignedLong(Ljava/lang/String;I)J", (Func_2<StringValue, IntValue>) (a, b) -> j(Long.parseUnsignedLong(str(a), i(b))));
		METHODS.put("java/lang/Long.parseUnsignedLong(Ljava/lang/String;)J", (Func_1<StringValue>) (a) -> j(Long.parseUnsignedLong(str(a))));
	}

	private static void floats() {
		METHODS.put("java/lang/Float.toString(F)Ljava/lang/String;", (Func_1<FloatValue>) (a) -> str(Float.toString(f(a))));
		METHODS.put("java/lang/Float.hashCode(F)I", (Func_1<FloatValue>) (a) -> i(Float.hashCode(f(a))));
		METHODS.put("java/lang/Float.min(FF)F", (Func_2<FloatValue, FloatValue>) (a, b) -> f(Float.min(f(a), f(b))));
		METHODS.put("java/lang/Float.max(FF)F", (Func_2<FloatValue, FloatValue>) (a, b) -> f(Float.max(f(a), f(b))));
		METHODS.put("java/lang/Float.isInfinite(F)Z", (Func_1<FloatValue>) (a) -> z(Float.isInfinite(f(a))));
		METHODS.put("java/lang/Float.isFinite(F)Z", (Func_1<FloatValue>) (a) -> z(Float.isFinite(f(a))));
		METHODS.put("java/lang/Float.floatToRawIntBits(F)I", (Func_1<FloatValue>) (a) -> i(Float.floatToRawIntBits(f(a))));
		METHODS.put("java/lang/Float.floatToIntBits(F)I", (Func_1<FloatValue>) (a) -> i(Float.floatToIntBits(f(a))));
		METHODS.put("java/lang/Float.intBitsToFloat(I)F", (Func_1<IntValue>) (a) -> f(Float.intBitsToFloat(i(a))));
		METHODS.put("java/lang/Float.float16ToFloat(S)F", (Func_1<IntValue>) (a) -> f(Float.float16ToFloat(s(a))));
		METHODS.put("java/lang/Float.floatToFloat16(F)S", (Func_1<FloatValue>) (a) -> s(Float.floatToFloat16(f(a))));
		METHODS.put("java/lang/Float.compare(FF)I", (Func_2<FloatValue, FloatValue>) (a, b) -> i(Float.compare(f(a), f(b))));
		METHODS.put("java/lang/Float.toHexString(F)Ljava/lang/String;", (Func_1<FloatValue>) (a) -> str(Float.toHexString(f(a))));
		METHODS.put("java/lang/Float.isNaN(F)Z", (Func_1<FloatValue>) (a) -> z(Float.isNaN(f(a))));
		METHODS.put("java/lang/Float.sum(FF)F", (Func_2<FloatValue, FloatValue>) (a, b) -> f(Float.sum(f(a), f(b))));
		METHODS.put("java/lang/Float.parseFloat(Ljava/lang/String;)F", (Func_1<StringValue>) (a) -> f(Float.parseFloat(str(a))));
	}

	private static void doubles() {
		METHODS.put("java/lang/Double.toString(D)Ljava/lang/String;", (Func_1<DoubleValue>) (a) -> str(Double.toString(d(a))));
		METHODS.put("java/lang/Double.hashCode(D)I", (Func_1<DoubleValue>) (a) -> i(Double.hashCode(d(a))));
		METHODS.put("java/lang/Double.min(DD)D", (Func_2<DoubleValue, DoubleValue>) (a, b) -> d(Double.min(d(a), d(b))));
		METHODS.put("java/lang/Double.max(DD)D", (Func_2<DoubleValue, DoubleValue>) (a, b) -> d(Double.max(d(a), d(b))));
		METHODS.put("java/lang/Double.isInfinite(D)Z", (Func_1<DoubleValue>) (a) -> z(Double.isInfinite(d(a))));
		METHODS.put("java/lang/Double.isFinite(D)Z", (Func_1<DoubleValue>) (a) -> z(Double.isFinite(d(a))));
		METHODS.put("java/lang/Double.doubleToRawLongBits(D)J", (Func_1<DoubleValue>) (a) -> j(Double.doubleToRawLongBits(d(a))));
		METHODS.put("java/lang/Double.doubleToLongBits(D)J", (Func_1<DoubleValue>) (a) -> j(Double.doubleToLongBits(d(a))));
		METHODS.put("java/lang/Double.longBitsToDouble(J)D", (Func_1<LongValue>) (a) -> d(Double.longBitsToDouble(j(a))));
		METHODS.put("java/lang/Double.compare(DD)I", (Func_2<DoubleValue, DoubleValue>) (a, b) -> i(Double.compare(d(a), d(b))));
		METHODS.put("java/lang/Double.toHexString(D)Ljava/lang/String;", (Func_1<DoubleValue>) (a) -> str(Double.toHexString(d(a))));
		METHODS.put("java/lang/Double.isNaN(D)Z", (Func_1<DoubleValue>) (a) -> z(Double.isNaN(d(a))));
		METHODS.put("java/lang/Double.sum(DD)D", (Func_2<DoubleValue, DoubleValue>) (a, b) -> d(Double.sum(d(a), d(b))));
		METHODS.put("java/lang/Double.parseDouble(Ljava/lang/String;)D", (Func_1<StringValue>) (a) -> d(Double.parseDouble(str(a))));
	}

	private static void math() {
		METHODS.put("java/lang/Math.abs(D)D", (Func_1<DoubleValue>) (a) -> d(Math.abs(d(a))));
		METHODS.put("java/lang/Math.abs(F)F", (Func_1<FloatValue>) (a) -> f(Math.abs(f(a))));
		METHODS.put("java/lang/Math.abs(J)J", (Func_1<LongValue>) (a) -> j(Math.abs(j(a))));
		METHODS.put("java/lang/Math.abs(I)I", (Func_1<IntValue>) (a) -> i(Math.abs(i(a))));
		METHODS.put("java/lang/Math.sin(D)D", (Func_1<DoubleValue>) (a) -> d(Math.sin(d(a))));
		METHODS.put("java/lang/Math.cos(D)D", (Func_1<DoubleValue>) (a) -> d(Math.cos(d(a))));
		METHODS.put("java/lang/Math.tan(D)D", (Func_1<DoubleValue>) (a) -> d(Math.tan(d(a))));
		METHODS.put("java/lang/Math.atan2(DD)D", (Func_2<DoubleValue, DoubleValue>) (a, b) -> d(Math.atan2(d(a), d(b))));
		METHODS.put("java/lang/Math.sqrt(D)D", (Func_1<DoubleValue>) (a) -> d(Math.sqrt(d(a))));
		METHODS.put("java/lang/Math.log(D)D", (Func_1<DoubleValue>) (a) -> d(Math.log(d(a))));
		METHODS.put("java/lang/Math.log10(D)D", (Func_1<DoubleValue>) (a) -> d(Math.log10(d(a))));
		METHODS.put("java/lang/Math.pow(DD)D", (Func_2<DoubleValue, DoubleValue>) (a, b) -> d(Math.pow(d(a), d(b))));
		METHODS.put("java/lang/Math.exp(D)D", (Func_1<DoubleValue>) (a) -> d(Math.exp(d(a))));
		METHODS.put("java/lang/Math.min(DD)D", (Func_2<DoubleValue, DoubleValue>) (a, b) -> d(Math.min(d(a), d(b))));
		METHODS.put("java/lang/Math.min(FF)F", (Func_2<FloatValue, FloatValue>) (a, b) -> f(Math.min(f(a), f(b))));
		METHODS.put("java/lang/Math.min(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.min(j(a), j(b))));
		METHODS.put("java/lang/Math.min(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Math.min(i(a), i(b))));
		METHODS.put("java/lang/Math.max(DD)D", (Func_2<DoubleValue, DoubleValue>) (a, b) -> d(Math.max(d(a), d(b))));
		METHODS.put("java/lang/Math.max(FF)F", (Func_2<FloatValue, FloatValue>) (a, b) -> f(Math.max(f(a), f(b))));
		METHODS.put("java/lang/Math.max(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.max(j(a), j(b))));
		METHODS.put("java/lang/Math.max(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Math.max(i(a), i(b))));
		METHODS.put("java/lang/Math.floor(D)D", (Func_1<DoubleValue>) (a) -> d(Math.floor(d(a))));
		METHODS.put("java/lang/Math.ceil(D)D", (Func_1<DoubleValue>) (a) -> d(Math.ceil(d(a))));
		METHODS.put("java/lang/Math.rint(D)D", (Func_1<DoubleValue>) (a) -> d(Math.rint(d(a))));
		METHODS.put("java/lang/Math.round(F)I", (Func_1<FloatValue>) (a) -> i(Math.round(f(a))));
		METHODS.put("java/lang/Math.round(D)J", (Func_1<DoubleValue>) (a) -> j(Math.round(d(a))));
		METHODS.put("java/lang/Math.addExact(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.addExact(j(a), j(b))));
		METHODS.put("java/lang/Math.addExact(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Math.addExact(i(a), i(b))));
		METHODS.put("java/lang/Math.decrementExact(I)I", (Func_1<IntValue>) (a) -> i(Math.decrementExact(i(a))));
		METHODS.put("java/lang/Math.decrementExact(J)J", (Func_1<LongValue>) (a) -> j(Math.decrementExact(j(a))));
		METHODS.put("java/lang/Math.incrementExact(J)J", (Func_1<LongValue>) (a) -> j(Math.incrementExact(j(a))));
		METHODS.put("java/lang/Math.incrementExact(I)I", (Func_1<IntValue>) (a) -> i(Math.incrementExact(i(a))));
		METHODS.put("java/lang/Math.multiplyExact(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.multiplyExact(j(a), j(b))));
		METHODS.put("java/lang/Math.multiplyExact(JI)J", (Func_2<LongValue, IntValue>) (a, b) -> j(Math.multiplyExact(j(a), i(b))));
		METHODS.put("java/lang/Math.multiplyExact(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Math.multiplyExact(i(a), i(b))));
		METHODS.put("java/lang/Math.multiplyHigh(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.multiplyHigh(j(a), j(b))));
		METHODS.put("java/lang/Math.unsignedMultiplyHigh(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.unsignedMultiplyHigh(j(a), j(b))));
		METHODS.put("java/lang/Math.negateExact(I)I", (Func_1<IntValue>) (a) -> i(Math.negateExact(i(a))));
		METHODS.put("java/lang/Math.negateExact(J)J", (Func_1<LongValue>) (a) -> j(Math.negateExact(j(a))));
		METHODS.put("java/lang/Math.subtractExact(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.subtractExact(j(a), j(b))));
		METHODS.put("java/lang/Math.subtractExact(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Math.subtractExact(i(a), i(b))));
		METHODS.put("java/lang/Math.fma(DDD)D", (Func_3<DoubleValue, DoubleValue, DoubleValue>) (a, b, c) -> d(Math.fma(d(a), d(b), d(c))));
		METHODS.put("java/lang/Math.fma(FFF)F", (Func_3<FloatValue, FloatValue, FloatValue>) (a, b, c) -> f(Math.fma(f(a), f(b), f(c))));
		METHODS.put("java/lang/Math.copySign(DD)D", (Func_2<DoubleValue, DoubleValue>) (a, b) -> d(Math.copySign(d(a), d(b))));
		METHODS.put("java/lang/Math.copySign(FF)F", (Func_2<FloatValue, FloatValue>) (a, b) -> f(Math.copySign(f(a), f(b))));
		METHODS.put("java/lang/Math.signum(D)D", (Func_1<DoubleValue>) (a) -> d(Math.signum(d(a))));
		METHODS.put("java/lang/Math.signum(F)F", (Func_1<FloatValue>) (a) -> f(Math.signum(f(a))));
		METHODS.put("java/lang/Math.clamp(FFF)F", (Func_3<FloatValue, FloatValue, FloatValue>) (a, b, c) -> f(Math.clamp(f(a), f(b), f(c))));
		METHODS.put("java/lang/Math.clamp(JJJ)J", (Func_3<LongValue, LongValue, LongValue>) (a, b, c) -> j(Math.clamp(j(a), j(b), j(c))));
		METHODS.put("java/lang/Math.clamp(DDD)D", (Func_3<DoubleValue, DoubleValue, DoubleValue>) (a, b, c) -> d(Math.clamp(d(a), d(b), d(c))));
		METHODS.put("java/lang/Math.clamp(JII)I", (Func_3<LongValue, IntValue, IntValue>) (a, b, c) -> i(Math.clamp(j(a), i(b), i(c))));
		METHODS.put("java/lang/Math.scalb(FI)F", (Func_2<FloatValue, IntValue>) (a, b) -> f(Math.scalb(f(a), i(b))));
		METHODS.put("java/lang/Math.scalb(DI)D", (Func_2<DoubleValue, IntValue>) (a, b) -> d(Math.scalb(d(a), i(b))));
		METHODS.put("java/lang/Math.getExponent(D)I", (Func_1<DoubleValue>) (a) -> i(Math.getExponent(d(a))));
		METHODS.put("java/lang/Math.getExponent(F)I", (Func_1<FloatValue>) (a) -> i(Math.getExponent(f(a))));
		METHODS.put("java/lang/Math.floorMod(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Math.floorMod(i(a), i(b))));
		METHODS.put("java/lang/Math.floorMod(JI)I", (Func_2<LongValue, IntValue>) (a, b) -> i(Math.floorMod(j(a), i(b))));
		METHODS.put("java/lang/Math.floorMod(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.floorMod(j(a), j(b))));
		METHODS.put("java/lang/Math.asin(D)D", (Func_1<DoubleValue>) (a) -> d(Math.asin(d(a))));
		METHODS.put("java/lang/Math.acos(D)D", (Func_1<DoubleValue>) (a) -> d(Math.acos(d(a))));
		METHODS.put("java/lang/Math.atan(D)D", (Func_1<DoubleValue>) (a) -> d(Math.atan(d(a))));
		METHODS.put("java/lang/Math.cbrt(D)D", (Func_1<DoubleValue>) (a) -> d(Math.cbrt(d(a))));
		METHODS.put("java/lang/Math.IEEEremainder(DD)D", (Func_2<DoubleValue, DoubleValue>) (a, b) -> d(Math.IEEEremainder(d(a), d(b))));
		METHODS.put("java/lang/Math.floorDiv(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.floorDiv(j(a), j(b))));
		METHODS.put("java/lang/Math.floorDiv(JI)J", (Func_2<LongValue, IntValue>) (a, b) -> j(Math.floorDiv(j(a), i(b))));
		METHODS.put("java/lang/Math.floorDiv(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Math.floorDiv(i(a), i(b))));
		METHODS.put("java/lang/Math.ceilDiv(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.ceilDiv(j(a), j(b))));
		METHODS.put("java/lang/Math.ceilDiv(JI)J", (Func_2<LongValue, IntValue>) (a, b) -> j(Math.ceilDiv(j(a), i(b))));
		METHODS.put("java/lang/Math.ceilDiv(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Math.ceilDiv(i(a), i(b))));
		METHODS.put("java/lang/Math.ceilMod(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.ceilMod(j(a), j(b))));
		METHODS.put("java/lang/Math.ceilMod(JI)I", (Func_2<LongValue, IntValue>) (a, b) -> i(Math.ceilMod(j(a), i(b))));
		METHODS.put("java/lang/Math.ceilMod(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Math.ceilMod(i(a), i(b))));
		METHODS.put("java/lang/Math.sinh(D)D", (Func_1<DoubleValue>) (a) -> d(Math.sinh(d(a))));
		METHODS.put("java/lang/Math.cosh(D)D", (Func_1<DoubleValue>) (a) -> d(Math.cosh(d(a))));
		METHODS.put("java/lang/Math.tanh(D)D", (Func_1<DoubleValue>) (a) -> d(Math.tanh(d(a))));
		METHODS.put("java/lang/Math.hypot(DD)D", (Func_2<DoubleValue, DoubleValue>) (a, b) -> d(Math.hypot(d(a), d(b))));
		METHODS.put("java/lang/Math.expm1(D)D", (Func_1<DoubleValue>) (a) -> d(Math.expm1(d(a))));
		METHODS.put("java/lang/Math.log1p(D)D", (Func_1<DoubleValue>) (a) -> d(Math.log1p(d(a))));
		METHODS.put("java/lang/Math.toRadians(D)D", (Func_1<DoubleValue>) (a) -> d(Math.toRadians(d(a))));
		METHODS.put("java/lang/Math.toDegrees(D)D", (Func_1<DoubleValue>) (a) -> d(Math.toDegrees(d(a))));
		METHODS.put("java/lang/Math.random()D", (Func_0) () -> d(Math.random()));
		METHODS.put("java/lang/Math.divideExact(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Math.divideExact(i(a), i(b))));
		METHODS.put("java/lang/Math.divideExact(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.divideExact(j(a), j(b))));
		METHODS.put("java/lang/Math.floorDivExact(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.floorDivExact(j(a), j(b))));
		METHODS.put("java/lang/Math.floorDivExact(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Math.floorDivExact(i(a), i(b))));
		METHODS.put("java/lang/Math.ceilDivExact(JJ)J", (Func_2<LongValue, LongValue>) (a, b) -> j(Math.ceilDivExact(j(a), j(b))));
		METHODS.put("java/lang/Math.ceilDivExact(II)I", (Func_2<IntValue, IntValue>) (a, b) -> i(Math.ceilDivExact(i(a), i(b))));
		METHODS.put("java/lang/Math.toIntExact(J)I", (Func_1<LongValue>) (a) -> i(Math.toIntExact(j(a))));
		METHODS.put("java/lang/Math.multiplyFull(II)J", (Func_2<IntValue, IntValue>) (a, b) -> j(Math.multiplyFull(i(a), i(b))));
		METHODS.put("java/lang/Math.absExact(J)J", (Func_1<LongValue>) (a) -> j(Math.absExact(j(a))));
		METHODS.put("java/lang/Math.absExact(I)I", (Func_1<IntValue>) (a) -> i(Math.absExact(i(a))));
		METHODS.put("java/lang/Math.ulp(D)D", (Func_1<DoubleValue>) (a) -> d(Math.ulp(d(a))));
		METHODS.put("java/lang/Math.ulp(F)F", (Func_1<FloatValue>) (a) -> f(Math.ulp(f(a))));
		METHODS.put("java/lang/Math.nextAfter(FD)F", (Func_2<FloatValue, DoubleValue>) (a, b) -> f(Math.nextAfter(f(a), d(b))));
		METHODS.put("java/lang/Math.nextAfter(DD)D", (Func_2<DoubleValue, DoubleValue>) (a, b) -> d(Math.nextAfter(d(a), d(b))));
		METHODS.put("java/lang/Math.nextUp(D)D", (Func_1<DoubleValue>) (a) -> d(Math.nextUp(d(a))));
		METHODS.put("java/lang/Math.nextUp(F)F", (Func_1<FloatValue>) (a) -> f(Math.nextUp(f(a))));
		METHODS.put("java/lang/Math.nextDown(D)D", (Func_1<DoubleValue>) (a) -> d(Math.nextDown(d(a))));
		METHODS.put("java/lang/Math.nextDown(F)F", (Func_1<FloatValue>) (a) -> f(Math.nextDown(f(a))));

		// Duplicate for StrictMath
		new ArrayList<>(METHODS.entrySet()).iterator().forEachRemaining((e) -> {
			String key = e.getKey();
			if (key.startsWith("java/lang/Math.")) {
				METHODS.put(key.replace("/Math.", "/StrictMath."), e.getValue());
			}
		});
	}
}
