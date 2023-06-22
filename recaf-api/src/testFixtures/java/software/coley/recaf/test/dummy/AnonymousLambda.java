package software.coley.recaf.test.dummy;

import java.util.function.Supplier;

/**
 * Dummy class to test lambda replacing usage of anonymous classes are not broken in remapping.
 */
@SuppressWarnings("all")
public class AnonymousLambda {
	public static void main(String[] args) {
		run();
	}

	public static String run() {
		Supplier<String> supplierA = () -> "One: " + Supplier.class.getName();
		StringSupplier supplierB = () -> "Two: " + StringSupplier.class.getName();

		String a = supplierA.get();
		String b = supplierB.get();
		System.out.println(a);
		System.out.println(b);
		return a + b;
	}
}
