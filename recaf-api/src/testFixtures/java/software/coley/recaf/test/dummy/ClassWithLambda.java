package software.coley.recaf.test.dummy;

import java.util.Collections;
import java.util.Map;

/**
 * Class to test basic existence of lambdas with.
 */
@SuppressWarnings("all")
public class ClassWithLambda {
	static final Map<String, String> map = Collections.emptyMap();

	static void runnable() {
		new Thread(() -> {
			System.out.println("foo");
		});
	}

	static void consumer() {
		map.values().stream().findFirst()
				.ifPresent(s -> System.out.println(s));
	}

	static void predicate() {
		map.values().stream().findFirst()
				.filter(s -> s.isBlank());
	}
}
