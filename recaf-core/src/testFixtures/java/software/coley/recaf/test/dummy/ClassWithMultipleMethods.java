package software.coley.recaf.test.dummy;

/**
 * Dummy class with multiple methods.
 */
public class ClassWithMultipleMethods {
	public static String append(String one, String two) {
		return one + two;
	}

	public static String append(String one, String two, String three) {
		return append(append(one, two), three);
	}

	public static int add(int a, int b) {
		return a + b;
	}

	public static int add(int a, int b, int c) {
		return add(add(a, b), c);
	}
}
