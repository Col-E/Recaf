package me.coley.recaf;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.Sets;
import me.coley.recaf.parse.source.SourceCode;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static java.util.Collections.*;
import static me.coley.recaf.util.JavaParserUtil.*;


/**
 * Tests for workspace resource source code bindings.
 *
 * @author Matt
 */
public class SourceCodeTest extends Base {
	@Nested
	public class SourceUsage {
		private JavaResource resource;
		private Workspace workspace;

		@BeforeEach
		public void setup() {
			try {
				Path file = getClasspathFile("calc.jar");
				resource = new JarResource(file);
				resource.getClasses();
				if(!resource.setClassSources(file))
					fail("Failed to read sources!");
				workspace = new Workspace(resource);
			} catch(IOException ex) {
				fail(ex);
			}
		}

		@Test
		public void testNodeAtPos() {
			SourceCode code = resource.getClassSource("Start");
			// Line 7: Two tabs then this:
			//
			// Scanner scanner = new Scanner(System.in);
			//
			// First "Scanner" is an AST Tyoe
			Node node = code.getNodeAt(7, 5); // Scanner
			assertTrue(node instanceof ClassOrInterfaceType);
			assertEquals("Scanner", ((ClassOrInterfaceType)node).asString());
			// "scanner" is just a SimpleName, so we return the parent VariableDeclarator
			node = code.getNodeAt(7, 13); // scanner
			assertTrue(node instanceof VariableDeclarator);
			assertEquals("scanner", ((VariableDeclarator)node).getNameAsString());
			// Second "Scanner" is also an AST Type
			node = code.getNodeAt(7, 27); // Scanner
			assertTrue(node instanceof ClassOrInterfaceType);
			assertEquals("Scanner", ((ClassOrInterfaceType)node).asString());
			// "System.in" is a FieldAccessExpr
			// - "System" is a NameExpr - Field.scope
			// - "in" is a NameExpr - Field.name
			node = code.getNodeAt(7, 34); // System
			assertTrue(node instanceof FieldAccessExpr);
			assertTrue(((FieldAccessExpr)node).getScope() instanceof NameExpr);
			assertEquals("System", ((NameExpr)((FieldAccessExpr)node).getScope()).getNameAsString());
			assertEquals("in", ((FieldAccessExpr)node).getNameAsString());
		}

		@Test
		public void testClassResolve() {
			// Enable advanced resolving
			workspace.analyzeSources();
			// Line 7: Two tabs then this:
			//
			// Scanner scanner = new Scanner(System.in);
			//
			SourceCode code = resource.getClassSource("calc/Calculator");
			Node node = code.getNodeAt(6, 37); // String
			assertTrue(node instanceof ClassOrInterfaceType);
			ClassOrInterfaceType classType = (ClassOrInterfaceType) node;
			ResolvedType dec = classType.resolve();
			assertEquals("java/lang/String", toInternal(dec));
			//
			node = code.getNodeAt(22, 18); // Exponent
			assertTrue(node instanceof ClassOrInterfaceType);
			classType = (ClassOrInterfaceType) node;
			dec = classType.resolve();
			assertEquals("calc/Exponent", toInternal(dec));
			//
			node = code.getNodeAt(10, 25); // int
			assertTrue(node instanceof PrimitiveType);
			PrimitiveType primType = (PrimitiveType) node;
			ResolvedPrimitiveType decPrim = primType.resolve();
			assertEquals("java/lang/Integer", toInternal(decPrim));
		}

		@Test
		public void testFieldResolve() {
			// Enable advanced resolving
			workspace.analyzeSources();
			// Line 7: Two tabs then this:
			//
			// Scanner scanner = new Scanner(System.in);
			//
			SourceCode code = resource.getClassSource("Start");
			Node node = code.getNodeAt(7, 34);
			assertTrue(node instanceof FieldAccessExpr);
			FieldAccessExpr fieldExpr = (FieldAccessExpr) node;
			ResolvedFieldDeclaration dec = (ResolvedFieldDeclaration) fieldExpr.resolve();
			assertEquals("java/lang/System", getOwner(dec));
			assertEquals("in", dec.getName());
			assertEquals("Ljava/io/InputStream;", getDescriptor(dec));
		}

		@Test
		public void testMethodResolve() {
			// Enable advanced resolving
			workspace.analyzeSources();
			// Line 18: Three tabs then this:
			//
			// return new Parenthesis(i).accept(expression);
			//
			SourceCode code = resource.getClassSource("calc/Calculator");
			Node node = code.getNodeAt(18, 33);
			assertTrue(node instanceof MethodCallExpr);
			MethodCallExpr callExpr = (MethodCallExpr) node;
			ResolvedMethodDeclaration dec = callExpr.resolve();
			assertEquals("calc/Parenthesis", getOwner(dec));
			assertEquals("accept", dec.getName());
			assertEquals("(Ljava/lang/String;)D", getDescriptor(dec));
			//
			node = code.getNodeAt(44, 16);
			assertTrue(node instanceof MethodCallExpr);
			callExpr = (MethodCallExpr) node;
			dec = callExpr.resolve();
			assertEquals("java/io/PrintStream", getOwner(dec));
			assertEquals("println", dec.getName());
			assertEquals("(Ljava/lang/String;)V", getDescriptor(dec));
		}

		@Test
		public void testNoImports() {
			List<String> imports = resource.getClassSource("calc/Expression").getImports();
			assertEquals(0, imports.size());
		}

		@Test
		public void testImpliedImports() {
			List<String> imports = resource.getClassSource("calc/Expression").getAllImports();
			assertEquals(8, imports.size() - SourceCode.LANG_PACKAGE_NAMES.length);
			for(String name : resource.getClasses().keySet())
				// Implied imports include classes in the same package
				// - of which there should be 8
				if(name.startsWith("calc/"))
					assertTrue(imports.contains(name));
		}

		@Test
		public void testExplicitImports() {
			List<String> imports = resource.getClassSource("calc/MatchUtil").getImports();
			// Imports only two classes
			assertEquals(2, imports.size());
			assertTrue(imports.contains("java/util/regex/Matcher"));
			assertTrue(imports.contains("java/util/regex/Pattern"));
		}

		@Test
		public void testWildcardImport() {
			List<String> imports = resource.getClassSource("Start").getImports();
			assertEquals(9, imports.size());
			for(String name : resource.getClasses().keySet())
				// Should have imported the entire package "calc.*"
				// which is all the remaining classes.
				if(name.startsWith("calc/"))
					assertTrue(imports.contains(name));
			// Also imports scanner
			assertTrue(imports.contains("java/util/Scanner"));
		}

		@Test
		public void testSurrounding() {
			// Test that the 5th line of the source file + a context radius of 1 line
			// matches the constructor of the given class.
			//
			// public Constant(int i) {
			//     super(i);
			// }
			String expected = "\tpublic Constant(int i) {\n\t\tsuper(i);\n\t}";
			String actual = resource.getClassSource("calc/Constant").getSurrounding(5, 1);
			assertEquals(expected, actual);
		}
	}

	@Nested
	public class SourceLoading {
		@Test
		public void testDefaultSourceLoading() {
			JavaResource resource;
			try {
				Path file = getClasspathFile("calc.jar");
				resource = new JarResource(file);
				resource.getClasses();
				if(!resource.setClassSources(file))
					fail("Failed to read sources!");
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertMatchingSource(resource);
		}

		@Test
		public void testSingleClassSourceLoading() {
			JavaResource resource;
			try {
				resource = new ClassResource(getClasspathFile("Hello.class"));
				resource.getClasses();
				if(!resource.setClassSources(getClasspathFile("Hello.java")))
					fail("Failed to read sources!");
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertMatchingSource(resource);
		}

		@Test
		public void testUrlDeferLoading() {
			JavaResource resource;
			try {
				resource = new UrlResource(getClasspathUrl("calc.jar"));
				resource.getClasses();
				if(!resource.setClassSources(getClasspathFile("calc.jar"))) {
					fail("Failed to read sources!");
				}
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertMatchingSource(resource);
		}

		@Test
		public void testJarFailsOnMissingFile() {
			JavaResource resource;
			try {
				Path file = getClasspathFile("calc.jar");
				resource = new JarResource(file);
				resource.getClasses();
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertThrows(IOException.class, () -> resource.setClassSources(Paths.get("Does/Not/Exist")));
		}

		@Test
		public void testJarFailsOnBadFileType() {
			JavaResource resource;
			Path source;
			try {
				Path file = getClasspathFile("calc.jar");
				source = getClasspathFile("Hello.class");
				resource = new JarResource(file);
				resource.getClasses();
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertThrows(IOException.class, () -> resource.setClassSources(source));
		}

		@Test
		public void testMavenLoading() {
			MavenResource resource;
			try {
				resource = new MavenResource("org.ow2.asm", "asm", "7.2-beta");
				resource.getClasses();
				if(!resource.fetchSources())
					fail("Failed to fetch sources from maven: " + resource.getCoords());
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertMatchingSource(resource);
		}
	}

	/**
	 * Asserts that the given resource's classes all have mappings to source files.<br>
	 * The given resource must not have any inner classes.
	 *
	 * @param resource
	 * 		Resource to check.
	 */
	private static void assertMatchingSource(JavaResource resource) {
		Set<String> expectedSrcNames = resource.getClasses().keySet()
				.stream().filter(name -> !name.contains("$") && !name.equals("module-info"))
				.collect(Collectors.toSet());
		Set<String> foundSrcNames = resource.getClassSources().values().stream()
				.map(SourceCode::getInternalName).collect(Collectors.toSet());
		// Show that all classes (no inners in this sample) have source code mappings
		Set<String> difference = Sets.difference(expectedSrcNames, foundSrcNames);
		assertNotEquals(emptySet(), expectedSrcNames);
		assertNotEquals(emptySet(), foundSrcNames);
		assertEquals(emptySet(), difference);
	}
}
