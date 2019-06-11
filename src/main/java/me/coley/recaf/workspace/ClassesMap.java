package me.coley.recaf.workspace;

import me.coley.recaf.Input;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.ClassUtil;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.Set;


/**
 * FileMap for input classes.
 *
 * @author Matt
 */
public class ClassesMap extends FileMap<String, ClassNode> {
	public ClassesMap(Input input, Set<String> keys) {
		super(input, keys);
	}

	@Override
	ClassNode castValue(byte[] file) {
		try {
			return ClassUtil.getNode(file);
		} catch(IOException e) {
			Logging.fatal(e);
		}
		return null;
	}

	@Override
	byte[] castBytes(ClassNode value) {
		try {
			return ClassUtil.getBytes(value);
		} catch(Exception e) {
			Logging.warn("Failed to convert to raw byte[]: '" + value.name + "' due to the " +
					"following error: ");
			Logging.error(e);
		}
		try {
			return getFile(getPath(value.name));
		} catch(IOException e) {
			Logging.warn("Failed to fetch fallback value of byte[] for: '" + value.name + "' due "
					+ "to the following error: ");
			Logging.error(e);
		}
		return new byte[0];
	}

	@Override
	String castKey(Object in) {
		return in.toString();
	}
}
