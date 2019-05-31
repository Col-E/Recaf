package me.coley.recaf.util;

import java.util.ArrayList;

/**
 * List with maximum size. Oldest entries are purged when new entries are added
 * that exceed the max size of the list.
 * 
 * @author Matt
 *
 * @param <E>
 */
@SuppressWarnings("serial")
public class RollingList<E> extends ArrayList<E> {
	private final int max;

	public RollingList(int max) {
		this.max = (max - 1);
	}

	@Override
	public boolean add(E k) {
		boolean r = super.add(k);
		if (size() > max) {
			removeRange(0, size() - max - 1);
		}
		return r;
	}
}