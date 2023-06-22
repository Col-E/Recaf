package software.coley.recaf.services.mapping.gen;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.TestBase;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.gen.filter.ExcludeClassesFilter;
import software.coley.recaf.services.mapping.gen.filter.ExcludeModifiersNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeModifiersNameFilter;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.*;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.TextMatchMode;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MappingGenerator}
 */
public class MappingGeneratorTest extends TestBase {
	static Workspace workspace;
	static WorkspaceResource resource;
	static NameGenerator nameGenerator;
	static InheritanceGraph inheritanceGraph;
	static MappingGenerator mappingGenerator;

	@BeforeAll
	static void setup() throws IOException {
		nameGenerator = new NameGenerator() {
			@Nonnull
			@Override
			public String mapClass(@Nonnull ClassInfo info) {
				return "mapped/" + info.getName();
			}

			@Nonnull
			@Override
			public String mapField(@Nonnull ClassInfo owner, @Nonnull FieldMember field) {
				return "mapped" + StringUtil.uppercaseFirstChar(field.getName());
			}

			@Nonnull
			@Override
			public String mapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
				return "mapped" + StringUtil.uppercaseFirstChar(method.getName());
			}
		};
		workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				AccessibleFields.class,
				AccessibleMethods.class,
				AccessibleMethodsChild.class,
				StringConsumer.class,
				StringConsumerUser.class
		));
		resource = workspace.getPrimaryResource();
		workspaceManager.setCurrent(workspace);
		inheritanceGraph = recaf.get(InheritanceGraph.class);
		mappingGenerator = recaf.get(MappingGenerator.class);
	}

	@Test
	void testGeneral() {
		// Apply and assert no unexpected values exist
		Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, null);

		// Should not generate names for internal classes
		assertNull(mappings.getMappedClassName("java/lang/Object"));
		assertNull(mappings.getMappedClassName("java/lang/enum"));

		// Should not generate names for constructors/override/library methods
		//  - but still generate names for members
		String className = AccessibleFields.class.getName().replace('.', '/');
		assertNull(mappings.getMappedMethodName(className, "hashCode", "()I"));
		assertNull(mappings.getMappedMethodName(className, "<init>>", "()V"));
		assertNotNull(mappings.getMappedFieldName(className, "CONSTANT_FIELD", "I"));
		assertNotNull(mappings.getMappedFieldName(className, "privateFinalField", "I"));
		assertNotNull(mappings.getMappedFieldName(className, "protectedField", "I"));
		assertNotNull(mappings.getMappedFieldName(className, "publicField", "I"));
		assertNotNull(mappings.getMappedFieldName(className, "packageField", "I"));
	}

	@Nested
	class Filters {
		@Test
		void testDefaultMapAll() {
			// Empty filter with default to 'true' for mapping
			//  - All classes/fields/methods should be renamed (except <init>/<clinit> and library methods)
			NameGeneratorFilter filter = new NameGeneratorFilter(null, true) {
				// Empty
			};

			// Apply and assert all items are mapped
			Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, filter);
			for (ClassInfo info : resource.getJvmClassBundle()) {
				assertNotNull(mappings.getMappedClassName(info.getName()));
				for (FieldMember field : info.getFields())
					assertNotNull(mappings.getMappedFieldName(info.getName(), field.getName(), field.getDescriptor()),
							"Field not mapped: " + info.getName() + "." + field.getName());
				for (MethodMember method : info.getMethods()) {
					String mappedMethodName =
							mappings.getMappedMethodName(info.getName(), method.getName(), method.getDescriptor());
					if (method.getName().startsWith("<"))
						assertNull(mappedMethodName, "Constructor/static-init had generated mappings");
					else if (inheritanceGraph.getVertex(info.getName())
							.isLibraryMethod(method.getName(), method.getDescriptor()))
						assertNull(mappedMethodName, "Library method had generated mappings");
					else
						assertNotNull(mappedMethodName, "Method not mapped: " + info.getName() + "." + method.getName());
				}
			}
		}

		@Test
		void testDefaultMapNothing() {
			// Empty filter with default to 'false' for mapping
			//  - Nothing should generate
			NameGeneratorFilter filter = new NameGeneratorFilter(null, false) {
				// Empty
			};

			// Apply and assert nothing was generated
			Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, filter);
			IntermediateMappings intermediate = mappings.exportIntermediate();
			assertEquals(0, intermediate.getClasses().size());
			assertEquals(0, intermediate.getFields().size());
			assertEquals(0, intermediate.getMethods().size());
		}

		@Test
		void testExcludeClassNameFilterWithInheritance() {
			// Filter to exclude classes by name and by extension their declared members
			//  - Classes extending them with overridden methods, the overrides should not be remapped
			//
			// We make this filter so that it will target 'AccessibleMethods' which is the base type.
			// The child type 'AccessibleMethodsChild' will not be matched by this filter.
			// The default action of naming this not affect the methods declared in the child class since
			// those methods are overrides of an affected class.
			ExcludeClassesFilter filter =
					new ExcludeClassesFilter(null, "AccessibleMethods", TextMatchMode.ENDS_WITH);

			// Apply and assert all items are mapped except the base classes types
			Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, filter);
			for (ClassInfo info : resource.getJvmClassBundle()) {
				String mappedClass = mappings.getMappedClassName(info.getName());

				// Class should not be renamed if it matches the exclusion filter
				boolean isTargetExclude = info.getName().endsWith("AccessibleMethods");
				boolean isSuperTargetExclude = info.getSuperName().endsWith("AccessibleMethods");
				if (isTargetExclude)
					assertNull(mappedClass);
				else
					assertNotNull(mappedClass);

				// Fields in classes should be remapped if the class does not match the exclusion filter
				for (FieldMember field : info.getFields()) {
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
				for (MethodMember method : info.getMethods()) {
					String mappedMethod =
							mappings.getMappedMethodName(info.getName(), method.getName(), method.getDescriptor());
					if (method.getName().startsWith("<"))
						assertNull(mappedMethod, "Constructor/static-init had generated mappings");
					else if (inheritanceGraph.getVertex(info.getName()).isLibraryMethod(method.getName(), method.getDescriptor()))
						assertNull(mappedMethod, "Library method had generated mappings");
					else if (isTargetExclude)
						assertNull(mappedMethod,
								"Excluded class has method mapped: " + info.getName() + "." + method.getName());
					else if (isSuperTargetExclude &&
							resource.getJvmClassBundle().get(info.getSuperName())
									.getDeclaredMethod(method.getName(), method.getDescriptor()) != null)
						assertNull(mappedMethod,
								"Child of excluded class has method mapped: " + info.getName() + "." + method.getName());
					else
						assertNotNull(mappedMethod, "Method not mapped: " + info.getName() + "." + method.getName());
				}
			}
		}

		@Test
		void testExcludeModifiersOnAll() {
			// Filter to exclude anything private, protected, and public, leaving only package-private items
			ExcludeModifiersNameFilter filter =
					new ExcludeModifiersNameFilter(null, List.of(Opcodes.ACC_PRIVATE, Opcodes.ACC_PROTECTED, Opcodes.ACC_PUBLIC), true, true, true);

			// Generate mappings
			Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, filter);

			// There is 1 package-private field in our workspace
			//  - 'packageField' in 'AccessibleFields'
			// There are only 2 package-private method in our workspace:
			//  - 'packageMethod' in 'AccessibleMethods'
			//  - 'packageMethod' in 'AccessibleMethodsChild'
			IntermediateMappings intermediate = mappings.exportIntermediate();
			assertEquals(0, intermediate.getClasses().size());
			assertEquals(1, intermediate.getFields().size());
			assertEquals(2, intermediate.getMethods().size());
		}

		@Test
		void testExcludeNotTargetingClassYieldsAllClassesMapped() {
			// Generic exception filter that targets fields/methods.
			//  - The classes are not targeted, and since this is an exclude this makes the default action map
			//  - Thus all classes should get mapped
			ExcludeModifiersNameFilter filter =
					new ExcludeModifiersNameFilter(null, List.of(Opcodes.ACC_PRIVATE), false, true, true);

			// Generate mappings and assert all classes were mapped.
			Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, filter);
			IntermediateMappings intermediate = mappings.exportIntermediate();
			assertEquals(5, intermediate.getClasses().size());
		}

		@Test
		void testIncludeModifiers() {
			// Filter to include only private methods
			IncludeModifiersNameFilter filter =
					new IncludeModifiersNameFilter(null, List.of(Opcodes.ACC_PRIVATE), false, false, true);

			// Generate mappings
			Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, filter);

			// There is only 1 private method in our workspace:
			//  - 'privateMethod' in 'AccessibleMethods'
			IntermediateMappings intermediate = mappings.exportIntermediate();
			assertEquals(0, intermediate.getClasses().size());
			assertEquals(0, intermediate.getFields().size());
			assertEquals(1, intermediate.getMethods().size());
		}
	}
}
