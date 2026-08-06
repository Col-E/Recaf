package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.util.analysis.ReFrame;
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
import software.coley.recaf.util.analysis.value.impl.ArrayValueImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
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
		registerMethodHandler("java/lang/String", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "length", "()I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.length()));
		registerMethodHandler("java/lang/String", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/String", "hashCode", "()I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/String", "getChars", "(II[CI)V", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> {
			receiver.getChars(i((IntValue) args.get(0)), i((IntValue) args.get(1)), arrc((ArrayValue) args.get(2)), i((IntValue) args.get(3)));
			return null;
		});
		registerMethodHandler("java/lang/String", "compareTo", "(Ljava/lang/String;)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.compareTo(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "indexOf", "(Ljava/lang/String;II)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.indexOf(str((StringValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)))));
		registerMethodHandler("java/lang/String", "indexOf", "(Ljava/lang/String;)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.indexOf(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "indexOf", "(I)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.indexOf(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "indexOf", "(II)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.indexOf(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "indexOf", "(III)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.indexOf(i((IntValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)))));
		registerMethodHandler("java/lang/String", "indexOf", "(Ljava/lang/String;I)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.indexOf(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "charAt", "(I)C", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> c(receiver.charAt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "codePointAt", "(I)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.codePointAt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "codePointBefore", "(I)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.codePointBefore(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "codePointCount", "(II)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.codePointCount(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "offsetByCodePoints", "(II)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.offsetByCodePoints(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "getBytes", "()[B", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> arrb(receiver.getBytes()));
		registerMethodHandler("java/lang/String", "getBytes", "(Ljava/lang/String;)[B", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> arrb(receiver.getBytes(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "getBytes", "(II[BI)V", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> {
			receiver.getBytes(i((IntValue) args.get(0)), i((IntValue) args.get(1)), arrb((ArrayValue) args.get(2)), i((IntValue) args.get(3)));
			return null;
		});
		registerMethodHandler("java/lang/String", "contentEquals", "(Ljava/lang/CharSequence;)Z", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> z(receiver.contentEquals(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "regionMatches", "(ZILjava/lang/String;II)Z", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> z(receiver.regionMatches(z((IntValue) args.get(0)), i((IntValue) args.get(1)), str((StringValue) args.get(2)), i((IntValue) args.get(3)), i((IntValue) args.get(4)))));
		registerMethodHandler("java/lang/String", "regionMatches", "(ILjava/lang/String;II)Z", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> z(receiver.regionMatches(i((IntValue) args.get(0)), str((StringValue) args.get(1)), i((IntValue) args.get(2)), i((IntValue) args.get(3)))));
		registerMethodHandler("java/lang/String", "startsWith", "(Ljava/lang/String;)Z", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> z(receiver.startsWith(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "startsWith", "(Ljava/lang/String;I)Z", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> z(receiver.startsWith(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "lastIndexOf", "(Ljava/lang/String;)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.lastIndexOf(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "lastIndexOf", "(II)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.lastIndexOf(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "lastIndexOf", "(Ljava/lang/String;I)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.lastIndexOf(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "lastIndexOf", "(I)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.lastIndexOf(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "substring", "(I)Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.substring(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "substring", "(II)Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.substring(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "isEmpty", "()Z", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> z(receiver.isEmpty()));
		registerMethodHandler("java/lang/String", "replace", "(CC)Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.replace(c((IntValue) args.get(0)), c((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.replace(str((StringValue) args.get(0)), str((StringValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "matches", "(Ljava/lang/String;)Z", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> z(receiver.matches(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "replaceFirst", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.replaceFirst(str((StringValue) args.get(0)), str((StringValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.replaceAll(str((StringValue) args.get(0)), str((StringValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.split(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "split", "(Ljava/lang/String;I)[Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.split(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "splitWithDelimiters", "(Ljava/lang/String;I)[Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.splitWithDelimiters(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "toLowerCase", "()Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.toLowerCase()));
		registerMethodHandler("java/lang/String", "toUpperCase", "()Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.toUpperCase()));
		registerMethodHandler("java/lang/String", "trim", "()Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.trim()));
		registerMethodHandler("java/lang/String", "strip", "()Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.strip()));
		registerMethodHandler("java/lang/String", "stripLeading", "()Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.stripLeading()));
		registerMethodHandler("java/lang/String", "stripTrailing", "()Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.stripTrailing()));
		registerMethodHandler("java/lang/String", "repeat", "(I)Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.repeat(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "isBlank", "()Z", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> z(receiver.isBlank()));
		registerMethodHandler("java/lang/String", "toCharArray", "()[C", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.toCharArray()));
		registerMethodHandler("java/lang/String", "equalsIgnoreCase", "(Ljava/lang/String;)Z", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> z(receiver.equalsIgnoreCase(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "compareToIgnoreCase", "(Ljava/lang/String;)I", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> i(receiver.compareToIgnoreCase(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "endsWith", "(Ljava/lang/String;)Z", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> z(receiver.endsWith(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "subSequence", "(II)Ljava/lang/CharSequence;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.subSequence(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.concat(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> z(receiver.contains(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "indent", "(I)Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.indent(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "stripIndent", "()Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.stripIndent()));
		registerMethodHandler("java/lang/String", "translateEscapes", "()Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.translateEscapes()));
		registerMethodHandler("java/lang/String", "formatted", "([Ljava/lang/Object;)Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.formatted(arrobj((ArrayValue) args.get(0)))));
		registerMethodHandler("java/lang/String", "intern", "()Ljava/lang/String;", (ReFrame frame, ReValue host, String receiver, List<ReValue> args) -> str(receiver.intern()));

		// java.lang.StackTraceElement
		registerMethodHandler("java/lang/StackTraceElement", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, StackTraceElement receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/StackTraceElement", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, StackTraceElement receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/StackTraceElement", "hashCode", "()I", (ReFrame frame, ReValue host, StackTraceElement receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;", (ReFrame frame, ReValue host, StackTraceElement receiver, List<ReValue> args) -> str(receiver.getClassName()));
		registerMethodHandler("java/lang/StackTraceElement", "isNativeMethod", "()Z", (ReFrame frame, ReValue host, StackTraceElement receiver, List<ReValue> args) -> z(receiver.isNativeMethod()));
		registerMethodHandler("java/lang/StackTraceElement", "getFileName", "()Ljava/lang/String;", (ReFrame frame, ReValue host, StackTraceElement receiver, List<ReValue> args) -> str(receiver.getFileName()));
		registerMethodHandler("java/lang/StackTraceElement", "getLineNumber", "()I", (ReFrame frame, ReValue host, StackTraceElement receiver, List<ReValue> args) -> i(receiver.getLineNumber()));
		registerMethodHandler("java/lang/StackTraceElement", "getModuleName", "()Ljava/lang/String;", (ReFrame frame, ReValue host, StackTraceElement receiver, List<ReValue> args) -> str(receiver.getModuleName()));
		registerMethodHandler("java/lang/StackTraceElement", "getModuleVersion", "()Ljava/lang/String;", (ReFrame frame, ReValue host, StackTraceElement receiver, List<ReValue> args) -> str(receiver.getModuleVersion()));
		registerMethodHandler("java/lang/StackTraceElement", "getClassLoaderName", "()Ljava/lang/String;", (ReFrame frame, ReValue host, StackTraceElement receiver, List<ReValue> args) -> str(receiver.getClassLoaderName()));
		registerMethodHandler("java/lang/StackTraceElement", "getMethodName", "()Ljava/lang/String;", (ReFrame frame, ReValue host, StackTraceElement receiver, List<ReValue> args) -> str(receiver.getMethodName()));

		// java.lang.StringBuilder
		registerMethodHandler("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/StringBuilder", "append", "(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(str((StringValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(Ljava/lang/CharSequence;II)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(str((StringValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "([C)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(arrc((ArrayValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "([CII)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(arrc((ArrayValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(objl((ObjectValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(str((StringValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(j((LongValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(F)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(f((FloatValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(D)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(d((DoubleValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(Z)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(z((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(c((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.append(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "reverse", "()Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.reverse();
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "compareTo", "(Ljava/lang/StringBuilder;)I", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<StringBuilder>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuilder", "indexOf", "(Ljava/lang/String;I)I", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.indexOf(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuilder", "indexOf", "(Ljava/lang/String;)I", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.indexOf(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuilder", "insert", "(ILjava/lang/CharSequence;)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), str((StringValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(ILjava/lang/String;)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), str((StringValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(I[C)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), arrc((ArrayValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(II)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(ID)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), d((DoubleValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(IF)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), f((FloatValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(IJ)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), j((LongValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(IC)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), c((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(IZ)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), z((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(ILjava/lang/CharSequence;II)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), str((StringValue) args.get(1)), i((IntValue) args.get(2)), i((IntValue) args.get(3)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(ILjava/lang/Object;)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), objl((ObjectValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "insert", "(I[CII)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), arrc((ArrayValue) args.get(1)), i((IntValue) args.get(2)), i((IntValue) args.get(3)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "lastIndexOf", "(Ljava/lang/String;I)I", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.lastIndexOf(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuilder", "lastIndexOf", "(Ljava/lang/String;)I", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.lastIndexOf(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuilder", "replace", "(IILjava/lang/String;)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.replace(i((IntValue) args.get(0)), i((IntValue) args.get(1)), str((StringValue) args.get(2)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "repeat", "(Ljava/lang/CharSequence;I)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.repeat(str((StringValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "repeat", "(II)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.repeat(i((IntValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "delete", "(II)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.delete(i((IntValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "appendCodePoint", "(I)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.appendCodePoint(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "deleteCharAt", "(I)Ljava/lang/StringBuilder;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.deleteCharAt(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuilder", "length", "()I", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.length()));
		registerMethodHandler("java/lang/StringBuilder", "getChars", "(II[CI)V", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.getChars(i((IntValue) args.get(0)), i((IntValue) args.get(1)), arrc((ArrayValue) args.get(2)), i((IntValue) args.get(3)));
			return null;
		});
		registerMethodHandler("java/lang/StringBuilder", "charAt", "(I)C", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> c(receiver.charAt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuilder", "codePointAt", "(I)I", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.codePointAt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuilder", "codePointBefore", "(I)I", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.codePointBefore(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuilder", "codePointCount", "(II)I", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.codePointCount(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuilder", "offsetByCodePoints", "(II)I", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.offsetByCodePoints(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuilder", "substring", "(II)Ljava/lang/String;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> str(receiver.substring(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuilder", "substring", "(I)Ljava/lang/String;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> str(receiver.substring(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuilder", "subSequence", "(II)Ljava/lang/CharSequence;", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> str(receiver.subSequence(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuilder", "setLength", "(I)V", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.setLength(i((IntValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/lang/StringBuilder", "capacity", "()I", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> i(receiver.capacity()));
		registerMethodHandler("java/lang/StringBuilder", "ensureCapacity", "(I)V", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.ensureCapacity(i((IntValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/lang/StringBuilder", "trimToSize", "()V", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.trimToSize();
			return null;
		});
		registerMethodHandler("java/lang/StringBuilder", "setCharAt", "(IC)V", (ReFrame frame, ReValue host, StringBuilder receiver, List<ReValue> args) -> {
			receiver.setCharAt(i((IntValue) args.get(0)), c((IntValue) args.get(1)));
			return null;
		});

		// java.lang.Boolean
		registerMethodHandler("java/lang/Boolean", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Boolean receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Boolean", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, Boolean receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Boolean", "hashCode", "()I", (ReFrame frame, ReValue host, Boolean receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Boolean", "compareTo", "(Ljava/lang/Boolean;)I", (ReFrame frame, ReValue host, Boolean receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Boolean>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Boolean", "booleanValue", "()Z", (ReFrame frame, ReValue host, Boolean receiver, List<ReValue> args) -> z(receiver.booleanValue()));

		// java.lang.Byte
		registerMethodHandler("java/lang/Byte", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Byte receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Byte", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, Byte receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Byte", "hashCode", "()I", (ReFrame frame, ReValue host, Byte receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Byte", "compareTo", "(Ljava/lang/Byte;)I", (ReFrame frame, ReValue host, Byte receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Byte>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Byte", "byteValue", "()B", (ReFrame frame, ReValue host, Byte receiver, List<ReValue> args) -> b(receiver.byteValue()));
		registerMethodHandler("java/lang/Byte", "shortValue", "()S", (ReFrame frame, ReValue host, Byte receiver, List<ReValue> args) -> s(receiver.shortValue()));
		registerMethodHandler("java/lang/Byte", "intValue", "()I", (ReFrame frame, ReValue host, Byte receiver, List<ReValue> args) -> i(receiver.intValue()));
		registerMethodHandler("java/lang/Byte", "longValue", "()J", (ReFrame frame, ReValue host, Byte receiver, List<ReValue> args) -> j(receiver.longValue()));
		registerMethodHandler("java/lang/Byte", "floatValue", "()F", (ReFrame frame, ReValue host, Byte receiver, List<ReValue> args) -> f(receiver.floatValue()));
		registerMethodHandler("java/lang/Byte", "doubleValue", "()D", (ReFrame frame, ReValue host, Byte receiver, List<ReValue> args) -> d(receiver.doubleValue()));

		// java.lang.Character
		registerMethodHandler("java/lang/Character", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Character receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Character", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, Character receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Character", "hashCode", "()I", (ReFrame frame, ReValue host, Character receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Character", "compareTo", "(Ljava/lang/Character;)I", (ReFrame frame, ReValue host, Character receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Character>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Character", "charValue", "()C", (ReFrame frame, ReValue host, Character receiver, List<ReValue> args) -> c(receiver.charValue()));

		// java.lang.Short
		registerMethodHandler("java/lang/Short", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Short receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Short", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, Short receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Short", "hashCode", "()I", (ReFrame frame, ReValue host, Short receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Short", "compareTo", "(Ljava/lang/Short;)I", (ReFrame frame, ReValue host, Short receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Short>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Short", "byteValue", "()B", (ReFrame frame, ReValue host, Short receiver, List<ReValue> args) -> b(receiver.byteValue()));
		registerMethodHandler("java/lang/Short", "shortValue", "()S", (ReFrame frame, ReValue host, Short receiver, List<ReValue> args) -> s(receiver.shortValue()));
		registerMethodHandler("java/lang/Short", "intValue", "()I", (ReFrame frame, ReValue host, Short receiver, List<ReValue> args) -> i(receiver.intValue()));
		registerMethodHandler("java/lang/Short", "longValue", "()J", (ReFrame frame, ReValue host, Short receiver, List<ReValue> args) -> j(receiver.longValue()));
		registerMethodHandler("java/lang/Short", "floatValue", "()F", (ReFrame frame, ReValue host, Short receiver, List<ReValue> args) -> f(receiver.floatValue()));
		registerMethodHandler("java/lang/Short", "doubleValue", "()D", (ReFrame frame, ReValue host, Short receiver, List<ReValue> args) -> d(receiver.doubleValue()));

		// java.lang.Integer
		registerMethodHandler("java/lang/Integer", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Integer receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Integer", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, Integer receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Integer", "hashCode", "()I", (ReFrame frame, ReValue host, Integer receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Integer", "compareTo", "(Ljava/lang/Integer;)I", (ReFrame frame, ReValue host, Integer receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Integer>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Integer", "byteValue", "()B", (ReFrame frame, ReValue host, Integer receiver, List<ReValue> args) -> b(receiver.byteValue()));
		registerMethodHandler("java/lang/Integer", "shortValue", "()S", (ReFrame frame, ReValue host, Integer receiver, List<ReValue> args) -> s(receiver.shortValue()));
		registerMethodHandler("java/lang/Integer", "intValue", "()I", (ReFrame frame, ReValue host, Integer receiver, List<ReValue> args) -> i(receiver.intValue()));
		registerMethodHandler("java/lang/Integer", "longValue", "()J", (ReFrame frame, ReValue host, Integer receiver, List<ReValue> args) -> j(receiver.longValue()));
		registerMethodHandler("java/lang/Integer", "floatValue", "()F", (ReFrame frame, ReValue host, Integer receiver, List<ReValue> args) -> f(receiver.floatValue()));
		registerMethodHandler("java/lang/Integer", "doubleValue", "()D", (ReFrame frame, ReValue host, Integer receiver, List<ReValue> args) -> d(receiver.doubleValue()));

		// java.lang.Long
		registerMethodHandler("java/lang/Long", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Long receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Long", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, Long receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Long", "hashCode", "()I", (ReFrame frame, ReValue host, Long receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Long", "compareTo", "(Ljava/lang/Long;)I", (ReFrame frame, ReValue host, Long receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Long>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Long", "byteValue", "()B", (ReFrame frame, ReValue host, Long receiver, List<ReValue> args) -> b(receiver.byteValue()));
		registerMethodHandler("java/lang/Long", "shortValue", "()S", (ReFrame frame, ReValue host, Long receiver, List<ReValue> args) -> s(receiver.shortValue()));
		registerMethodHandler("java/lang/Long", "intValue", "()I", (ReFrame frame, ReValue host, Long receiver, List<ReValue> args) -> i(receiver.intValue()));
		registerMethodHandler("java/lang/Long", "longValue", "()J", (ReFrame frame, ReValue host, Long receiver, List<ReValue> args) -> j(receiver.longValue()));
		registerMethodHandler("java/lang/Long", "floatValue", "()F", (ReFrame frame, ReValue host, Long receiver, List<ReValue> args) -> f(receiver.floatValue()));
		registerMethodHandler("java/lang/Long", "doubleValue", "()D", (ReFrame frame, ReValue host, Long receiver, List<ReValue> args) -> d(receiver.doubleValue()));

		// java.lang.Float
		registerMethodHandler("java/lang/Float", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Float receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Float", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, Float receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Float", "hashCode", "()I", (ReFrame frame, ReValue host, Float receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Float", "isInfinite", "()Z", (ReFrame frame, ReValue host, Float receiver, List<ReValue> args) -> z(receiver.isInfinite()));
		registerMethodHandler("java/lang/Float", "compareTo", "(Ljava/lang/Float;)I", (ReFrame frame, ReValue host, Float receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Float>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Float", "byteValue", "()B", (ReFrame frame, ReValue host, Float receiver, List<ReValue> args) -> b(receiver.byteValue()));
		registerMethodHandler("java/lang/Float", "shortValue", "()S", (ReFrame frame, ReValue host, Float receiver, List<ReValue> args) -> s(receiver.shortValue()));
		registerMethodHandler("java/lang/Float", "intValue", "()I", (ReFrame frame, ReValue host, Float receiver, List<ReValue> args) -> i(receiver.intValue()));
		registerMethodHandler("java/lang/Float", "longValue", "()J", (ReFrame frame, ReValue host, Float receiver, List<ReValue> args) -> j(receiver.longValue()));
		registerMethodHandler("java/lang/Float", "floatValue", "()F", (ReFrame frame, ReValue host, Float receiver, List<ReValue> args) -> f(receiver.floatValue()));
		registerMethodHandler("java/lang/Float", "doubleValue", "()D", (ReFrame frame, ReValue host, Float receiver, List<ReValue> args) -> d(receiver.doubleValue()));
		registerMethodHandler("java/lang/Float", "isNaN", "()Z", (ReFrame frame, ReValue host, Float receiver, List<ReValue> args) -> z(receiver.isNaN()));

		// java.lang.Double
		registerMethodHandler("java/lang/Double", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Double receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Double", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, Double receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/Double", "hashCode", "()I", (ReFrame frame, ReValue host, Double receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/lang/Double", "isInfinite", "()Z", (ReFrame frame, ReValue host, Double receiver, List<ReValue> args) -> z(receiver.isInfinite()));
		registerMethodHandler("java/lang/Double", "compareTo", "(Ljava/lang/Double;)I", (ReFrame frame, ReValue host, Double receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<Double>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/Double", "byteValue", "()B", (ReFrame frame, ReValue host, Double receiver, List<ReValue> args) -> b(receiver.byteValue()));
		registerMethodHandler("java/lang/Double", "shortValue", "()S", (ReFrame frame, ReValue host, Double receiver, List<ReValue> args) -> s(receiver.shortValue()));
		registerMethodHandler("java/lang/Double", "intValue", "()I", (ReFrame frame, ReValue host, Double receiver, List<ReValue> args) -> i(receiver.intValue()));
		registerMethodHandler("java/lang/Double", "longValue", "()J", (ReFrame frame, ReValue host, Double receiver, List<ReValue> args) -> j(receiver.longValue()));
		registerMethodHandler("java/lang/Double", "floatValue", "()F", (ReFrame frame, ReValue host, Double receiver, List<ReValue> args) -> f(receiver.floatValue()));
		registerMethodHandler("java/lang/Double", "doubleValue", "()D", (ReFrame frame, ReValue host, Double receiver, List<ReValue> args) -> d(receiver.doubleValue()));
		registerMethodHandler("java/lang/Double", "isNaN", "()Z", (ReFrame frame, ReValue host, Double receiver, List<ReValue> args) -> z(receiver.isNaN()));

		// java.util.Random
		registerMethodHandler("java/util/Random", "nextDouble", "()D", (ReFrame frame, ReValue host, Random receiver, List<ReValue> args) -> d(receiver.nextDouble()));
		registerMethodHandler("java/util/Random", "nextInt", "()I", (ReFrame frame, ReValue host, Random receiver, List<ReValue> args) -> i(receiver.nextInt()));
		registerMethodHandler("java/util/Random", "nextInt", "(I)I", (ReFrame frame, ReValue host, Random receiver, List<ReValue> args) -> i(receiver.nextInt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/util/Random", "nextBytes", "([B)V", (ReFrame frame, ReValue host, Random receiver, List<ReValue> args) -> {
			receiver.nextBytes(arrb((ArrayValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/Random", "setSeed", "(J)V", (ReFrame frame, ReValue host, Random receiver, List<ReValue> args) -> {
			receiver.setSeed(j((LongValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/Random", "nextLong", "()J", (ReFrame frame, ReValue host, Random receiver, List<ReValue> args) -> j(receiver.nextLong()));
		registerMethodHandler("java/util/Random", "nextBoolean", "()Z", (ReFrame frame, ReValue host, Random receiver, List<ReValue> args) -> z(receiver.nextBoolean()));
		registerMethodHandler("java/util/Random", "nextFloat", "()F", (ReFrame frame, ReValue host, Random receiver, List<ReValue> args) -> f(receiver.nextFloat()));
		registerMethodHandler("java/util/Random", "nextGaussian", "()D", (ReFrame frame, ReValue host, Random receiver, List<ReValue> args) -> d(receiver.nextGaussian()));

		// java.util.List
		registerMethodHandler("java/util/List", "remove", "(I)Ljava/lang/Object;", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.remove(i((IntValue) args.get(0)))));
		registerMethodHandler("java/util/List", "remove", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> z(receiver.remove(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/List", "size", "()I", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> i(receiver.size()));
		registerMethodHandler("java/util/List", "get", "(I)Ljava/lang/Object;", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.get(i((IntValue) args.get(0)))));
		registerMethodHandler("java/util/List", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/List", "hashCode", "()I", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/util/List", "indexOf", "(Ljava/lang/Object;)I", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> i(receiver.indexOf(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/List", "clear", "()V", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> {
			receiver.clear();
			return null;
		});
		registerMethodHandler("java/util/List", "lastIndexOf", "(Ljava/lang/Object;)I", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> i(receiver.lastIndexOf(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/List", "isEmpty", "()Z", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> z(receiver.isEmpty()));
		registerMethodHandler("java/util/List", "add", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> z(receiver.add(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/List", "add", "(ILjava/lang/Object;)V", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> {
			receiver.add(i((IntValue) args.get(0)), objl((ObjectValue) args.get(1)));
			return null;
		});
		registerMethodHandler("java/util/List", "subList", "(II)Ljava/util/List;", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.subList(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/util/List", "toArray", "()[Ljava/lang/Object;", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.toArray()));
		registerMethodHandler("java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.toArray(arrobj((ArrayValue) args.get(0)))));
		registerMethodHandler("java/util/List", "contains", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> z(receiver.contains(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/List", "set", "(ILjava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.set(i((IntValue) args.get(0)), objl((ObjectValue) args.get(1)))));
		registerMethodHandler("java/util/List", "getFirst", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.getFirst()));
		registerMethodHandler("java/util/List", "getLast", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.getLast()));
		registerMethodHandler("java/util/List", "addFirst", "(Ljava/lang/Object;)V", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> {
			receiver.addFirst(objl((ObjectValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/List", "addLast", "(Ljava/lang/Object;)V", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> {
			receiver.addLast(objl((ObjectValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/List", "removeFirst", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.removeFirst()));
		registerMethodHandler("java/util/List", "removeLast", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.removeLast()));
		registerMethodHandler("java/util/List", "reversed", "()Ljava/util/List;", (ReFrame frame, ReValue host, List receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.reversed()));

		// java.lang.CharSequence
		registerMethodHandler("java/lang/CharSequence", "length", "()I", (ReFrame frame, ReValue host, CharSequence receiver, List<ReValue> args) -> i(receiver.length()));
		registerMethodHandler("java/lang/CharSequence", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, CharSequence receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/CharSequence", "charAt", "(I)C", (ReFrame frame, ReValue host, CharSequence receiver, List<ReValue> args) -> c(receiver.charAt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/CharSequence", "isEmpty", "()Z", (ReFrame frame, ReValue host, CharSequence receiver, List<ReValue> args) -> z(receiver.isEmpty()));
		registerMethodHandler("java/lang/CharSequence", "subSequence", "(II)Ljava/lang/CharSequence;", (ReFrame frame, ReValue host, CharSequence receiver, List<ReValue> args) -> str(receiver.subSequence(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));

		// java.util.ArrayList
		registerMethodHandler("java/util/ArrayList", "remove", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> z(receiver.remove(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "remove", "(I)Ljava/lang/Object;", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.remove(i((IntValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "size", "()I", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> i(receiver.size()));
		registerMethodHandler("java/util/ArrayList", "get", "(I)Ljava/lang/Object;", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.get(i((IntValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "hashCode", "()I", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/util/ArrayList", "clone", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.clone()));
		registerMethodHandler("java/util/ArrayList", "indexOf", "(Ljava/lang/Object;)I", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> i(receiver.indexOf(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "clear", "()V", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> {
			receiver.clear();
			return null;
		});
		registerMethodHandler("java/util/ArrayList", "lastIndexOf", "(Ljava/lang/Object;)I", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> i(receiver.lastIndexOf(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "isEmpty", "()Z", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> z(receiver.isEmpty()));
		registerMethodHandler("java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> z(receiver.add(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "add", "(ILjava/lang/Object;)V", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> {
			receiver.add(i((IntValue) args.get(0)), objl((ObjectValue) args.get(1)));
			return null;
		});
		registerMethodHandler("java/util/ArrayList", "subList", "(II)Ljava/util/List;", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.subList(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/util/ArrayList", "toArray", "()[Ljava/lang/Object;", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.toArray()));
		registerMethodHandler("java/util/ArrayList", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.toArray(arrobj((ArrayValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "contains", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> z(receiver.contains(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/util/ArrayList", "set", "(ILjava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.set(i((IntValue) args.get(0)), objl((ObjectValue) args.get(1)))));
		registerMethodHandler("java/util/ArrayList", "ensureCapacity", "(I)V", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> {
			receiver.ensureCapacity(i((IntValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/ArrayList", "trimToSize", "()V", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> {
			receiver.trimToSize();
			return null;
		});
		registerMethodHandler("java/util/ArrayList", "getFirst", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.getFirst()));
		registerMethodHandler("java/util/ArrayList", "getLast", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.getLast()));
		registerMethodHandler("java/util/ArrayList", "addFirst", "(Ljava/lang/Object;)V", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> {
			receiver.addFirst(objl((ObjectValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/ArrayList", "addLast", "(Ljava/lang/Object;)V", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> {
			receiver.addLast(objl((ObjectValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/util/ArrayList", "removeFirst", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.removeFirst()));
		registerMethodHandler("java/util/ArrayList", "removeLast", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, ArrayList receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.removeLast()));

		// java.util.Base64$Encoder
		registerMethodHandler("java/util/Base64$Encoder", "encode", "([B)[B", (ReFrame frame, ReValue host, Base64.Encoder receiver, List<ReValue> args) -> arrb(receiver.encode(arrb((ArrayValue) args.get(0)))));
		registerMethodHandler("java/util/Base64$Encoder", "encode", "([B[B)I", (ReFrame frame, ReValue host, Base64.Encoder receiver, List<ReValue> args) -> {
			// Original generation:
			//  - i(receiver.encode(arrb((ArrayValue)args.get(0)), arrb((ArrayValue)args.get(1))))
			ArrayValue destinationValue = (ArrayValue) args.get(1);
			byte[] destination = arrb(destinationValue);
			try {
				int written = receiver.encode(arrb((ArrayValue) args.get(0)), destination);
				replaceByteArrayContents(frame, destinationValue, destination, 0, written);
				return i(written);
			} catch (Throwable t) {
				// The JDK may have written a prefix before reporting a destination error.
				if (destination != null)
					replaceByteArrayContents(frame, destinationValue, destination, 0, destination.length);
				throw t;
			}
		});
		registerMethodHandler("java/util/Base64$Encoder", "encode", "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, Base64.Encoder receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.encode(requireRealInstance(args.get(0), ByteBuffer.class))));
		registerMethodHandler("java/util/Base64$Encoder", "encodeToString", "([B)Ljava/lang/String;", (ReFrame frame, ReValue host, Base64.Encoder receiver, List<ReValue> args) -> str(receiver.encodeToString(arrb((ArrayValue) args.get(0)))));
		registerMethodHandler("java/util/Base64$Encoder", "withoutPadding", "()Ljava/util/Base64$Encoder;", (ReFrame frame, ReValue host, Base64.Encoder receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.withoutPadding()));
		registerMethodHandler("java/util/Base64$Encoder", "wrap", "(Ljava/io/OutputStream;)Ljava/io/OutputStream;", (ReFrame frame, ReValue host, Base64.Encoder receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.wrap(requireRealInstance(args.get(0), OutputStream.class))));

		// java.util.Base64$Decoder
		registerMethodHandler("java/util/Base64$Decoder", "decode", "([B)[B", (ReFrame frame, ReValue host, Base64.Decoder receiver, List<ReValue> args) -> arrb(receiver.decode(arrb((ArrayValue) args.get(0)))));
		registerMethodHandler("java/util/Base64$Decoder", "decode", "([B[B)I", (ReFrame frame, ReValue host, Base64.Decoder receiver, List<ReValue> args) -> {
			// Original generation:
			//  - i(receiver.decode(arrb((ArrayValue)args.get(0)), arrb((ArrayValue)args.get(1))))
			ArrayValue destinationValue = (ArrayValue) args.get(1);
			byte[] destination = arrb(destinationValue);
			try {
				int written = receiver.decode(arrb((ArrayValue) args.get(0)), destination);
				replaceByteArrayContents(frame, destinationValue, destination, 0, written);
				return i(written);
			} catch (Throwable t) {
				// Preserve bytes produced before malformed input is reported.
				if (destination != null)
					replaceByteArrayContents(frame, destinationValue, destination, 0, destination.length);
				throw t;
			}
		});
		registerMethodHandler("java/util/Base64$Decoder", "decode", "(Ljava/lang/String;)[B", (ReFrame frame, ReValue host, Base64.Decoder receiver, List<ReValue> args) -> arrb(receiver.decode(str((StringValue) args.get(0)))));
		registerMethodHandler("java/util/Base64$Decoder", "decode", "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, Base64.Decoder receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.decode(requireRealInstance(args.get(0), ByteBuffer.class))));
		registerMethodHandler("java/util/Base64$Decoder", "wrap", "(Ljava/io/InputStream;)Ljava/io/InputStream;", (ReFrame frame, ReValue host, Base64.Decoder receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.wrap(requireRealInstance(args.get(0), InputStream.class))));

		// java.nio.Buffer
		registerMethodHandler("java/nio/Buffer", "reset", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> {
			receiver.reset();
			return host;
		});
		registerMethodHandler("java/nio/Buffer", "clear", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> {
			receiver.clear();
			return host;
		});
		registerMethodHandler("java/nio/Buffer", "position", "(I)Ljava/nio/Buffer;", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> {
			receiver.position(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/Buffer", "position", "()I", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> i(receiver.position()));
		registerMethodHandler("java/nio/Buffer", "limit", "()I", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> i(receiver.limit()));
		registerMethodHandler("java/nio/Buffer", "limit", "(I)Ljava/nio/Buffer;", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> {
			receiver.limit(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/Buffer", "remaining", "()I", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> i(receiver.remaining()));
		registerMethodHandler("java/nio/Buffer", "isDirect", "()Z", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> z(receiver.isDirect()));
		registerMethodHandler("java/nio/Buffer", "hasArray", "()Z", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> z(receiver.hasArray()));
		registerMethodHandler("java/nio/Buffer", "array", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.array()));
		registerMethodHandler("java/nio/Buffer", "arrayOffset", "()I", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> i(receiver.arrayOffset()));
		registerMethodHandler("java/nio/Buffer", "capacity", "()I", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> i(receiver.capacity()));
		registerMethodHandler("java/nio/Buffer", "mark", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> {
			receiver.mark();
			return host;
		});
		registerMethodHandler("java/nio/Buffer", "flip", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> {
			receiver.flip();
			return host;
		});
		registerMethodHandler("java/nio/Buffer", "rewind", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> {
			receiver.rewind();
			return host;
		});
		registerMethodHandler("java/nio/Buffer", "hasRemaining", "()Z", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> z(receiver.hasRemaining()));
		registerMethodHandler("java/nio/Buffer", "isReadOnly", "()Z", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> z(receiver.isReadOnly()));
		registerMethodHandler("java/nio/Buffer", "slice", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.slice()));
		registerMethodHandler("java/nio/Buffer", "slice", "(II)Ljava/nio/Buffer;", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.slice(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/nio/Buffer", "duplicate", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, Buffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.duplicate()));

		// java.nio.ByteBuffer
		//  - The bulk-read handlers are manually implemented because they synchronize evaluator arrays and are not fully generated.
		registerMethodHandler("java/nio/ByteBuffer", "reset", "()Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.reset();
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "get", "([B)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			ArrayValue destinationValue = (ArrayValue) args.get(0);
			byte[] destination = arrb((ArrayValue) args.get(0));
			try {receiver.get(destination);} catch (Throwable t) {
				if (destination != null)
					replaceByteArrayContents(frame, destinationValue, destination, 0, destination.length);
				throw t;
			}
			replaceByteArrayContents(frame, destinationValue, destination, 0, destination.length);
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "get", "(I)B", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> b(receiver.get(i((IntValue) args.get(0)))));
		registerMethodHandler("java/nio/ByteBuffer", "get", "()B", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> b(receiver.get()));
		registerMethodHandler("java/nio/ByteBuffer", "get", "([BII)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			ArrayValue destinationValue = (ArrayValue) args.get(0);
			byte[] destination = arrb((ArrayValue) args.get(0));
			try {receiver.get(destination, i((IntValue) args.get(1)), i((IntValue) args.get(2)));} catch (Throwable t) {
				if (destination != null)
					replaceByteArrayContents(frame, destinationValue, destination, i((IntValue) args.get(1)), i((IntValue) args.get(2)));
				throw t;
			}
			replaceByteArrayContents(frame, destinationValue, destination, i((IntValue) args.get(1)), i((IntValue) args.get(2)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "get", "(I[B)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			ArrayValue destinationValue = (ArrayValue) args.get(1);
			byte[] destination = arrb((ArrayValue) args.get(1));
			try {receiver.get(i((IntValue) args.get(0)), destination);} catch (Throwable t) {
				if (destination != null)
					replaceByteArrayContents(frame, destinationValue, destination, 0, destination.length);
				throw t;
			}
			replaceByteArrayContents(frame, destinationValue, destination, 0, destination.length);
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "get", "(I[BII)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			ArrayValue destinationValue = (ArrayValue) args.get(1);
			byte[] destination = arrb((ArrayValue) args.get(1));
			try {
				receiver.get(i((IntValue) args.get(0)), destination, i((IntValue) args.get(2)), i((IntValue) args.get(3)));
			} catch (Throwable t) {
				if (destination != null)
					replaceByteArrayContents(frame, destinationValue, destination, i((IntValue) args.get(2)), i((IntValue) args.get(3)));
				throw t;
			}
			replaceByteArrayContents(frame, destinationValue, destination, i((IntValue) args.get(2)), i((IntValue) args.get(3)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "put", "(IB)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.put(i((IntValue) args.get(0)), b((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "put", "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.put(requireRealInstance(args.get(0), ByteBuffer.class));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "put", "(ILjava/nio/ByteBuffer;II)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.put(i((IntValue) args.get(0)), requireRealInstance(args.get(1), ByteBuffer.class), i((IntValue) args.get(2)), i((IntValue) args.get(3)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "put", "([B)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.put(arrb((ArrayValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "put", "(I[B)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.put(i((IntValue) args.get(0)), arrb((ArrayValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "put", "(I[BII)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.put(i((IntValue) args.get(0)), arrb((ArrayValue) args.get(1)), i((IntValue) args.get(2)), i((IntValue) args.get(3)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "put", "([BII)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.put(arrb((ArrayValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "put", "(B)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.put(b((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "equals", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> z(receiver.equals(objl((ObjectValue) args.get(0)))));
		registerMethodHandler("java/nio/ByteBuffer", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/nio/ByteBuffer", "hashCode", "()I", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> i(receiver.hashCode()));
		registerMethodHandler("java/nio/ByteBuffer", "compareTo", "(Ljava/nio/ByteBuffer;)I", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> i(receiver.compareTo(requireRealInstance(args.get(0), ByteBuffer.class))));
		registerMethodHandler("java/nio/ByteBuffer", "getShort", "()S", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> s(receiver.getShort()));
		registerMethodHandler("java/nio/ByteBuffer", "getShort", "(I)S", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> s(receiver.getShort(i((IntValue) args.get(0)))));
		registerMethodHandler("java/nio/ByteBuffer", "putShort", "(IS)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.putShort(i((IntValue) args.get(0)), s((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "putShort", "(S)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.putShort(s((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "getChar", "(I)C", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> c(receiver.getChar(i((IntValue) args.get(0)))));
		registerMethodHandler("java/nio/ByteBuffer", "getChar", "()C", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> c(receiver.getChar()));
		registerMethodHandler("java/nio/ByteBuffer", "putChar", "(IC)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.putChar(i((IntValue) args.get(0)), c((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "putChar", "(C)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.putChar(c((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "getInt", "(I)I", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> i(receiver.getInt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/nio/ByteBuffer", "getInt", "()I", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> i(receiver.getInt()));
		registerMethodHandler("java/nio/ByteBuffer", "putInt", "(II)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.putInt(i((IntValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "putInt", "(I)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.putInt(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "getLong", "(I)J", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> j(receiver.getLong(i((IntValue) args.get(0)))));
		registerMethodHandler("java/nio/ByteBuffer", "getLong", "()J", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> j(receiver.getLong()));
		registerMethodHandler("java/nio/ByteBuffer", "putLong", "(J)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.putLong(j((LongValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "putLong", "(IJ)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.putLong(i((IntValue) args.get(0)), j((LongValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "getFloat", "()F", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> f(receiver.getFloat()));
		registerMethodHandler("java/nio/ByteBuffer", "getFloat", "(I)F", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> f(receiver.getFloat(i((IntValue) args.get(0)))));
		registerMethodHandler("java/nio/ByteBuffer", "putFloat", "(F)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.putFloat(f((FloatValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "putFloat", "(IF)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.putFloat(i((IntValue) args.get(0)), f((FloatValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "getDouble", "(I)D", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> d(receiver.getDouble(i((IntValue) args.get(0)))));
		registerMethodHandler("java/nio/ByteBuffer", "getDouble", "()D", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> d(receiver.getDouble()));
		registerMethodHandler("java/nio/ByteBuffer", "putDouble", "(ID)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.putDouble(i((IntValue) args.get(0)), d((DoubleValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "putDouble", "(D)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.putDouble(d((DoubleValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "clear", "()Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.clear();
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "position", "(I)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.position(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "mismatch", "(Ljava/nio/ByteBuffer;)I", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> i(receiver.mismatch(requireRealInstance(args.get(0), ByteBuffer.class))));
		registerMethodHandler("java/nio/ByteBuffer", "limit", "(I)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.limit(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "isDirect", "()Z", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> z(receiver.isDirect()));
		registerMethodHandler("java/nio/ByteBuffer", "hasArray", "()Z", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> z(receiver.hasArray()));
		registerMethodHandler("java/nio/ByteBuffer", "array", "()[B", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> arrb(receiver.array()));
		registerMethodHandler("java/nio/ByteBuffer", "arrayOffset", "()I", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> i(receiver.arrayOffset()));
		registerMethodHandler("java/nio/ByteBuffer", "mark", "()Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.mark();
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "flip", "()Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.flip();
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "rewind", "()Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.rewind();
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "slice", "(II)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.slice(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/nio/ByteBuffer", "slice", "()Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.slice()));
		registerMethodHandler("java/nio/ByteBuffer", "duplicate", "()Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.duplicate()));
		registerMethodHandler("java/nio/ByteBuffer", "alignmentOffset", "(II)I", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> i(receiver.alignmentOffset(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/nio/ByteBuffer", "asReadOnlyBuffer", "()Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.asReadOnlyBuffer()));
		registerMethodHandler("java/nio/ByteBuffer", "compact", "()Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.compact();
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "order", "(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.order(requireRealInstance(args.get(0), ByteOrder.class));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "order", "()Ljava/nio/ByteOrder;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.order()));
		registerMethodHandler("java/nio/ByteBuffer", "alignedSlice", "(I)Ljava/nio/ByteBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.alignedSlice(i((IntValue) args.get(0)))));
		registerMethodHandler("java/nio/ByteBuffer", "asCharBuffer", "()Ljava/nio/CharBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.asCharBuffer()));
		registerMethodHandler("java/nio/ByteBuffer", "asShortBuffer", "()Ljava/nio/ShortBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.asShortBuffer()));
		registerMethodHandler("java/nio/ByteBuffer", "asIntBuffer", "()Ljava/nio/IntBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.asIntBuffer()));
		registerMethodHandler("java/nio/ByteBuffer", "asLongBuffer", "()Ljava/nio/LongBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.asLongBuffer()));
		registerMethodHandler("java/nio/ByteBuffer", "asFloatBuffer", "()Ljava/nio/FloatBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.asFloatBuffer()));
		registerMethodHandler("java/nio/ByteBuffer", "asDoubleBuffer", "()Ljava/nio/DoubleBuffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.asDoubleBuffer()));
		registerMethodHandler("java/nio/ByteBuffer", "reset", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.reset();
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "clear", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.clear();
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "position", "(I)Ljava/nio/Buffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.position(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "position", "()I", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> i(receiver.position()));
		registerMethodHandler("java/nio/ByteBuffer", "limit", "()I", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> i(receiver.limit()));
		registerMethodHandler("java/nio/ByteBuffer", "limit", "(I)Ljava/nio/Buffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.limit(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "remaining", "()I", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> i(receiver.remaining()));
		registerMethodHandler("java/nio/ByteBuffer", "array", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.array()));
		registerMethodHandler("java/nio/ByteBuffer", "capacity", "()I", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> i(receiver.capacity()));
		registerMethodHandler("java/nio/ByteBuffer", "mark", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.mark();
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "flip", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.flip();
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "rewind", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> {
			receiver.rewind();
			return host;
		});
		registerMethodHandler("java/nio/ByteBuffer", "hasRemaining", "()Z", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> z(receiver.hasRemaining()));
		registerMethodHandler("java/nio/ByteBuffer", "isReadOnly", "()Z", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> z(receiver.isReadOnly()));
		registerMethodHandler("java/nio/ByteBuffer", "slice", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.slice()));
		registerMethodHandler("java/nio/ByteBuffer", "slice", "(II)Ljava/nio/Buffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.slice(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/nio/ByteBuffer", "duplicate", "()Ljava/nio/Buffer;", (ReFrame frame, ReValue host, ByteBuffer receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.duplicate()));

		// java.nio.ByteOrder
		registerMethodHandler("java/nio/ByteOrder", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, ByteOrder receiver, List<ReValue> args) -> str(receiver.toString()));

		// java.io.OutputStream
		//  - java.io.ByteArrayOutputStream
		registerMethodHandler("java/io/OutputStream", "write", "(I)V", (ReFrame frame, ReValue host, OutputStream receiver, List<ReValue> args) -> {
			receiver.write(i((IntValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/io/OutputStream", "write", "([B)V", (ReFrame frame, ReValue host, OutputStream receiver, List<ReValue> args) -> {
			receiver.write(arrb((ArrayValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/io/OutputStream", "write", "([BII)V", (ReFrame frame, ReValue host, OutputStream receiver, List<ReValue> args) -> {
			receiver.write(arrb((ArrayValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)));
			return null;
		});
		registerMethodHandler("java/io/OutputStream", "flush", "()V", (ReFrame frame, ReValue host, OutputStream receiver, List<ReValue> args) -> {
			receiver.flush();
			return null;
		});
		registerMethodHandler("java/io/OutputStream", "close", "()V", (ReFrame frame, ReValue host, OutputStream receiver, List<ReValue> args) -> {
			receiver.close();
			return null;
		});

		// java.io.ByteArrayOutputStream
		registerMethodHandler("java/io/ByteArrayOutputStream", "toByteArray", "()[B", (ReFrame frame, ReValue host, ByteArrayOutputStream receiver, List<ReValue> args) -> arrb(receiver.toByteArray()));

		// java.io.InputStream
		//  - java.io.ByteArrayInputStream
		registerMethodHandler("java/io/InputStream", "read", "()I", (ReFrame frame, ReValue host, InputStream receiver, List<ReValue> args) -> i(receiver.read()));
		registerMethodHandler("java/io/InputStream", "read", "([B)I", (ReFrame frame, ReValue host, InputStream receiver, List<ReValue> args) -> {
			ArrayValue destinationValue = (ArrayValue) args.get(0);
			byte[] destination = arrb(destinationValue);
			int read;
			try {
				read = receiver.read(destination);
			} catch (Throwable t) {
				if (destination != null)
					replaceByteArrayContents(frame, destinationValue, destination, 0, destination.length);
				throw t;
			}
			replaceByteArrayContents(frame, destinationValue, destination, 0, read);
			return i(read);
		});
		registerMethodHandler("java/io/InputStream", "read", "([BII)I", (ReFrame frame, ReValue host, InputStream receiver, List<ReValue> args) -> {
			ArrayValue destinationValue = (ArrayValue) args.get(0);
			byte[] destination = arrb(destinationValue);
			int offset = i((IntValue) args.get(1));
			int length = i((IntValue) args.get(2));
			int read;
			try {
				read = receiver.read(destination, offset, length);
			} catch (Throwable t) {
				if (destination != null)
					replaceByteArrayContents(frame, destinationValue, destination, offset, length);
				throw t;
			}
			replaceByteArrayContents(frame, destinationValue, destination, offset, read);
			return i(read);
		});
		registerMethodHandler("java/io/InputStream", "readAllBytes", "()[B", (ReFrame frame, ReValue host, InputStream receiver, List<ReValue> args) -> arrb(receiver.readAllBytes()));
		registerMethodHandler("java/io/InputStream", "close", "()V", (ReFrame frame, ReValue host, InputStream receiver, List<ReValue> args) -> {
			receiver.close();
			return null;
		});

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

		// java.lang.StackTraceElement
		registerMapper(StackTraceElement.class, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V", (host, parameters) -> new StackTraceElement(str((StringValue) parameters.get(0)), str((StringValue) parameters.get(1)), str((StringValue) parameters.get(2)), str((StringValue) parameters.get(3)), str((StringValue) parameters.get(4)), str((StringValue) parameters.get(5)), i((IntValue) parameters.get(6))));
		registerMapper(StackTraceElement.class, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V", (host, parameters) -> new StackTraceElement(str((StringValue) parameters.get(0)), str((StringValue) parameters.get(1)), str((StringValue) parameters.get(2)), i((IntValue) parameters.get(3))));

		// java.io.ByteArrayInputStream
		registerMapper(ByteArrayInputStream.class, "([B)V", (host, parameters) -> new ByteArrayInputStream(arrb((ArrayValue) parameters.get(0))));
		registerMapper(ByteArrayInputStream.class, "([BII)V", (host, parameters) -> new ByteArrayInputStream(arrb((ArrayValue) parameters.get(0)), i((IntValue) parameters.get(1)), i((IntValue) parameters.get(2))));

		// java.io.ByteArrayOutputStream
		registerMapper(ByteArrayOutputStream.class, "()V", (host, parameters) -> new ByteArrayOutputStream());
		registerMapper(ByteArrayOutputStream.class, "(I)V", (host, parameters) -> new ByteArrayOutputStream(i((IntValue) parameters.get(0))));

		// java.util.ArrayList
		registerMapper(ArrayList.class, "()V", (host, parameters) -> new ArrayList());
		registerMapper(ArrayList.class, "(I)V", (host, parameters) -> new ArrayList(i((IntValue) parameters.get(0))));
	}

	/**
	 * @see InstanceStaticMapperGenerator
	 */
	private void registerStaticMappers() {
		// java.util.Base64
		registerStaticMapper(Base64.class, "getEncoder()Ljava/util/Base64$Encoder;", (host, parameters) -> Base64.getEncoder());
		registerStaticMapper(Base64.class, "getUrlEncoder()Ljava/util/Base64$Encoder;", (host, parameters) -> Base64.getUrlEncoder());
		registerStaticMapper(Base64.class, "getMimeEncoder()Ljava/util/Base64$Encoder;", (host, parameters) -> Base64.getMimeEncoder());
		registerStaticMapper(Base64.class, "getMimeEncoder(I[B)Ljava/util/Base64$Encoder;", (host, parameters) -> Base64.getMimeEncoder(i((IntValue) parameters.get(0)), arrb((ArrayValue) parameters.get(1))));
		registerStaticMapper(Base64.class, "getDecoder()Ljava/util/Base64$Decoder;", (host, parameters) -> Base64.getDecoder());
		registerStaticMapper(Base64.class, "getUrlDecoder()Ljava/util/Base64$Decoder;", (host, parameters) -> Base64.getUrlDecoder());
		registerStaticMapper(Base64.class, "getMimeDecoder()Ljava/util/Base64$Decoder;", (host, parameters) -> Base64.getMimeDecoder());

		// java.nio.ByteBuffer
		registerStaticMapper(ByteBuffer.class, "wrap([B)Ljava/nio/ByteBuffer;", (host, parameters) -> ByteBuffer.wrap(arrb((ArrayValue) parameters.get(0))));
		registerStaticMapper(ByteBuffer.class, "wrap([BII)Ljava/nio/ByteBuffer;", (host, parameters) -> ByteBuffer.wrap(arrb((ArrayValue) parameters.get(0)), i((IntValue) parameters.get(1)), i((IntValue) parameters.get(2))));
		registerStaticMapper(ByteBuffer.class, "allocate(I)Ljava/nio/ByteBuffer;", (host, parameters) -> ByteBuffer.allocate(i((IntValue) parameters.get(0))));
		registerStaticMapper(ByteBuffer.class, "allocateDirect(I)Ljava/nio/ByteBuffer;", (host, parameters) -> ByteBuffer.allocateDirect(i((IntValue) parameters.get(0))));
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
		InstanceMapper mapper = mappers.get(min.owner + '.' + min.name + min.desc);
		return mapper != null ? mapper : mappers.get(min.owner + '.' + min.desc);
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

	/**
	 * Register a static mapper without making the owner allocatable through {@code NEW}.
	 *
	 * @param owner
	 * 		Owner of the static factory method.
	 * @param desc
	 * 		Factory method name and descriptor.
	 * @param mapper
	 * 		Mapper for the static factory method.
	 */
	private void registerStaticMapper(@Nonnull Class<?> owner, @Nonnull String desc, @Nonnull InstanceMapper mapper) {
		String internalName = owner.getName().replace('.', '/');
		mappers.put(internalName + '.' + desc, mapper);
	}

	/**
	 * @param owner
	 * 		Owner of the method.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 * @param handler
	 * 		Handler to register.
	 */
	private void registerMethodHandler(@Nonnull String owner, @Nonnull String name, @Nonnull String desc, @Nonnull MethodInvokeHandler<?> handler) {
		methodHandlers.put(owner + '.' + name + desc, handler);
	}

	private void registerMapper(@Nonnull Class<?> type, @Nonnull String desc, @Nonnull InstanceMapper mapper) {
		String internalName = type.getName().replace('.', '/');
		supportedTypes.add(internalName);
		mappers.put(internalName + '.' + desc, mapper);
	}

	/**
	 * Require a host-backed evaluator value of the requested type.
	 *
	 * @param value
	 * 		Evaluator value to unwrap.
	 * @param type
	 * 		Required host type.
	 * @param <T>
	 * 		Required host type.
	 *
	 * @return The host instance.
	 *
	 * @throws IllegalArgumentException
	 * 		When the value is {@code null}, evaluator-only, or backed by another type.
	 */
	@Nonnull
	private static <T> T requireRealInstance(@Nonnull ReValue value, @Nonnull Class<T> type) {
		if (value instanceof InstancedObjectValue<?> instanced) {
			Object realInstance = instanced.getRealInstance();
			if (type.isInstance(realInstance))
				return type.cast(realInstance);
		}
		throw new IllegalArgumentException("Expected host-backed " + type.getName());
	}

	/**
	 * Replace the known portion of an evaluator byte array with host-written bytes.
	 *
	 * @param frame
	 * 		Active evaluator frame.
	 * @param original
	 * 		Original evaluator array.
	 * @param hostContents
	 * 		Host array containing the updated bytes.
	 * @param offset
	 * 		First destination index to copy.
	 * @param length
	 * 		Maximum number of bytes to copy.
	 */
	private static void replaceByteArrayContents(@Nonnull ReFrame frame, @Nonnull ArrayValue original,
	                                             @Nonnull byte[] hostContents, int offset, int length) {
		// Skip if there is nothing to copy, or if the original array is malformed.
		if (length <= 0)
			return;
		int originalLength = original.getFirstDimensionLength().orElse(-1);
		if (originalLength < 0)
			return;

		// Clamp the copied range so malformed host results cannot escape the modeled array.
		int start = Math.max(0, offset);
		int end = (int) Math.min(Math.min((long) originalLength, hostContents.length), (long) start + length);
		if (start >= end)
			return;

		ArrayValue replacement = new ArrayValueImpl(original.type(), original.nullness(), originalLength,
				index -> index >= start && index < end ? i(hostContents[index]) : original.getValue(index));
		frame.replaceValue(original, replacement);
	}
}
