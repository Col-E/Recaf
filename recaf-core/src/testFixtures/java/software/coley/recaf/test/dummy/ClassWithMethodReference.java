package software.coley.recaf.test.dummy;

import java.util.function.Supplier;

public class ClassWithMethodReference {
	static {
		Supplier<String> fooSupplier = ClassWithMethodReference::foo;

		System.out.println(foo());
		System.out.println(fooSupplier.get());
	}

	static String foo() {
		return "foo";
	}
}
