package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.util.analysis.gen.InstanceMapperGenerator;
import software.coley.recaf.util.analysis.gen.InstanceMethodInvokeHandlerGenerator;
import software.coley.recaf.util.analysis.gen.InstanceStaticMapperGenerator;
import software.coley.recaf.util.analysis.lookup.BasicLookupUtils;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Factory for creating real instances of supported types and handling method calls on them.
 *
 * @author Matt Coley
 */
public class InstanceFactory extends BasicLookupUtils {
	private final Map<String, InstanceMapper> mappers = new HashMap<>();
	private final Map<String, MethodInvokeHandler<?>> methodHandlers = new HashMap<>();
	private final Set<String> supportedTypes = new HashSet<>();

	/**
	 * Register supported types and method handlers.
	 */
	public InstanceFactory() {
		registerCtorMappers();
		registerStaticMappers();
		registerMethodHandlers();
	}

	/**
	 * @see InstanceMethodInvokeHandlerGenerator
	 */
	@SuppressWarnings("all")
	private void registerMethodHandlers() {
		// java.lang.String
		registerMethodHandler("java/lang/String", "equals", "(Ljava/lang/Object;)Z", (ReValue host, String receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "length", "()I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.length()));
		registerMethodHandler("java/lang/String", "toString", "()Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/String", "hashCode", "()I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/String", "getChars", "(II[CI)V", (ReValue host, String receiver, List<ReValue> args) -> {
			receiver.getChars(i((IntValue) args.get(0)), i((IntValue) args.get(1)), arrc((ArrayValue) args.get(2)), i((IntValue) args.get(3)));
			return null;
		});
		registerMethodHandler("java/lang/String", "compareTo", "(Ljava/lang/String;)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.compareTo(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "indexOf", "(Ljava/lang/String;II)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.indexOf(str((StringValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)))));
		registerMethodHandler("java/lang/String", "indexOf", "(Ljava/lang/String;)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.indexOf(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "indexOf", "(I)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.indexOf(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "indexOf", "(II)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.indexOf(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "indexOf", "(III)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.indexOf(i((IntValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)))));
		registerMethodHandler("java/lang/String", "indexOf", "(Ljava/lang/String;I)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.indexOf(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "charAt", "(I)C", (ReValue host, String receiver, List<ReValue> args) -> c(receiver.charAt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "codePointAt", "(I)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.codePointAt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "codePointBefore", "(I)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.codePointBefore(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "codePointCount", "(II)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.codePointCount(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "offsetByCodePoints", "(II)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.offsetByCodePoints(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "getBytes", "()[B", (ReValue host, String receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.getBytes()));
		registerMethodHandler("java/lang/String", "getBytes", "(Ljava/lang/String;)[B", (ReValue host, String receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.getBytes(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "getBytes", "(II[BI)V", (ReValue host, String receiver, List<ReValue> args) -> {
			receiver.getBytes(i((IntValue) args.get(0)), i((IntValue) args.get(1)), arrb((ArrayValue) args.get(2)), i((IntValue) args.get(3)));
			return null;
		});
		registerMethodHandler("java/lang/String", "contentEquals", "(Ljava/lang/CharSequence;)Z", (ReValue host, String receiver, List<ReValue> args) -> z(receiver.contentEquals(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "regionMatches", "(ZILjava/lang/String;II)Z", (ReValue host, String receiver, List<ReValue> args) -> z(receiver.regionMatches(z((IntValue) args.get(0)), i((IntValue) args.get(1)), str((StringValue) args.get(2)), i((IntValue) args.get(3)), i((IntValue) args.get(4)))));
		registerMethodHandler("java/lang/String", "regionMatches", "(ILjava/lang/String;II)Z", (ReValue host, String receiver, List<ReValue> args) -> z(receiver.regionMatches(i((IntValue) args.get(0)), str((StringValue) args.get(1)), i((IntValue) args.get(2)), i((IntValue) args.get(3)))));
		registerMethodHandler("java/lang/String", "startsWith", "(Ljava/lang/String;)Z", (ReValue host, String receiver, List<ReValue> args) -> z(receiver.startsWith(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "startsWith", "(Ljava/lang/String;I)Z", (ReValue host, String receiver, List<ReValue> args) -> z(receiver.startsWith(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "lastIndexOf", "(Ljava/lang/String;)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.lastIndexOf(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "lastIndexOf", "(II)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.lastIndexOf(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "lastIndexOf", "(Ljava/lang/String;I)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.lastIndexOf(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "lastIndexOf", "(I)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.lastIndexOf(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "substring", "(I)Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.substring(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "substring", "(II)Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.substring(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "isEmpty", "()Z", (ReValue host, String receiver, List<ReValue> args) -> z(receiver.isEmpty()));
		registerMethodHandler("java/lang/String", "replace", "(CC)Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.replace(c((IntValue) args.get(0)), c((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.replace(str((StringValue) args.get(0)), str((StringValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "matches", "(Ljava/lang/String;)Z", (ReValue host, String receiver, List<ReValue> args) -> z(receiver.matches(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "replaceFirst", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.replaceFirst(str((StringValue) args.get(0)), str((StringValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.replaceAll(str((StringValue) args.get(0)), str((StringValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.split(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "split", "(Ljava/lang/String;I)[Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.split(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "splitWithDelimiters", "(Ljava/lang/String;I)[Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.splitWithDelimiters(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "toLowerCase", "()Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.toLowerCase()));
		registerMethodHandler("java/lang/String", "toUpperCase", "()Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.toUpperCase()));
		registerMethodHandler("java/lang/String", "trim", "()Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.trim()));
		registerMethodHandler("java/lang/String", "strip", "()Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.strip()));
		registerMethodHandler("java/lang/String", "stripLeading", "()Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.stripLeading()));
		registerMethodHandler("java/lang/String", "stripTrailing", "()Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.stripTrailing()));
		registerMethodHandler("java/lang/String", "repeat", "(I)Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.repeat(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "isBlank", "()Z", (ReValue host, String receiver, List<ReValue> args) -> z(receiver.isBlank()));
		registerMethodHandler("java/lang/String", "toCharArray", "()[C", (ReValue host, String receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.toCharArray()));
		registerMethodHandler("java/lang/String", "equalsIgnoreCase", "(Ljava/lang/String;)Z", (ReValue host, String receiver, List<ReValue> args) -> z(receiver.equalsIgnoreCase(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "compareToIgnoreCase", "(Ljava/lang/String;)I", (ReValue host, String receiver, List<ReValue> args) -> i(receiver.compareToIgnoreCase(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "endsWith", "(Ljava/lang/String;)Z", (ReValue host, String receiver, List<ReValue> args) -> z(receiver.endsWith(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "subSequence", "(II)Ljava/lang/CharSequence;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.subSequence(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.concat(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", (ReValue host, String receiver, List<ReValue> args) -> z(receiver.contains(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "indent", "(I)Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.indent(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "stripIndent", "()Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.stripIndent()));
		registerMethodHandler("java/lang/String", "translateEscapes", "()Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.translateEscapes()));
		registerMethodHandler("java/lang/String", "formatted", "([Ljava/lang/Object;)Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.formatted(arrobj((ArrayValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "intern", "()Ljava/lang/String;", (ReValue host, String receiver, List<ReValue> args) -> str(receiver.intern()));

		// java.lang.StringBuilder
		registerMethodHandler("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/StringBuilder", "append", "(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(str((StringValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(Ljava/lang/CharSequence;II)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(str((StringValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "([C)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(arrc((ArrayValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "([CII)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(arrc((ArrayValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(objl((ObjectValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(str((StringValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(j((LongValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(F)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(f((FloatValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(D)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(d((DoubleValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(Z)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(z((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(c((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "reverse", "()Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.reverse();
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "compareTo", "(Ljava/lang/StringBuilder;)I", (ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<StringBuilder>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuilder", "indexOf", "(Ljava/lang/String;I)I", (ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.indexOf(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuilder", "indexOf", "(Ljava/lang/String;)I", (ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.indexOf(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuilder", "insert", "(ILjava/lang/CharSequence;)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), str((StringValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(ILjava/lang/String;)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), str((StringValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(I[C)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), arrc((ArrayValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(II)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(ID)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), d((DoubleValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(IF)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), f((FloatValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(IJ)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), j((LongValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(IC)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), c((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(IZ)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), z((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(ILjava/lang/CharSequence;II)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), str((StringValue) args.get(1)), i((IntValue) args.get(2)), i((IntValue) args.get(3)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(ILjava/lang/Object;)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), objl((ObjectValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(I[CII)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), arrc((ArrayValue) args.get(1)), i((IntValue) args.get(2)), i((IntValue) args.get(3)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "lastIndexOf", "(Ljava/lang/String;I)I", (ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.lastIndexOf(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuilder", "lastIndexOf", "(Ljava/lang/String;)I", (ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.lastIndexOf(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuilder", "replace", "(IILjava/lang/String;)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.replace(i((IntValue) args.get(0)), i((IntValue) args.get(1)), str((StringValue) args.get(2)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "repeat", "(Ljava/lang/CharSequence;I)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.repeat(str((StringValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "repeat", "(II)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.repeat(i((IntValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "delete", "(II)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.delete(i((IntValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "appendCodePoint", "(I)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.appendCodePoint(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "deleteCharAt", "(I)Ljava/lang/StringBuilder;", (ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.deleteCharAt(i((IntValue) args.get(0)));
			return host;
		});

		// java.lang.Boolean
		registerMethodHandler("java/lang/Boolean", "equals", "(Ljava/lang/Object;)Z", (ReValue host, Boolean receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Boolean", "toString", "()Ljava/lang/String;", (ReValue host, Boolean receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Boolean", "hashCode", "()I", (ReValue host, Boolean receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Boolean", "compareTo", "(Ljava/lang/Boolean;)I", (ReValue host, Boolean receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Boolean>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Boolean", "booleanValue", "()Z", (ReValue host, Boolean receiver, List<ReValue> args) -> z(receiver.booleanValue()));

		// java.lang.Byte
		registerMethodHandler("java/lang/Byte", "equals", "(Ljava/lang/Object;)Z", (ReValue host, Byte receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Byte", "toString", "()Ljava/lang/String;", (ReValue host, Byte receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Byte", "hashCode", "()I", (ReValue host, Byte receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Byte", "compareTo", "(Ljava/lang/Byte;)I", (ReValue host, Byte receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Byte>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Byte", "byteValue", "()B", (ReValue host, Byte receiver, List<ReValue> args) -> b(receiver.byteValue()));
		registerMethodHandler("java/lang/Byte", "shortValue", "()S", (ReValue host, Byte receiver, List<ReValue> args) -> s(receiver.shortValue()));
		registerMethodHandler("java/lang/Byte", "intValue", "()I", (ReValue host, Byte receiver, List<ReValue> args) -> i(receiver.intValue()));
		registerMethodHandler("java/lang/Byte", "longValue", "()J", (ReValue host, Byte receiver, List<ReValue> args) -> j(receiver.longValue()));
		registerMethodHandler("java/lang/Byte", "floatValue", "()F", (ReValue host, Byte receiver, List<ReValue> args) -> f(receiver.floatValue()));
		registerMethodHandler("java/lang/Byte", "doubleValue", "()D", (ReValue host, Byte receiver, List<ReValue> args) -> d(receiver.doubleValue()));

		// java.lang.Character
		registerMethodHandler("java/lang/Character", "equals", "(Ljava/lang/Object;)Z", (ReValue host, Character receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Character", "toString", "()Ljava/lang/String;", (ReValue host, Character receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Character", "hashCode", "()I", (ReValue host, Character receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Character", "compareTo", "(Ljava/lang/Character;)I", (ReValue host, Character receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Character>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Character", "charValue", "()C", (ReValue host, Character receiver, List<ReValue> args) -> c(receiver.charValue()));

		// java.lang.Short
		registerMethodHandler("java/lang/Short", "equals", "(Ljava/lang/Object;)Z", (ReValue host, Short receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Short", "toString", "()Ljava/lang/String;", (ReValue host, Short receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Short", "hashCode", "()I", (ReValue host, Short receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Short", "compareTo", "(Ljava/lang/Short;)I", (ReValue host, Short receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Short>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Short", "byteValue", "()B", (ReValue host, Short receiver, List<ReValue> args) -> b(receiver.byteValue()));
		registerMethodHandler("java/lang/Short", "shortValue", "()S", (ReValue host, Short receiver, List<ReValue> args) -> s(receiver.shortValue()));
		registerMethodHandler("java/lang/Short", "intValue", "()I", (ReValue host, Short receiver, List<ReValue> args) -> i(receiver.intValue()));
		registerMethodHandler("java/lang/Short", "longValue", "()J", (ReValue host, Short receiver, List<ReValue> args) -> j(receiver.longValue()));
		registerMethodHandler("java/lang/Short", "floatValue", "()F", (ReValue host, Short receiver, List<ReValue> args) -> f(receiver.floatValue()));
		registerMethodHandler("java/lang/Short", "doubleValue", "()D", (ReValue host, Short receiver, List<ReValue> args) -> d(receiver.doubleValue()));

		// java.lang.Integer
		registerMethodHandler("java/lang/Integer", "equals", "(Ljava/lang/Object;)Z", (ReValue host, Integer receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Integer", "toString", "()Ljava/lang/String;", (ReValue host, Integer receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Integer", "hashCode", "()I", (ReValue host, Integer receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Integer", "compareTo", "(Ljava/lang/Integer;)I", (ReValue host, Integer receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Integer>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Integer", "byteValue", "()B", (ReValue host, Integer receiver, List<ReValue> args) -> b(receiver.byteValue()));
		registerMethodHandler("java/lang/Integer", "shortValue", "()S", (ReValue host, Integer receiver, List<ReValue> args) -> s(receiver.shortValue()));
		registerMethodHandler("java/lang/Integer", "intValue", "()I", (ReValue host, Integer receiver, List<ReValue> args) -> i(receiver.intValue()));
		registerMethodHandler("java/lang/Integer", "longValue", "()J", (ReValue host, Integer receiver, List<ReValue> args) -> j(receiver.longValue()));
		registerMethodHandler("java/lang/Integer", "floatValue", "()F", (ReValue host, Integer receiver, List<ReValue> args) -> f(receiver.floatValue()));
		registerMethodHandler("java/lang/Integer", "doubleValue", "()D", (ReValue host, Integer receiver, List<ReValue> args) -> d(receiver.doubleValue()));

		// java.lang.Long
		registerMethodHandler("java/lang/Long", "equals", "(Ljava/lang/Object;)Z", (ReValue host, Long receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Long", "toString", "()Ljava/lang/String;", (ReValue host, Long receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Long", "hashCode", "()I", (ReValue host, Long receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Long", "compareTo", "(Ljava/lang/Long;)I", (ReValue host, Long receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Long>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Long", "byteValue", "()B", (ReValue host, Long receiver, List<ReValue> args) -> b(receiver.byteValue()));
		registerMethodHandler("java/lang/Long", "shortValue", "()S", (ReValue host, Long receiver, List<ReValue> args) -> s(receiver.shortValue()));
		registerMethodHandler("java/lang/Long", "intValue", "()I", (ReValue host, Long receiver, List<ReValue> args) -> i(receiver.intValue()));
		registerMethodHandler("java/lang/Long", "longValue", "()J", (ReValue host, Long receiver, List<ReValue> args) -> j(receiver.longValue()));
		registerMethodHandler("java/lang/Long", "floatValue", "()F", (ReValue host, Long receiver, List<ReValue> args) -> f(receiver.floatValue()));
		registerMethodHandler("java/lang/Long", "doubleValue", "()D", (ReValue host, Long receiver, List<ReValue> args) -> d(receiver.doubleValue()));

		// java.lang.Float
		registerMethodHandler("java/lang/Float", "equals", "(Ljava/lang/Object;)Z", (ReValue host, Float receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Float", "toString", "()Ljava/lang/String;", (ReValue host, Float receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Float", "hashCode", "()I", (ReValue host, Float receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Float", "isInfinite", "()Z", (ReValue host, Float receiver, List<ReValue> args) -> z(receiver.isInfinite()));
		registerMethodHandler("java/lang/Float", "compareTo", "(Ljava/lang/Float;)I", (ReValue host, Float receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Float>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Float", "byteValue", "()B", (ReValue host, Float receiver, List<ReValue> args) -> b(receiver.byteValue()));
		registerMethodHandler("java/lang/Float", "shortValue", "()S", (ReValue host, Float receiver, List<ReValue> args) -> s(receiver.shortValue()));
		registerMethodHandler("java/lang/Float", "intValue", "()I", (ReValue host, Float receiver, List<ReValue> args) -> i(receiver.intValue()));
		registerMethodHandler("java/lang/Float", "longValue", "()J", (ReValue host, Float receiver, List<ReValue> args) -> j(receiver.longValue()));
		registerMethodHandler("java/lang/Float", "floatValue", "()F", (ReValue host, Float receiver, List<ReValue> args) -> f(receiver.floatValue()));
		registerMethodHandler("java/lang/Float", "doubleValue", "()D", (ReValue host, Float receiver, List<ReValue> args) -> d(receiver.doubleValue()));
		registerMethodHandler("java/lang/Float", "isNaN", "()Z", (ReValue host, Float receiver, List<ReValue> args) -> z(receiver.isNaN()));

		// java.lang.Double
		registerMethodHandler("java/lang/Double", "equals", "(Ljava/lang/Object;)Z", (ReValue host, Double receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Double", "toString", "()Ljava/lang/String;", (ReValue host, Double receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Double", "hashCode", "()I", (ReValue host, Double receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Double", "isInfinite", "()Z", (ReValue host, Double receiver, List<ReValue> args) -> z(receiver.isInfinite()));
		registerMethodHandler("java/lang/Double", "compareTo", "(Ljava/lang/Double;)I", (ReValue host, Double receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Double>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Double", "byteValue", "()B", (ReValue host, Double receiver, List<ReValue> args) -> b(receiver.byteValue()));
		registerMethodHandler("java/lang/Double", "shortValue", "()S", (ReValue host, Double receiver, List<ReValue> args) -> s(receiver.shortValue()));
		registerMethodHandler("java/lang/Double", "intValue", "()I", (ReValue host, Double receiver, List<ReValue> args) -> i(receiver.intValue()));
		registerMethodHandler("java/lang/Double", "longValue", "()J", (ReValue host, Double receiver, List<ReValue> args) -> j(receiver.longValue()));
		registerMethodHandler("java/lang/Double", "floatValue", "()F", (ReValue host, Double receiver, List<ReValue> args) -> f(receiver.floatValue()));
		registerMethodHandler("java/lang/Double", "doubleValue", "()D", (ReValue host, Double receiver, List<ReValue> args) -> d(receiver.doubleValue()));
		registerMethodHandler("java/lang/Double", "isNaN", "()Z", (ReValue host, Double receiver, List<ReValue> args) -> z(receiver.isNaN()));

		// java.util.Random
		registerMethodHandler("java/util/Random", "nextDouble", "()D", (ReValue host, Random receiver, List<ReValue> args) -> d(receiver.nextDouble()));
		registerMethodHandler("java/util/Random", "nextInt", "()I", (ReValue host, Random receiver, List<ReValue> args) -> i(receiver.nextInt()));
		registerMethodHandler("java/util/Random", "nextInt", "(I)I", (ReValue host, Random receiver, List<ReValue> args) -> i(receiver.nextInt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/util/Random", "nextBytes", "([B)V", (ReValue host, Random receiver, List<ReValue> args) -> {
			receiver.nextBytes(arrb((ArrayValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/Random", "setSeed", "(J)V", (ReValue host, Random receiver, List<ReValue> args) -> {
			receiver.setSeed(j((LongValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/Random", "nextLong", "()J", (ReValue host, Random receiver, List<ReValue> args) -> j(receiver.nextLong()));
		registerMethodHandler("java/util/Random", "nextBoolean", "()Z", (ReValue host, Random receiver, List<ReValue> args) -> z(receiver.nextBoolean()));
		registerMethodHandler("java/util/Random", "nextFloat", "()F", (ReValue host, Random receiver, List<ReValue> args) -> f(receiver.nextFloat()));
		registerMethodHandler("java/util/Random", "nextGaussian", "()D", (ReValue host, Random receiver, List<ReValue> args) -> d(receiver.nextGaussian()));

		// java.util.List
		registerMethodHandler("java/util/List", "remove", "(I)Ljava/lang/Object;", (ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.remove(i((IntValue) args.get(0)))));
		registerMethodHandler("java/util/List", "remove", "(Ljava/lang/Object;)Z", (ReValue host, List receiver, List<ReValue> args) -> z(receiver.remove(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/List", "size", "()I", (ReValue host, List receiver, List<ReValue> args) -> i(receiver.size()));
		registerMethodHandler("java/util/List", "get", "(I)Ljava/lang/Object;", (ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.get(i((IntValue) args.get(0)))));
		registerMethodHandler("java/util/List", "equals", "(Ljava/lang/Object;)Z", (ReValue host, List receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/List", "hashCode", "()I", (ReValue host, List receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/util/List", "indexOf", "(Ljava/lang/Object;)I", (ReValue host, List receiver, List<ReValue> args) -> i(receiver.indexOf(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/List", "clear", "()V", (ReValue host, List receiver, List<ReValue> args) -> {
			receiver.clear();
			return null;
		});
		registerMethodHandler("java/util/List", "lastIndexOf", "(Ljava/lang/Object;)I", (ReValue host, List receiver, List<ReValue> args) -> i(receiver.lastIndexOf(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/List", "isEmpty", "()Z", (ReValue host, List receiver, List<ReValue> args) -> z(receiver.isEmpty()));
		registerMethodHandler("java/util/List", "add", "(Ljava/lang/Object;)Z", (ReValue host, List receiver, List<ReValue> args) -> z(receiver.add(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/List", "add", "(ILjava/lang/Object;)V", (ReValue host, List receiver, List<ReValue> args) -> {
			receiver.add(i((IntValue) args.get(0)), objl((ObjectValue) args.get(1)));
			return null;
		});
		registerMethodHandler("java/util/List", "subList", "(II)Ljava/util/List;", (ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.subList(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/util/List", "toArray", "()[Ljava/lang/Object;", (ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.toArray()));
		registerMethodHandler("java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", (ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.toArray(arrobj((ArrayValue) args.get(0)))));
		registerMethodHandler("java/util/List", "contains", "(Ljava/lang/Object;)Z", (ReValue host, List receiver, List<ReValue> args) -> z(receiver.contains(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/List", "set", "(ILjava/lang/Object;)Ljava/lang/Object;", (ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.set(i((IntValue) args.get(0)), objl((ObjectValue) args.get(1)))));
		registerMethodHandler("java/util/List", "getFirst", "()Ljava/lang/Object;", (ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.getFirst()));
		registerMethodHandler("java/util/List", "getLast", "()Ljava/lang/Object;", (ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.getLast()));
		registerMethodHandler("java/util/List", "addFirst", "(Ljava/lang/Object;)V", (ReValue host, List receiver, List<ReValue> args) -> {
			receiver.addFirst(objl((ObjectValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/List", "addLast", "(Ljava/lang/Object;)V", (ReValue host, List receiver, List<ReValue> args) -> {
			receiver.addLast(objl((ObjectValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/List", "removeFirst", "()Ljava/lang/Object;", (ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.removeFirst()));
		registerMethodHandler("java/util/List", "removeLast", "()Ljava/lang/Object;", (ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.removeLast()));
		registerMethodHandler("java/util/List", "reversed", "()Ljava/util/List;", (ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.reversed()));

		// java.lang.CharSequence
		registerMethodHandler("java/lang/CharSequence", "length", "()I", (ReValue host, CharSequence receiver, List<ReValue> args) -> i(receiver.length()));
		registerMethodHandler("java/lang/CharSequence", "toString", "()Ljava/lang/String;", (ReValue host, CharSequence receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/CharSequence", "charAt", "(I)C", (ReValue host, CharSequence receiver, List<ReValue> args) -> c(receiver.charAt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/CharSequence", "isEmpty", "()Z", (ReValue host, CharSequence receiver, List<ReValue> args) -> z(receiver.isEmpty()));
		registerMethodHandler("java/lang/CharSequence", "subSequence", "(II)Ljava/lang/CharSequence;", (ReValue host, CharSequence receiver, List<ReValue> args) -> str(receiver.subSequence(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));

		// java.util.ArrayList
		registerMethodHandler("java/util/ArrayList", "remove", "(Ljava/lang/Object;)Z", (ReValue host, ArrayList receiver, List<ReValue> args) -> z(receiver.remove(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "remove", "(I)Ljava/lang/Object;", (ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.remove(i((IntValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "size", "()I", (ReValue host, ArrayList receiver, List<ReValue> args) -> i(receiver.size()));
		registerMethodHandler("java/util/ArrayList", "get", "(I)Ljava/lang/Object;", (ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.get(i((IntValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "equals", "(Ljava/lang/Object;)Z", (ReValue host, ArrayList receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "hashCode", "()I", (ReValue host, ArrayList receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/util/ArrayList", "clone", "()Ljava/lang/Object;", (ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.clone()));
		registerMethodHandler("java/util/ArrayList", "indexOf", "(Ljava/lang/Object;)I", (ReValue host, ArrayList receiver, List<ReValue> args) -> i(receiver.indexOf(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "clear", "()V", (ReValue host, ArrayList receiver, List<ReValue> args) -> {
			receiver.clear();
			return null;
		});
		registerMethodHandler("java/util/ArrayList", "lastIndexOf", "(Ljava/lang/Object;)I", (ReValue host, ArrayList receiver, List<ReValue> args) -> i(receiver.lastIndexOf(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "isEmpty", "()Z", (ReValue host, ArrayList receiver, List<ReValue> args) -> z(receiver.isEmpty()));
		registerMethodHandler("java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", (ReValue host, ArrayList receiver, List<ReValue> args) -> z(receiver.add(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "add", "(ILjava/lang/Object;)V", (ReValue host, ArrayList receiver, List<ReValue> args) -> {
			receiver.add(i((IntValue) args.get(0)), objl((ObjectValue) args.get(1)));
			return null;
		});
		registerMethodHandler("java/util/ArrayList", "subList", "(II)Ljava/util/List;", (ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.subList(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/util/ArrayList", "toArray", "()[Ljava/lang/Object;", (ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.toArray()));
		registerMethodHandler("java/util/ArrayList", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", (ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.toArray(arrobj((ArrayValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "contains", "(Ljava/lang/Object;)Z", (ReValue host, ArrayList receiver, List<ReValue> args) -> z(receiver.contains(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "set", "(ILjava/lang/Object;)Ljava/lang/Object;", (ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.set(i((IntValue) args.get(0)), objl((ObjectValue) args.get(1)))));
		registerMethodHandler("java/util/ArrayList", "ensureCapacity", "(I)V", (ReValue host, ArrayList receiver, List<ReValue> args) -> {
			receiver.ensureCapacity(i((IntValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/ArrayList", "trimToSize", "()V", (ReValue host, ArrayList receiver, List<ReValue> args) -> {
			receiver.trimToSize();
			return null;
		});
		registerMethodHandler("java/util/ArrayList", "getFirst", "()Ljava/lang/Object;", (ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.getFirst()));
		registerMethodHandler("java/util/ArrayList", "getLast", "()Ljava/lang/Object;", (ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.getLast()));
		registerMethodHandler("java/util/ArrayList", "addFirst", "(Ljava/lang/Object;)V", (ReValue host, ArrayList receiver, List<ReValue> args) -> {
			receiver.addFirst(objl((ObjectValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/ArrayList", "addLast", "(Ljava/lang/Object;)V", (ReValue host, ArrayList receiver, List<ReValue> args) -> {
			receiver.addLast(objl((ObjectValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/ArrayList", "removeFirst", "()Ljava/lang/Object;", (ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.removeFirst()));
		registerMethodHandler("java/util/ArrayList", "removeLast", "()Ljava/lang/Object;", (ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.removeLast()));
	}

	/**
	 * @see InstanceMapperGenerator
	 */
	@SuppressWarnings("all")
	private void registerCtorMappers() {
		// java.lang.String
		registerMapper(String.class, "([BLjava/lang/String;)V", (host, parameters) -> new String(arrb((ArrayValue) parameters.get(0)), str((StringValue) parameters.get(1))));
		registerMapper(String.class, "([BII)V", (host, parameters) -> new String(arrb((ArrayValue) parameters.get(0)), i((IntValue) parameters.get(1)), i((IntValue) parameters.get(2))));
		registerMapper(String.class, "([B)V", (host, parameters) -> new String(arrb((ArrayValue) parameters.get(0))));
		registerMapper(String.class, "([BB)V", (host, parameters) -> new String(arrb((ArrayValue) parameters.get(0)), b((IntValue) parameters.get(1))));
		registerMapper(String.class, "([CII)V", (host, parameters) -> new String(arrc((ArrayValue) parameters.get(0)), i((IntValue) parameters.get(1)), i((IntValue) parameters.get(2))));
		registerMapper(String.class, "([C)V", (host, parameters) -> new String(arrc((ArrayValue) parameters.get(0))));
		registerMapper(String.class, "(Ljava/lang/String;)V", (host, parameters) -> new String(str((StringValue) parameters.get(0))));
		registerMapper(String.class, "()V", (host, parameters) -> new String());
		registerMapper(String.class, "([BIILjava/lang/String;)V", (host, parameters) -> new String(arrb((ArrayValue) parameters.get(0)), i((IntValue) parameters.get(1)), i((IntValue) parameters.get(2)), str((StringValue) parameters.get(3))));
		registerMapper(String.class, "([BI)V", (host, parameters) -> new String(arrb((ArrayValue) parameters.get(0)), i((IntValue) parameters.get(1))));
		registerMapper(String.class, "([BIII)V", (host, parameters) -> new String(arrb((ArrayValue) parameters.get(0)), i((IntValue) parameters.get(1)), i((IntValue) parameters.get(2)), i((IntValue) parameters.get(3))));
		registerMapper(String.class, "([III)V", (host, parameters) -> new String(arri((ArrayValue) parameters.get(0)), i((IntValue) parameters.get(1)), i((IntValue) parameters.get(2))));

		// java.lang.StringBuilder
		registerMapper(StringBuilder.class, "(Ljava/lang/CharSequence;)V", (host, parameters) -> new StringBuilder(str((StringValue) parameters.get(0))));
		registerMapper(StringBuilder.class, "(Ljava/lang/String;)V", (host, parameters) -> new StringBuilder(str((StringValue) parameters.get(0))));
		registerMapper(StringBuilder.class, "(I)V", (host, parameters) -> new StringBuilder(i((IntValue) parameters.get(0))));
		registerMapper(StringBuilder.class, "()V", (host, parameters) -> new StringBuilder());

		// java.lang.Boolean
		registerMapper(Boolean.class, "(Z)V", (host, parameters) -> new Boolean(z((IntValue) parameters.get(0))));
		registerMapper(Boolean.class, "(Ljava/lang/String;)V", (host, parameters) -> new Boolean(str((StringValue) parameters.get(0))));

		// java.lang.Byte
		registerMapper(Byte.class, "(B)V", (host, parameters) -> new Byte(b((IntValue) parameters.get(0))));
		registerMapper(Byte.class, "(Ljava/lang/String;)V", (host, parameters) -> new Byte(str((StringValue) parameters.get(0))));

		// java.lang.Character
		registerMapper(Character.class, "(C)V", (host, parameters) -> new Character(c((IntValue) parameters.get(0))));

		// java.lang.Short
		registerMapper(Short.class, "(S)V", (host, parameters) -> new Short(s((IntValue) parameters.get(0))));
		registerMapper(Short.class, "(Ljava/lang/String;)V", (host, parameters) -> new Short(str((StringValue) parameters.get(0))));

		// java.lang.Integer
		registerMapper(Integer.class, "(I)V", (host, parameters) -> new Integer(i((IntValue) parameters.get(0))));
		registerMapper(Integer.class, "(Ljava/lang/String;)V", (host, parameters) -> new Integer(str((StringValue) parameters.get(0))));

		// java.lang.Long
		registerMapper(Long.class, "(Ljava/lang/String;)V", (host, parameters) -> new Long(str((StringValue) parameters.get(0))));
		registerMapper(Long.class, "(J)V", (host, parameters) -> new Long(j((LongValue) parameters.get(0))));

		// java.lang.Float
		registerMapper(Float.class, "(Ljava/lang/String;)V", (host, parameters) -> new Float(str((StringValue) parameters.get(0))));
		registerMapper(Float.class, "(D)V", (host, parameters) -> new Float(d((DoubleValue) parameters.get(0))));
		registerMapper(Float.class, "(F)V", (host, parameters) -> new Float(f((FloatValue) parameters.get(0))));

		// java.lang.Double
		registerMapper(Double.class, "(D)V", (host, parameters) -> new Double(d((DoubleValue) parameters.get(0))));
		registerMapper(Double.class, "(Ljava/lang/String;)V", (host, parameters) -> new Double(str((StringValue) parameters.get(0))));

		// java.util.Random
		registerMapper(Random.class, "(J)V", (host, parameters) -> new Random(j((LongValue) parameters.get(0))));
		registerMapper(Random.class, "()V", (host, parameters) -> new Random(0));

		// java.util.ArrayList
		registerMapper(ArrayList.class, "()V", (host, parameters) -> new ArrayList());
		registerMapper(ArrayList.class, "(I)V", (host, parameters) -> new ArrayList(i((IntValue) parameters.get(0))));
	}

	/**
	 * @see InstanceStaticMapperGenerator
	 */
	private void registerStaticMappers() {
		// TODO: Create mappers for static constructors of supported types
	}

	/**
	 * @param min
	 * 		Method instruction to find a handler for.
	 *
	 * @return Handler for the method instruction, if supported.
	 */
	@Nullable
	public MethodInvokeHandler<?> getMethodHandler(@Nonnull MethodInsnNode min) {
		return methodHandlers.get(min.owner + '.' + min.name + min.desc);
	}

	/**
	 * @param min
	 * 		Method instruction to find a mapper for.
	 *
	 * @return Mapper for the method instruction, if supported.
	 */
	@Nullable
	public InstanceMapper getMapper(@Nonnull MethodInsnNode min) {
		return mappers.get(min.owner + '.' + min.desc);
	}

	/**
	 * @param type
	 * 		Type to check for support.
	 *
	 * @return {@code true} if the type is supported, {@code false} otherwise.
	 */
	public boolean isSupportedType(@Nonnull String type) {
		return supportedTypes.contains(type);
	}

	private void registerMethodHandler(@Nonnull String owner, @Nonnull String name, @Nonnull String desc, @Nonnull MethodInvokeHandler<?> handler) {
		methodHandlers.put(owner + '.' + name + desc, handler);
	}

	private void registerMapper(@Nonnull Class<?> type, @Nonnull String desc, @Nonnull InstanceMapper mapper) {
		String internalName = type.getName().replace('.', '/');
		supportedTypes.add(internalName);
		mappers.put(internalName + '.' + desc, mapper);
	}
}
