package me.coley.recaf.mapping;

import me.coley.recaf.TestUtils;
import me.coley.recaf.mapping.impl.IntermediateMappings;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

	@Test
	void testMapFromIntermediate() {
		// Setup base mappings
		MappingsAdapter adapter = new MappingsAdapter("TEST", true, true);
		String oldClassName = "Foo";
		String newClassName = "Bar";
		String oldMethodName = "say";
		String newMethodName = "speak";
		String oldFieldName = "syntax";
		String newFieldName = "pattern";
		String methodDesc  = "(Ljava/lang/String;)V";
		String fieldDesc  = "Ljava/lang/String;";
		adapter.addClass(oldClassName, newClassName);
		adapter.addField(oldClassName, oldFieldName, fieldDesc, newFieldName);
		adapter.addMethod(oldClassName, oldMethodName, methodDesc, newMethodName);
		// Assert registered mapping types can import from the intermediate
		MappingsManager manager = new MappingsManager();
		assertTrue(manager.getRegisteredImpls().size() > 1);
		for (MappingsTool tool : manager.getRegisteredImpls()) {
			Mappings mappings = tool.create();
			assertTrue(mappings.supportsExportIntermediate());
			mappings.importIntermediate(adapter.exportIntermediate());
			assertEquals(newClassName, mappings.getMappedClassName(oldClassName));
			assertEquals(newFieldName, mappings.getMappedFieldName(oldClassName, oldFieldName, fieldDesc));
			assertEquals(newMethodName, mappings.getMappedMethodName(oldClassName, oldMethodName, methodDesc));
			if (mappings.supportsExportText())
				System.out.println(mappings.exportText());
		}
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
		public Map<String, String> exportAsmFormatted() {
			return null;
		}

		@Override
		public String implementationName() {
			return null;
		}

		@Override
		public void parse(String mappingsText) {}

		@Override
		public String exportText() { return null; }

		@Override
		public IntermediateMappings exportIntermediate() {
			return null;
		}

		@Override
		public void importIntermediate(IntermediateMappings mappings) {
			// no-op
		}
	}
}
