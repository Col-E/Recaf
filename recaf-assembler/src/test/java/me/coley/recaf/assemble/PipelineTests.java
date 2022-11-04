package me.coley.recaf.assemble;

import me.coley.recaf.assemble.pipeline.AssemblerPipeline;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for utilizing {@link AssemblerPipeline}.
 */
public class PipelineTests {
	private static final String SELF_TYPE = "Test";
	private static final boolean USE_PREFIX = false;

	@Nested
	class Passing {
		@ParameterizedTest
		@MethodSource("me.coley.recaf.assemble.PipelineTests#passingPaths")
		public void testPassing(Path file) throws IOException {
			String text = Files.readString(file);
			AssemblerPipeline pipeline = createPipeline();
			assertTrue(generate(pipeline, text));
		}
	}

	public static AssemblerPipeline createPipeline() {
		AssemblerPipeline pipeline = new AssemblerPipeline();
		FailureListener listener = new FailureListener();
		pipeline.addAstValidationListener(listener);
		pipeline.addParserFailureListener(listener);
		pipeline.addBytecodeFailureListener(listener);
		pipeline.addBytecodeValidationListener(listener);
		return pipeline;
	}

	public static boolean generate(AssemblerPipeline pipeline, String text) {
		return generate(SELF_TYPE, USE_PREFIX, pipeline, text);
	}

	public static boolean generate(String selfType, boolean useDotPrefix, AssemblerPipeline pipeline, String text) {
		pipeline.setDoUseAnalysis(true);
		pipeline.setType(selfType);
		pipeline.setText(text);
		// Update and validate AST
		if (pipeline.updateAst(useDotPrefix) && pipeline.validateAst()) {
			// Generate definition
			if (pipeline.isMethod())
				return pipeline.generateMethod();
			else if (pipeline.isField())
				return pipeline.generateField();
			else if (pipeline.isClass())
				return pipeline.generateClass();
		}
		return false;
	}

	public static List<Path> passingPaths() throws IOException {
		return Files.walk(Paths.get("src/test/resources/passing"))
				.filter(p -> p.toString().endsWith(".txt"))
				.collect(Collectors.toList());
	}

}
