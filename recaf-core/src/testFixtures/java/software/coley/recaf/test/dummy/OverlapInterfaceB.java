package software.coley.recaf.test.dummy;

/**
 * Dummy class to test for renaming with overlapping definitions from interfaces.
 *
 * @see OverlapClassAB
 */
@SuppressWarnings("all")
public interface OverlapInterfaceB {
	void methodA();

	void methodB();
}
