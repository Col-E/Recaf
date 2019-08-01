package me.coley.recaf;

import me.coley.recaf.search.*;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for the Search api
 *
 * @author Matt
 */
public class SearchTest extends Base {
	private static JavaResource base;
	private static Workspace workspace;

	@BeforeAll
	public static void setup() {
		try {
			base = new JarResource(getClasspathFile("calc.jar"));
			workspace = new Workspace(base);
		} catch(IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testClassNameEquals() {
		// Setup search - Equality for "Start"
		List<Query> queries = Arrays.asList(new ClassNameQuery("Start", StringMatchMode.EQUALS));
		SearchVisitor sv = new SearchVisitor(workspace, queries);
		// Run search
		workspace.getPrimaryClassReaders().forEach(cr -> cr.accept(sv, ClassReader.SKIP_DEBUG));
		// Show results
		List<SearchResult> results = sv.getAllResults();
		assertEquals(1, results.size());
		assertEquals("Start", ((ClassResult)results.get(0)).getName());
	}

	@Test
	public void testClassNameStartsWith() {
		// Setup search - Starts with for "Start"
		List<Query> queries = Arrays.asList(new ClassNameQuery("S", StringMatchMode.STARTS_WITH));
		SearchVisitor sv = new SearchVisitor(workspace, queries);
		// Run search
		workspace.getPrimaryClassReaders().forEach(cr -> cr.accept(sv, ClassReader.SKIP_DEBUG));
		// Show results
		List<SearchResult> results = sv.getAllResults();
		assertEquals(1, results.size());
		assertEquals("Start", ((ClassResult)results.get(0)).getName());
	}

	@Test
	public void testClassNameEndsWith() {
		// Setup search - Ends with for "ParenTHESIS"
		List<Query> queries = Arrays.asList(new ClassNameQuery("thesis", StringMatchMode.ENDS_WITH));
		SearchVisitor sv = new SearchVisitor(workspace, queries);
		// Run search
		workspace.getPrimaryClassReaders().forEach(cr -> cr.accept(sv, ClassReader.SKIP_DEBUG));
		// Show results
		List<SearchResult> results = sv.getAllResults();
		assertEquals(1, results.size());
		assertEquals("calc/Parenthesis", ((ClassResult)results.get(0)).getName());
	}

	@Test
	public void testClassNameRegex() {
		// Setup search - Regex for "Start" by matching only word characters (no package splits)
		List<Query> queries = Arrays.asList(new ClassNameQuery("^\\w+$", StringMatchMode.REGEX));
		SearchVisitor sv = new SearchVisitor(workspace, queries);
		// Run search
		workspace.getPrimaryClassReaders().forEach(cr -> cr.accept(sv, ClassReader.SKIP_DEBUG));
		// Show results
		List<SearchResult> results = sv.getAllResults();
		assertEquals(1, results.size());
		assertEquals("Start", ((ClassResult)results.get(0)).getName());
	}

	@Test
	public void testClassInheritance() {
		// Setup search - All implementations of "Expression"
		List<Query> queries = Arrays.asList(new ClassInheritanceQuery(workspace, "calc/Expression"));
		SearchVisitor sv = new SearchVisitor(workspace, queries);
		// Run search
		workspace.getPrimaryClassReaders().forEach(cr -> cr.accept(sv, ClassReader.SKIP_DEBUG));
		// Show results
		Set<String> results = sv.getAllResults().stream()
				.map(res -> ((ClassResult)res).getName())
				.collect(Collectors.toSet());
		assertEquals(5, results.size());
		assertTrue(results.contains("calc/Parenthesis"));
		assertTrue(results.contains("calc/Exponent"));
		assertTrue(results.contains("calc/MultAndDiv"));
		assertTrue(results.contains("calc/AddAndSub"));
		assertTrue(results.contains("calc/Constant"));
	}
}
