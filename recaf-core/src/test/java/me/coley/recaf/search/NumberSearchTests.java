package me.coley.recaf.search;

import me.coley.recaf.TestUtils;
import me.coley.recaf.search.result.Result;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.source.JarContentSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NumberSearchTests extends TestUtils {
	private static Resource snake;

	@BeforeAll
	static void setup() throws IOException {
		snake = new Resource(new JarContentSource(jarsDir.resolve("DemoGame.jar")));
		snake.read();
	}

	@Test
	void testEquals() {
		List<Result> results = search(snake, 500, NumberMatchMode.EQUALS);
		assertEquals(1, results.size(), "Should contain exactly one match for '500'");
	}

	@Test
	void testGreater() {
		List<Result> results = search(snake, 0, NumberMatchMode.GREATER_THAN);
		assertTrue(results.size() > 0, "Should contain more than one match for '> 0'");
	}

	@Test
	void testLess() {
		List<Result> results = search(snake, 500, NumberMatchMode.LESS_THAN);
		assertTrue(results.size() > 0, "Should contain more than one match for '< 500'");
	}

	@Test
	void testChain() {
		List<Result> results = search(snake, 500, NumberMatchMode.EQUALS);
		assertEquals(1, results.size(), "Should contain exactly one match for '500'");
	}

	private static List<Result> search(Resource resource, Number number, NumberMatchMode mode) {
		return new Search().number(number, mode).run(resource);
	}
}
