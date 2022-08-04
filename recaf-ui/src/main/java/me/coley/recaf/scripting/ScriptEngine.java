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
 * Base script engine for running scripts.
 *
 * @author Wolfie / win32kbase
 */
public class ScriptEngine {
	private static final Logger logger = Logging.get(ScriptEngine.class);
	private static final Map<String, GenerateResult> SCRIPT_CLASS_CACHE = new HashMap<>();
	private static final String SCRIPT_PACKAGE_NAME = "me.coley.recaf.scripting.generated";
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
		return handleExecute(script);
	}

	private static ScriptResult handleExecute(String script) {
		logger.info("Executing script");
		String className = "Script" + Math.abs(script.hashCode());
		GenerateResult result = SCRIPT_CLASS_CACHE.computeIfAbsent(className, n -> generate(n, script));
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

	private static GenerateResult generate(String className, String script) {
		Set<String> imports = new HashSet<>(DEFAULT_IMPORTS);
		Matcher matcher = RegexUtil.getMatcher("import ([\\w\\.\\*]+);?", script);
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
		JavacCompiler compiler = new JavacCompiler();
		List<CompilerDiagnostic> diagnostics = new ArrayList<>();
		Map<String, CompileOption<?>> options = compiler.getDefaultOptions();
		compiler.setCompileListener(diagnostic -> diagnostics.add(
				new CompilerDiagnostic(
						(int) diagnostic.getLineNumber() - 1,
						diagnostic.getMessage(Locale.US))
		));
		compiler.setLogging(false);
		compiler.setDebug(options, JavacCompiler.createDebugValue(true, true, true));
		CompilerResult result = compiler.compile(className, code.toString(), options);
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

	private static class GenerateResult {
		private final Class<?> cls;
		private final List<CompilerDiagnostic> diagnostics;

		private GenerateResult(Class<?> cls, List<CompilerDiagnostic> diagnostics) {
			this.cls = cls;
			this.diagnostics = diagnostics;
		}
	}
}
