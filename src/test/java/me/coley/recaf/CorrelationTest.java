package me.coley.recaf;

import me.coley.recaf.graph.flow.FlowBuilder;
import me.coley.recaf.graph.flow.FlowVertex;
import me.coley.recaf.mapping.Correlation;
import me.coley.recaf.mapping.CorrelationResult;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for correlation analysis.
 *
 * @author Matt
 */
public class CorrelationTest extends Base {
	private JavaResource base;

	@BeforeEach
	public void setup() {
		try {
			// Load base calculator program, and the obfuscated version
			// - renamed class & method names
			// - no control flow obfuscation, so flow analysis should be the same
			base = new JarResource(getClasspathFile("calc.jar"));
		} catch(IOException ex) {
			// Thrown if loading classpath resources fails
			fail(ex);
		}
	}

	/**
	 * The renamed jar is the same as the base jar except all the identifiers have been renamed.
	 * Flow analysis should result in the same exact paths.
	 */
	@Nested
	public class WithRenamed {
		private JavaResource target;
		private Workspace workspace;

		@BeforeEach
		public void setup() {
			try {
				// Load base calculator program, and the obfuscated version
				// - renamed class & method names
				// - no control flow obfuscation, so flow analysis should be the same
				target = new JarResource(getClasspathFile("calc-renamed.jar"));
				workspace = new Workspace(target, Collections.singletonList(base));
			} catch(IOException ex) {
				// Thrown if loading classpath resources fails
				fail(ex);
			}
		}


		@Test
		public void testSameFlowInObfuscatedJar() {
			// Run correlation analysis
			Correlation correlation = new Correlation(workspace, base, target);
			Set<CorrelationResult> results = correlation.analyze();
			// Assert there are no differences in flow
			assertEquals(1, results.size());
			CorrelationResult result = results.iterator().next();
			assertEquals(Collections.emptySet(), result.getDifference());
		}

		@Test
		public void testMappingsInObfuscatedJar() {
			// Run correlation analysis
			Correlation correlation = new Correlation(workspace, base, target);
			Set<CorrelationResult> results = correlation.analyze();
			assertEquals(1, results.size());
			CorrelationResult result = results.iterator().next();
			// Create mappings
			Map<String, String> mappings = result.getMappings();
			// 8 class renames
			// 5 static method renames
			// 7 instance method renames
			assertEquals(20, mappings.size());
		}
	}

	/**
	 * The modified jar is the same as the base jar except the main method has an additional
	 * logging call. This messes up the flow analysis algorithm used by the correlation mapper
	 * unless the entry point is manually specified to be a common point <i>after</i> the main
	 * method.
	 */
	@Nested
	public class WithModified {
		private JavaResource target;
		private Workspace workspace;

		@BeforeEach
		public void setup() {
			try {
				// Load base calculator program, and the modified version
				// - added a single call to main(String[])
				target = new JarResource(getClasspathFile("calc-modified.jar"));
				workspace = new Workspace(target, Collections.singletonList(base));
			} catch(IOException ex) {
				// Thrown if loading classpath resources fails
				fail(ex);
			}
		}


		@Test
		public void testFaultyFlowModifiedEntry() {
			// Run correlation analysis
			Correlation correlation = new Correlation(workspace, base, target);
			Set<CorrelationResult> results = correlation.analyze();
			// Assert there the difference is the main method (entry)
			assertEquals(1, results.size());
			CorrelationResult result = results.iterator().next();
			Set<FlowBuilder.Flow> difference = result.getDifference();
			assertEquals(1, difference.size());
			FlowBuilder.Flow vertex = difference.iterator().next();
			assertEquals("Start", vertex.getValue().getOwner());
			assertEquals("main", vertex.getValue().getName());
			assertEquals("([Ljava/lang/String;)V", vertex.getValue().getDesc());
		}

		@Test
		public void testSameFlowInModifiedJarByChaningTheEntryPoint() {
			// Run correlation analysis
			Correlation correlation = new Correlation(workspace, base, target);
			FlowVertex baseEntrt = workspace.getFlowGraph().getVertex(
					new ClassReader(base.getClasses().get("calc/Calculator")), "evaluate", "(Ljava/lang/String;)D");
			FlowVertex targetEntrt = workspace.getFlowGraph().getVertex(
					new ClassReader(target.getClasses().get("calc/Calculator")), "evaluate", "(Ljava/lang/String;)D");
			// Assert there is no difference in flow since the only difference (main method)
			// is no longer a part of the flow graph.
			CorrelationResult result = correlation.analyze(baseEntrt, targetEntrt);
			assertEquals(Collections.emptySet(), result.getDifference());
		}

		@Test
		public void testMappingsInModifiedJar() {
			// Run correlation analysis
			Correlation correlation = new Correlation(workspace, base, target);
			Set<CorrelationResult> results = correlation.analyze();
			assertEquals(1, results.size());
			CorrelationResult result = results.iterator().next();
			// Create mappings
			Map<String, String> mappings = result.getMappings();
			// There should be NO mappings since the entry point
			// has been modified, which throws the entire thing off.
			assertEquals(0, mappings.size());
		}

		@Test
		public void testMappingsInModifiedJarByChaningTheEntryPoint() {
			// Run correlation analysis
			Correlation correlation = new Correlation(workspace, base, target);
			FlowVertex baseEntrt = workspace.getFlowGraph().getVertex(
					new ClassReader(base.getClasses().get("calc/Calculator")), "evaluate", "(Ljava/lang/String;)D");
			FlowVertex targetEntrt = workspace.getFlowGraph().getVertex(
					new ClassReader(target.getClasses().get("calc/Calculator")), "evaluate", "(Ljava/lang/String;)D");
			// Assert there is no difference in flow since the only difference (main method)
			// is no longer a part of the flow graph.
			CorrelationResult result = correlation.analyze(baseEntrt, targetEntrt);
			// Create mappings
			Map<String, String> mappings = result.getMappings();
			// We know that the analysis has run and they match if
			// "testSameFlowInModifiedJarByChaningTheEntryPoint" passes.
			//
			// However, there should be NO mappings since the jar has no identifiers renamed.
			assertEquals(0, mappings.size());
		}
	}
}
