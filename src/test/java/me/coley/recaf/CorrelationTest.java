package me.coley.recaf;

import me.coley.recaf.graph.flow.FlowBuilder;
import me.coley.recaf.graph.flow.FlowVertex;
import me.coley.recaf.mapping.Correlation;
import me.coley.recaf.mapping.CorrelationResult;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
	@Test
	public void testSameFlowInObfuscatedJar() {
		try {
			// Load base calculator program, and the obfuscated version
			// - renamed class & method names
			// - no control flow obfuscation, so flow analysis should be the same
			JavaResource base = new JarResource(getClasspathFile("calc.jar"));
			JavaResource target = new JarResource(getClasspathFile("calc-renamed.jar"));
			Workspace workspace = new Workspace(target, Arrays.asList(base));
			// Run correlation analysis
			Correlation correlation = new Correlation(workspace, base, target);
			Set<CorrelationResult> results = correlation.analyze();
			// Assert there are no differences in flow
			assertEquals(1, results.size());
			CorrelationResult result = results.iterator().next();
			assertEquals(Collections.emptySet(), result.getDifference());
		} catch(IOException ex) {
			// Thrown if loading classpath resources fails
			fail(ex);
		}
	}

	@Test
	public void testFaultyFlowModifiedEntry() {
		try {
			// Load base calculator program, and the modified version
			// - added a single call to main(String[])
			JavaResource base = new JarResource(getClasspathFile("calc.jar"));
			JavaResource target = new JarResource(getClasspathFile("calc-modified.jar"));
			Workspace workspace = new Workspace(target, Arrays.asList(base));
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
		} catch(IOException ex) {
			// Thrown if loading classpath resources fails
			fail(ex);
		}
	}

	@Test
	public void testSameFlowInModifiedJarByChaningTheEntryPoint() {
		// Don't you love stupidly verbose names?
		try {
			// Load base calculator program, and the modified version
			// - added a single call to main(String[])
			JavaResource base = new JarResource(getClasspathFile("calc.jar"));
			JavaResource target = new JarResource(getClasspathFile("calc-modified.jar"));
			Workspace workspace = new Workspace(target, Arrays.asList(base));
			// Run correlation analysis
			Correlation correlation = new Correlation(workspace, base, target);
			FlowVertex baseEntrt = workspace.getFlowGraph().getVertex(new ClassReader(base.getClasses().get("calc/Calculator")), "evaluate", "(Ljava/lang/String;)D");
			FlowVertex targetEntrt = workspace.getFlowGraph().getVertex(new ClassReader(target.getClasses().get("calc/Calculator")), "evaluate", "(Ljava/lang/String;)D");
			// Assert there is no difference in flow since the only difference (main method)
			// is no longer a part of the flow graph.
			CorrelationResult result = correlation.analyze(baseEntrt, targetEntrt);
			assertEquals(Collections.emptySet(), result.getDifference());
		} catch(IOException ex) {
			// Thrown if loading classpath resources fails
			fail(ex);
		}
	}
}
