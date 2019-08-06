package me.coley.recaf;

import com.google.common.collect.Sets;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static java.util.Collections.*;

/**
 * Tests for workspace resource source code bindings.
 *
 * @author Matt
 */
public class SourceCodeTest extends Base {

	@Nested
	public class SourceUsage {
		private JavaResource resource;

		@BeforeEach
		public void setup() {
			try {
				File file = getClasspathFile("calc.jar");
				resource = new JarResource(file);
				resource.getClasses();
				if(!resource.setClassSources(file))
					fail("Failed to read sources!");
			} catch(IOException ex) {
				fail(ex);
			}
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
				File file = getClasspathFile("calc.jar");
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
		public void testSingleClassFailsOnJar() {
			JavaResource resource;
			try {
				resource = new ClassResource(getClasspathFile("Hello.class"));
				resource.getClasses();
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertThrows(IOException.class, () -> resource.setClassSources(getClasspathFile("calc.jar")));
		}

		@Test
		public void testJarFailsOnMissingFile() {
			JavaResource resource;
			try {
				File file = getClasspathFile("calc.jar");
				resource = new JarResource(file);
				resource.getClasses();
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertThrows(IOException.class, () -> resource.setClassSources(new File("Does/Not/Exist")));
		}

		@Test
		public void testJarFailsOnBadFileType() {
			JavaResource resource;
			File source;
			try {
				File file = getClasspathFile("calc.jar");
				source = getClasspathFile("Hello.class");
				resource = new JarResource(file);
				resource.getClasses();
			} catch(IOException ex) {
				fail(ex);
				return;
			}
			assertThrows(IOException.class, () -> resource.setClassSources(source));
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
		Set<String> expectedSrcNames = resource.getClasses().keySet();
		Set<String> foundSrcNames = resource.getClassSources().values().stream()
				.map(SourceCode::getInternalName).collect(Collectors.toSet());
		// Show that all classes (no inners in this sample) have source code mappings
		Set<String> difference = Sets.difference(expectedSrcNames, foundSrcNames);
		assertNotEquals(emptySet(), expectedSrcNames);
		assertNotEquals(emptySet(), foundSrcNames);
		assertEquals(emptySet(), difference);
	}
}
