package me.coley.recaf.mapping;

import me.coley.recaf.TestUtils;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MappingTests extends TestUtils {
	private static final String NAME_SAMPLE = "Sample";
	private static final String NAME_RENAMED = "Renamed";

	@Test
	void testRenameSample() throws IOException {
		Path classPath = sourcesDir.resolve("Sample.class");
		byte[] classRaw = Files.readAllBytes(classPath);
		// Read the class, but pass it through the remapping visitor
		Mappings mappings = new SampleMappings();
		ClassReader cr = new ClassReader(classRaw);
		ClassNode node = new ClassNode();
		cr.accept(new RemappingVisitor(node, mappings), 0);
		// Assert node has been renamed
		assertNotNull(node.name, "Class should have been parsed");
		assertEquals(NAME_RENAMED, node.name, "Class 'Sample' was not renamed");
	}

	/**
	 * Dummy mappings that only renames the name "Sample".
	 */
	private static class SampleMappings implements Mappings {
		@Override
		public String getMappedClassName(String internalName) {
			if (internalName.equals(NAME_SAMPLE)) {
				return NAME_RENAMED;
			}
			return null;
		}

		@Override
		public String getMappedFieldName(String ownerName, String fieldName, String fieldDesc) {
			return null;
		}

		@Override
		public String getMappedMethodName(String ownerName, String methodName, String methodDesc) {
			return null;
		}

		@Override
		public String getMappedVariableName(String className, String methodName, String methodDesc,
											String name, String desc, int index) {
			return null;
		}

		@Override
		public Map<String, String> toAsmFormattedMappings() {
			return null;
		}

		@Override
		public String implementationName() {
			return null;
		}
	}
}
