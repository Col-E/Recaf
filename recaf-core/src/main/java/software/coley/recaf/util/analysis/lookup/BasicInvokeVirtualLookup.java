package software.coley.recaf.util.analysis.lookup;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Basic implementation of {@link InvokeVirtualLookup} for common static fields.
 * <br>
 * Mostly auto-generated, see {@link BasicLookupGenerator#main(String[])}
 *
 * @author Matt Coley
 */
public class BasicInvokeVirtualLookup extends BasicLookupUtils implements InvokeVirtualLookup {
	private static final Map<String, Func> METHODS = new HashMap<>();
	private static final DebuggingLogger logger = Logging.get(BasicInvokeVirtualLookup.class);

	@Nonnull
	@Override
	public ReValue get(@Nonnull MethodInsnNode method, @Nonnull ReValue context, @Nonnull List<? extends ReValue> values) {
		String key = getKey(method);
		Func func = METHODS.get(key);
		ReValue value = null;
		if (func != null)
			try {
				List<ReValue> params = new ArrayList<>(values.size() + 1);
				params.add(context);
				params.addAll(values);
				value = func.apply(params);
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
		strings();

		// primitives
		booleans();
		bytes();
		chars();
		shorts();
		ints();
		longs();
		floats();
		doubles();
	}

	private static void booleans() {
		METHODS.put("java/lang/Boolean.equals(Ljava/lang/Object;)Z", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> z(BasicLookupUtils.<Boolean>obj(ctx).equals(obj(a))));
		METHODS.put("java/lang/Boolean.toString()Ljava/lang/String;", (Func_1<ObjectValue>) (ctx) -> str(BasicLookupUtils.<Boolean>obj(ctx).toString()));
		METHODS.put("java/lang/Boolean.hashCode()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Boolean>obj(ctx).hashCode()));
		METHODS.put("java/lang/Boolean.compareTo(Ljava/lang/Boolean;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Boolean>obj(ctx).compareTo(BasicLookupUtils.<Boolean>obj(a))));
		METHODS.put("java/lang/Boolean.compareTo(Ljava/lang/Object;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Boolean>obj(ctx).compareTo(obj(a))));
		METHODS.put("java/lang/Boolean.booleanValue()Z", (Func_1<ObjectValue>) (ctx) -> z(BasicLookupUtils.<Boolean>obj(ctx).booleanValue()));
	}

	private static void bytes() {
		METHODS.put("java/lang/Byte.equals(Ljava/lang/Object;)Z", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> z(BasicLookupUtils.<Byte>obj(ctx).equals(obj(a))));
		METHODS.put("java/lang/Byte.toString()Ljava/lang/String;", (Func_1<ObjectValue>) (ctx) -> str(BasicLookupUtils.<Byte>obj(ctx).toString()));
		METHODS.put("java/lang/Byte.hashCode()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Byte>obj(ctx).hashCode()));
		METHODS.put("java/lang/Byte.compareTo(Ljava/lang/Byte;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Byte>obj(ctx).compareTo(BasicLookupUtils.<Byte>obj(a))));
		METHODS.put("java/lang/Byte.compareTo(Ljava/lang/Object;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Byte>obj(ctx).compareTo(obj(a))));
		METHODS.put("java/lang/Byte.byteValue()B", (Func_1<ObjectValue>) (ctx) -> b(BasicLookupUtils.<Byte>obj(ctx).byteValue()));
		METHODS.put("java/lang/Byte.shortValue()S", (Func_1<ObjectValue>) (ctx) -> s(BasicLookupUtils.<Byte>obj(ctx).shortValue()));
		METHODS.put("java/lang/Byte.intValue()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Byte>obj(ctx).intValue()));
		METHODS.put("java/lang/Byte.longValue()J", (Func_1<ObjectValue>) (ctx) -> j(BasicLookupUtils.<Byte>obj(ctx).longValue()));
		METHODS.put("java/lang/Byte.floatValue()F", (Func_1<ObjectValue>) (ctx) -> f(BasicLookupUtils.<Byte>obj(ctx).floatValue()));
		METHODS.put("java/lang/Byte.doubleValue()D", (Func_1<ObjectValue>) (ctx) -> d(BasicLookupUtils.<Byte>obj(ctx).doubleValue()));
	}

	private static void chars() {
		METHODS.put("java/lang/Character.equals(Ljava/lang/Object;)Z", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> z(BasicLookupUtils.<Character>obj(ctx).equals(obj(a))));
		METHODS.put("java/lang/Character.toString()Ljava/lang/String;", (Func_1<ObjectValue>) (ctx) -> str(BasicLookupUtils.<Character>obj(ctx).toString()));
		METHODS.put("java/lang/Character.hashCode()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Character>obj(ctx).hashCode()));
		METHODS.put("java/lang/Character.compareTo(Ljava/lang/Character;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Character>obj(ctx).compareTo(BasicLookupUtils.<Character>obj(a))));
		METHODS.put("java/lang/Character.compareTo(Ljava/lang/Object;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Character>obj(ctx).compareTo(obj(a))));
		METHODS.put("java/lang/Character.charValue()C", (Func_1<ObjectValue>) (ctx) -> c(BasicLookupUtils.<Character>obj(ctx).charValue()));
	}

	private static void shorts() {
		METHODS.put("java/lang/Short.equals(Ljava/lang/Object;)Z", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> z(BasicLookupUtils.<Short>obj(ctx).equals(obj(a))));
		METHODS.put("java/lang/Short.toString()Ljava/lang/String;", (Func_1<ObjectValue>) (ctx) -> str(BasicLookupUtils.<Short>obj(ctx).toString()));
		METHODS.put("java/lang/Short.hashCode()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Short>obj(ctx).hashCode()));
		METHODS.put("java/lang/Short.compareTo(Ljava/lang/Short;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Short>obj(ctx).compareTo(BasicLookupUtils.<Short>obj(a))));
		METHODS.put("java/lang/Short.compareTo(Ljava/lang/Object;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Short>obj(ctx).compareTo(obj(a))));
		METHODS.put("java/lang/Short.byteValue()B", (Func_1<ObjectValue>) (ctx) -> b(BasicLookupUtils.<Short>obj(ctx).byteValue()));
		METHODS.put("java/lang/Short.shortValue()S", (Func_1<ObjectValue>) (ctx) -> s(BasicLookupUtils.<Short>obj(ctx).shortValue()));
		METHODS.put("java/lang/Short.intValue()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Short>obj(ctx).intValue()));
		METHODS.put("java/lang/Short.longValue()J", (Func_1<ObjectValue>) (ctx) -> j(BasicLookupUtils.<Short>obj(ctx).longValue()));
		METHODS.put("java/lang/Short.floatValue()F", (Func_1<ObjectValue>) (ctx) -> f(BasicLookupUtils.<Short>obj(ctx).floatValue()));
		METHODS.put("java/lang/Short.doubleValue()D", (Func_1<ObjectValue>) (ctx) -> d(BasicLookupUtils.<Short>obj(ctx).doubleValue()));
	}

	private static void ints() {
		METHODS.put("java/lang/Integer.equals(Ljava/lang/Object;)Z", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> z(BasicLookupUtils.<Integer>obj(ctx).equals(obj(a))));
		METHODS.put("java/lang/Integer.toString()Ljava/lang/String;", (Func_1<ObjectValue>) (ctx) -> str(BasicLookupUtils.<Integer>obj(ctx).toString()));
		METHODS.put("java/lang/Integer.hashCode()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Integer>obj(ctx).hashCode()));
		METHODS.put("java/lang/Integer.compareTo(Ljava/lang/Object;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Integer>obj(ctx).compareTo(BasicLookupUtils.<Integer>obj(a))));
		METHODS.put("java/lang/Integer.compareTo(Ljava/lang/Integer;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Integer>obj(ctx).compareTo(BasicLookupUtils.<Integer>obj(a))));
		METHODS.put("java/lang/Integer.byteValue()B", (Func_1<ObjectValue>) (ctx) -> b(BasicLookupUtils.<Integer>obj(ctx).byteValue()));
		METHODS.put("java/lang/Integer.shortValue()S", (Func_1<ObjectValue>) (ctx) -> s(BasicLookupUtils.<Integer>obj(ctx).shortValue()));
		METHODS.put("java/lang/Integer.intValue()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Integer>obj(ctx).intValue()));
		METHODS.put("java/lang/Integer.longValue()J", (Func_1<ObjectValue>) (ctx) -> j(BasicLookupUtils.<Integer>obj(ctx).longValue()));
		METHODS.put("java/lang/Integer.floatValue()F", (Func_1<ObjectValue>) (ctx) -> f(BasicLookupUtils.<Integer>obj(ctx).floatValue()));
		METHODS.put("java/lang/Integer.doubleValue()D", (Func_1<ObjectValue>) (ctx) -> d(BasicLookupUtils.<Integer>obj(ctx).doubleValue()));
	}

	private static void longs() {
		METHODS.put("java/lang/Long.equals(Ljava/lang/Object;)Z", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> z(BasicLookupUtils.<Long>obj(ctx).equals(obj(a))));
		METHODS.put("java/lang/Long.toString()Ljava/lang/String;", (Func_1<ObjectValue>) (ctx) -> str(BasicLookupUtils.<Long>obj(ctx).toString()));
		METHODS.put("java/lang/Long.hashCode()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Long>obj(ctx).hashCode()));
		METHODS.put("java/lang/Long.compareTo(Ljava/lang/Object;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Long>obj(ctx).compareTo(obj(a))));
		METHODS.put("java/lang/Long.compareTo(Ljava/lang/Long;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Long>obj(ctx).compareTo(BasicLookupUtils.<Long>obj(a))));
		METHODS.put("java/lang/Long.byteValue()B", (Func_1<ObjectValue>) (ctx) -> b(BasicLookupUtils.<Long>obj(ctx).byteValue()));
		METHODS.put("java/lang/Long.shortValue()S", (Func_1<ObjectValue>) (ctx) -> s(BasicLookupUtils.<Long>obj(ctx).shortValue()));
		METHODS.put("java/lang/Long.intValue()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Long>obj(ctx).intValue()));
		METHODS.put("java/lang/Long.longValue()J", (Func_1<ObjectValue>) (ctx) -> j(BasicLookupUtils.<Long>obj(ctx).longValue()));
		METHODS.put("java/lang/Long.floatValue()F", (Func_1<ObjectValue>) (ctx) -> f(BasicLookupUtils.<Long>obj(ctx).floatValue()));
		METHODS.put("java/lang/Long.doubleValue()D", (Func_1<ObjectValue>) (ctx) -> d(BasicLookupUtils.<Long>obj(ctx).doubleValue()));
	}

	private static void floats() {
		METHODS.put("java/lang/Float.equals(Ljava/lang/Object;)Z", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> z(BasicLookupUtils.<Float>obj(ctx).equals(obj(a))));
		METHODS.put("java/lang/Float.toString()Ljava/lang/String;", (Func_1<ObjectValue>) (ctx) -> str(BasicLookupUtils.<Float>obj(ctx).toString()));
		METHODS.put("java/lang/Float.hashCode()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Float>obj(ctx).hashCode()));
		METHODS.put("java/lang/Float.isInfinite()Z", (Func_1<ObjectValue>) (ctx) -> z(BasicLookupUtils.<Float>obj(ctx).isInfinite()));
		METHODS.put("java/lang/Float.compareTo(Ljava/lang/Object;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Float>obj(ctx).compareTo(obj(a))));
		METHODS.put("java/lang/Float.compareTo(Ljava/lang/Float;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Float>obj(ctx).compareTo(BasicLookupUtils.<Float>obj(a))));
		METHODS.put("java/lang/Float.byteValue()B", (Func_1<ObjectValue>) (ctx) -> b(BasicLookupUtils.<Float>obj(ctx).byteValue()));
		METHODS.put("java/lang/Float.shortValue()S", (Func_1<ObjectValue>) (ctx) -> s(BasicLookupUtils.<Float>obj(ctx).shortValue()));
		METHODS.put("java/lang/Float.intValue()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Float>obj(ctx).intValue()));
		METHODS.put("java/lang/Float.longValue()J", (Func_1<ObjectValue>) (ctx) -> j(BasicLookupUtils.<Float>obj(ctx).longValue()));
		METHODS.put("java/lang/Float.floatValue()F", (Func_1<ObjectValue>) (ctx) -> f(BasicLookupUtils.<Float>obj(ctx).floatValue()));
		METHODS.put("java/lang/Float.doubleValue()D", (Func_1<ObjectValue>) (ctx) -> d(BasicLookupUtils.<Float>obj(ctx).doubleValue()));
		METHODS.put("java/lang/Float.isNaN()Z", (Func_1<ObjectValue>) (ctx) -> z(BasicLookupUtils.<Float>obj(ctx).isNaN()));
	}

	private static void doubles() {
		METHODS.put("java/lang/Double.equals(Ljava/lang/Object;)Z", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> z(BasicLookupUtils.<Double>obj(ctx).equals(obj(a))));
		METHODS.put("java/lang/Double.toString()Ljava/lang/String;", (Func_1<ObjectValue>) (ctx) -> str(BasicLookupUtils.<Double>obj(ctx).toString()));
		METHODS.put("java/lang/Double.hashCode()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Double>obj(ctx).hashCode()));
		METHODS.put("java/lang/Double.isInfinite()Z", (Func_1<ObjectValue>) (ctx) -> z(BasicLookupUtils.<Double>obj(ctx).isInfinite()));
		METHODS.put("java/lang/Double.compareTo(Ljava/lang/Double;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Double>obj(ctx).compareTo(BasicLookupUtils.<Double>obj(a))));
		METHODS.put("java/lang/Double.compareTo(Ljava/lang/Object;)I", (Func_2<ObjectValue, ObjectValue>) (ctx, a) -> i(BasicLookupUtils.<Double>obj(ctx).compareTo(BasicLookupUtils.<Double>obj(a))));
		METHODS.put("java/lang/Double.byteValue()B", (Func_1<ObjectValue>) (ctx) -> b(BasicLookupUtils.<Double>obj(ctx).byteValue()));
		METHODS.put("java/lang/Double.shortValue()S", (Func_1<ObjectValue>) (ctx) -> s(BasicLookupUtils.<Double>obj(ctx).shortValue()));
		METHODS.put("java/lang/Double.intValue()I", (Func_1<ObjectValue>) (ctx) -> i(BasicLookupUtils.<Double>obj(ctx).intValue()));
		METHODS.put("java/lang/Double.longValue()J", (Func_1<ObjectValue>) (ctx) -> j(BasicLookupUtils.<Double>obj(ctx).longValue()));
		METHODS.put("java/lang/Double.floatValue()F", (Func_1<ObjectValue>) (ctx) -> f(BasicLookupUtils.<Double>obj(ctx).floatValue()));
		METHODS.put("java/lang/Double.doubleValue()D", (Func_1<ObjectValue>) (ctx) -> d(BasicLookupUtils.<Double>obj(ctx).doubleValue()));
		METHODS.put("java/lang/Double.isNaN()Z", (Func_1<ObjectValue>) (ctx) -> z(BasicLookupUtils.<Double>obj(ctx).isNaN()));
	}

	private static void strings() {
		METHODS.put("java/lang/String.equals(Ljava/lang/Object;)Z", (Func_2<StringValue, ObjectValue>) (ctx, a) -> z(str(ctx).equals(obj(a))));
		METHODS.put("java/lang/String.length()I", (Func_1<StringValue>) (ctx) -> i(str(ctx).length()));
		METHODS.put("java/lang/String.toString()Ljava/lang/String;", (Func_1<StringValue>) ctx -> ctx);
		METHODS.put("java/lang/String.hashCode()I", (Func_1<StringValue>) (ctx) -> i(str(ctx).hashCode()));
		METHODS.put("java/lang/String.compareTo(Ljava/lang/Object;)I", (Func_2<StringValue, ObjectValue>) (ctx, a) -> {
			if (a instanceof StringValue as)
				return i(str(ctx).compareTo(str(as)));
			throw new IllegalArgumentException();
		});
		METHODS.put("java/lang/String.compareTo(Ljava/lang/String;)I", (Func_2<StringValue, StringValue>) (ctx, a) -> i(str(ctx).compareTo(str(a))));
		METHODS.put("java/lang/String.indexOf(Ljava/lang/String;II)I", (Func_4<StringValue, StringValue, IntValue, IntValue>) (ctx, a, b, c) -> i(str(ctx).indexOf(str(a), i(b), i(c))));
		METHODS.put("java/lang/String.indexOf(Ljava/lang/String;)I", (Func_2<StringValue, StringValue>) (ctx, a) -> i(str(ctx).indexOf(str(a))));
		METHODS.put("java/lang/String.indexOf(I)I", (Func_2<StringValue, IntValue>) (ctx, a) -> i(str(ctx).indexOf(i(a))));
		METHODS.put("java/lang/String.indexOf(II)I", (Func_3<StringValue, IntValue, IntValue>) (ctx, a, b) -> i(str(ctx).indexOf(i(a), i(b))));
		METHODS.put("java/lang/String.indexOf(III)I", (Func_4<StringValue, IntValue, IntValue, IntValue>) (ctx, a, b, c) -> i(str(ctx).indexOf(i(a), i(b), i(c))));
		METHODS.put("java/lang/String.indexOf(Ljava/lang/String;I)I", (Func_3<StringValue, StringValue, IntValue>) (ctx, a, b) -> i(str(ctx).indexOf(str(a), i(b))));
		METHODS.put("java/lang/String.charAt(I)C", (Func_2<StringValue, IntValue>) (ctx, a) -> c(str(ctx).charAt(i(a))));
		METHODS.put("java/lang/String.codePointAt(I)I", (Func_2<StringValue, IntValue>) (ctx, a) -> i(str(ctx).codePointAt(i(a))));
		METHODS.put("java/lang/String.codePointBefore(I)I", (Func_2<StringValue, IntValue>) (ctx, a) -> i(str(ctx).codePointBefore(i(a))));
		METHODS.put("java/lang/String.codePointCount(II)I", (Func_3<StringValue, IntValue, IntValue>) (ctx, a, b) -> i(str(ctx).codePointCount(i(a), i(b))));
		METHODS.put("java/lang/String.offsetByCodePoints(II)I", (Func_3<StringValue, IntValue, IntValue>) (ctx, a, b) -> i(str(ctx).offsetByCodePoints(i(a), i(b))));
		METHODS.put("java/lang/String.getBytes()[B", (Func_1<StringValue>) (ctx) -> arrb(str(ctx).getBytes()));
		METHODS.put("java/lang/String.getBytes(Ljava/lang/String;)[B", (Func_2<StringValue, StringValue>) (ctx, a) -> {
			try {
				return arrb(str(ctx).getBytes(str(a)));
			} catch (UnsupportedEncodingException e) {
				return ArrayValue.VAL_BYTES;
			}
		});
		METHODS.put("java/lang/String.contentEquals(Ljava/lang/CharSequence;)Z", (Func_2<StringValue, StringValue>) (ctx, a) -> z(str(ctx).contentEquals(str(a))));
		METHODS.put("java/lang/String.regionMatches(ZILjava/lang/String;II)Z", (Func_6<StringValue, IntValue, IntValue, StringValue, IntValue, IntValue>) (ctx, a, b, c, d, e) -> z(str(ctx).regionMatches(z(a), i(b), str(c), i(d), i(e))));
		METHODS.put("java/lang/String.regionMatches(ILjava/lang/String;II)Z", (Func_5<StringValue, IntValue, StringValue, IntValue, IntValue>) (ctx, a, b, c, d) -> z(str(ctx).regionMatches(i(a), str(b), i(c), i(d))));
		METHODS.put("java/lang/String.startsWith(Ljava/lang/String;)Z", (Func_2<StringValue, StringValue>) (ctx, a) -> z(str(ctx).startsWith(str(a))));
		METHODS.put("java/lang/String.startsWith(Ljava/lang/String;I)Z", (Func_3<StringValue, StringValue, IntValue>) (ctx, a, b) -> z(str(ctx).startsWith(str(a), i(b))));
		METHODS.put("java/lang/String.lastIndexOf(Ljava/lang/String;)I", (Func_2<StringValue, StringValue>) (ctx, a) -> i(str(ctx).lastIndexOf(str(a))));
		METHODS.put("java/lang/String.lastIndexOf(II)I", (Func_3<StringValue, IntValue, IntValue>) (ctx, a, b) -> i(str(ctx).lastIndexOf(i(a), i(b))));
		METHODS.put("java/lang/String.lastIndexOf(Ljava/lang/String;I)I", (Func_3<StringValue, StringValue, IntValue>) (ctx, a, b) -> i(str(ctx).lastIndexOf(str(a), i(b))));
		METHODS.put("java/lang/String.lastIndexOf(I)I", (Func_2<StringValue, IntValue>) (ctx, a) -> i(str(ctx).lastIndexOf(i(a))));
		METHODS.put("java/lang/String.substring(I)Ljava/lang/String;", (Func_2<StringValue, IntValue>) (ctx, a) -> str(str(ctx).substring(i(a))));
		METHODS.put("java/lang/String.substring(II)Ljava/lang/String;", (Func_3<StringValue, IntValue, IntValue>) (ctx, a, b) -> str(str(ctx).substring(i(a), i(b))));
		METHODS.put("java/lang/String.isEmpty()Z", (Func_1<StringValue>) (ctx) -> z(str(ctx).isEmpty()));
		METHODS.put("java/lang/String.replace(CC)Ljava/lang/String;", (Func_3<StringValue, IntValue, IntValue>) (ctx, a, b) -> str(str(ctx).replace(c(a), c(b))));
		METHODS.put("java/lang/String.replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", (Func_3<StringValue, StringValue, StringValue>) (ctx, a, b) -> str(str(ctx).replace(str(a), str(b))));
		METHODS.put("java/lang/String.matches(Ljava/lang/String;)Z", (Func_2<StringValue, StringValue>) (ctx, a) -> z(str(ctx).matches(str(a))));
		METHODS.put("java/lang/String.replaceFirst(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (Func_3<StringValue, StringValue, StringValue>) (ctx, a, b) -> str(str(ctx).replaceFirst(str(a), str(b))));
		METHODS.put("java/lang/String.replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (Func_3<StringValue, StringValue, StringValue>) (ctx, a, b) -> str(str(ctx).replaceAll(str(a), str(b))));
		METHODS.put("java/lang/String.split(Ljava/lang/String;)[Ljava/lang/String;", (Func_2<StringValue, StringValue>) (ctx, a) -> arrstr(str(ctx).split(str(a))));
		METHODS.put("java/lang/String.split(Ljava/lang/String;I)[Ljava/lang/String;", (Func_3<StringValue, StringValue, IntValue>) (ctx, a, b) -> arrstr(str(ctx).split(str(a), i(b))));
		METHODS.put("java/lang/String.splitWithDelimiters(Ljava/lang/String;I)[Ljava/lang/String;", (Func_3<StringValue, StringValue, IntValue>) (ctx, a, b) -> arrstr(str(ctx).splitWithDelimiters(str(a), i(b))));
		METHODS.put("java/lang/String.toLowerCase()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).toLowerCase()));
		METHODS.put("java/lang/String.toUpperCase()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).toUpperCase()));
		METHODS.put("java/lang/String.trim()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).trim()));
		METHODS.put("java/lang/String.strip()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).strip()));
		METHODS.put("java/lang/String.stripLeading()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).stripLeading()));
		METHODS.put("java/lang/String.stripTrailing()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).stripTrailing()));
		METHODS.put("java/lang/String.repeat(I)Ljava/lang/String;", (Func_2<StringValue, IntValue>) (ctx, a) -> str(str(ctx).repeat(i(a))));
		METHODS.put("java/lang/String.isBlank()Z", (Func_1<StringValue>) (ctx) -> z(str(ctx).isBlank()));
		METHODS.put("java/lang/String.toCharArray()[C", (Func_1<StringValue>) (ctx) -> arrc(str(ctx).toCharArray()));
		METHODS.put("java/lang/String.equalsIgnoreCase(Ljava/lang/String;)Z", (Func_2<StringValue, StringValue>) (ctx, a) -> z(str(ctx).equalsIgnoreCase(str(a))));
		METHODS.put("java/lang/String.compareToIgnoreCase(Ljava/lang/String;)I", (Func_2<StringValue, StringValue>) (ctx, a) -> i(str(ctx).compareToIgnoreCase(str(a))));
		METHODS.put("java/lang/String.endsWith(Ljava/lang/String;)Z", (Func_2<StringValue, StringValue>) (ctx, a) -> z(str(ctx).endsWith(str(a))));
		METHODS.put("java/lang/String.subSequence(II)Ljava/lang/CharSequence;", (Func_3<StringValue, IntValue, IntValue>) (ctx, a, b) -> str(str(ctx).subSequence(i(a), i(b))));
		METHODS.put("java/lang/String.concat(Ljava/lang/String;)Ljava/lang/String;", (Func_2<StringValue, StringValue>) (ctx, a) -> str(str(ctx).concat(str(a))));
		METHODS.put("java/lang/String.contains(Ljava/lang/CharSequence;)Z", (Func_2<StringValue, StringValue>) (ctx, a) -> z(str(ctx).contains(str(a))));
		METHODS.put("java/lang/String.indent(I)Ljava/lang/String;", (Func_2<StringValue, IntValue>) (ctx, a) -> str(str(ctx).indent(i(a))));
		METHODS.put("java/lang/String.stripIndent()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).stripIndent()));
		METHODS.put("java/lang/String.translateEscapes()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).translateEscapes()));
		METHODS.put("java/lang/String.formatted([Ljava/lang/Object;)Ljava/lang/String;", (Func_2<StringValue, ArrayValue>) (ctx, a) -> str(str(ctx).formatted(arrobj(a))));
		METHODS.put("java/lang/String.intern()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).intern()));
		METHODS.put("java/lang/CharSequence.length()I", (Func_1<StringValue>) (ctx) -> i(str(ctx).length()));
		METHODS.put("java/lang/CharSequence.toString()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx)));
		METHODS.put("java/lang/CharSequence.charAt(I)C", (Func_2<StringValue, IntValue>) (ctx, a) -> c(str(ctx).charAt(i(a))));
		METHODS.put("java/lang/CharSequence.isEmpty()Z", (Func_1<StringValue>) (ctx) -> z(str(ctx).isEmpty()));
		METHODS.put("java/lang/CharSequence.subSequence(II)Ljava/lang/CharSequence;", (Func_3<StringValue, IntValue, IntValue>) (ctx, a, b) -> str(str(ctx).subSequence(i(a), i(b))));
	}
}
