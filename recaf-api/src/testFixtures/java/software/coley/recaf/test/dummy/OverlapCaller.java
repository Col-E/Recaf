package software.coley.recaf.test.dummy;


/**
 * Dummy class to test for renaming with overlapping definitions from interfaces.
 *
 * @see OverlapClassAB
 */
@SuppressWarnings("all")
public class OverlapCaller {
	public static void main(String[] args) {
		run();
	}

	public static void run() {
		Object object = new OverlapClassAB();

		OverlapInterfaceA a = (OverlapInterfaceA) object;
		a.methodA();

		OverlapInterfaceB b = (OverlapInterfaceB) object;
		b.methodA();
		b.methodB();

		OverlapClassAB ab = (OverlapClassAB) object;
		ab.methodA();
		ab.methodB();
	}
}
