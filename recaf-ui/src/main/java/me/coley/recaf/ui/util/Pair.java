package me.coley.recaf.ui.util;

/**
 *
 * @author Wolfie / win32kbase
 *
 * @param <A> Left of pair
 * @param <B> Right of pair
 */

public class Pair<A, B> {
    private A left;
    private B right;

    public Pair(A left, B right) {
        this.left = left;
        this.right = right;
    }

    public A getLeft() {
        return this.left;
    }

    public B getRight() {
        return this.right;
    }

    public void setLeft(A left) {
        this.left = left;
    }

    public void setRight(B right) {
        this.right = right;
    }
}