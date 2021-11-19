package me.coley.recaf.parse;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithDeclaration;
import me.coley.recaf.code.ItemInfo;

/**
 * Wrapper for results of resolving what is at some parsed Java code at a given position.
 *
 * @author Matt Coley
 */
public class ParseHitResult {
	private final ItemInfo info;
	private final Node node;

	/**
	 * @param info
	 * 		Item at location.
	 * @param node
	 * 		Node representing the item.
	 */
	public ParseHitResult(ItemInfo info, Node node) {
		this.info = info;
		this.node = node;
	}

	/**
	 * @return Item at location.
	 */
	public ItemInfo getInfo() {
		return info;
	}

	/**
	 * @return Node representing the item.
	 */
	public Node getNode() {
		return node;
	}

	/**
	 * @return {@code true} when the {@link #getNode() node} denotes a declaration rather than a reference.
	 */
	public boolean isDeclaration() {
		// Because of the way we handle JavaParser resolving this works fine for methods/classes...
		// But for fields, we could just be matching a 'SimpleName'.
		if (node instanceof NodeWithDeclaration || node instanceof TypeDeclaration)
			return true;
		// Check for field declaration. Its the 2nd parent up. You can't rely on the 1st being a
		// 'VariableDeclarator' because then you'd treat local variables as fields...
		if (node.getParentNode().isPresent() && node.getParentNode().get().getParentNode().isPresent() &&
				node.getParentNode().get().getParentNode().get() instanceof FieldDeclaration)
			return true;
		// Not a declaration
		return false;
	}
}
