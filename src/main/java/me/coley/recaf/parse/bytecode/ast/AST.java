package me.coley.recaf.parse.bytecode.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Base AST.
 *
 * @author Matt
 */
public abstract class AST {
	private final int line;
	private final int start;
	private final List<AST> children = new ArrayList<>();
	private AST parent;
	private AST next;
	private AST prev;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 */
	public AST(int line, int start) {
		this.line = line;
		this.start = start;
	}

	/**
	 * @return Line number this node is written on<i>(1-indexed)</i>.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @return Offset from line start this node starts at.
	 */
	public int getStart() {
		return start;
	}

	/**
	 * @return Children nodes.
	 */
	public List<AST> getChildren() {
		return children;
	}

	/**
	 * @param ast
	 * 		Child node to add.
	 */
	public void addChild(AST ast) {
		// Link parent/child
		getChildren().add(ast);
		ast.setParent(this);
		// Link prev/next for children
		if (getChildren().size() > 1) {
			AST prev = getChildren().get(getChildren().size() - 2);
			prev.setNext(ast);
			ast.setPrev(prev);
		}
	}

	/**
	 * @param type
	 * 		Class of AST node type.
	 * @param <T>
	 * 		Type of AST node.
	 *
	 * @return List of AST nodes of the given class type in the AST.
	 */
	public <T> List<T> search(Class<T> type) {
		return search(type, new ArrayList<>());
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> search(Class<T> type, List<T> list) {
		for(AST ast : getChildren())
			if(type.isAssignableFrom(ast.getClass()))
				list.add((T) ast);
			else
				ast.search(type, list);
		return list;
	}

	/**
	 * @return Parent node.
	 */
	public AST getParent() {
		return parent;
	}

	/**
	 * @param parent
	 * 		Parent node.
	 */
	public void setParent(AST parent) {
		this.parent = parent;
	}

	/**
	 * @return Adjacent node.
	 */
	public AST getNext() {
		return next;
	}

	/**
	 * @param next
	 * 		Adjacent node.
	 */
	public void setNext(AST next) {
		this.next = next;
	}

	/**
	 * @return Adjacent node.
	 */
	public AST getPrev() {
		return prev;
	}

	/**
	 * @param prev
	 * 		Adjacent node.
	 */
	public void setPrev(AST prev) {
		this.prev = prev;
	}

	/**
	 * @return String representation of this node.
	 */
	public abstract String print();

	@Override
	public String toString() {
		return print();
	}
}
