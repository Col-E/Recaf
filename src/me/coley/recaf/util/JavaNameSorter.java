package me.coley.recaf.util;

import java.util.Comparator;

/**
 * Sorter of java names.
 *
 * @author Matt
 */
public class JavaNameSorter implements Comparator<String> {

	@Override
	public int compare(String s1, String s2) {
		String[] split1 = s1.split("/");
		String[] split2 = s2.split("/");
		int l1 = split1.length;
		int l2 = split2.length;
		int len = Math.min(l1, l2);
		for (int i = 0; i < len; i++) {
			String p1 = split1[i];
			String p2 = split2[i];
			if (i == len - 1 && l1 != l2) {
				return (l1 > l2) ? -1 : 1;
			}
			int cmp = p1.compareTo(p2);
			if (cmp != 0) {
				return cmp;
			}
		}
		return 0;
	}
}