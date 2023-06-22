package software.coley.recaf.test.dummy;

/**
 * Dummy class that uses {@link StringList}
 */
public class StringListUser {
	public static void main(String[] args) {
		StringList list = StringList.of("foo");
		for (String string : list.unique()) {
			System.out.println(string);
		}
	}
}
