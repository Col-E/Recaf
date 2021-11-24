package me.coley.recaf.ui.util;

/**
 * @param <A>
 * 		Left type of pair.
 * @param <B>
 * 		Right type of pair.
 *
 * @author Wolfie / win32kbase
 */
public class Pair<A, B> {
	private A left;
	private B right;

	/**
	 * @param left
	 * 		Initial left value.
	 * @param right
	 * 		Initial right value.
	 */
	public Pair(A left, B right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * @return Left value.
	 */
	public A getLeft() {
		return this.left;
	}

	/**
	 * @return Right value.
	 */
	public B getRight() {
		return this.right;
	}

	/**
	 * @param left
	 * 		New left value.
	 */
	public void setLeft(A left) {
		this.left = left;
	}

	/**
	 * @param right
	 * 		New right value.
	 */
	public void setRight(B right) {
		this.right = right;
	}
}