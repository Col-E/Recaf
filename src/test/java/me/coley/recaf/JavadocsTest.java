package me.coley.recaf;

import me.coley.recaf.parse.javadoc.*;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

// TODO: The calc docs are REALLY simple and don't provide a robust test backing. Find better sample.
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
	public void testField(String file) {
		load(file);
		Javadocs docs = base.getClassDocs("calc/Expression");
		List<DocField> fields = docs.getFields();
		//
		assertEquals(1, fields.size());
		DocField field = fields.get(0);
		assertEquals("i", field.getName());
		assertEquals("int", field.getType());
		assertEquals("", field.getDescription());
		assertEquals("protected", field.getModifiers().get(0));
		assertEquals("final", field.getModifiers().get(1));
	}

	@ParameterizedTest
	@ValueSource(strings = { JAVA8, JAVA12 })
	public void testMethod(String file) {
		load(file);
		Javadocs docs = base.getClassDocs("calc/Expression");
		List<DocMethod> methods = docs.getMethods();
		//
		assertEquals(2, methods.size());
		//
		DocMethod method = methods.get(0);
		assertEquals("evaluate", method.getName());
		assertEquals("double", method.getReturnType());
		assertEquals("Evaluates an expression as a level deeper than the current one in the expression tree,", method.getDescription());
		assertEquals("protected", method.getModifiers().get(0));
		assertEquals("final", method.getModifiers().get(1));
		assertEquals("Evaluated result.", method.getReturnDescription());
		assertEquals(1, method.getParameters().size());
		DocParameter parameter = method.getParameters().get(0);
		assertEquals("expression", parameter.getName());
		assertEquals("Some math expression.", parameter.getDescription());
		//
		method = methods.get(1);
		assertEquals("accept", method.getName());
		assertEquals("double", method.getReturnType());
		assertEquals("", method.getDescription());
		assertEquals("public", method.getModifiers().get(0));
		assertEquals("abstract", method.getModifiers().get(1));
		assertEquals("Evaluated result.", method.getReturnDescription());
		assertEquals(1, method.getParameters().size());
		parameter = method.getParameters().get(0);
		assertEquals("expression", parameter.getName());
		assertEquals("Some math expression.", parameter.getDescription());
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
		List<String> subclasses = docs.getSubclasses();
		// Inheritance should be ordered by "child > parent > root"
		String[] keys = {
				"calc/AddAndSub", "calc/Constant", "calc/Exponent", "calc/MultAndDiv", "calc/Parenthesis"
		};
		assertEquals(keys.length, subclasses.size());
		for(int i = 0; i < keys.length; i++)
			assertEquals(keys[i], subclasses.get(i));
	}

	@ParameterizedTest
	@ValueSource(strings = { JAVA8, JAVA12 })
	public void testNoSubclasses(String file) {
		load(file);
		Javadocs docs = base.getClassDocs("calc/Exponent");
		List<String> subclasses = docs.getSubclasses();
		assertEquals(0, subclasses.size());
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

	@Test
	public void testMavenLoading() {
		MavenResource resource;
		try {
			resource = new MavenResource("org.ow2.asm", "asm", "7.2-beta");
			resource.getClasses();
			if(!resource.fetchJavadoc())
				fail("Failed to fetch sources from maven: " + resource.getCoords());
			assertTrue(resource.getClassDocs().size() > 0);
		} catch(IOException ex) {
			fail(ex);
			return;
		}
	}
}
