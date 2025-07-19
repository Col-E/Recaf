package software.coley.recaf.util.analysis.lookup;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;

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
		String key = method.owner + "." + method.name + method.desc;
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

	static {
		strings();
	}

	private static void strings() {
		METHODS.put("java/lang/String.length()I", (Func_1<StringValue>) (ctx) -> i(str(ctx).length()));
		METHODS.put("java/lang/String.toString()Ljava/lang/String;", (Func_1<StringValue>) ctx -> ctx);
		METHODS.put("java/lang/String.hashCode()I", (Func_1<StringValue>) (ctx) -> i(str(ctx).hashCode()));
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
		//METHODS.put("java/lang/String.getBytes()[B", (Func_1<StringValue>) (ctx) -> arr(str(ctx).getBytes()));
		//METHODS.put("java/lang/String.getBytes(Ljava/lang/String;)[B", (Func_2<StringValue, StringValue>) (ctx, a) -> {
		//	try {
		//		return arr(str(ctx).getBytes(str(a)));
		//	} catch (UnsupportedEncodingException e) {
		//		return null;
		//	}
		//});
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
		METHODS.put("java/lang/String.matches(Ljava/lang/String;)Z", (Func_2<StringValue, StringValue>) (ctx, a) -> z(str(ctx).matches(str(a))));
		METHODS.put("java/lang/String.replaceFirst(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (Func_3<StringValue, StringValue, StringValue>) (ctx, a, b) -> str(str(ctx).replaceFirst(str(a), str(b))));
		METHODS.put("java/lang/String.replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (Func_3<StringValue, StringValue, StringValue>) (ctx, a, b) -> str(str(ctx).replaceAll(str(a), str(b))));
		//METHODS.put("java/lang/String.split(Ljava/lang/String;)[Ljava/lang/String;", (Func_2<StringValue, StringValue>) (ctx, a) -> arr(str(ctx).split(str(a))));
		//METHODS.put("java/lang/String.split(Ljava/lang/String;I)[Ljava/lang/String;", (Func_3<StringValue, StringValue, IntValue>) (ctx, a, b) -> arr(str(ctx).split(str(a), i(b))));
		METHODS.put("java/lang/String.splitWithDelimiters(Ljava/lang/String;I)[Ljava/lang/String;", (Func_3<StringValue, StringValue, IntValue>) (ctx, a, b) -> arr(str(ctx).splitWithDelimiters(str(a), i(b))));
		METHODS.put("java/lang/String.toLowerCase()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).toLowerCase()));
		METHODS.put("java/lang/String.toUpperCase()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).toUpperCase()));
		METHODS.put("java/lang/String.trim()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).trim()));
		METHODS.put("java/lang/String.strip()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).strip()));
		METHODS.put("java/lang/String.stripLeading()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).stripLeading()));
		METHODS.put("java/lang/String.stripTrailing()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).stripTrailing()));
		METHODS.put("java/lang/String.repeat(I)Ljava/lang/String;", (Func_2<StringValue, IntValue>) (ctx, a) -> str(str(ctx).repeat(i(a))));
		METHODS.put("java/lang/String.isBlank()Z", (Func_1<StringValue>) (ctx) -> z(str(ctx).isBlank()));
		//METHODS.put("java/lang/String.toCharArray()[C", (Func_1<StringValue>) (ctx) -> arr(str(ctx).toCharArray()));
		METHODS.put("java/lang/String.equalsIgnoreCase(Ljava/lang/String;)Z", (Func_2<StringValue, StringValue>) (ctx, a) -> z(str(ctx).equalsIgnoreCase(str(a))));
		METHODS.put("java/lang/String.compareToIgnoreCase(Ljava/lang/String;)I", (Func_2<StringValue, StringValue>) (ctx, a) -> i(str(ctx).compareToIgnoreCase(str(a))));
		METHODS.put("java/lang/String.endsWith(Ljava/lang/String;)Z", (Func_2<StringValue, StringValue>) (ctx, a) -> z(str(ctx).endsWith(str(a))));
		METHODS.put("java/lang/String.concat(Ljava/lang/String;)Ljava/lang/String;", (Func_2<StringValue, StringValue>) (ctx, a) -> str(str(ctx).concat(str(a))));
		METHODS.put("java/lang/String.indent(I)Ljava/lang/String;", (Func_2<StringValue, IntValue>) (ctx, a) -> str(str(ctx).indent(i(a))));
		METHODS.put("java/lang/String.stripIndent()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).stripIndent()));
		METHODS.put("java/lang/String.translateEscapes()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).translateEscapes()));
		METHODS.put("java/lang/String.intern()Ljava/lang/String;", (Func_1<StringValue>) (ctx) -> str(str(ctx).intern()));
	}
}
