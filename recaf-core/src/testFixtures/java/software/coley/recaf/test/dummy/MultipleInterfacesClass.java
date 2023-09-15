package software.coley.recaf.test.dummy;

import java.util.Comparator;

/**
 * Dummy class to test for multiple interfaces on.
 */
@SuppressWarnings("all")
public class MultipleInterfacesClass implements AutoCloseable, Comparator<String> {
	@Override
	public void close() throws Exception {
		// no-op
	}

	@Override
	public int compare(String o1, String o2) {
		return 0;
	}
}
