package software.coley.recaf.test.dummy;

/**
 * Dummy class to test exceptions / catch blocks.
 */
@SuppressWarnings("all")
public class ClassWithExceptions {
	static int readInt(Object input) throws NumberFormatException {
		try {
			return Integer.parseInt(input.toString());
		} catch (NullPointerException ex) {
			throw new NumberFormatException("input was null");
		} catch (NumberFormatException ex) {
			throw ex;
		}
	}
}
