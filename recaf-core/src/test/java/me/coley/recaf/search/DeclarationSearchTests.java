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

public class DeclarationSearchTests extends TestUtils {
	private static Resource snake;

	@BeforeAll
	static void setup() throws IOException {
		snake = new Resource(new JarContentSource(jarsDir.resolve("DemoGame.jar")));
		snake.read();
	}

	@Test
	void testZeroMatch() {
		List<Result> results = search(snake, TextMatchMode.EQUALS, "NoMatch", null, null);
		assertEquals(0, results.size(), "Should zero matches");
	}


	@Test
	void testFindMain() {
		List<Result> results = search(snake, TextMatchMode.EQUALS, null, "main", "([Ljava/lang/String;)V");
		assertEquals(1, results.size(), "Should contain exactly one match for 'main(String[])'");
	}

	@Test
	void testFindModelMemberDecs() {
		List<Result> results = search(snake, TextMatchMode.CONTAINS, "Model", null, null);
		assertTrue(results.size() > 0, "Should contain multiple model member declarations");
	}

	private static List<Result> search(Resource resource, TextMatchMode mode,
									   String owner, String name, String desc) {
		return new Search().declaration(owner, name, desc, mode).run(resource);
	}
}
