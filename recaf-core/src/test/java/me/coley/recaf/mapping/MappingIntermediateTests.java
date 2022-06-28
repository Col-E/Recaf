package me.coley.recaf.mapping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for intermediate mapping interoperability.
 */
public class MappingIntermediateTests {
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
			else
				System.out.println("Mappings does not support text export: " + mappings.implementationName() + "\n");
		}
	}
}
