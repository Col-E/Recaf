package me.coley.recaf.metadata;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Comments metadata handler.
 *
 * @author Matt
 */
public class Comments {
	public static final String TYPE = "Lme/coley/recaf/metadata/InsnComment;";
	public static final String KEY_PREFIX = "At_";
	private final Map<Integer, String> indexToComment = new TreeMap<>();

	/**
	 * Create an empty comments handler.
	 */
	public Comments() {
	}

	/**
	 * Create a comments handler that populates existing entries from the given method.
	 *
	 * @param method
	 * 		Method with comments.
	 */
	public Comments(MethodNode method) {
		parse(method);
	}

	private void parse(MethodNode method) {
		if (method.visibleAnnotations == null) return;
		List<AnnotationNode> invalidAnnos = new ArrayList<>();
		for (AnnotationNode anno : method.visibleAnnotations) {
			if (anno.desc.equals(Comments.TYPE)) {
				for (int i = 0; i < anno.values.size(); i += 2) {
					Object keyInfo = anno.values.get(i);
					Object comment = anno.values.get(i + 1);
					// Skip malformed comments.
					boolean validTypes = keyInfo instanceof String && comment instanceof String;
					if (!validTypes || keyInfo.toString().length() <= KEY_PREFIX.length()) {
						invalidAnnos.add(anno);
						continue;
					}
					String key = ((String)keyInfo).substring(Comments.KEY_PREFIX.length());
					if (key.matches("\\d+"))
						indexToComment.put(Integer.parseInt(key), (String) comment);
				}
			}
		}
		// Prune invalid annos
		method.visibleAnnotations.removeIf(invalidAnnos::contains);
	}

	/**
	 * Adds a comment at the current instruction offset.
	 *
	 * @param index
	 * 		Method instruction index to insert the comment at.
	 * @param comment
	 * 		Comment string to add.
	 */
	public void addComment(int index, String comment) {
		String existing = indexToComment.get(index);
		if (existing != null) {
			comment = existing + "\n" + comment;
		}
		indexToComment.put(index, comment);
	}

	/**
	 * Write comments to the method.
	 *
	 * @param method
	 * 		Method to write to.
	 */
	public void applyTo(MethodNode method) {
		if (method.visibleAnnotations == null)
			method.visibleAnnotations = new ArrayList<>();
		// Remove old comments
		removeComments(method);
		// Add new comments
		indexToComment.forEach((index, comment) -> {
			AnnotationNode commentNode = new AnnotationNode(Comments.TYPE);
			commentNode.visit(Comments.KEY_PREFIX + index, comment);
			method.visibleAnnotations.add(commentNode);
		});
	}

	/**
	 * @param offset
	 * 		Instruction offset.
	 *
	 * @return Comment at offset.
	 */
	public String get(int offset) {
		return indexToComment.get(offset);
	}

	/**
	 * Removes comments from the given method.
	 *
	 * @param method
	 * 		Method that may contain comments.
	 */
	public static void removeComments(MethodNode method) {
		if (method.visibleAnnotations == null)
			return;
		method.visibleAnnotations.removeIf(node -> node.desc.equals(Comments.TYPE));
	}
}
