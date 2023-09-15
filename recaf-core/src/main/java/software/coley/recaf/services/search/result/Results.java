package software.coley.recaf.services.search.result;

import software.coley.collections.delegate.DelegatingSortedSet;

import java.util.Collections;
import java.util.TreeSet;

/**
 * Results wrapper for a search operation.
 *
 * @author Matt Coley
 */
public class Results extends DelegatingSortedSet<Result<?>> {
	/**
	 * New results backed by tree-set.
	 */
	public Results() {
		super(Collections.synchronizedNavigableSet(new TreeSet<>()));
	}
}
