package software.coley.recaf.services.mapping.format;

import org.junit.jupiter.api.Test;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingsAdapter;
import software.coley.recaf.test.TestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link MappingFileFormat} implementation support for importing from intermediate mappings.
 */
public class MappingIntermediateTest extends TestBase {
	@Test
	void testMapFromIntermediate() {
		// Setup base mappings
		MappingsAdapter adapter = new MappingsAdapter(true, true);
		String oldClassName = "Foo";
		String newClassName = "Bar";
		String oldMethodName = "say";
		String newMethodName = "speak";
		String oldFieldName = "syntax";
		String newFieldName = "pattern";
		String oldVariableName = "message";
		String newVariableName = "text";
		String methodDesc = "(Ljava/lang/String;)V";
		String fieldDesc = "Ljava/lang/String;";
		String variableDesc = "Ljava/lang/String;";
		adapter.addClass(oldClassName, newClassName);
		adapter.addField(oldClassName, oldFieldName, fieldDesc, newFieldName);
		adapter.addMethod(oldClassName, oldMethodName, methodDesc, newMethodName);
		adapter.addVariable(oldClassName, oldMethodName, methodDesc, oldVariableName, variableDesc, 1, newVariableName);

		IntermediateMappings intermediate = adapter.exportIntermediate();
		assertEquals(newVariableName, intermediate.getMappedVariableName(oldClassName, oldMethodName, methodDesc,
				oldVariableName, variableDesc, -1));

		// Assert registered mapping types can import from the intermediate
		MappingFormatManager formatManager = recaf.get(MappingFormatManager.class);
		assertTrue(formatManager.getMappingFileFormats().size() > 1);
		for (String formatName : formatManager.getMappingFileFormats()) {
			MappingFileFormat format = formatManager.createFormatInstance(formatName);
			assertNotNull(format, "Could not get format: " + formatName);

			// Export and print
			if (format.supportsExportText())
				assertDoesNotThrow(() -> format.exportText(adapter));
		}
	}

	@Test
	void testVariableOnlyIntermediateImport() {
		String owner = "Foo";
		String methodName = "say";
		String methodDesc = "(Ljava/lang/String;)V";
		String variableName = "message";
		String variableDesc = "Ljava/lang/String;";
		String newVariableName = "text";

		// Build mappings with variable entries only.
		IntermediateMappings intermediate = new IntermediateMappings();
		intermediate.addVariable(owner, methodName, methodDesc, variableDesc, variableName, 1, newVariableName);
		MappingsAdapter adapter = new MappingsAdapter(true, true);
		adapter.importIntermediate(intermediate);

		// Assert that the variable mapping was imported correctly.
		assertEquals(newVariableName, adapter.getMappedVariableName(owner, methodName, methodDesc,
				variableName, variableDesc, 1));
	}
}
