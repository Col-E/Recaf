package software.coley.recaf.services.mapping.format;

import org.junit.jupiter.api.Test;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests various {@link MappingFileFormat} implementation's ability to parse input texts.
 */
public class MappingImplementationTests {
	private static final String NAME_SAMPLE = "Sample";
	private static final String NAME_RENAMED = "Renamed";

	@Test
	void testTinyV1() {
		String mappingsText = """
				v1\tintermediary\tnamed
				CLASS\ttest/Greetings\trename/Hello
				FIELD\ttest/Greetings\tLjava/lang/String;\toldField\tnewField
				METHOD\ttest/Greetings\t()V\tsay\tspeak""";
		MappingFileFormat format = new TinyV1Mappings();
		IntermediateMappings mappings = format.parse(mappingsText);
		assertInheritMap(mappings);
	}

	@Test
	void testSimple() {
		String mappingsText = """
				test/Greetings rename/Hello
				test/Greetings.oldField Ljava/lang/String; newField
				test/Greetings.say()V speak""";
		MappingFileFormat format = new SimpleMappings();
		IntermediateMappings mappings = format.parse(mappingsText);
		assertInheritMap(mappings);
	}

	@Test
	void testSrg() {
		String mappingsText = """
				CL: test/Greetings rename/Hello
				FD: test/Greetings/oldField newField
				MD: test/Greetings/say ()V speak""";
		MappingFileFormat format = new SrgMappings();
		IntermediateMappings mappings = format.parse(mappingsText);
		assertInheritMap(mappings);
	}

	@Test
	void testSrgPackageMapping() {
		String mappingsText = """
				PK: test rename
				""";
		MappingFileFormat format = new SrgMappings();
		IntermediateMappings mappings = format.parse(mappingsText);
		assertEquals("rename/Greetings", mappings.getMappedClassName("test/Greetings"));
	}

	@Test
	void testProguard() {
		String mappingsText = """
				# Backwards format because proguard mappings are intended to be undone, not applied
				rename.Hello -> test.Greetings:
				    java.lang.String newField -> oldField
				    void speak() -> say""";
		MappingFileFormat format = new ProguardMappings();
		IntermediateMappings mappings = format.parse(mappingsText);
		assertInheritMap(mappings);
	}

	@Test
	void testEnigma() {
		String mappingsText = """
				CLASS test/Greetings rename/Hello
				\tFIELD oldField newField Ljava/lang/String;
				\tMETHOD say speak ()V""";
		MappingFileFormat format = new EnigmaMappings();
		IntermediateMappings mappings = format.parse(mappingsText);
		assertInheritMap(mappings);
	}

	@Test
	void testJadx() {
		String mappingsText = """
				c test.Greetings = Hello
				f test.Greetings.oldField:Ljava/lang/String; = newField
				m test.Greetings.say()V = speak""";
		MappingFileFormat format = new JadxMappings();
		IntermediateMappings mappings = format.parse(mappingsText);

		// Cannot use same 'assertInheritMap(...)' because Jadx format doesn't allow package renaming
		assertEquals("test/Hello", mappings.getMappedClassName("test/Greetings"));
		assertEquals("newField", mappings.getMappedFieldName("test/Greetings", "oldField", "Ljava/lang/String;"));
		assertEquals("speak", mappings.getMappedMethodName("test/Greetings", "say", "()V"));
	}

	/**
	 * @param mappings
	 * 		Mappings to check.
	 */
	private void assertInheritMap(Mappings mappings) {
		assertEquals("rename/Hello", mappings.getMappedClassName("test/Greetings"));
		assertEquals("newField", mappings.getMappedFieldName("test/Greetings", "oldField", "Ljava/lang/String;"));
		assertEquals("speak", mappings.getMappedMethodName("test/Greetings", "say", "()V"));
	}
}