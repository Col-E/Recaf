package software.coley.recaf.services.mapping.format;

import org.junit.jupiter.api.Test;
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
		String methodDesc = "(Ljava/lang/String;)V";
		String fieldDesc = "Ljava/lang/String;";
		adapter.addClass(oldClassName, newClassName);
		adapter.addField(oldClassName, oldFieldName, fieldDesc, newFieldName);
		adapter.addMethod(oldClassName, oldMethodName, methodDesc, newMethodName);

		// Assert registered mapping types can import from the intermediate
		MappingFormatManager formatManager = recaf.get(MappingFormatManager.class);
		assertTrue(formatManager.getMappingFileFormats().size() > 1);
		for (String formatName : formatManager.getMappingFileFormats()) {
			MappingFileFormat format = formatManager.createFormatInstance(formatName);
			assertNotNull(format, "Could not get format: " + formatName);
			System.out.println("Intermediate -> " + format.implementationName());

			// Export and print
			if (format.supportsExportText())
				System.out.println(assertDoesNotThrow(() -> format.exportText(adapter)));
			else
				System.out.println("Mappings does not support text export: " + format.implementationName() + "\n");
		}
	}
}
