package me.coley.recaf.util;

import javassist.CtClass;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static me.coley.recaf.util.ReflectUtil.getDeclaredField;
import static me.coley.recaf.util.ReflectUtil.getDeclaredMethod;

/**
 * This exists because the Javassist API tries way too hard to keep things source-level.
 *
 * @author Matt Coley
 */
public class JavassistUtil {
	private static final Logger logger = Logging.get(JavassistUtil.class);
	// Javassist accessors
	private static Method javassistPoolItemGet;
	private static Field javassistClassThisIndex;

	/**
	 * @param ctClass
	 * 		The class to get the internal name of.
	 *
	 * @return Internal name of the class.
	 *
	 * @throws ReflectiveOperationException
	 * 		When the horrible spaghetti code of Javassist changes and this breaks.
	 */
	public static String getInternalName(CtClass ctClass) throws ReflectiveOperationException {
		ClassFile classFile = ctClass.getClassFile();
		int index = javassistClassThisIndex.getInt(classFile);
		return getInternalName(ctClass, index);
	}

	/**
	 * @param ctClass
	 * 		The class to pull data from the constant pool of.
	 * @param index
	 * 		Index of a {@code CP_CLASS} in the given class.
	 *
	 * @return Internal name of the class.
	 *
	 * @throws ReflectiveOperationException
	 * 		When the horrible spaghetti code of Javassist changes and this breaks.
	 */
	public static String getInternalName(CtClass ctClass, int index) throws ReflectiveOperationException {
		ClassFile classFile = ctClass.getClassFile();
		ConstPool constPool = classFile.getConstPool();
		Object cpClass = javassistPoolItemGet.invoke(constPool, index);
		Field utfIndexField = getDeclaredField(cpClass.getClass(), "name");
		int utfIndex = utfIndexField.getInt(cpClass);
		return constPool.getUtf8Info(utfIndex);
	}

	static {
		try {
			// I hate Javassist. This is awful.
			javassistPoolItemGet = getDeclaredMethod(ConstPool.class, "getItem", int.class);
			javassistClassThisIndex = getDeclaredField(ClassFile.class, "thisClass");
		} catch (Exception ex) {
			logger.error("Failed to get internal name/descriptor accessors! Internal Javassist API changed?", ex);
		}
	}
}
