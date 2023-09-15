package software.coley.recaf.test.dummy;

/**
 * Class to test embedded inner-outer relation, with regular inner classes.
 */
@SuppressWarnings("all")
public class ClassWithEmbeddedInners {
	public class A {
		public class B {
			public class C {
				public class D {
					public class E {

					}
				}
			}
		}
	}
}
