package software.coley.recaf.services.mapping.format;

import org.junit.jupiter.api.Test;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests various {@link MappingFileFormat} implementation's ability to parse input texts.
 */
public class MappingImplementationTest {
	@Test
	void testTinyV1() {
		String mappingsText = """
				v1\tintermediary\tnamed
				CLASS\ttest/Greetings\trename/Hello
				FIELD\ttest/Greetings\tLjava/lang/String;\toldField\tnewField
				METHOD\ttest/Greetings\t()V\tsay\tspeak""";
		MappingFileFormat format = new TinyV1Mappings();
		IntermediateMappings mappings = assertDoesNotThrow(() -> format.parse(mappingsText));
		assertInheritMap(mappings);
	}

	@Test
	void testTinyV1WithTwoOutputs() {
		String mappingsText = """
				v1\tintermediary\tobfuscated\tnamed
				CLASS\ttest/Greetings\ta\trename/Hello
				FIELD\ttest/Greetings\tLjava/lang/String;\toldField\tb\tnewField
				METHOD\ttest/Greetings\t()V\tsay\tc\tspeak""";
		MappingFileFormat format = new TinyV1Mappings();
		IntermediateMappings mappings = assertDoesNotThrow(() -> format.parse(mappingsText));
		assertInheritMap(mappings);

		// Extra asserts for the intermediate 'obfuscated' column
		assertEquals("rename/Hello", mappings.getMappedClassName("a"));
		assertEquals("newField", mappings.getMappedFieldName("a", "b", "Ljava/lang/String;"));
		assertEquals("speak", mappings.getMappedMethodName("a", "c", "()V"));
	}

	@Test
	void testTinyV2() {
		String mappingsText = """
				tiny\t2\t0\tofficial\tobfuscated\tnamed
				c\ttest/Greetings\ta\trename/Hello
				\tf\tLjava/lang/String;\toldField\tb\tnewField
				\tm\t()V\tsay\tc\tspeak
				""";
		MappingFileFormat format = new TinyV2Mappings();
		IntermediateMappings mappings = assertDoesNotThrow(() -> format.parse(mappingsText));
		assertInheritMap(mappings);
	}

	@Test
	void testSimple() {
		String mappingsText = """
				test/Greetings rename/Hello
				test/Greetings.oldField Ljava/lang/String; newField
				test/Greetings.say()V speak""";
		MappingFileFormat format = new SimpleMappings();
		IntermediateMappings mappings = assertDoesNotThrow(() -> format.parse(mappingsText));
		assertInheritMap(mappings);
	}

	@Test
	void testSrg() {
		String mappingsText = """
				CL: test/Greetings rename/Hello
				FD: test/Greetings/oldField newField
				MD: test/Greetings/say ()V speak""";
		MappingFileFormat format = new SrgMappings();
		IntermediateMappings mappings = assertDoesNotThrow(() -> format.parse(mappingsText));
		assertInheritMap(mappings);
	}

	@Test
	void testSrgWithCleanName() {
		String mappingsText = """
				CL: test/Greetings rename/Hello
				FD: test/Greetings/oldField rename/Hello/newField
				MD: test/Greetings/say ()V rename/Hello/speak""";
		MappingFileFormat format = new SrgMappings();
		IntermediateMappings mappings = assertDoesNotThrow(() -> format.parse(mappingsText));
		assertInheritMap(mappings);
	}

	@Test
	void testXSrg() {
		String mappingsText = """
				CL: test/Greetings rename/Hello
				FD: test/Greetings/oldField Ljava/lang/String; rename/Hello/newField Ljava/lang/String;
				MD: test/Greetings/say ()V rename/Hello/speak""";
		MappingFileFormat format = new SrgMappings();
		IntermediateMappings mappings = assertDoesNotThrow(() -> format.parse(mappingsText));
		assertInheritMap(mappings);
	}

	@Test
	void testSrgPackageMapping() {
		String mappingsText = """
				PK: test rename
				""";
		MappingFileFormat format = new SrgMappings();
		IntermediateMappings mappings = assertDoesNotThrow(() -> format.parse(mappingsText));
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
		IntermediateMappings mappings = assertDoesNotThrow(() -> format.parse(mappingsText));
		assertInheritMap(mappings);
	}

	@Test
	void testEnigma() {
		String mappingsText = """
				COMMENT foo
				CLASS test/Greetings rename/Hello
				\tFIELD oldField newField Ljava/lang/String;
				\tMETHOD say speak ()V
				\tCLASS Inner RenamedInner""";
		String mappingsTextWithTrailingNewline = mappingsText + "\n";

		// The mapped names are optional, so we should be able to parse a sample with no
		// actual target names, and get an empty result.
		String mappingsTextWithNoDestinationNames = """
				CLASS test/Greetings
				\tFIELD oldField Ljava/lang/String;
				\tMETHOD say ()V""";
		MappingFileFormat format = new EnigmaMappings();
		IntermediateMappings mappings = assertDoesNotThrow(() -> format.parse(mappingsTextWithNoDestinationNames));
		assertTrue(mappings.getClasses().isEmpty());
		assertTrue(mappings.getFields().isEmpty());
		assertTrue(mappings.getMethods().isEmpty());

		// The format spec says there should be a trailing newline, but we'll support both cases
		mappings = assertDoesNotThrow(() -> format.parse(mappingsText));
		assertInheritMap(mappings);
		mappings = assertDoesNotThrow(() -> format.parse(mappingsTextWithTrailingNewline));
		assertInheritMap(mappings);
		assertEquals("rename/Hello$RenamedInner", mappings.getMappedClassName("test/Greetings$Inner"));

		// This is an extreme edge case of comments and newlines spread out randomly
		String sampleWithCommentsAndNewlines = """
				COMMENT class comment # ignored
				# also ignored
							\t
				CLASS test/Greetings rename/Hello
				\t# field comment up next
							\t
				# not indented but still ignored
				\tCOMMENT This is a comment on the field
							\t
				\tFIELD oldField newField Ljava/lang/String;
							\t
				\tMETHOD say speak ()V
							\t
				\t\t# There are no args
				\t\tARG noArgsHere""";
		mappings = assertDoesNotThrow(() -> format.parse(sampleWithCommentsAndNewlines));
		assertInheritMap(mappings);
	}

	@Test
	void testJadx() {
		String mappingsText = """
				c test.Greetings = Hello
				f test.Greetings.oldField:Ljava/lang/String; = newField
				m test.Greetings.say()V = speak""";
		MappingFileFormat format = new JadxMappings();
		IntermediateMappings mappings = assertDoesNotThrow(() -> format.parse(mappingsText));

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
