package software.coley.recaf.test.dummy;

public class ClassWithNestedInners {
	void foo() {
		NestedInterface itf = new BaseNestedClass.Deep1.Deep2();
		itf.nestedInterfaceMethod();
	}

	interface NestedInterface {
		void nestedInterfaceMethod();
	}

	interface NoopNestedInterface extends NestedInterface {
		@Override
		default void nestedInterfaceMethod() {
			System.out.println("Noop impl");
		}
	}

	static abstract class BaseNestedClass implements NestedInterface {
		@Override
		public void nestedInterfaceMethod() {
			baseClassImpl();
		}

		abstract void baseClassImpl();

		static class Deep1 extends BaseNestedClass {
			@Override
			void baseClassImpl() {
				System.out.println("Deep1 impl");
			}

			static class Deep2 extends BaseNestedClass {
				@Override
				void baseClassImpl() {
					System.out.println("Deep2 impl");
				}
			}
		}
	}
}
