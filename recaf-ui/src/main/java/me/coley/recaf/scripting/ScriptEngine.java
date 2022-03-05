package me.coley.recaf.scripting;

import bsh.EvalError;
import bsh.Interpreter;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScriptEngine {
    private static final Logger logger = Logging.get(ScriptEngine.class);
    private static final Interpreter interpreter = new Interpreter();

    static {
        try {
            interpreter.eval("import me.coley.recaf.scripting.impl.*;");
        } catch (EvalError e) {
            logger.error("Failed to import implementation classes: {}", e.getLocalizedMessage());
        }
    }

    public static boolean executeBsh(String script) {
        logger.info("Executing BeanShell script");
        try {
            interpreter.eval(script);
        }
        catch (EvalError e) {
            logger.error("Failed to evaluate BeanShell script: {}", e.getLocalizedMessage());
            return false;
        }

        return true;
    }

    public static boolean executeBsh(Path path) {
        try {
            return executeBsh(new String(Files.readAllBytes(path)));
        }
        catch (IOException e) {
            logger.error("Failed to read script: {}", path);
            return false;
        }
    }

}
