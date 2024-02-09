package software.coley.recaf.ui;

import org.junit.jupiter.api.BeforeAll;
import software.coley.recaf.util.TestEnvironment;

/**
 * Ensures child classes are marked as test env via {@link TestEnvironment#isTestEnv()}.
 *
 * @author Matt Coley
 */
public class BaseFxTest {
	@BeforeAll
	static void setup() {
		TestEnvironment.initTestEnv();
	}
}
