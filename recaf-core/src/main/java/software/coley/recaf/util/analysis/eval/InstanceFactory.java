package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import software.coley.recaf.util.analysis.ReFrame;
import software.coley.recaf.util.analysis.ReInterpreter;
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

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Factory for creating real instances of supported types and handling method calls on them.
 *
 * @author Matt Coley
 */
public class InstanceFactory extends BasicLookupUtils {
	private final Map<String, InstanceMapper> mappers = new HashMap<>();
	private final Map<String, MethodInvokeHandler<?>> methodHandlers = new HashMap<>();
	private final Map<String, MethodInvokeStaticHandler> staticMethodHandlers = new HashMap<>();
	private final Set<String> supportedTypes = new HashSet<>();

	/**
	 * Register supported types and method handlers.
	 */
	public InstanceFactory() {
		registerCtorMappers();
		registerCollectionCtorMappers();

		registerStaticMappers();
		registerCollectionStaticMappers();
		registerStaticMethodHandlers();

		registerMethodHandlers();
		registerCollectionMethodHandlers();
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

		// java.lang.StringBuffer
		registerMethodHandler("java/lang/StringBuffer", "length", "()I", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> i(receiver.length()));
		registerMethodHandler("java/lang/StringBuffer", "toString", "()Ljava/lang/String;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> str(receiver.toString()));
		registerMethodHandler("java/lang/StringBuffer", "append", "(I)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "append", "(J)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(j((LongValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "append", "(F)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(f((FloatValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "append", "([C)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(arrc((ArrayValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "append", "([CII)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(arrc((ArrayValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "append", "(Z)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(z((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "append", "(C)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(c((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "append", "(D)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(d((DoubleValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(str((StringValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "append", "(Ljava/lang/StringBuffer;)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(BasicLookupUtils.<StringBuffer>obj((ObjectValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "append", "(Ljava/lang/CharSequence;)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(str((StringValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "append", "(Ljava/lang/CharSequence;II)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(str((StringValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.append(objl((ObjectValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "reverse", "()Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.reverse();
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "getChars", "(II[CI)V", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.getChars(i((IntValue) args.get(0)), i((IntValue) args.get(1)), arrc((ArrayValue) args.get(2)), i((IntValue) args.get(3)));
			return null;
		});
		registerMethodHandler("java/lang/StringBuffer", "compareTo", "(Ljava/lang/StringBuffer;)I", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> i(receiver.compareTo(BasicLookupUtils.<StringBuffer>obj((ObjectValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuffer", "indexOf", "(Ljava/lang/String;I)I", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> i(receiver.indexOf(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuffer", "indexOf", "(Ljava/lang/String;)I", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> i(receiver.indexOf(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuffer", "insert", "(ILjava/lang/Object;)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), objl((ObjectValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "insert", "(I[CII)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), arrc((ArrayValue) args.get(1)), i((IntValue) args.get(2)), i((IntValue) args.get(3)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "insert", "(I[C)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), arrc((ArrayValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "insert", "(IF)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), f((FloatValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "insert", "(IJ)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), j((LongValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "insert", "(ILjava/lang/String;)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), str((StringValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "insert", "(ILjava/lang/CharSequence;)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), str((StringValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "insert", "(ID)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), d((DoubleValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "insert", "(ILjava/lang/CharSequence;II)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), str((StringValue) args.get(1)), i((IntValue) args.get(2)), i((IntValue) args.get(3)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "insert", "(IZ)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), z((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "insert", "(II)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "insert", "(IC)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.insert(i((IntValue) args.get(0)), c((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "charAt", "(I)C", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> c(receiver.charAt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuffer", "codePointAt", "(I)I", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> i(receiver.codePointAt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuffer", "codePointBefore", "(I)I", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> i(receiver.codePointBefore(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuffer", "codePointCount", "(II)I", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> i(receiver.codePointCount(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuffer", "offsetByCodePoints", "(II)I", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> i(receiver.offsetByCodePoints(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuffer", "lastIndexOf", "(Ljava/lang/String;I)I", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> i(receiver.lastIndexOf(str((StringValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuffer", "lastIndexOf", "(Ljava/lang/String;)I", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> i(receiver.lastIndexOf(str((StringValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuffer", "substring", "(I)Ljava/lang/String;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> str(receiver.substring(i((IntValue) args.get(0)))));
		registerMethodHandler("java/lang/StringBuffer", "substring", "(II)Ljava/lang/String;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> str(receiver.substring(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuffer", "replace", "(IILjava/lang/String;)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.replace(i((IntValue) args.get(0)), i((IntValue) args.get(1)), str((StringValue) args.get(2)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "repeat", "(Ljava/lang/CharSequence;I)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.repeat(str((StringValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "repeat", "(II)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.repeat(i((IntValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "subSequence", "(II)Ljava/lang/CharSequence;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> str(receiver.subSequence(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
		registerMethodHandler("java/lang/StringBuffer", "delete", "(II)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.delete(i((IntValue) args.get(0)), i((IntValue) args.get(1)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "setLength", "(I)V", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.setLength(i((IntValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/lang/StringBuffer", "capacity", "()I", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> i(receiver.capacity()));
		registerMethodHandler("java/lang/StringBuffer", "ensureCapacity", "(I)V", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.ensureCapacity(i((IntValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/lang/StringBuffer", "trimToSize", "()V", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.trimToSize();
			return null;
		});
		registerMethodHandler("java/lang/StringBuffer", "setCharAt", "(IC)V", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.setCharAt(i((IntValue) args.get(0)), c((IntValue) args.get(1)));
			return null;
		});
		registerMethodHandler("java/lang/StringBuffer", "appendCodePoint", "(I)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.appendCodePoint(i((IntValue) args.get(0)));
			return host;
		});
		registerMethodHandler("java/lang/StringBuffer", "deleteCharAt", "(I)Ljava/lang/StringBuffer;", (ReFrame frame, ReValue host, StringBuffer receiver, List<ReValue> args) -> {
			receiver.deleteCharAt(i((IntValue) args.get(0)));
			return host;
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
			ArrayValue destinationValue = (ArrayValue) args.get(0);
			byte[] destination = arrb(destinationValue);
			receiver.nextBytes(destination);
			replaceByteArrayContents(frame, destinationValue, destination, 0, destination.length);
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

		// java.security.SecureRandom
		registerMapper(SecureRandom.class, "()V", (host, parameters) -> new SecureRandom());
		registerMapper(SecureRandom.class, "([B)V", (host, parameters) -> new SecureRandom(arrb((ArrayValue) parameters.get(0))));

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
		registerMethodHandler("java/io/ByteArrayOutputStream", "write", "(I)V", (ReFrame frame, ReValue host, ByteArrayOutputStream receiver, List<ReValue> args) -> {
			receiver.write(i((IntValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/io/ByteArrayOutputStream", "write", "([B)V", (ReFrame frame, ReValue host, ByteArrayOutputStream receiver, List<ReValue> args) -> {
			receiver.write(arrb((ArrayValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/io/ByteArrayOutputStream", "write", "([BII)V", (ReFrame frame, ReValue host, ByteArrayOutputStream receiver, List<ReValue> args) -> {
			receiver.write(arrb((ArrayValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)));
			return null;
		});

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

		// javax.crypto.Cipher
		registerMethodHandler("javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V", (ReFrame frame, ReValue host, Cipher receiver, List<ReValue> args) -> {
			receiver.init(i((IntValue) args.get(0)), requireRealInstance(args.get(1), Key.class), requireRealInstance(args.get(2), AlgorithmParameterSpec.class));
			return null;
		});
		registerMethodHandler("javax/crypto/Cipher", "doFinal", "([B)[B", (ReFrame frame, ReValue host, Cipher receiver, List<ReValue> args) -> arrb(receiver.doFinal(arrb((ArrayValue) args.get(0)))));
		registerMethodHandler("javax/crypto/Cipher", "init", "(ILjava/security/Key;)V", (ReFrame frame, ReValue host, Cipher receiver, List<ReValue> args) -> {
			receiver.init(i((IntValue) args.get(0)), requireRealInstance(args.get(1), Key.class));
			return null;
		});
		registerMethodHandler("javax/crypto/Cipher", "update", "([B)[B", (ReFrame frame, ReValue host, Cipher receiver, List<ReValue> args) ->
				arrb(receiver.update(arrb((ArrayValue) args.get(0)))));
		registerMethodHandler("javax/crypto/Cipher", "update", "([BII)[B", (ReFrame frame, ReValue host, Cipher receiver, List<ReValue> args) ->
				arrb(receiver.update(arrb((ArrayValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)))));
		registerMethodHandler("javax/crypto/Cipher", "doFinal", "()[B", (ReFrame frame, ReValue host, Cipher receiver, List<ReValue> args) ->
				arrb(receiver.doFinal()));

		// java.security.SecureRandom
		registerMethodHandler("java/security/SecureRandom", "nextBytes", "([B)V", (ReFrame frame, ReValue host, SecureRandom receiver, List<ReValue> args) -> {
			ArrayValue destinationValue = (ArrayValue) args.get(0);
			byte[] destination = arrb(destinationValue);
			receiver.nextBytes(destination);
			replaceByteArrayContents(frame, destinationValue, destination, 0, destination.length);
			return null;
		});
		registerMethodHandler("java/security/SecureRandom", "setSeed", "([B)V", (ReFrame frame, ReValue host, SecureRandom receiver, List<ReValue> args) -> {
			receiver.setSeed(arrb((ArrayValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/security/SecureRandom", "setSeed", "(J)V", (ReFrame frame, ReValue host, SecureRandom receiver, List<ReValue> args) -> {
			receiver.setSeed(j((LongValue) args.get(0)));
			return null;
		});
		registerMethodHandler("java/security/SecureRandom", "nextDouble", "()D", (ReFrame frame, ReValue host, SecureRandom receiver, List<ReValue> args) -> d(receiver.nextDouble()));
		registerMethodHandler("java/security/SecureRandom", "nextInt", "()I", (ReFrame frame, ReValue host, SecureRandom receiver, List<ReValue> args) -> i(receiver.nextInt()));
		registerMethodHandler("java/security/SecureRandom", "nextInt", "(I)I", (ReFrame frame, ReValue host, SecureRandom receiver, List<ReValue> args) -> i(receiver.nextInt(i((IntValue) args.get(0)))));
		registerMethodHandler("java/security/SecureRandom", "nextLong", "()J", (ReFrame frame, ReValue host, SecureRandom receiver, List<ReValue> args) -> j(receiver.nextLong()));
		registerMethodHandler("java/security/SecureRandom", "nextBoolean", "()Z", (ReFrame frame, ReValue host, SecureRandom receiver, List<ReValue> args) -> z(receiver.nextBoolean()));
		registerMethodHandler("java/security/SecureRandom", "nextFloat", "()F", (ReFrame frame, ReValue host, SecureRandom receiver, List<ReValue> args) -> f(receiver.nextFloat()));
		registerMethodHandler("java/security/SecureRandom", "nextGaussian", "()D", (ReFrame frame, ReValue host, SecureRandom receiver, List<ReValue> args) -> d(receiver.nextGaussian()));
		registerMethodHandler("java/security/SecureRandom", "generateSeed", "(I)[B", (ReFrame frame, ReValue host, SecureRandom receiver, List<ReValue> args) -> arrb(receiver.generateSeed(i((IntValue) args.get(0)))));

		// javax.crypto.KeyGenerator
		registerMethodHandler("javax/crypto/KeyGenerator", "init", "(I)V", (ReFrame frame, ReValue host, KeyGenerator receiver, List<ReValue> args) -> {
			receiver.init(i((IntValue) args.get(0)));
			return null;
		});
		registerMethodHandler("javax/crypto/KeyGenerator", "init", "(ILjava/security/SecureRandom;)V", (ReFrame frame, ReValue host, KeyGenerator receiver, List<ReValue> args) -> {
			receiver.init(i((IntValue) args.get(0)), requireRealInstance(args.get(1), SecureRandom.class));
			return null;
		});
		registerMethodHandler("javax/crypto/KeyGenerator", "generateKey", "()Ljavax/crypto/SecretKey;", (ReFrame frame, ReValue host, KeyGenerator receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.generateKey()));

		// javax.crypto.SecretKeyFactory
		registerMethodHandler("javax/crypto/SecretKeyFactory", "generateSecret", "(Ljava/security/spec/KeySpec;)Ljavax/crypto/SecretKey;", (ReFrame frame, ReValue host, SecretKeyFactory receiver, List<ReValue> args) -> new InstancedObjectValue<>(receiver.generateSecret(requireRealInstance(args.get(0), KeySpec.class))));

		// java.security.Key
		registerMethodHandler("java/security/Key", "getAlgorithm", "()Ljava/lang/String;", (ReFrame frame, ReValue host, Key receiver, List<ReValue> args) -> str(receiver.getAlgorithm()));
		registerMethodHandler("java/security/Key", "getFormat", "()Ljava/lang/String;", (ReFrame frame, ReValue host, Key receiver, List<ReValue> args) -> str(receiver.getFormat()));
		registerMethodHandler("java/security/Key", "getEncoded", "()[B", (ReFrame frame, ReValue host, Key receiver, List<ReValue> args) -> arrb(receiver.getEncoded()));
		registerMethodHandler("javax/crypto/SecretKey", "getAlgorithm", "()Ljava/lang/String;", (ReFrame frame, ReValue host, Key receiver, List<ReValue> args) -> str(receiver.getAlgorithm()));
		registerMethodHandler("javax/crypto/SecretKey", "getFormat", "()Ljava/lang/String;", (ReFrame frame, ReValue host, Key receiver, List<ReValue> args) -> str(receiver.getFormat()));
		registerMethodHandler("javax/crypto/SecretKey", "getEncoded", "()[B", (ReFrame frame, ReValue host, Key receiver, List<ReValue> args) -> arrb(receiver.getEncoded()));

		// javax.crypto.CipherInputStream
		registerMethodHandler("javax/crypto/CipherInputStream", "read", "()I", (ReFrame frame, ReValue host, InputStream receiver, List<ReValue> args) -> i(receiver.read()));
		registerMethodHandler("javax/crypto/CipherInputStream", "read", "([B)I", (ReFrame frame, ReValue host, InputStream receiver, List<ReValue> args) -> {
			ArrayValue destinationValue = (ArrayValue) args.get(0);
			byte[] destination = arrb(destinationValue);
			int read = receiver.read(destination);
			replaceByteArrayContents(frame, destinationValue, destination, 0, read);
			return i(read);
		});
		registerMethodHandler("javax/crypto/CipherInputStream", "read", "([BII)I", (ReFrame frame, ReValue host, InputStream receiver, List<ReValue> args) -> {
			ArrayValue destinationValue = (ArrayValue) args.get(0);
			byte[] destination = arrb(destinationValue);
			int offset = i((IntValue) args.get(1));
			int length = i((IntValue) args.get(2));
			int read = receiver.read(destination, offset, length);
			replaceByteArrayContents(frame, destinationValue, destination, offset, read);
			return i(read);
		});
		registerMethodHandler("javax/crypto/CipherInputStream", "readAllBytes", "()[B", (ReFrame frame, ReValue host, InputStream receiver, List<ReValue> args) -> arrb(receiver.readAllBytes()));
		registerMethodHandler("javax/crypto/CipherInputStream", "close", "()V", (ReFrame frame, ReValue host, InputStream receiver, List<ReValue> args) -> {
			receiver.close();
			return null;
		});

		// javax.crypto.CipherOutputStream
		registerMethodHandler("javax/crypto/CipherOutputStream", "write", "(I)V", (ReFrame frame, ReValue host, OutputStream receiver, List<ReValue> args) -> {
			receiver.write(i((IntValue) args.get(0)));
			return null;
		});
		registerMethodHandler("javax/crypto/CipherOutputStream", "write", "([B)V", (ReFrame frame, ReValue host, OutputStream receiver, List<ReValue> args) -> {
			receiver.write(arrb((ArrayValue) args.get(0)));
			return null;
		});
		registerMethodHandler("javax/crypto/CipherOutputStream", "write", "([BII)V", (ReFrame frame, ReValue host, OutputStream receiver, List<ReValue> args) -> {
			receiver.write(arrb((ArrayValue) args.get(0)), i((IntValue) args.get(1)), i((IntValue) args.get(2)));
			return null;
		});
		registerMethodHandler("javax/crypto/CipherOutputStream", "flush", "()V", (ReFrame frame, ReValue host, OutputStream receiver, List<ReValue> args) -> {
			receiver.flush();
			return null;
		});
		registerMethodHandler("javax/crypto/CipherOutputStream", "close", "()V", (ReFrame frame, ReValue host, OutputStream receiver, List<ReValue> args) -> {
			receiver.close();
			return null;
		});
	}

	/**
	 * Unlike others which are generated via {@link InstanceMapperGenerator} most of these are
	 * manually written in such a way that allows re-use across multiple collection types.
	 */
	@SuppressWarnings("all")
	private void registerCollectionMethodHandlers() {
		String[] collectionOwners = {
				"java/util/Collection",
				"java/util/List",
				"java/util/Set",
				"java/util/SortedSet",
				"java/util/NavigableSet",
				"java/util/ArrayList",
				"java/util/LinkedList",
				"java/util/HashSet",
				"java/util/LinkedHashSet",
				"java/util/TreeSet"
		};
		for (String owner : collectionOwners)
			registerCollectionMethods(owner);

		String[] listOwners = {"java/util/List", "java/util/ArrayList", "java/util/LinkedList"};
		for (String owner : listOwners)
			registerListMethods(owner);

		String[] sortedSetOwners = {"java/util/SortedSet", "java/util/TreeSet"};
		for (String owner : sortedSetOwners) {
			registerMethodHandler(owner, "first", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, SortedSet<?> receiver, List<ReValue> args) -> fromHostObject(receiver.first()));
			registerMethodHandler(owner, "last", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, SortedSet<?> receiver, List<ReValue> args) -> fromHostObject(receiver.last()));
		}

		String[] navigableSetOwners = {"java/util/NavigableSet", "java/util/TreeSet"};
		for (String owner : navigableSetOwners) {
			registerMethodHandler(owner, "lower", "(Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, NavigableSet<Object> receiver, List<ReValue> args) -> fromHostObject(receiver.lower(toHostObject(args.get(0)))));
			registerMethodHandler(owner, "floor", "(Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, NavigableSet<Object> receiver, List<ReValue> args) -> fromHostObject(receiver.floor(toHostObject(args.get(0)))));
			registerMethodHandler(owner, "ceiling", "(Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, NavigableSet<Object> receiver, List<ReValue> args) -> fromHostObject(receiver.ceiling(toHostObject(args.get(0)))));
			registerMethodHandler(owner, "higher", "(Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, NavigableSet<Object> receiver, List<ReValue> args) -> fromHostObject(receiver.higher(toHostObject(args.get(0)))));
		}

		String[] mapOwners = {
				"java/util/Map", "java/util/SortedMap", "java/util/NavigableMap",
				"java/util/HashMap", "java/util/LinkedHashMap", "java/util/TreeMap"
		};
		for (String owner : mapOwners)
			registerMapMethods(owner);

		String[] sortedMapOwners = {"java/util/SortedMap", "java/util/TreeMap"};
		for (String owner : sortedMapOwners) {
			registerMethodHandler(owner, "firstKey", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, SortedMap<?, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.firstKey()));
			registerMethodHandler(owner, "lastKey", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, SortedMap<?, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.lastKey()));
		}

		String[] navigableMapOwners = {"java/util/NavigableMap", "java/util/TreeMap"};
		for (String owner : navigableMapOwners) {
			registerMethodHandler(owner, "lowerKey", "(Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, NavigableMap<Object, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.lowerKey(toHostObject(args.get(0)))));
			registerMethodHandler(owner, "floorKey", "(Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, NavigableMap<Object, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.floorKey(toHostObject(args.get(0)))));
			registerMethodHandler(owner, "ceilingKey", "(Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, NavigableMap<Object, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.ceilingKey(toHostObject(args.get(0)))));
			registerMethodHandler(owner, "higherKey", "(Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, NavigableMap<Object, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.higherKey(toHostObject(args.get(0)))));
		}

		registerIteratorMethods();
		registerEntryMethods();
	}

	@SuppressWarnings("all")
	private void registerCollectionMethods(@Nonnull String owner) {
		registerMethodHandler(owner, "size", "()I", (ReFrame frame, ReValue host, Collection<?> receiver, List<ReValue> args) -> i(receiver.size()));
		registerMethodHandler(owner, "isEmpty", "()Z", (ReFrame frame, ReValue host, Collection<?> receiver, List<ReValue> args) -> z(receiver.isEmpty()));
		registerMethodHandler(owner, "contains", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Collection<?> receiver, List<ReValue> args) -> z(receiver.contains(toHostObject(args.get(0)))));
		registerMethodHandler(owner, "add", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Collection<Object> receiver, List<ReValue> args) -> z(receiver.add(toHostObject(args.get(0)))));
		registerMethodHandler(owner, "remove", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Collection<?> receiver, List<ReValue> args) -> z(receiver.remove(toHostObject(args.get(0)))));
		registerMethodHandler(owner, "containsAll", "(Ljava/util/Collection;)Z", (ReFrame frame, ReValue host, Collection<?> receiver, List<ReValue> args) -> z(receiver.containsAll((Collection<?>) toHostObject(args.get(0)))));
		registerMethodHandler(owner, "addAll", "(Ljava/util/Collection;)Z", (ReFrame frame, ReValue host, Collection<Object> receiver, List<ReValue> args) -> z(receiver.addAll((Collection<?>) toHostObject(args.get(0)))));
		registerMethodHandler(owner, "removeAll", "(Ljava/util/Collection;)Z", (ReFrame frame, ReValue host, Collection<?> receiver, List<ReValue> args) -> z(receiver.removeAll((Collection<?>) toHostObject(args.get(0)))));
		registerMethodHandler(owner, "retainAll", "(Ljava/util/Collection;)Z", (ReFrame frame, ReValue host, Collection<?> receiver, List<ReValue> args) -> z(receiver.retainAll((Collection<?>) toHostObject(args.get(0)))));
		registerMethodHandler(owner, "clear", "()V", (ReFrame frame, ReValue host, Collection<?> receiver, List<ReValue> args) -> {
			receiver.clear();
			return null;
		});
		registerMethodHandler(owner, "iterator", "()Ljava/util/Iterator;", (ReFrame frame, ReValue host, Collection<?> receiver, List<ReValue> args) -> fromHostObject(receiver.iterator()));
		registerMethodHandler(owner, "toArray", "()[Ljava/lang/Object;", (ReFrame frame, ReValue host, Collection<?> receiver, List<ReValue> args) -> fromHostObject(receiver.toArray()));
		registerMethodHandler(owner, "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", (ReFrame frame, ReValue host, Collection<?> receiver, List<ReValue> args) -> fromHostObject(receiver.toArray(toHostObjectArray(args.get(0)))));
	}

	@SuppressWarnings("all")
	private void registerListMethods(@Nonnull String owner) {
		registerMethodHandler(owner, "get", "(I)Ljava/lang/Object;", (ReFrame frame, ReValue host, List<?> receiver, List<ReValue> args) -> fromHostObject(receiver.get(i((IntValue) args.get(0)))));
		registerMethodHandler(owner, "set", "(ILjava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, List<Object> receiver, List<ReValue> args) -> fromHostObject(receiver.set(i((IntValue) args.get(0)), toHostObject(args.get(1)))));
		registerMethodHandler(owner, "add", "(ILjava/lang/Object;)V", (ReFrame frame, ReValue host, List<Object> receiver, List<ReValue> args) -> {
			receiver.add(i((IntValue) args.get(0)), toHostObject(args.get(1)));
			return null;
		});
		registerMethodHandler(owner, "remove", "(I)Ljava/lang/Object;", (ReFrame frame, ReValue host, List<?> receiver, List<ReValue> args) -> fromHostObject(receiver.remove(i((IntValue) args.get(0)))));
		registerMethodHandler(owner, "indexOf", "(Ljava/lang/Object;)I", (ReFrame frame, ReValue host, List<?> receiver, List<ReValue> args) -> i(receiver.indexOf(toHostObject(args.get(0)))));
		registerMethodHandler(owner, "lastIndexOf", "(Ljava/lang/Object;)I", (ReFrame frame, ReValue host, List<?> receiver, List<ReValue> args) -> i(receiver.lastIndexOf(toHostObject(args.get(0)))));
		registerMethodHandler(owner, "subList", "(II)Ljava/util/List;", (ReFrame frame, ReValue host, List<?> receiver, List<ReValue> args) -> fromHostObject(receiver.subList(i((IntValue) args.get(0)), i((IntValue) args.get(1)))));
	}

	@SuppressWarnings("all")
	private void registerMapMethods(@Nonnull String owner) {
		registerMethodHandler(owner, "size", "()I", (ReFrame frame, ReValue host, Map<?, ?> receiver, List<ReValue> args) -> i(receiver.size()));
		registerMethodHandler(owner, "isEmpty", "()Z", (ReFrame frame, ReValue host, Map<?, ?> receiver, List<ReValue> args) -> z(receiver.isEmpty()));
		registerMethodHandler(owner, "containsKey", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Map<?, ?> receiver, List<ReValue> args) -> z(receiver.containsKey(toHostObject(args.get(0)))));
		registerMethodHandler(owner, "containsValue", "(Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Map<?, ?> receiver, List<ReValue> args) -> z(receiver.containsValue(toHostObject(args.get(0)))));
		registerMethodHandler(owner, "get", "(Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, Map<?, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.get(toHostObject(args.get(0)))));
		registerMethodHandler(owner, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, Map<Object, Object> receiver, List<ReValue> args) -> fromHostObject(receiver.put(toHostObject(args.get(0)), toHostObject(args.get(1)))));
		registerMethodHandler(owner, "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, Map<?, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.remove(toHostObject(args.get(0)))));
		registerMethodHandler(owner, "putAll", "(Ljava/util/Map;)V", (ReFrame frame, ReValue host, Map<Object, Object> receiver, List<ReValue> args) -> {
			receiver.putAll((Map<?, ?>) toHostObject(args.get(0)));
			return null;
		});
		registerMethodHandler(owner, "clear", "()V", (ReFrame frame, ReValue host, Map<?, ?> receiver, List<ReValue> args) -> {
			receiver.clear();
			return null;
		});
		registerMethodHandler(owner, "keySet", "()Ljava/util/Set;", (ReFrame frame, ReValue host, Map<?, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.keySet()));
		registerMethodHandler(owner, "values", "()Ljava/util/Collection;", (ReFrame frame, ReValue host, Map<?, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.values()));
		registerMethodHandler(owner, "entrySet", "()Ljava/util/Set;", (ReFrame frame, ReValue host, Map<?, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.entrySet()));
		registerMethodHandler(owner, "getOrDefault", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, Map<Object, Object> receiver, List<ReValue> args) -> fromHostObject(receiver.getOrDefault(toHostObject(args.get(0)), toHostObject(args.get(1)))));
		registerMethodHandler(owner, "putIfAbsent", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, Map<Object, Object> receiver, List<ReValue> args) -> fromHostObject(receiver.putIfAbsent(toHostObject(args.get(0)), toHostObject(args.get(1)))));
		registerMethodHandler(owner, "remove", "(Ljava/lang/Object;Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Map<?, ?> receiver, List<ReValue> args) -> z(receiver.remove(toHostObject(args.get(0)), toHostObject(args.get(1)))));
		registerMethodHandler(owner, "replace", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, Map<Object, Object> receiver, List<ReValue> args) -> fromHostObject(receiver.replace(toHostObject(args.get(0)), toHostObject(args.get(1)))));
		registerMethodHandler(owner, "replace", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z", (ReFrame frame, ReValue host, Map<Object, Object> receiver, List<ReValue> args) -> z(receiver.replace(toHostObject(args.get(0)), toHostObject(args.get(1)), toHostObject(args.get(2)))));
	}

	@SuppressWarnings("all")
	private void registerIteratorMethods() {
		String owner = "java/util/Iterator";
		registerMethodHandler(owner, "hasNext", "()Z", (ReFrame frame, ReValue host, Iterator<?> receiver, List<ReValue> args) -> z(receiver.hasNext()));
		registerMethodHandler(owner, "next", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, Iterator<?> receiver, List<ReValue> args) -> fromHostObject(receiver.next()));
		registerMethodHandler(owner, "remove", "()V", (ReFrame frame, ReValue host, Iterator<?> receiver, List<ReValue> args) -> {
			receiver.remove();
			return null;
		});
	}

	@SuppressWarnings("all")
	private void registerEntryMethods() {
		String owner = "java/util/Map$Entry";
		registerMethodHandler(owner, "getKey", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, Map.Entry<?, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.getKey()));
		registerMethodHandler(owner, "getValue", "()Ljava/lang/Object;", (ReFrame frame, ReValue host, Map.Entry<?, ?> receiver, List<ReValue> args) -> fromHostObject(receiver.getValue()));
		registerMethodHandler(owner, "setValue", "(Ljava/lang/Object;)Ljava/lang/Object;", (ReFrame frame, ReValue host, Map.Entry<Object, Object> receiver, List<ReValue> args) -> fromHostObject(receiver.setValue(toHostObject(args.get(0)))));
	}

	@SuppressWarnings("all")
	private void registerCollectionCtorMappers() {
		// Copy constructors retain the source collection's host-backed identity and contents.
		registerMapper(ArrayList.class, "(Ljava/util/Collection;)V", (host, parameters) -> new ArrayList<>((Collection<?>) toHostObject(parameters.get(0))));
		registerMapper(LinkedList.class, "()V", (host, parameters) -> new LinkedList<>());
		registerMapper(LinkedList.class, "(Ljava/util/Collection;)V", (host, parameters) -> new LinkedList<>((Collection<?>) toHostObject(parameters.get(0))));
		registerMapper(HashSet.class, "()V", (host, parameters) -> new HashSet<>());
		registerMapper(HashSet.class, "(I)V", (host, parameters) -> new HashSet<>(i((IntValue) parameters.get(0))));
		registerMapper(HashSet.class, "(IF)V", (host, parameters) -> new HashSet<>(i((IntValue) parameters.get(0)), f((FloatValue) parameters.get(1))));
		registerMapper(HashSet.class, "(Ljava/util/Collection;)V", (host, parameters) -> new HashSet<>((Collection<?>) toHostObject(parameters.get(0))));
		registerMapper(LinkedHashSet.class, "()V", (host, parameters) -> new LinkedHashSet<>());
		registerMapper(LinkedHashSet.class, "(I)V", (host, parameters) -> new LinkedHashSet<>(i((IntValue) parameters.get(0))));
		registerMapper(LinkedHashSet.class, "(IF)V", (host, parameters) -> new LinkedHashSet<>(i((IntValue) parameters.get(0)), f((FloatValue) parameters.get(1))));
		registerMapper(LinkedHashSet.class, "(Ljava/util/Collection;)V", (host, parameters) -> new LinkedHashSet<>((Collection<?>) toHostObject(parameters.get(0))));
		registerMapper(TreeSet.class, "()V", (host, parameters) -> new TreeSet<>());
		registerMapper(TreeSet.class, "(Ljava/util/Collection;)V", (host, parameters) -> new TreeSet<>((Collection<?>) toHostObject(parameters.get(0))));
		registerMapper(HashMap.class, "()V", (host, parameters) -> new HashMap<>());
		registerMapper(HashMap.class, "(I)V", (host, parameters) -> new HashMap<>(i((IntValue) parameters.get(0))));
		registerMapper(HashMap.class, "(IF)V", (host, parameters) -> new HashMap<>(i((IntValue) parameters.get(0)), f((FloatValue) parameters.get(1))));
		registerMapper(HashMap.class, "(Ljava/util/Map;)V", (host, parameters) -> new HashMap<>((Map<?, ?>) toHostObject(parameters.get(0))));
		registerMapper(LinkedHashMap.class, "()V", (host, parameters) -> new LinkedHashMap<>());
		registerMapper(LinkedHashMap.class, "(I)V", (host, parameters) -> new LinkedHashMap<>(i((IntValue) parameters.get(0))));
		registerMapper(LinkedHashMap.class, "(IF)V", (host, parameters) -> new LinkedHashMap<>(i((IntValue) parameters.get(0)), f((FloatValue) parameters.get(1))));
		registerMapper(LinkedHashMap.class, "(IFZ)V", (host, parameters) -> new LinkedHashMap<>(i((IntValue) parameters.get(0)), f((FloatValue) parameters.get(1)), z((IntValue) parameters.get(2))));
		registerMapper(LinkedHashMap.class, "(Ljava/util/Map;)V", (host, parameters) -> new LinkedHashMap<>((Map<?, ?>) toHostObject(parameters.get(0))));
		registerMapper(TreeMap.class, "()V", (host, parameters) -> new TreeMap<>());
		registerMapper(TreeMap.class, "(Ljava/util/Map;)V", (host, parameters) -> new TreeMap<>((Map<?, ?>) toHostObject(parameters.get(0))));
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

		// java.lang.StringBuffer
		registerMapper(StringBuffer.class, "(Ljava/lang/CharSequence;)V", (host, parameters) -> new StringBuffer(str((StringValue) parameters.get(0))));
		registerMapper(StringBuffer.class, "(Ljava/lang/String;)V", (host, parameters) -> new StringBuffer(str((StringValue) parameters.get(0))));
		registerMapper(StringBuffer.class, "(I)V", (host, parameters) -> new StringBuffer(i((IntValue) parameters.get(0))));
		registerMapper(StringBuffer.class, "()V", (host, parameters) -> new StringBuffer());

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

		// javax.crypto.spec
		registerMapper(SecretKeySpec.class, "([BLjava/lang/String;)V", (host, parameters) -> new SecretKeySpec(arrb((ArrayValue) parameters.get(0)), str((StringValue) parameters.get(1))));
		registerMapper(IvParameterSpec.class, "([B)V", (host, parameters) -> new IvParameterSpec(arrb((ArrayValue) parameters.get(0))));

		// javax.crypto.spec.PBEKeySpec
		registerMapper(PBEKeySpec.class, "([C)V", (host, parameters) -> new PBEKeySpec(arrc((ArrayValue) parameters.get(0))));
		registerMapper(PBEKeySpec.class, "([C[BII)V", (host, parameters) -> new PBEKeySpec(arrc((ArrayValue) parameters.get(0)), arrb((ArrayValue) parameters.get(1)), i((IntValue) parameters.get(2)), i((IntValue) parameters.get(3))));
		registerMapper(PBEKeySpec.class, "([C[BI)V", (host, parameters) -> new PBEKeySpec(arrc((ArrayValue) parameters.get(0)), arrb((ArrayValue) parameters.get(1)), i((IntValue) parameters.get(2))));

		// javax.crypto streams
		registerMapper(CipherInputStream.class, "(Ljava/io/InputStream;Ljavax/crypto/Cipher;)V", (host, parameters) -> new CipherInputStream(requireRealInstance(parameters.get(0), InputStream.class), requireRealInstance(parameters.get(1), Cipher.class)));
		registerMapper(CipherOutputStream.class, "(Ljava/io/OutputStream;Ljavax/crypto/Cipher;)V", (host, parameters) -> new CipherOutputStream(requireRealInstance(parameters.get(0), OutputStream.class), requireRealInstance(parameters.get(1), Cipher.class)));
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

		// javax.crypto.Cipher
		registerStaticMapper(Cipher.class, "getInstance(Ljava/lang/String;)Ljavax/crypto/Cipher;", (host, parameters) -> Cipher.getInstance(str((StringValue) parameters.get(0))));

		// javax.crypto.KeyGenerator
		registerStaticMapper(KeyGenerator.class, "getInstance(Ljava/lang/String;)Ljavax/crypto/KeyGenerator;", (host, parameters) -> KeyGenerator.getInstance(str((StringValue) parameters.get(0))));

		// javax.crypto.SecretKeyFactory
		registerStaticMapper(SecretKeyFactory.class, "getInstance(Ljava/lang/String;)Ljavax/crypto/SecretKeyFactory;", (host, parameters) -> SecretKeyFactory.getInstance(str((StringValue) parameters.get(0))));

		// java.security.SecureRandom
		registerStaticMapper(SecureRandom.class, "getInstance(Ljava/lang/String;)Ljava/security/SecureRandom;", (host, parameters) -> SecureRandom.getInstance(str((StringValue) parameters.get(0))));
		registerStaticMapper(SecureRandom.class, "getInstanceStrong()Ljava/security/SecureRandom;", (host, parameters) -> SecureRandom.getInstanceStrong());
	}

	/**
	 * Register static methods that mutate evaluator state rather than mapping a host instance.
	 */
	private void registerStaticMethodHandlers() {
		// Its just one method. How complex can it be?
		//  [aware.gif]
		registerStaticMethodHandler("java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", new MethodInvokeStaticHandler() {
			@Nullable
			@Override
			public ReValue invoke(@Nonnull ReFrame frame, @Nonnull ReInterpreter interpreter,
			                      @Nonnull MethodInsnNode instruction, @Nonnull List<ReValue> args) throws Throwable {
				// Resolve references first so known nulls retain the JVM's exception precedence.
				ReValue sourceValue = args.get(0);
				ReValue destinationValue = args.get(2);
				if (sourceValue instanceof ObjectValue sourceObject
						&& sourceObject.isNull()
						|| destinationValue instanceof ObjectValue destinationObject
						&& destinationObject.isNull())
					throw new NullPointerException();
				ArrayValue source = requireArray(sourceValue, instruction, "source");
				ArrayValue destination = requireArray(destinationValue, instruction, "destination");

				// Resolve concrete positions and lengths before checking the copy range.
				int sourcePosition = requireIndex(args.get(1), instruction, "source position");
				int destinationPosition = requireIndex(args.get(3), instruction, "destination position");
				int length = requireIndex(args.get(4), instruction, "length");
				int sourceLength = requireLength(source, instruction, "source");
				int destinationLength = requireLength(destination, instruction, "destination");

				// Check for out-of-bounds copy ranges. The JVM checks for negative values first, then overflows.
				if (sourcePosition < 0
						|| destinationPosition < 0
						|| length < 0
						|| (long) sourcePosition + length > sourceLength
						|| (long) destinationPosition + length > destinationLength)
					throw new ArrayIndexOutOfBoundsException();

				// Primitive arrays require exact component types while reference elements are checked individually.
				Type sourceComponentType = getComponentType(source);
				Type destinationComponentType = getComponentType(destination);
				boolean sourceReferenceArray = sourceComponentType.getSort() == Type.OBJECT || sourceComponentType.getSort() == Type.ARRAY;
				boolean destinationReferenceArray = destinationComponentType.getSort() == Type.OBJECT || destinationComponentType.getSort() == Type.ARRAY;
				if (sourceReferenceArray != destinationReferenceArray || !sourceReferenceArray && !sourceComponentType.equals(destinationComponentType))
					throw new ArrayStoreException();

				// Snapshot the source before constructing the destination so overlaps have std::memmove semantics.
				List<ReValue> copiedValues = new ArrayList<>(length);
				for (int i = 0; i < length; i++) {
					ReValue value = source.getValue(sourcePosition + i);
					if (value == null)
						throw new AnalyzerException(instruction, "Unknown source value in System.arraycopy");
					if (destinationReferenceArray)
						validateReference(destinationComponentType, value, interpreter, instruction);
					copiedValues.add(value);
				}

				// Preserve every destination slot outside the copied range in one replacement value.
				List<ReValue> destinationValues = new ArrayList<>(destinationLength);
				for (int i = 0; i < destinationLength; i++) {
					ReValue value = i >= destinationPosition
							&& i < (long) destinationPosition + length
							? copiedValues.get(i - destinationPosition)
							: destination.getValue(i);
					if (value == null)
						throw new AnalyzerException(instruction, "Unknown destination value in System.arraycopy");
					destinationValues.add(value);
				}

				// Create a replacement array value with the new contents and replace the destination value in the frame.
				ArrayValue replacement = new ArrayValueImpl(destination.type(), destination.nullness(), destinationLength, destinationValues::get);
				frame.replaceValue(destination, replacement);
				return null;
			}

			@Nonnull
			private static ArrayValue requireArray(@Nonnull ReValue value, @Nonnull MethodInsnNode instruction,
			                                       @Nonnull String role) throws AnalyzerException {
				if (!(value instanceof ArrayValue array)) {
					if (value instanceof ObjectValue object && object.isNotNull())
						throw new ArrayStoreException();
					throw new AnalyzerException(instruction, "System.arraycopy " + role + " is not a known array");
				}
				if (!array.isNotNull())
					throw new AnalyzerException(instruction, "System.arraycopy " + role + " may be null");
				return array;
			}

			private static int requireIndex(@Nonnull ReValue value, @Nonnull MethodInsnNode instruction,
			                                @Nonnull String role) throws AnalyzerException {
				if (value instanceof IntValue intValue && intValue.value().isPresent())
					return intValue.value().getAsInt();
				throw new AnalyzerException(instruction, "System.arraycopy " + role + " is unknown");
			}

			private static int requireLength(@Nonnull ArrayValue array, @Nonnull MethodInsnNode instruction,
			                                 @Nonnull String role) throws AnalyzerException {
				if (array.getFirstDimensionLength().isPresent())
					return array.getFirstDimensionLength().getAsInt();
				throw new AnalyzerException(instruction, "System.arraycopy " + role + " length is unknown");
			}

			@Nonnull
			private static Type getComponentType(@Nonnull ArrayValue array) {
				return Type.getType(array.type().getDescriptor().substring(1));
			}

			private static void validateReference(@Nonnull Type destinationComponentType, @Nonnull ReValue value,
			                                      @Nonnull ReInterpreter interpreter, @Nonnull MethodInsnNode instruction) throws AnalyzerException {
				if (!(value instanceof ObjectValue object))
					throw new AnalyzerException(instruction, "System.arraycopy contains an unknown reference value");
				if (object.isNull())
					return;
				if (!object.isNotNull())
					throw new AnalyzerException(instruction, "System.arraycopy contains a possibly null reference value");
				if (!interpreter.isAssignableFrom(destinationComponentType.getInternalName(), object.type().getInternalName()))
					throw new ArrayStoreException();
			}
		});

	}

	/**
	 * Similar to {@link #registerCollectionMethodHandlers()} this is a collection of static mappers
	 * that are manually written to allow re-use across multiple collection types.
	 */
	@SuppressWarnings("all")
	private void registerCollectionStaticMappers() {
		// java.util.List
		registerStaticMapper(List.class, "of()Ljava/util/List;", (host, parameters) -> List.of());
		for (int count = 1; count <= 10; count++)
			registerStaticMapper(List.class, "of(" + "Ljava/lang/Object;".repeat(count) + ")Ljava/util/List;", (host, parameters) -> listOf(parameters));
		registerStaticMapper(List.class, "of([Ljava/lang/Object;)Ljava/util/List;", (host, parameters) -> List.of(toHostObjectArray(parameters.get(0))));
		registerStaticMapper(List.class, "copyOf(Ljava/util/Collection;)Ljava/util/List;", (host, parameters) -> List.copyOf((Collection<?>) toHostObject(parameters.get(0))));

		// java.util.Set
		registerStaticMapper(Set.class, "of()Ljava/util/Set;", (host, parameters) -> Set.of());
		for (int count = 1; count <= 10; count++)
			registerStaticMapper(Set.class, "of(" + "Ljava/lang/Object;".repeat(count) + ")Ljava/util/Set;", (host, parameters) -> setOf(parameters));
		registerStaticMapper(Set.class, "of([Ljava/lang/Object;)Ljava/util/Set;", (host, parameters) -> Set.of(toHostObjectArray(parameters.get(0))));
		registerStaticMapper(Set.class, "copyOf(Ljava/util/Collection;)Ljava/util/Set;", (host, parameters) -> Set.copyOf((Collection<?>) toHostObject(parameters.get(0))));

		// java.util.Map
		registerStaticMapper(Map.class, "of()Ljava/util/Map;", (host, parameters) -> Map.of());
		for (int count = 1; count <= 10; count++)
			registerStaticMapper(Map.class, "of(" + "Ljava/lang/Object;Ljava/lang/Object;".repeat(count) + ")Ljava/util/Map;", (host, parameters) -> mapOf(parameters));
		registerStaticMapper(Map.class, "entry(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map$Entry;", (host, parameters) -> Map.entry(toHostObject(parameters.get(0)), toHostObject(parameters.get(1))));
		registerStaticMapper(Map.class, "ofEntries([Ljava/util/Map$Entry;)Ljava/util/Map;", (host, parameters) -> mapOfEntries(toHostObjectArray(parameters.get(0))));
		registerStaticMapper(Map.class, "copyOf(Ljava/util/Map;)Ljava/util/Map;", (host, parameters) -> Map.copyOf((Map<?, ?>) toHostObject(parameters.get(0))));

		// java.util.Collections
		registerStaticMapper(Collections.class, "emptyList()Ljava/util/List;", (host, parameters) -> Collections.emptyList());
		registerStaticMapper(Collections.class, "emptySet()Ljava/util/Set;", (host, parameters) -> Collections.emptySet());
		registerStaticMapper(Collections.class, "emptyMap()Ljava/util/Map;", (host, parameters) -> Collections.emptyMap());
		registerStaticMapper(Collections.class, "emptySortedSet()Ljava/util/SortedSet;", (host, parameters) -> Collections.emptySortedSet());
		registerStaticMapper(Collections.class, "emptyNavigableSet()Ljava/util/NavigableSet;", (host, parameters) -> Collections.emptyNavigableSet());
		registerStaticMapper(Collections.class, "emptySortedMap()Ljava/util/SortedMap;", (host, parameters) -> Collections.emptySortedMap());
		registerStaticMapper(Collections.class, "emptyNavigableMap()Ljava/util/NavigableMap;", (host, parameters) -> Collections.emptyNavigableMap());
		registerStaticMapper(Collections.class, "singleton(Ljava/lang/Object;)Ljava/util/Set;", (host, parameters) -> Collections.singleton(toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "singletonList(Ljava/lang/Object;)Ljava/util/List;", (host, parameters) -> Collections.singletonList(toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "singletonMap(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;", (host, parameters) -> Collections.singletonMap(toHostObject(parameters.get(0)), toHostObject(parameters.get(1))));
		registerStaticMapper(Collections.class, "nCopies(ILjava/lang/Object;)Ljava/util/List;", (host, parameters) -> Collections.nCopies(i((IntValue) parameters.get(0)), toHostObject(parameters.get(1))));
		registerStaticMapper(Collections.class, "unmodifiableCollection(Ljava/util/Collection;)Ljava/util/Collection;", (host, parameters) -> Collections.unmodifiableCollection((Collection<?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "unmodifiableList(Ljava/util/List;)Ljava/util/List;", (host, parameters) -> Collections.unmodifiableList((List<?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "unmodifiableSet(Ljava/util/Set;)Ljava/util/Set;", (host, parameters) -> Collections.unmodifiableSet((Set<?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "unmodifiableSortedSet(Ljava/util/SortedSet;)Ljava/util/SortedSet;", (host, parameters) -> Collections.unmodifiableSortedSet((SortedSet<?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "unmodifiableNavigableSet(Ljava/util/NavigableSet;)Ljava/util/NavigableSet;", (host, parameters) -> Collections.unmodifiableNavigableSet((NavigableSet<?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "unmodifiableMap(Ljava/util/Map;)Ljava/util/Map;", (host, parameters) -> Collections.unmodifiableMap((Map<?, ?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "unmodifiableSortedMap(Ljava/util/SortedMap;)Ljava/util/SortedMap;", (host, parameters) -> Collections.unmodifiableSortedMap((SortedMap<?, ?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "unmodifiableNavigableMap(Ljava/util/NavigableMap;)Ljava/util/NavigableMap;", (host, parameters) -> Collections.unmodifiableNavigableMap((NavigableMap<?, ?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "synchronizedCollection(Ljava/util/Collection;)Ljava/util/Collection;", (host, parameters) -> Collections.synchronizedCollection((Collection<?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "synchronizedList(Ljava/util/List;)Ljava/util/List;", (host, parameters) -> Collections.synchronizedList((List<?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "synchronizedSet(Ljava/util/Set;)Ljava/util/Set;", (host, parameters) -> Collections.synchronizedSet((Set<?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "synchronizedSortedSet(Ljava/util/SortedSet;)Ljava/util/SortedSet;", (host, parameters) -> Collections.synchronizedSortedSet((SortedSet<?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "synchronizedNavigableSet(Ljava/util/NavigableSet;)Ljava/util/NavigableSet;", (host, parameters) -> Collections.synchronizedNavigableSet((NavigableSet<?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "synchronizedMap(Ljava/util/Map;)Ljava/util/Map;", (host, parameters) -> Collections.synchronizedMap((Map<?, ?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "synchronizedSortedMap(Ljava/util/SortedMap;)Ljava/util/SortedMap;", (host, parameters) -> Collections.synchronizedSortedMap((SortedMap<?, ?>) toHostObject(parameters.get(0))));
		registerStaticMapper(Collections.class, "synchronizedNavigableMap(Ljava/util/NavigableMap;)Ljava/util/NavigableMap;", (host, parameters) -> Collections.synchronizedNavigableMap((NavigableMap<?, ?>) toHostObject(parameters.get(0))));
	}

	@SuppressWarnings("all")
	private static List<?> listOf(@Nonnull List<ReValue> parameters) {
		return switch (parameters.size()) {
			case 1 -> List.of((Object) toHostObject(parameters.get(0)));
			case 2 -> List.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)));
			case 3 ->
					List.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)));
			case 4 ->
					List.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)));
			case 5 ->
					List.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)));
			case 6 ->
					List.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)));
			case 7 ->
					List.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)));
			case 8 ->
					List.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)));
			case 9 ->
					List.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)), toHostObject(parameters.get(8)));
			case 10 ->
					List.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)), toHostObject(parameters.get(8)), toHostObject(parameters.get(9)));
			default -> throw new IllegalArgumentException("Unsupported List.of arity: " + parameters.size());
		};
	}

	@SuppressWarnings("all")
	private static Set<?> setOf(@Nonnull List<ReValue> parameters) {
		return switch (parameters.size()) {
			case 1 -> Set.of((Object) toHostObject(parameters.get(0)));
			case 2 -> Set.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)));
			case 3 ->
					Set.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)));
			case 4 ->
					Set.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)));
			case 5 ->
					Set.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)));
			case 6 ->
					Set.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)));
			case 7 ->
					Set.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)));
			case 8 ->
					Set.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)));
			case 9 ->
					Set.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)), toHostObject(parameters.get(8)));
			case 10 ->
					Set.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)), toHostObject(parameters.get(8)), toHostObject(parameters.get(9)));
			default -> throw new IllegalArgumentException("Unsupported Set.of arity: " + parameters.size());
		};
	}

	@SuppressWarnings("all")
	private static Map<?, ?> mapOf(@Nonnull List<ReValue> parameters) {
		return switch (parameters.size()) {
			case 2 -> Map.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)));
			case 4 ->
					Map.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)));
			case 6 ->
					Map.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)));
			case 8 ->
					Map.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)));
			case 10 ->
					Map.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)), toHostObject(parameters.get(8)), toHostObject(parameters.get(9)));
			case 12 ->
					Map.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)), toHostObject(parameters.get(8)), toHostObject(parameters.get(9)), toHostObject(parameters.get(10)), toHostObject(parameters.get(11)));
			case 14 ->
					Map.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)), toHostObject(parameters.get(8)), toHostObject(parameters.get(9)), toHostObject(parameters.get(10)), toHostObject(parameters.get(11)), toHostObject(parameters.get(12)), toHostObject(parameters.get(13)));
			case 16 ->
					Map.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)), toHostObject(parameters.get(8)), toHostObject(parameters.get(9)), toHostObject(parameters.get(10)), toHostObject(parameters.get(11)), toHostObject(parameters.get(12)), toHostObject(parameters.get(13)), toHostObject(parameters.get(14)), toHostObject(parameters.get(15)));
			case 18 ->
					Map.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)), toHostObject(parameters.get(8)), toHostObject(parameters.get(9)), toHostObject(parameters.get(10)), toHostObject(parameters.get(11)), toHostObject(parameters.get(12)), toHostObject(parameters.get(13)), toHostObject(parameters.get(14)), toHostObject(parameters.get(15)), toHostObject(parameters.get(16)), toHostObject(parameters.get(17)));
			case 20 ->
					Map.of(toHostObject(parameters.get(0)), toHostObject(parameters.get(1)), toHostObject(parameters.get(2)), toHostObject(parameters.get(3)), toHostObject(parameters.get(4)), toHostObject(parameters.get(5)), toHostObject(parameters.get(6)), toHostObject(parameters.get(7)), toHostObject(parameters.get(8)), toHostObject(parameters.get(9)), toHostObject(parameters.get(10)), toHostObject(parameters.get(11)), toHostObject(parameters.get(12)), toHostObject(parameters.get(13)), toHostObject(parameters.get(14)), toHostObject(parameters.get(15)), toHostObject(parameters.get(16)), toHostObject(parameters.get(17)), toHostObject(parameters.get(18)), toHostObject(parameters.get(19)));
			default -> throw new IllegalArgumentException("Unsupported Map.of arity: " + parameters.size());
		};
	}

	@SuppressWarnings("all")
	private static Map<?, ?> mapOfEntries(@Nonnull Object[] entries) {
		Map.Entry<?, ?>[] mapEntries = new Map.Entry<?, ?>[entries.length];
		for (int i = 0; i < entries.length; i++)
			mapEntries[i] = (Map.Entry<?, ?>) entries[i];
		return Map.ofEntries(mapEntries);
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
	 * Finds a frame-aware handler for a static method invocation.
	 *
	 * @param method
	 * 		Method instruction to find a handler for.
	 *
	 * @return Handler for the method instruction, or {@code null} if no handler is registered.
	 */
	@Nullable
	public MethodInvokeStaticHandler getStaticMethodHandler(@Nonnull MethodInsnNode method) {
		return staticMethodHandlers.get(method.owner + '.' + method.name + method.desc);
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
	 * 		Internal name of the method owner.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 * @param handler
	 * 		Handler to register.
	 */
	private void registerStaticMethodHandler(@Nonnull String owner, @Nonnull String name, @Nonnull String desc,
	                                         @Nonnull MethodInvokeStaticHandler handler) {
		staticMethodHandlers.put(owner + '.' + name + desc, handler);
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

	/**
	 * @param type
	 * 		Class to register as allocatable.
	 * @param desc
	 * 		Constructor descriptor to register.
	 * @param mapper
	 * 		Mapper to register for the constructor.
	 */
	private void registerMapper(@Nonnull Class<?> type, @Nonnull String desc, @Nonnull InstanceMapper mapper) {
		String internalName = type.getName().replace('.', '/');
		supportedTypes.add(internalName);
		mappers.put(internalName + '.' + desc, mapper);
	}

	/**
	 * Convert an evaluator object into the host object expected by a collection API.
	 *
	 * @param value
	 * 		Evaluator object to unwrap.
	 *
	 * @return Host-backed object, scalar, or {@code null}.
	 *
	 * @throws IllegalArgumentException
	 * 		When the value is unknown or evaluator-only.
	 */
	private static Object toHostObject(@Nonnull ReValue value) {
		if (value instanceof InstancedObjectValue<?> instanced) {
			Object realInstance = instanced.getRealInstance();
			if (realInstance != null)
				return realInstance;
		}
		if (value instanceof ObjectValue objectValue)
			return obj(objectValue);
		throw new IllegalArgumentException("Unsupported evaluator object: " + value);
	}

	/**
	 * Convert an evaluator object array into host objects.
	 *
	 * @param value
	 * 		Evaluator array to unwrap.
	 *
	 * @return Host array containing the converted elements.
	 *
	 * @throws IllegalArgumentException
	 * 		When the value is not a known object array.
	 */
	@Nullable
	private static Object[] toHostObjectArray(@Nonnull ReValue value) {
		if (!(value instanceof ArrayValue array))
			throw new IllegalArgumentException("Expected evaluator object array: " + value);
		if (array.isNull())
			return null;
		if (array.getFirstDimensionLength().isEmpty() || !array.hasKnownValue())
			throw new IllegalArgumentException("Unknown evaluator object array: " + value);
		int length = array.getFirstDimensionLength().getAsInt();
		Object[] objects = new Object[length];
		for (int i = 0; i < length; i++)
			objects[i] = toHostObject(array.getValue(i));
		return objects;
	}

	/**
	 * Convert a host result back into the evaluator representation while retaining host identity.
	 *
	 * @param value
	 * 		Host result, possibly {@code null}.
	 *
	 * @return Evaluator representation, using the evaluator null value for a {@code null} host result.
	 */
	@Nonnull
	private static ReValue fromHostObject(@Nullable Object value) {
		if (value == null)
			return ObjectValue.VAL_OBJECT_NULL;
		if (value.getClass().isArray())
			return new InstancedObjectValue<>(value).unmap();
		if (value instanceof CharSequence || value instanceof Number || value instanceof Character)
			return obj(value);
		if (value instanceof Boolean bool)
			return new software.coley.recaf.util.analysis.value.impl.BoxedBooleanValueImpl(bool);
		return new InstancedObjectValue<>(value);
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
