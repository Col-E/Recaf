package software.coley.recaf.test.dummy;

import java.io.PrintStream;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Class with a variety of stuff in it.
 */
@SuppressWarnings("all")
public abstract class ClassWithFieldsAndMethods implements Supplier<Integer> {
	private static final int CONST_INT = 12345567;

	private final int finalInt;

	public ClassWithFieldsAndMethods(int finalInt) {this.finalInt = finalInt;}

	public void methodWithParameters(String foo, long wide, float decimal, double d, List<String> strings) {
		// no-op
	}

	public void methodWithLocalVariables() {
		PrintStream out = System.out;
		String message = UUID.randomUUID().toString();
		out.println(message);
	}

	public int plusTwo() {
		return finalInt + 2;
	}

	private int minusOne() {
		return finalInt - 1;
	}

	public static int getConstInt() {
		return CONST_INT;
	}
}
