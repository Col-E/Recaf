package me.coley.recaf.search;

import me.coley.recaf.TestUtils;
import me.coley.recaf.search.result.Result;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.source.JarContentSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReferenceSearchTests extends TestUtils {
	private static Resource snake;

	@BeforeAll
	static void setup() throws IOException {
		snake = new Resource(new JarContentSource(jarsDir.resolve("DemoGame.jar")));
		snake.read();
	}

	@Test
	void testFindFxAppInit() {
		List<Result> results = search(snake, TextMatchMode.EQUALS, "javafx/application/Application", "<init>", "()V");
		assertEquals(1, results.size(), "Should contain exactly one match for 'new Application()'");
	}

	@Test
	void testFindFxRefs() {
		List<Result> results = search(snake, TextMatchMode.CONTAINS, "javafx", null, null);
		assertTrue(results.size() > 0, "Should contain multiple JavaFX references");
	}

	private static List<Result> search(Resource resource, TextMatchMode mode,
									   String owner, String name, String desc) {
		return new Search().reference(owner, name, desc, mode).run(resource);
	}
}
