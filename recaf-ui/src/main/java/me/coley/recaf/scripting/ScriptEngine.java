package me.coley.recaf.scripting;

import bsh.EvalError;
import bsh.Interpreter;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
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

	static {
		try {
		    // This allows the interpreter to call the utility classes without needing to
            // have a long qualified name.
			interpreter.eval("import me.coley.recaf.scripting.impl.*;");
		} catch (EvalError e) {
			logger.error("Failed to import implementation classes: {}", e.getLocalizedMessage());
		}
	}

	public static boolean execute(String script) {
		logger.info("Executing BeanShell script");
		try {
			interpreter.eval(script);
            return true;
		} catch (EvalError e) {
			logger.error("Failed to evaluate BeanShell script: {}", e.getLocalizedMessage());
			return false;
		}
	}

	public static boolean execute(Path path) {
		try {
			return execute(new String(Files.readAllBytes(path)));
		} catch (IOException e) {
			logger.error("Failed to read script: {}", path);
			return false;
		}
	}
}
