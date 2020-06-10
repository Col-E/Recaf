package me.coley.recaf;

import me.coley.recaf.search.*;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static me.coley.recaf.search.StringMatchMode.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Search api.
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
	public void testStringResultContext() {
		// Setup search - String "EVAL: " in Calculator.evaluate(int, String)
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug()
				.query(new StringQuery("EVAL", STARTS_WITH)).build();
		// Show results
		List<SearchResult> results = collector.getAllResults();
		assertEquals(1, results.size());
		StringResult res = (StringResult) results.get(0);
		assertEquals("EVAL: ", res.getText());
		// Assert context shows the string is in the expected method
		// - res context is of the LDC insn
		// - parent is of the method containing the String
		contextEquals(res.getContext().getParent(), "calc/Calculator", "evaluate", "(ILjava/lang/String;)D");
	}

	@Test
	public void testValue() {
		// Setup search - Calculator.MAX_DEPTH = 30
		// - Javac inlines constants, but keeps the field constant value attribute
		// - So there should be the field const and the inline value in results
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug()
				.query(new ValueQuery(30)).build();
		// Show results
		List<SearchResult> results = collector.getAllResults();
		assertEquals(2, results.size());
		ValueResult resField = (ValueResult) results.get(0);
		contextEquals(resField.getContext(), "calc/Calculator", "MAX_DEPTH", "I");
		ValueResult resInsn = (ValueResult) results.get(1);
		contextEquals(resInsn.getContext().getParent(), "calc/Calculator", "evaluate", "(ILjava/lang/String;)D");
	}

	@Test
	public void testOverlappingResultsInMethodCode() {
		// Setup search - two queries that have results in the same method:
		// - Calculator.evaluate(int, String)
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug()
				.query(new ValueQuery(30))
				.query(new StringQuery("EVAL: ", STARTS_WITH))
				.build();
		// Show results
		List<SearchResult> results = collector.getOverlappingResults();
		assertEquals(2, results.size());
		boolean isValFirst = results.get(0) instanceof ValueResult;
		ValueResult resVal = isValFirst ?
				(ValueResult) results.get(0) : (ValueResult) results.get(1);
		StringResult resStr = isValFirst ?
				(StringResult) results.get(1) : (StringResult) results.get(0);
		assertEquals(30, resVal.getValue());
		assertEquals("EVAL: ", resStr.getText());
		contextEquals(resVal.getContext().getParent(), "calc/Calculator", "evaluate", "(ILjava/lang/String;)D");
		contextEquals(resStr.getContext().getParent(), "calc/Calculator", "evaluate", "(ILjava/lang/String;)D");
	}

	@Test
	public void testOverlapToNarrowResults() {
		// Setup search - two queries, one for a class qualifier, other for a value
		// - Effectively search for a value in a specific class
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug()
				.query(new ClassNameQuery("calc/Parenthesis", EQUALS))
				.query(new ValueQuery(0))
				.build();
		// Show results
		List<SearchResult> results = collector.getOverlappingResults();
		// Ensure all contexts match the given type
		assertEquals(4, results.size());
		Context.ClassContext expected = Context.withClass(Opcodes.ACC_PUBLIC, "calc/Parenthesis");
		for (SearchResult result : results) {
			// Get root context of result
			Context<?> context = result.getContext();
			while (context.getParent() != null)
				context = context.getParent();
			// Assert all results are in the expected class
			// There are value results in other classes but we wanted this search to be
			// narrowed to the scope of the Parenthesis class.
			assertTrue(context instanceof Context.ClassContext);
			assertEquals(expected.getName(), ((Context.ClassContext) context).getName());
		}
	}

	@Test
	public void testOverlapExcludesOtherClasses() {
		// Setup search - fetch all strings and only return them if they're sharing the
		// same context as the "30" value result.
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug()
				.query(new ValueQuery(30))
				.query(new StringQuery("", CONTAINS))
				.build();
		// Show results
		List<SearchResult> results = collector.getOverlappingResults();
		// All results should be instruction-level in the method Calculator.evaluate(int, String)
		// So asserting that all results parents are in this method should be valid
		for (SearchResult result : results) {
			contextEquals(result.getContext().getParent(), "calc/Calculator", "evaluate", "(ILjava/lang/String;)D");
		}
	}

	@Test
	public void testMemberDefAnyInClass() {
		// Setup search - Any member in "Expression"
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug().skipCode()
				.query(new MemberDefinitionQuery("calc/Expression", null, null, EQUALS)).build();
		// Show results - should be given four (field + 3 methods)
		Set<String> results = collector.getAllResults().stream()
				.map(Object::toString)
				.collect(Collectors.toSet());
		assertEquals(4, results.size());
		assertTrue(results.contains("calc/Expression.i I"));
		assertTrue(results.contains("calc/Expression.<init>(I)V"));
		assertTrue(results.contains("calc/Expression.accept(Ljava/lang/String;)D"));
		assertTrue(results.contains("calc/Expression.evaluate(Ljava/lang/String;)D"));
	}

	@Test
	public void testMemberDefAnyIntField() {
		// Setup search - Any int member in any class
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug().skipCode()
				.query(new MemberDefinitionQuery(null, null, "I", EQUALS)).build();
		// Show results - should be the given three
		Set<String> results = collector.getAllResults().stream()
				.map(Object::toString)
				.collect(Collectors.toSet());
		assertEquals(3, results.size());
		assertTrue(results.contains("calc/Parenthesis.LEVEL_UNSET I"));
		assertTrue(results.contains("calc/Calculator.MAX_DEPTH I"));
		assertTrue(results.contains("calc/Expression.i I"));
	}

	@Test
	public void testClassReference() {
		// Setup search - References to the "Exponent" class
		// - Should be 3 references in "Exponent"
		// - 2 "this" variables in "calc/Exponent"
		// - 1 type reference in "calc/Calculator"
		SearchCollector collector = SearchBuilder.in(workspace)
				.query(new ClassReferenceQuery("calc/Exponent")).build();
		// Show results
		List<SearchResult> results = collector.getAllResults();
		assertEquals(3, results.size());
		int calc = 0, exp = 0;
		for (SearchResult res : results) {
			if (res.getContext() instanceof Context.InsnContext) {
				Context.InsnContext insnContext = (Context.InsnContext) res.getContext();
				String owner = insnContext.getParent().getParent().getName();
				if (!owner.equals("calc/Calculator"))
					fail("Unexpected result in: " + owner);
				calc++;
			} else if(res.getContext() instanceof Context.LocalContext) {
				Context.LocalContext localContext = (Context.LocalContext) res.getContext();
				String owner = localContext.getParent().getParent().getName();
				if (!owner.equals("calc/Exponent"))
					fail("Unexpected result in: " + owner);
				exp++;
			}
		}
		// - LOCAL this
		// - LOCAL this
		assertEquals(2, exp);
		// - NEW Exponent
		assertEquals(1, calc);
	}

	@Test
	public void testMemberReference() {
		// Setup search - References to the "Calculator.log(int, String)" method
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug()
				.query(new MemberReferenceQuery("calc/Calculator", "log", null, EQUALS)).build();
		// Show results
		List<SearchResult> results = collector.getAllResults();
		assertEquals(2, results.size());
		for (SearchResult res : results) {
			Context.InsnContext insnContext = (Context.InsnContext) res.getContext();
			String owner = insnContext.getParent().getParent().getName();
			if (!owner.equals("calc/Calculator")) {
				fail("Unexpected result in: " + owner);
			}
		}
	}

	@Test
	public void testNoMemberReferenceWhenCodeSkipped() {
		// Setup search - References to the "Calculator.log(int, String)" method
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug().skipCode()
				.query(new MemberReferenceQuery("calc/Calculator", "log", null, EQUALS)).build();
		// Show results
		List<SearchResult> results = collector.getAllResults();
		assertEquals(0, results.size());
	}

	@Test
	public void testClassNameEquals() {
		// Setup search - Equality for "Start"
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug().skipCode()
				.query(new ClassNameQuery("Start", EQUALS)).build();
		// Show results
		List<SearchResult> results = collector.getAllResults();
		assertEquals(1, results.size());
		assertEquals("Start", ((ClassResult)results.get(0)).getName());
	}

	@Test
	public void testClassNameStartsWith() {
		// Setup search - Starts with for "Start"
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug().skipCode()
				.query(new ClassNameQuery("S", STARTS_WITH)).build();
		// Show results
		List<SearchResult> results = collector.getAllResults();
		assertEquals(1, results.size());
		assertEquals("Start", ((ClassResult)results.get(0)).getName());
	}

	@Test
	public void testClassNameEndsWith() {
		// Setup search - Ends with for "ParenTHESIS"
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug().skipCode()
				.query(new ClassNameQuery("thesis", ENDS_WITH)).build();
		// Show results
		List<SearchResult> results = collector.getAllResults();
		assertEquals(1, results.size());
		assertEquals("calc/Parenthesis", ((ClassResult)results.get(0)).getName());
	}

	@Test
	public void testClassNameRegex() {
		// Setup search - Regex for "Start" by matching only word characters (no package splits)
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug().skipCode()
				.query(new ClassNameQuery("^\\w+$", REGEX)).build();
		// Show results
		List<SearchResult> results = collector.getAllResults();
		assertEquals(1, results.size());
		assertEquals("Start", ((ClassResult)results.get(0)).getName());
	}

	@Test
	public void testClassInheritance() {
		// Setup search - All implementations of "Expression"
		SearchCollector collector = SearchBuilder.in(workspace).skipDebug().skipCode()
				.query(new ClassInheritanceQuery(workspace, "calc/Expression")).build();
		// Show results
		Set<String> results = collector.getAllResults().stream()
				.map(res -> ((ClassResult)res).getName())
				.collect(Collectors.toSet());
		assertEquals(5, results.size());
		assertTrue(results.contains("calc/Parenthesis"));
		assertTrue(results.contains("calc/Exponent"));
		assertTrue(results.contains("calc/MultAndDiv"));
		assertTrue(results.contains("calc/AddAndSub"));
		assertTrue(results.contains("calc/Constant"));
	}

	private static void contextEquals(Context<?> context, String owner, String name, String desc) {
		assertTrue(context instanceof Context.MemberContext);
		Context.MemberContext member = (Context.MemberContext) context;
		assertEquals(owner, member.getParent().getName());
		assertEquals(name, member.getName());
		assertEquals(desc, member.getDesc());
	}
}
