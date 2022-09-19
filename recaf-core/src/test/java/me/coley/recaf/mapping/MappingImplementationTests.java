package me.coley.recaf.mapping;

import me.coley.recaf.TestUtils;
import me.coley.recaf.mapping.format.IntermediateMappings;
import me.coley.recaf.mapping.format.ProguardMappings;
import me.coley.recaf.mapping.format.SimpleMappings;
import me.coley.recaf.mapping.format.TinyV1Mappings;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for mapping implementations.
 */
public class MappingImplementationTests extends TestUtils {
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
	void testTinyV1() {
		try {
			String mappingsText = new String(Files.readAllBytes(mapsDir.resolve("inherit-method-map-tiny-1.txt")));
			Mappings mappings = new TinyV1Mappings();
			mappings.parse(mappingsText);
			assertInheritMap(mappings);
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
			assertInheritMap(mappings);
		} catch (IOException e) {
			fail(e);
		}
	}

	@Test
	void testProguard() {
		try {
			String mappingsText = new String(Files.readAllBytes(mapsDir.resolve("inherit-method-map-proguard.txt")));
			Mappings mappings = new ProguardMappings();
			mappings.parse(mappingsText);
			assertInheritMap(mappings);
		} catch (IOException e) {
			fail(e);
		}
	}

	// TODO: Test cases for other formats once supported
	//  - TinyV2
	//  - TSRG
	//  - JadX
	//  - Enigma

	/**
	 *
	 * @param mappings Mappings to check.
	 */
	private void assertInheritMap(Mappings mappings) {
		assertEquals("rename/Hello", mappings.getMappedClassName("test/Greetings"));
		assertEquals("newField", mappings.getMappedFieldName("test/Greetings", "oldField", "Ljava/lang/String;"));
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
