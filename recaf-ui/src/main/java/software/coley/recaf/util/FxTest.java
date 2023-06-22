package software.coley.recaf.util;

/**
 * Util for FX testing.
 * <br>
 * Some behaviors may be omitted when under a test environment.
 *
 * @author Matt Coley
 */
public class FxTest {
	private static final String KEY = "FX-TEST-ENV";
	private static boolean isTest;
	private static boolean checked;

	/**
	 * Apply key.
	 */
	public static void initTestEnv() {
		System.setProperty(KEY, "");
	}

	/**
	 * @return {@code true} when we are in a test environment.
	 */
	public static boolean isTestEnv() {
		// Only check once
		if (!checked) {
			isTest = System.getProperty(KEY) != null;
			checked = true;
		}
		return isTest;
	}
}
