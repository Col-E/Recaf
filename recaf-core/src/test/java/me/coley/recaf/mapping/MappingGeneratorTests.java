package me.coley.recaf.mapping;

import me.coley.recaf.TestUtils;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.mapping.gen.MappingGenerator;
import me.coley.recaf.mapping.gen.NameGenerator;
import me.coley.recaf.mapping.gen.NameGeneratorFilter;
import me.coley.recaf.mapping.gen.filters.ExcludeClassNameFilter;
import me.coley.recaf.mapping.format.IntermediateMappings;
import me.coley.recaf.search.TextMatchMode;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for mapping generation.
 */
public class MappingGeneratorTests extends TestUtils {
	private static Workspace workspace;
	private static NameGenerator nameGenerator;

	@BeforeAll
	static void setup() throws IOException {
		nameGenerator = new NameGenerator() {
			@Override
			public String mapClass(CommonClassInfo info) {
				return "mapped/" + info.getName();
			}

			@Override
			public String mapField(CommonClassInfo owner, FieldInfo info) {
				return "mapped" + StringUtil.uppercaseFirstChar(info.getName());
			}

			@Override
			public String mapMethod(CommonClassInfo owner, MethodInfo info) {
				return "mapped" + StringUtil.uppercaseFirstChar(info.getName());
			}
		};
		workspace = createWorkspace(jarsDir.resolve("DemoGame.jar"));
		// Assert correct workspace contents
		Resource primary = workspace.getResources().getPrimary();
		assertNotNull(primary.getClasses().get("game/FxMain"));
	}

	@Test
	void testGeneral() {
		Resource primary = workspace.getResources().getPrimary();
		InheritanceGraph graph = new InheritanceGraph(workspace);
		MappingGenerator generator = new MappingGenerator(primary, graph);
		generator.setNameGenerator(nameGenerator);
		// Apply and assert no unexpected values exist
		Mappings mappings = generator.generate();
		// Should not generate names for internal classes
		assertNull(mappings.getMappedClassName("java/lang/Object"));
		assertNull(mappings.getMappedClassName("java/lang/enum"));
		// Should not generate names for constructors/override/library methods
		//  - but still generate names for members
		assertNull(mappings.getMappedMethodName("game/Food", "hashCode", "()I"));
		assertNull(mappings.getMappedMethodName("game/Food", "<init>>", "(III)V"));
		assertNotNull(mappings.getMappedFieldName("game/Food", "x", "I"));
		assertNotNull(mappings.getMappedFieldName("game/Food", "y", "I"));
	}

	@Nested
	class Filters {
		@Test
		void testDefaultMapAll() {
			Resource primary = workspace.getResources().getPrimary();
			InheritanceGraph graph = new InheritanceGraph(workspace);
			MappingGenerator generator = new MappingGenerator(primary, graph);
			generator.setNameGenerator(nameGenerator);
			// Empty filter with default to 'true' for mapping
			//  - All classes/fields/methods should be renamed (except <init>/<clinit> and library methods)
			generator.setFilter(new NameGeneratorFilter(null, true) {

			});
			// Apply and assert all items are mapped
			Mappings mappings = generator.generate();
			for (ClassInfo info : primary.getClasses()) {
				assertNotNull(mappings.getMappedClassName(info.getName()));
				for (FieldInfo field : info.getFields())
					assertNotNull(mappings.getMappedFieldName(info.getName(), field.getName(), field.getDescriptor()),
							"Field not mapped: " + info.getName() + "." + field.getName());
				for (MethodInfo method : info.getMethods()) {
					String mappedMethodName =
							mappings.getMappedMethodName(info.getName(), method.getName(), method.getDescriptor());
					if (method.getName().startsWith("<"))
						assertNull(mappedMethodName, "Constructor/static-init had generated mappings");
					else if (graph.getVertex(info.getName()).isLibraryMethod(method.getName(), method.getDescriptor()))
						assertNull(mappedMethodName, "Library method had generated mappings");
					else
						assertNotNull(mappedMethodName, "Method not mapped: " + info.getName() + "." + method.getName());
				}
			}
		}

		@Test
		void testDefaultMapNothing() {
			Resource primary = workspace.getResources().getPrimary();
			InheritanceGraph graph = new InheritanceGraph(workspace);
			MappingGenerator generator = new MappingGenerator(primary, graph);
			generator.setNameGenerator(nameGenerator);
			// Empty filter with default to 'false' for mapping
			//  - Nothing should generate
			generator.setFilter(new NameGeneratorFilter(null, false) {

			});
			// Apply and assert nothing was generated
			Mappings mappings = generator.generate();
			IntermediateMappings intermediate = mappings.exportIntermediate();
			assertEquals(0, intermediate.getClasses().size());
			assertEquals(0, intermediate.getFields().size());
			assertEquals(0, intermediate.getMethods().size());
		}

		@Test
		void testExcludeClassNameFilter() {
			Resource primary = workspace.getResources().getPrimary();
			InheritanceGraph graph = new InheritanceGraph(workspace);
			MappingGenerator generator = new MappingGenerator(primary, graph);
			generator.setNameGenerator(nameGenerator);
			// Filter to exclude the base abstract classes (by name)
			//  - Classes extending them with overriden methods, the overrides should not be remapped
			generator.setFilter(new ExcludeClassNameFilter(null, "game/Abstract", TextMatchMode.STARTS_WITH));
			// Apply and assert all items are mapped except the base classes types
			Mappings mappings = generator.generate();
			for (ClassInfo info : primary.getClasses()) {
				String mappedClass = mappings.getMappedClassName(info.getName());
				// Class should not be renamed if it matches the exclusion filter
				boolean isTargetExclude = info.getName().startsWith("game/Abstract");
				boolean isSuperTargetExclude = info.getSuperName().startsWith("game/Abstract");
				if (isTargetExclude)
					assertNull(mappedClass);
				else
					assertNotNull(mappedClass);
				// Fields in classes should be remapped if the class does not match the exclusion filter
				for (FieldInfo field : info.getFields()) {
					String mappedField = mappings.getMappedFieldName(info.getName(), field.getName(), field.getDescriptor());
					if (isTargetExclude)
						assertNull(mappedField,
								"Excluded class has field mapped: " + info.getName() + "." + field.getName());
					else
						assertNotNull(mappedField,
								"Field not mapped: " + info.getName() + "." + field.getName());
				}
				// Methods in classes should be remapped if the class does not match the exclusion filter
				//  - methods defined in these classes cannot be renamed in child classes even if the child class
				//    does not match the exclusion filter
				for (MethodInfo method : info.getMethods()) {
					String mappedMethod =
							mappings.getMappedMethodName(info.getName(), method.getName(), method.getDescriptor());
					if (method.getName().startsWith("<"))
						assertNull(mappedMethod, "Constructor/static-init had generated mappings");
					else if (graph.getVertex(info.getName()).isLibraryMethod(method.getName(), method.getDescriptor()))
						assertNull(mappedMethod, "Library method had generated mappings");
					else if (isTargetExclude)
						assertNull(mappedMethod,
								"Excluded class has method mapped: " + info.getName() + "." + method.getName());
					else if (isSuperTargetExclude &&
							primary.getClasses().get(info.getSuperName())
									.findMethod(method.getName(), method.getDescriptor()) != null)
						assertNull(mappedMethod,
								"Child of excluded class has method mapped: " + info.getName() + "." + method.getName());
					else
						assertNotNull(mappedMethod, "Method not mapped: " + info.getName() + "." + method.getName());
				}
			}
		}
	}
}
