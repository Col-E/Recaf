package me.coley.recaf.ui.component.tree;

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import org.objectweb.asm.tree.ClassNode;

/**
 * Tree node containing ClassNode data.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class ASMTreeNode extends DefaultMutableTreeNode {
	/**
	 * Map of relative children names. Contains different data depending on if
	 * {@link #getNode()} is null. If the value is null then the current node
	 * instance represents a package.
	 * 
	 * <pre>
	 * {@code
	 * com/example/ 
	 *   - com/example/MyClass
	 *   - com/example/AnotherClass
	 * }
	 * </pre>
	 * 
	 * If the value is not null, then the current node instance could represent
	 * a class, field, or method.
	 * 
	 * Class example:
	 * 
	 * <pre>
	 * {@code
	 * com/example/MyClass
	 *   - field
	 *   - method
	 * }
	 * </pre>
	 * 
	 * Method example:
	 * 
	 * <pre>
	 * {@code
	 * com/example/MyClass
	 *   - opcode
	 *   - opcode
	 * }
	 * </pre>
	 *
	 * Field will not use the map, so no children.
	 */
	private final Map<String, ASMTreeNode> children = new HashMap<>();
	private final ClassNode node;

	public ASMTreeNode(String title, ClassNode node) {
		super(title);
		this.node = node;
	}

	public ASMTreeNode getChild(String name) {
		return children.get(name);
	}

	public void addChild(String name, ASMTreeNode node) {
		children.put(name, node);
	}

	public final ClassNode getNode() {
		return node;
	}
}