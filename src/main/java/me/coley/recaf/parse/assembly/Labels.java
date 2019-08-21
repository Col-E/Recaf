package me.coley.recaf.parse.assembly;

import org.objectweb.asm.tree.LabelNode;

import java.util.*;

/**
 * Labels manager.
 *
 * @author Matt
 */
public class Labels {
	private final Map<String, LabelNode> nameToLabel = new LinkedHashMap<>();
	// Labels to be used for variable ranges
	private LabelNode start;
	private LabelNode end;

	/**
	 * @param name
	 * 		Label name to register.
	 */
	public void register(String name) {
		nameToLabel.putIfAbsent(name, new LabelNode());
	}

	/**
	 * @return Set of registered label names.
	 */
	public Set<String> names() {
		return nameToLabel.keySet();
	}

	/**
	 * @param name
	 * 		Label identifier.
	 *
	 * @return Label instance.
	 */
	public LabelNode get(String name) {
		return nameToLabel.get(name);
	}

	/**
	 * @return Label to insert at the method start.
	 */
	public LabelNode getStart() {
		return start;
	}

	/**
	 * @return Label to insert at the method end.
	 */
	public LabelNode getEnd() {
		return end;
	}

	/**
	 * Clears label maps and creates new start/end labels.
	 */
	public void reset() {
		start = new LabelNode();
		end = new LabelNode();
		nameToLabel.clear();
	}
}
