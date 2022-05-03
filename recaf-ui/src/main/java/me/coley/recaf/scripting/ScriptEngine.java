package me.coley.recaf.scripting;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base script engine for evaluating Beanshell scripts.
 *
 * @author Wolfie / win32kbase
 */
public class ScriptEngine {
	private static final Logger logger = Logging.get(ScriptEngine.class);
	private static final Interpreter interpreter = new Interpreter();
	private static final String[] defaultImportedPackages = {
			"java.util",
			"me.coley.recaf",
			"me.coley.recaf.util",
			"me.coley.recaf.search",
			"me.coley.recaf.search.result",
			"me.coley.recaf.workspace",
			"me.coley.recaf.workspace.resource",
			"me.coley.recaf.scripting.impl"
	};

	static {
		// This allows the interpreter to call the utility classes without needing to
		// have a long qualified name.
		NameSpace nameSpace = interpreter.getNameSpace();
		for (String packageName : defaultImportedPackages)
			nameSpace.importPackage(packageName);
	}

	/**
	 * @param reader
	 * 		Reader to read text from.
	 *
	 * @return Script execution result.
	 */
	public static ScriptResult execute(Reader reader) {
		logger.info("Executing BeanShell script");
		try {
			return new ScriptResult(interpreter.eval(reader), null);
		} catch (EvalError e) {
			logger.error("Failed to evaluate BeanShell script: {}", e.getLocalizedMessage());
			return new ScriptResult(null, e);
		}
	}

	/**
	 * @param script
	 * 		Script text to execute.
	 *
	 * @return Script execution result.
	 */
	public static ScriptResult execute(String script) {
		return execute(new StringReader(script));
	}

	/**
	 * @param path
	 * 		Path to script file to execute.
	 *
	 * @return Script execution result.
	 */
	public static ScriptResult execute(Path path) {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return execute(reader);
		} catch (IOException ex) {
			logger.error("Failed to read script: {}", path);
			return new ScriptResult(null, ex);
		}
	}
}
