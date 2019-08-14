package me.coley.recaf;

import me.coley.recaf.parse.javadoc.Javadocs;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for workspace resource documentation bindings.
 *
 * @author Matt
 */
public class JavadocsTest extends Base {
	// paths to javadoc zipz
	private static final String JAVA8 = "calc-docs-8.zip";
	private static final String JAVA12 = "calc-docs-12.zip";
	private static JavaResource base;
	private static Workspace workspace;

	@BeforeEach
	public void setup() {
		try {
			base = new JarResource(getClasspathFile("calc.jar"));
			workspace = new Workspace(base);
		} catch(IOException ex) {
			fail(ex);
		}
	}

	private void load(String file) {
		try {
			base.setClassDocs(getClasspathFile(file));
		} catch(IOException ex) {
			fail(ex);
		}
	}

	// ============================================================================== //

	@ParameterizedTest
	@ValueSource(strings = { JAVA8, JAVA12 })
	public void testAllLoaded(String file) {
		load(file);
		for(String name : base.getClasses().keySet()) {
			// Start was not added in the docs
			if(name.equals("Start"))
				continue;
			assertNotNull(base.getClassDocs(name), name + " was null");
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { JAVA8, JAVA12 })
	public void testInheritance(String file) {
		load(file);
		Javadocs docs = base.getClassDocs("calc/Calculator");
		List<String> inheritance = docs.getInheritance();
		// Inheritance should be ordered by "child > parent > root"
		assertEquals(2, inheritance.size());
		assertEquals("calc/Calculator", inheritance.get(0));
		assertEquals("java/lang/Object", inheritance.get(1));
	}

	@ParameterizedTest
	@ValueSource(strings = { JAVA8, JAVA12 })
	public void testSubclasses(String file) {
		load(file);
		Javadocs docs = base.getClassDocs("calc/Expression");
		List<String> inheritance = docs.getSubclasses();
		// Inheritance should be ordered by "child > parent > root"
		String[] keys = {
				"calc/AddAndSub", "calc/Constant", "calc/Exponent", "calc/MultAndDiv", "calc/Parenthesis"
		};
		assertEquals(keys.length, inheritance.size());
		for(int i = 0; i < keys.length; i++)
			assertEquals(keys[i], inheritance.get(i));
	}

	@ParameterizedTest
	@ValueSource(strings = { JAVA8, JAVA12 })
	public void testDescription(String file) {
		load(file);
		String[][] keys =  {
				{ "calc/AddAndSub", "Addition and subtraction evaluator. Both included in one evaluator due to order equivalence." },
				{ "calc/Calculator", "Main calculator logic." },
				{ "calc/Constant", "Constant evaluator." },
				{ "calc/Exponent", "Exponent evaluator." },
				{ "calc/Expression", "Base expression class." },
				{ "calc/MatchUtil", Javadocs.NO_DESCRIPTION },
				{ "calc/MultAndDiv", "Multiple and division evaluator. Both included in one evaluator due to order equivalence." },
				{ "calc/Parenthesis", "Parenthesis evaluator." }
		};
		for (String[] part : keys)
			assertEquals(part[1], base.getClassDocs(part[0]).getDescription());
	}
}
