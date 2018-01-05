package me.coley.recaf.agent;

import java.lang.instrument.ClassDefinition;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.asm.Asm;

/**
 * Tracker for classes that have been updated in agent-mode. Allows updating the
 * definitions of only edited classes easily via the instrumentation.redefine
 * call.
 * 
 * @author Matt
 *
 */
@SuppressWarnings("rawtypes")
public class Marker {
	private static final Set<String> updated = new HashSet<>();

	// TODO: reference anything that changes a class here
	public static void mark(String name) {
		updated.add(name.replace("/", "."));
	}

	public static ClassDefinition[] getDefinitions() throws Exception {
		Class[] classes = getClasses();
		ClassDefinition[] definitions = new ClassDefinition[classes.length];
		for (int i = 0; i < classes.length; i++) {
			Class clazz = classes[i];
			byte[] clazzBytes = Asm.toBytes(getNode(clazz));
			definitions[i] = new ClassDefinition(clazz, clazzBytes);
		}
		return definitions;
	}

	private static Class[] getClasses() throws Exception {
		Set<String> copy = new HashSet<>(updated);
		updated.clear();
		Class[] classes = new Class[copy.size()];
		int i = 0;
		for (String name : copy) {
			classes[i++] = Class.forName(name);
		}
		return classes;
	}

	private static ClassNode getNode(Class clazz) {
		String key = clazz.getName().replace(".", "/");
		return Recaf.INSTANCE.jarData.classes.get(key);
	}
}
