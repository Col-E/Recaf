package me.coley.recaf.asm;

import java.io.IOException;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.Recaf;

/**
 * Custom ClassWriter which does not require the classes to be loaded in the
 * classpath for determining the common super-class.
 */
public class NonReflectionWriter extends ClassWriter {
	private static final String DEFAULT_PARENT = "java/lang/Object";
	private final Recaf recaf = Recaf.getInstance();
	private final Map<String, ClassNode> nodes;

	public NonReflectionWriter(int flags) {
		super(flags);
		this.nodes = recaf.jarData.classes;
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) {
		if (type1 == null || type2 == null) {
			return DEFAULT_PARENT;
		}
		ClassNode node1 = loadNode(type1);
		ClassNode node2 = loadNode(type2);
		if (node1 == null || node2 == null) {
			return DEFAULT_PARENT;
		} else if (isAssignableFrom(node1, node2)) {
			return type1;
		} else if (isAssignableFrom(node2, node1)) {
			return type2;
		}
		do {
			node1 = loadNode(node1.superName);
			if (node1 == null) {
				if (node2.name.equals(type1)) {
					return DEFAULT_PARENT;
				}
				node1 = loadNode(type2);
				node2 = loadNode(type1);
			} else if (isAssignableFrom(node1, node2)) {
				return type1;
			} else if (isAssignableFrom(node2, node1)) {
				return type2;
			}
		} while (!isAssignableFrom(node1, node2));
		return DEFAULT_PARENT;
	}

	/**
	 * Checks if the given parent is a superclass or superinterface of the given
	 * child.
	 *
	 * @param parent
	 * @param child
	 * @return
	 */
	private boolean isAssignableFrom(ClassNode parent, ClassNode child) {
		while (child.superName != null) {
			if (parent.name.equals(child.name) || child.interfaces.contains(parent.name)) {
				return true;
			}
			for (String interfac : child.interfaces) {
				if (isAssignableFrom(parent, loadNode(interfac))) {
					return true;
				}
			}
			child = loadNode(child.superName);
		}
		return false;
	}

	/**
	 * Load node from the node map.
	 *
	 * @param type
	 * @return
	 */
	private ClassNode loadNode(String type) {
		// Try loading from node list
		if (type == null) {
			return null;
		} else if (nodes.containsKey(type)) {
			return nodes.get(type);
		}
		// Try loading from runtime
		String nameStr = type.replace("/", ".");
		try {
			Class<?> clazz = Class.forName(nameStr);
			if (clazz == null) {
				return null;
			}
			return recaf.asm.getNode(clazz);
		} catch (IOException e) {
			return null;
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

}