package me.coley.recaf.mapping;

import me.coley.recaf.TestUtils;
import me.coley.recaf.mapping.impl.IntermediateMappings;
import me.coley.recaf.mapping.impl.SimpleMappings;
import me.coley.recaf.mapping.impl.TinyV1Mappings;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
		String methodDesc = "(Ljava/lang/String;)V";
		String fieldDesc = "Ljava/lang/String;";
		adapter.addClass(oldClassName, newClassName);
		adapter.addField(oldClassName, oldFieldName, fieldDesc, newFieldName);
		adapter.addMethod(oldClassName, oldMethodName, methodDesc, newMethodName);
		// Assert registered mapping types can import from the intermediate
		MappingsManager manager = new MappingsManager();
		assertTrue(manager.getRegisteredImpls().size() > 1);
		for (MappingsTool tool : manager.getRegisteredImpls()) {
			Mappings mappings = tool.create();
			System.out.println("Intermediate -> " + mappings.implementationName());
			assertTrue(mappings.supportsExportIntermediate());
			mappings.importIntermediate(adapter.exportIntermediate());
			assertEquals(newClassName, mappings.getMappedClassName(oldClassName));
			assertEquals(newFieldName, mappings.getMappedFieldName(oldClassName, oldFieldName, fieldDesc));
			assertEquals(newMethodName, mappings.getMappedMethodName(oldClassName, oldMethodName, methodDesc));
			if (mappings.supportsExportText())
				System.out.println(mappings.exportText());
		}
	}

	@Test
	void testAggregate() {
		IntermediateMappings mappings1 = new IntermediateMappings();
		IntermediateMappings mappings2 = new IntermediateMappings();
		IntermediateMappings mappings3 = new IntermediateMappings();

		mappings1.addClass("a", "b");
		mappings2.addClass("b", "c");
		mappings2.addField("b", "I", "oldName", "newName");
		mappings3.addClass("c", "d");
		mappings3.addField("c", "I", "newName", "brandNewName");

		AggregatedMappings aggregated = new AggregatedMappings();
		// Validate after first mapping pass
		aggregated.update(mappings1);
		assertEquals("b", aggregated.getMappedClassName("a"));
		// Validate after second mapping pass
		aggregated.update(mappings2);
		assertEquals("c", aggregated.getMappedClassName("a"));
		assertEquals("newName", aggregated.getMappedFieldName("a", "oldName", "I"));
		// Validate after third mapping pass
		aggregated.update(mappings3);
		assertEquals("d", aggregated.getMappedClassName("a"));
		assertEquals("brandNewName", aggregated.getMappedFieldName("a", "oldName", "I"));
	}

	@Test
	void testTinyV1() {
		try {
			String mappingsText = new String(Files.readAllBytes(mapsDir.resolve("inherit-method-map-tiny-1.txt")));
			Mappings mappings = new TinyV1Mappings();
			mappings.parse(mappingsText);
			assertInhertMap(mappings);
		} catch (IOException e) {
			fail(e);
		}
	}

	@Test
	void testSimple() {
		try {
			String mappingsText = new String(Files.readAllBytes(mapsDir.resolve("inherit-method-map-simple.txt")));
			Mappings mappings = new SimpleMappings();
			mappings.parse(mappingsText);
			assertInhertMap(mappings);
		} catch (IOException e) {
			fail(e);
		}
	}

	private void assertInhertMap(Mappings mappings) {
		assertEquals("rename/Hello", mappings.getMappedClassName("test/Greetings"));
		assertEquals("speak", mappings.getMappedMethodName("test/Greetings", "say", "()V"));
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
		public String implementationName() {
			return null;
		}

		@Override
		public void parse(String mappingsText) {
		}

		@Override
		public String exportText() {
			return null;
		}

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
