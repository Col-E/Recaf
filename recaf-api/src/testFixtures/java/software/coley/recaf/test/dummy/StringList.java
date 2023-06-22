package software.coley.recaf.test.dummy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Dummy class that's a list of strings.
 */
public class StringList extends ArrayList<String> {
	public static StringList of(String... args) {
		StringList strings = new StringList();
		strings.addAll(Arrays.asList(args));
		return strings;
	}

	public Set<String> unique() {
		return new LinkedHashSet<>(this);
	}
}
