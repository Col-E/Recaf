package me.coley.recaf.scripting;

import jregex.Matcher;
import me.coley.recaf.compile.CompileOption;
import me.coley.recaf.compile.CompilerDiagnostic;
import me.coley.recaf.compile.CompilerResult;
import me.coley.recaf.compile.javac.JavacCompiler;
import me.coley.recaf.util.DefineUtil;
import me.coley.recaf.util.RegexUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Engine for running scripts.
 *
 * @author Matt Coley
 */
public class ScriptEngine {
	private static final Logger logger = Logging.get(ScriptEngine.class);
	private static final Map<Integer, GenerateResult> SCRIPT_CLASS_CACHE = new HashMap<>();
	private static final String SCRIPT_PACKAGE_NAME = "me.coley.recaf.scripting.generated";
	private static final String PATTERN_PACKAGE = "package ([\\w\\.\\*]+);?";
	private static final String PATTERN_IMPORT = "import ([\\w\\.\\*]+);?";
	private static final String PATTERN_CLASS_NAME = "(?<=class)\\s+(\\w+)\\s+(?:implements|extends|\\{)";
	private static final List<String> DEFAULT_IMPORTS = Arrays.asList(
			"java.io.*",
			"java.nio.file.*",
			"java.util.*",
			"me.coley.recaf.*",
			"me.coley.recaf.code.*",
			"me.coley.recaf.util.*",
			"me.coley.recaf.util.threading.*",
			"me.coley.recaf.search.*",
			"me.coley.recaf.search.result.*",
			"me.coley.recaf.workspace.*",
			"me.coley.recaf.workspace.resource.*",
			"me.coley.recaf.scripting.impl.*",
			"org.objectweb.asm.*",
			"org.objectweb.asm.tree.*"
	);

	/**
	 * @param path
	 * 		Path to script file to execute.
	 *
	 * @return Script execution result.
	 */
	public static ScriptResult execute(Path path) throws IOException {
		String content = Files.readString(path);
		return execute(content);
	}

	/**
	 * @param script
	 * 		Script text to execute.
	 *
	 * @return Script execution result.
	 */
	public static ScriptResult execute(String script) {
		// TODO: Should make this async so calls from the UI don't freeze the UI
		return handleExecute(script);
	}

	private static ScriptResult handleExecute(String script) {
		int hash = script.hashCode();
		GenerateResult result;
		if (RegexUtil.matchesAny(PATTERN_CLASS_NAME, script)) {
			logger.info("Executing script class");
			result = SCRIPT_CLASS_CACHE.computeIfAbsent(hash, n -> generateStandardClass(script));
		} else {
			logger.info("Executing script");
			String className = "Script" + Math.abs(hash);
			result = SCRIPT_CLASS_CACHE.computeIfAbsent(hash, n -> generateScriptClass(className, script));
		}
		if (result.cls != null) {
			try {
				Method run = result.cls.getDeclaredMethod("run");
				run.invoke(null);
				logger.info("Successfully ran script");
				return new ScriptResult(result.diagnostics);
			} catch (Exception ex) {
				logger.error("Failed to execute script", ex);
				return new ScriptResult(result.diagnostics, ex);
			}
		} else {
			logger.error("Failed to compile script");
			return new ScriptResult(result.diagnostics);
		}
	}

	private static GenerateResult generateStandardClass(String source) {
		// Extract package name
		String packageName = SCRIPT_PACKAGE_NAME;
		Matcher matcher = RegexUtil.getMatcher(PATTERN_PACKAGE, source);
		if (matcher.find())
			packageName = matcher.group(1);
		else
			source = "package " + packageName +"; " + source;
		packageName = packageName.replace('.', '/');
		// Extract class name
		String className = null;
		matcher = RegexUtil.getMatcher(PATTERN_CLASS_NAME, source);
		if (matcher.find()) {
			className = packageName + "/" + matcher.group(1);
		} else {
			return new GenerateResult(null, List.of(new CompilerDiagnostic(-1, "Could not determine name of class")));
		}
		// Compile the class
		return defineClass(className, source);
	}

	private static GenerateResult generateScriptClass(String className, String script) {
		Set<String> imports = new HashSet<>(DEFAULT_IMPORTS);
		Matcher matcher = RegexUtil.getMatcher(PATTERN_IMPORT, script);
		while (matcher.find()) {
			// Record import statement
			String importIdentifier = matcher.group(1);
			imports.add(importIdentifier);
			// Replace text with spaces to maintain script character offsets
			String importMatch = script.substring(matcher.start(), matcher.end());
			script = script.replace(importMatch, StringUtil.repeat(" ", importMatch.length()));
		}
		// Create code (just a basic class with a static 'run' method)
		StringBuilder code = new StringBuilder(
				"public class " + className + " implements Opcodes { public static void run() {\n" + script + "\n" + "}}");
		for (String imp : imports)
			code.insert(0, "import " + imp + "; ");
		code.insert(0, "package " + SCRIPT_PACKAGE_NAME + "; ");
		className = SCRIPT_PACKAGE_NAME.replace('.', '/') + "/" + className;
		// Compile the class
		return defineClass(className, code.toString());
	}

	private static GenerateResult defineClass(String className, String source) {
		List<CompilerDiagnostic> diagnostics = new ArrayList<>();
		CompilerResult result = compile(className, source, diagnostics);
		if (result.wasSuccess()) {
			try {
				Class<?> cls = DefineUtil.create(result.getValue(), className);
				return new GenerateResult(cls, diagnostics);
			} catch (Exception ex) {
				logger.error("Failed to define generated script class", ex);
			}
		}
		return new GenerateResult(null, diagnostics);
	}

	private static CompilerResult compile(String className, String code, List<CompilerDiagnostic> diagnostics) {
		JavacCompiler compiler = new JavacCompiler();
		Map<String, CompileOption<?>> options = compiler.getDefaultOptions();
		compiler.setCompileListener(diagnostic -> diagnostics.add(
				new CompilerDiagnostic(
						(int) diagnostic.getLineNumber() - 1,
						diagnostic.getMessage(Locale.US))
		));
		compiler.setDebug(options, JavacCompiler.createDebugValue(true, true, true));
		return compiler.compile(className, code, options);
	}

	private static class GenerateResult {
		private final Class<?> cls;
		private final List<CompilerDiagnostic> diagnostics;

		private GenerateResult(Class<?> cls, List<CompilerDiagnostic> diagnostics) {
			this.cls = cls;
			this.diagnostics = diagnostics;
		}
	}
}
