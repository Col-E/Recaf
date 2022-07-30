package me.coley.recaf.decompile.cfr;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.decompile.DecompileOption;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.ReflectUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.util.CfrVersionInfo;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.getopt.OptionDecoderParam;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CFR decompiler implementation.
 *
 * @author Matt Coley
 */
public class CfrDecompiler extends Decompiler {

	/**
	 * New CFR decompiler instance.
	 */
	public CfrDecompiler() {
		super("CFR", CfrVersionInfo.VERSION);
	}

	@Override
	protected String decompileImpl(Map<String, DecompileOption<?>> options, Workspace workspace, ClassInfo classInfo) {
		ClassSource source = new ClassSource(workspace, this);
		source.setOverrideClass(classInfo);
		String name = classInfo.getName();
		SinkFactoryImpl sink = new SinkFactoryImpl();
		CfrDriver driver = new CfrDriver.Builder()
				.withClassFileSource(source)
				.withOutputSink(sink)
				.withOptions(convertOptions(options))
				.build();
		driver.analyse(Collections.singletonList(name));
		String decompile = sink.getDecompilation();
		if (decompile == null)
			return "// ERROR: Failed to decompile '" + name + "'";
		return clean(decompile, name);
	}

	@Override
	protected Map<String, DecompileOption<?>> createDefaultOptions() {
		Map<String, DecompileOption<?>> map = new HashMap<>();
		for (PermittedOptionProvider.ArgumentParam<?, ?> param : OptionsImpl.getFactory()
				.getArguments()) {
			String name = param.getName();
			String desc = getOptHelp(param);
			String defaultValue = getOptValue(param);
			if (defaultValue != null) {
				// For options that depend on other options or class file specifics, ignore them
				if (defaultValue.contains("Value of option") || defaultValue.contains("if class"))
					continue;
				// Only populate boolean options
				if (defaultValue.equals("true") || defaultValue.equals("false"))
					map.put(param.getName(), new DecompileOption<>(String.class, name, desc, defaultValue));
				// There are only a few non-boolean options, and even supplying 'recpass'
				// causes some issues in edge cases (https://github.com/leibnitz27/cfr/issues/253)
			}
		}
		// TODO: Possible manual changes here
		/*
		// antiobf depends on 'forceexceptionprune=true'
		map.put("forceexceptionprune", new DecompileOption<>(String.class, "forceexceptionprune",
				"Remove nested exception handlers if they don't change semantics", "true"));
		map.put("antiobf", new DecompileOption<>(String.class, "antiobf",
				"Undo various obfuscations", "true"));
		 */
		return map;
	}

	/**
	 * Fetch default value from configuration parameter.
	 *
	 * @param param
	 * 		Parameter.
	 *
	 * @return Default value as string, may be {@code null}.
	 */
	private String getOptValue(PermittedOptionProvider.ArgumentParam<?, ?> param) {
		try {
			Field fn = PermittedOptionProvider.ArgumentParam.class.getDeclaredField("fn");
			fn.setAccessible(true);
			OptionDecoderParam<?, ?> decoder = (OptionDecoderParam<?, ?>) fn.get(param);
			return decoder.getDefaultValue();
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Failed to fetch default value from Cfr parameter, did" +
					" the backend change?");
		}
	}

	/**
	 * Fetch help description from configuration parameter.
	 *
	 * @param param
	 * 		Parameter.
	 *
	 * @return Help description string, may be {@code null}.
	 */
	private String getOptHelp(PermittedOptionProvider.ArgumentParam<?, ?> param) {
		try {
			Field fn = PermittedOptionProvider.ArgumentParam.class.getDeclaredField("help");
			fn.setAccessible(true);
			String value = (String) fn.get(param);
			if (StringUtil.isNullOrEmpty(value))
				value = "";
			return value;
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Failed to fetch description from Cfr parameter, did" +
					" the backend change?");
		}
	}

	/**
	 * @param options
	 * 		Map of decompiler option values.
	 *
	 * @return Map of option values mapped to strings.
	 */
	private static Map<String, String> convertOptions(Map<String, DecompileOption<?>> options) {
		return options.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, v -> String.valueOf(v.getValue().getValue())));
	}

	/**
	 * Remove watermark and other oddities from decompilation output.
	 *
	 * @param decompilationText
	 * 		Decompilation text.
	 * @param className
	 * 		Class name.
	 *
	 * @return Decompilation without watermark.
	 */
	private static String clean(String decompilationText, String className) {
		// Get rid of header comment
		if (decompilationText.startsWith("/*\n * Decompiled with CFR"))
			decompilationText = decompilationText.substring(decompilationText.indexOf("*/") + 3);
		// JavaParser does NOT like inline comments like this.
		decompilationText = decompilationText.replace("/* synthetic */ ", "");
		decompilationText = decompilationText.replace("/* bridge */ ", "");
		decompilationText = decompilationText.replace("/* enum */ ", "");
		// Fix inner class names being busted in decompilation text, needs to be "Inner$1"
		// instead of "Inner.1", as generated by CFR
		String classSimpleName = className.contains("/") ?
				className.substring(className.lastIndexOf('/') + 1) : className;
		if (classSimpleName.contains("$")) {
			String incorrectlyDecompiledClassSimpleName = classSimpleName.replace('$', '.');

			int indexOfincorrectlyDecompiledClassSimpleName =
					decompilationText.indexOf(incorrectlyDecompiledClassSimpleName);
			if (indexOfincorrectlyDecompiledClassSimpleName == -1) {
				// Generated CFR output does not match expectations.
				// Don't attempt to fix up matters and lets this pass through
				// with an indication that we encountered this challenge.
				// One example of this happening is in
				//   https://mvnrepository.com/artifact/com.google.code.gson/gson/2.2.4
				// with (true) class com.google.gson.internal.$Gson$Types$GenericArrayTypeImpl
				// being decompiled by CFR to com.google.gson.internal.$Gson$Types.GenericArrayTypeImpl
				// Note that singular dot in there generated by CFR.
				decompilationText = "// ERROR: Unable to apply inner class name fixup" + System.lineSeparator()
						+ decompilationText;
				return decompilationText;
			}

			decompilationText = decompilationText.replace(incorrectlyDecompiledClassSimpleName, classSimpleName);

			String startText = decompilationText.substring(0, decompilationText.indexOf(classSimpleName));
			String startTextCopy = startText;
			Set<AccessFlag> allowed = AccessFlag.getApplicableFlags(AccessFlag.Type.CLASS);
			for (AccessFlag acc : AccessFlag.values()) {
				if (allowed.contains(acc))
					continue;
				if (startText.contains(acc.getName() + " ")) {
					startText = startText.replace(startText,
							startText.replace(acc.getCodeFriendlyName() + " ", ""));
				}
			}
			decompilationText = decompilationText.replace(startTextCopy, startText);
		}
		return decompilationText;
	}

	static {
		try {
			Field field = ReflectUtil.getDeclaredField(DecompilerComment.class, "comment");
			ReflectUtil.quietSet(DecompilerComment.RENAME_MEMBERS, field, "Duplicate member names detected");
			ReflectUtil.quietSet(DecompilerComment.ILLEGAL_IDENTIFIERS, field, "Illegal identifiers detected");
			ReflectUtil.quietSet(DecompilerComment.MALFORMED_SWITCH, field, "Recovered potentially malformed switches");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
