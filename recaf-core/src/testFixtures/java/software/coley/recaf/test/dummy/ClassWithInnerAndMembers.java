package software.coley.recaf.test.dummy;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to test basic inner-outer relation, for a regular inner class with fields.
 */
@SuppressWarnings("all")
public class ClassWithInnerAndMembers {
	private int foo = 10;

	void outer() {
		System.out.println(foo);
	}

	void outerToInner(TheInner inner) {
		inner.inner();
		System.out.println(inner.bar);
		System.out.println(inner.strings.getFirst());
	}

	public class TheInner {
		private final List<String> strings = new ArrayList<>();
		private int bar = 10;

		void inner() {
			System.out.println(foo);
			System.out.println(bar);
		}

		void innerToOuter() {
			outer();
		}
	}
}
