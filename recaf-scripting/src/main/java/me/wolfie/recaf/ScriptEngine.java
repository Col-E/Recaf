package me.wolfie.recaf;

import com.google.common.reflect.ClassPath;
import me.coley.recaf.util.logging.Logging;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScriptEngine {
    private static final Logger logger = Logging.get(ScriptEngine.class);
    private static final Globals vmGlobals = JsePlatform.standardGlobals();

    @SuppressWarnings("all")
    private static void load() {
        try {
            ClassPath.from(ScriptEngine.class.getClassLoader()).getTopLevelClassesRecursive("me.wolfie.recaf.impl").forEach((ci) -> {
                try {
                    ci.load().newInstance();
                } catch (Exception e) {
                    logger.error("Failed to construct library implementation class {}", ci.getSimpleName());
                }
            });
        } catch (IOException e) {
            logger.error("Failed to get top level classes from impl package");
        }
    }

    public static void initialize(LuaValue value) {
        LuaTable library = new LuaTable();

        Class<?> parent = value.getClass();
        for (Class<?> klass : parent.getClasses()) {
            logger.info(klass.getSimpleName());
            try {
                library.set(klass.getSimpleName(), (LuaValue) klass.newInstance());
                logger.info("Registering method {}", klass.getSimpleName());
            } catch (Exception e) {
                logger.error("Failed to register method {}", klass.getSimpleName());
            }
        }

        vmGlobals.set(parent.getSimpleName(), library);
        logger.info("Registered library {}", parent.getSimpleName());
    }

    public static void execute(Path path) {
        try {
            execute(new String(Files.readAllBytes(path)));
        }
        catch (IOException e) {
            logger.error("Failed to execute script at {}", path);
        }
    }

    public static void execute(String script) {
        logger.info("Executing script");
        logger.info(System.getProperty("java.class.path"));
        LuaValue chunk = vmGlobals.load(script);
        LuaValue retn = chunk.call();
    }

    static {
        load();
    }
}
