package software.coley.recaf.test.dummy;

/**
 * Dummy class to test for renaming with overlapping definitions from interfaces.
 */
@SuppressWarnings("all")
public class OverlapClassAB implements OverlapInterfaceA, OverlapInterfaceB {
	@Override
	public void methodA() {
		// Both interfaces declare this
	}

	@Override
	public void methodB() {
		// Only B declares this
	}
}
