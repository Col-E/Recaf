package software.coley.recaf.services.mapping.gen;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.StubClassInfo;
import software.coley.recaf.info.StubFieldMember;
import software.coley.recaf.info.StubMethodMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.aggregate.AggregatedMappings;
import software.coley.recaf.services.mapping.data.FieldMapping;
import software.coley.recaf.services.mapping.data.MethodMapping;
import software.coley.recaf.services.mapping.gen.filter.ExcludeClassesFilter;
import software.coley.recaf.services.mapping.gen.filter.ExcludeExistingMappedFilter;
import software.coley.recaf.services.mapping.gen.filter.ExcludeModifiersNameFilter;
import software.coley.recaf.services.mapping.gen.filter.ExcludeNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeClassesFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeKeywordNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeLongNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeModifiersNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeNonAsciiNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeNonJavaIdentifierNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeWhitespaceNameFilter;
import software.coley.recaf.services.mapping.gen.filter.NameGeneratorFilter;
import software.coley.recaf.services.mapping.gen.naming.NameGenerator;
import software.coley.recaf.services.search.match.StringPredicate;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.AccessibleFields;
import software.coley.recaf.test.dummy.AccessibleMethods;
import software.coley.recaf.test.dummy.AccessibleMethodsChild;
import software.coley.recaf.test.dummy.StringConsumer;
import software.coley.recaf.test.dummy.StringConsumerUser;
import software.coley.recaf.util.EscapeUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	static StringPredicateProvider strMatchProvider;

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

			@Nonnull
			@Override
			public String mapVariable(@Nonnull ClassInfo owner, @Nonnull MethodMember declaringMethod, @Nonnull LocalVariable variable) {
				return "mapped" + StringUtil.uppercaseFirstChar(variable.getName());
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
		inheritanceGraph = recaf.get(InheritanceGraphService.class).getCurrentWorkspaceInheritanceGraph();
		mappingGenerator = recaf.get(MappingGenerator.class);
		strMatchProvider = recaf.get(StringPredicateProvider.class);
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
		className = StringConsumerUser.class.getName().replace('.', '/');
		assertNotNull(mappings.getMappedVariableName(className, "main", "([Ljava/lang/String;)V", "args", "[Ljava/lang/String;", 0));
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
					else {
						assertNotNull(mappedMethodName, "Method not mapped: " + info.getName() + "." + method.getName());
						for (LocalVariable variable : method.getLocalVariables()) {
							// Skip asserts for "this" local variable, we want to keep that one.
							if (variable.getIndex() == 0 && variable.getName().equals("this"))
								continue;

							String mappedVariableName = mappings.getMappedVariableName(info.getName(),
									method.getName(), method.getDescriptor(), variable.getName(),
									variable.getDescriptor(), variable.getIndex());
							assertNotNull(mappedMethodName, "Method variable not mapped: " + info.getName() + "."
									+ method.getName() + " - " + variable.getName());
						}
					}
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
			assertEquals(0, intermediate.getVariables().size());
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
					new ExcludeClassesFilter(null, strMatchProvider.newEndsWithPredicate("AccessibleMethods"));

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

			// None of the methods included for mapping have any variables
			assertTrue(intermediate.getVariables().isEmpty());
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
		void testExcludeNameFilter() {
			// Filter to exclude:
			// - Any class with 'Accessible' in the name
			// - Any field with 'Field' in the name
			// - Any method with 'Method' in the name
			// - Any variable with 'args' in the name
			// Each filter is independent, so if a class is named 'AccessibleThing' the class won't be renamed,
			// but its fields/methods/variables will still be renamed as long as their names do not contain the excluded text.
			ExcludeNameFilter filter = new ExcludeNameFilter(null,
					strMatchProvider.newContainsPredicate("Accessible"), // Class filter
					strMatchProvider.newContainsPredicate("Field"), // Field filter
					strMatchProvider.newContainsPredicate("Method"), // Method filter
					strMatchProvider.newContainsPredicate("args") // Variable filter
			);

			// Generate mappings
			Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, filter);
			IntermediateMappings intermediate = mappings.exportIntermediate();

			// Only StringConsumer and StringConsumerUser should be mapped since they do not have 'Accessible' in their name.
			assertEquals(2, intermediate.getClasses().size());

			// Only AccessibleFields has fields, but only the constant field will match since it is 'FIELD' in capital letters.
			Map<String, List<FieldMapping>> classesWithMappedFields = intermediate.getFields();
			List<FieldMapping> fieldMappings = classesWithMappedFields.get(AccessibleFields.class.getName().replace('.', '/'));
			assertEquals(1, classesWithMappedFields.size());
			assertEquals(1, fieldMappings.size());
			assertEquals("CONSTANT_FIELD", fieldMappings.getFirst().getOldName());

			// Only StringConsumer and StringConsumerUser will have mapped methods.
			// The AccessibleMethods and AccessibleMethodsChild classes only have methods with 'Method' in the name, so they will be excluded.
			Map<String, List<MethodMapping>> classesWithMappedMethods = intermediate.getMethods();
			assertEquals(2, classesWithMappedMethods.size());
			List<MethodMapping> consumerMethodMappings = classesWithMappedMethods.get(StringConsumer.class.getName().replace('.', '/'));
			List<MethodMapping> consumerUserMethodMappings = classesWithMappedMethods.get(StringConsumerUser.class.getName().replace('.', '/'));
			assertEquals(1, consumerMethodMappings.size());
			assertEquals("accept", consumerMethodMappings.getFirst().getOldName());
			assertEquals(1, consumerUserMethodMappings.size());
			assertEquals("main", consumerUserMethodMappings.getFirst().getOldName());

			// Nothing
			assertEquals(0, intermediate.getVariables().size());
		}

		@Test
		@SuppressWarnings("DataFlowIssue")
		void testExcludeExistingMappedFilter() {
			// Setup dummy aggregate mappings with some existing mappings to test against.
			// Normally these would be filled with user mappings when using the UI.
			AggregatedMappings aggregate = new AggregatedMappings(workspace);
			aggregate.addClass("com/example/AccessibleFields", "mapped/MappedFields");
			aggregate.addField("com/example/AccessibleFields", "I", "CONSTANT_FIELD", "MAPPED_FIELD");
			aggregate.addClass("com/example/AccessibleMethods", "mapped/MappedMethods");
			aggregate.addMethod("com/example/AccessibleMethods", "()V", "publicMethod", "mappedMethod");

			// Now verify that if we feed it names of the mapped class and members, that the filter will exclude them from being mapped again.
			ExcludeExistingMappedFilter filter = new ExcludeExistingMappedFilter(null, aggregate);
			StubClassInfo preFieldsClass = new StubClassInfo("com/example/AccessibleFields",
					List.of(new StubFieldMember("CONSTANT_FIELD", "I", 0)), List.of());
			StubClassInfo preMethodsClass = new StubClassInfo("com/example/AccessibleMethods",
					List.of(), List.of(new StubMethodMember("publicMethod", "()V", 0)));
			StubClassInfo postFieldsClass = new StubClassInfo("mapped/MappedFields",
					List.of(new StubFieldMember("MAPPED_FIELD", "I", 0)), List.of());
			StubClassInfo postMethodsClass = new StubClassInfo("mapped/MappedMethods",
					List.of(), List.of(new StubMethodMember("mappedMethod", "()V", 0)));

			// Original names can be mapped because they are not the resulting names in the aggregate.
			assertTrue(filter.shouldMapClass(preFieldsClass));
			assertTrue(filter.shouldMapClass(preMethodsClass));

			// Same idea for members, the original names can be mapped.
			assertTrue(filter.shouldMapField(preFieldsClass, preFieldsClass.getFirstDeclaredFieldByName("CONSTANT_FIELD")));
			assertTrue(filter.shouldMapMethod(preMethodsClass, preMethodsClass.getFirstDeclaredMethodByName("publicMethod")));

			// Classes that are clearly mapped as a result of a prior operation (thus appearing in the aggregate) should not be mapped again.
			assertFalse(filter.shouldMapClass(postFieldsClass));
			assertFalse(filter.shouldMapClass(postMethodsClass));

			// Same idea again for members, the mapped names should not be mapped again.
			assertFalse(filter.shouldMapField(postFieldsClass, postFieldsClass.getFirstDeclaredFieldByName("MAPPED_FIELD")));
			assertFalse(filter.shouldMapMethod(preMethodsClass, postMethodsClass.getFirstDeclaredMethodByName("mappedMethod")));
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
			List<MethodMapping> methodMappings = intermediate.getMethods().get(AccessibleMethods.class.getName().replace('.', '/'));
			assertEquals(1, methodMappings.size());
			assertEquals("privateMethod", methodMappings.get(0).getOldName());
		}

		@Test
		void testIncludeNameFilter() {
			StringPredicate predicate = strMatchProvider.newContainsPredicate("AccessibleMethods");
			IncludeNameFilter filter =
					new IncludeNameFilter(null, predicate, predicate, predicate, predicate);

			// Generate mappings
			Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, filter);

			// There are only two classes that contain the given text, AccessibleMethods and AccessibleMethodsChild
			IntermediateMappings intermediate = mappings.exportIntermediate();
			assertEquals(2, intermediate.getClasses().size());
			assertEquals(0, intermediate.getFields().size());
			assertEquals(0, intermediate.getMethods().size());
			assertEquals(0, intermediate.getVariables().size());
		}

		/** Similar to {@link #testIncludeNameFilter()} but with a filter that includes members when a class is mapped */
		@Test
		void testIncludeClassesFilter() {
			IncludeClassesFilter filter =
					new IncludeClassesFilter(null, strMatchProvider.newContainsPredicate("AccessibleMethods"));

			// Generate mappings
			Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, filter);

			// There are only two classes that contain the given text, AccessibleMethods and AccessibleMethodsChild
			// All of their methods should be remapped. They contain no fields.
			IntermediateMappings intermediate = mappings.exportIntermediate();
			assertEquals(2, intermediate.getClasses().size());
			assertEquals(0, intermediate.getFields().size());
			assertEquals(2, intermediate.getMethods().size());
			assertEquals(0, intermediate.getVariables().size()); // None of the methods have locals
			String key = AccessibleMethods.class.getName().replace('.', '/');
			String keyChild = AccessibleMethodsChild.class.getName().replace('.', '/');
			List<MethodMapping> methodMappings = intermediate.getMethods().get(key);
			List<MethodMapping> methodMappingsChild = intermediate.getMethods().get(keyChild);
			assertEquals(workspace.findClass(key).getValue().getMethods().size() - 1, methodMappings.size()); // -1 because <init>
			assertEquals(workspace.findClass(keyChild).getValue().getMethods().size() - 1, methodMappingsChild.size());
		}

		@Test
		void testIncludeKeywordNameFilter() {
			IncludeKeywordNameFilter filter = new IncludeKeywordNameFilter(null);

			// Classes with keywords in their name should be mapped, even if the keyword is not a standalone part of the name.
			assertTrue(filter.shouldMapClass(new StubClassInfo("com/example/void")));
			assertTrue(filter.shouldMapClass(new StubClassInfo("com/void/Example")));
			assertTrue(filter.shouldMapClass(new StubClassInfo("void/example/Example")));
			assertTrue(filter.shouldMapClass(new StubClassInfo("void")));

			// Edge case: 'package-info' and 'module-info' are not keywords, but they contain the keyword 'package' and 'module' respectively.
			assertFalse(filter.shouldMapClass(new StubClassInfo("package-info")));
			assertFalse(filter.shouldMapClass(new StubClassInfo("module-info")));
		}

		@Test
		void testIncludeWhitespaceNameFilter() {
			IncludeWhitespaceNameFilter filter = new IncludeWhitespaceNameFilter(null);

			// Classes with spaces in their name should be mapped, even if the spaces are not a standalone part of the name.
			Set<String> whitespaceStrings = EscapeUtil.getWhitespaceStrings();
			assertFalse(whitespaceStrings.isEmpty(), "No whitespaces registered in EscapeUtil");
			for (String space : whitespaceStrings) {
				assertTrue(filter.shouldMapClass(new StubClassInfo("com/example/_".replace("_", space))));
				assertTrue(filter.shouldMapClass(new StubClassInfo("com/_/Example".replace("_", space))));
				assertTrue(filter.shouldMapClass(new StubClassInfo("_/example/Example".replace("_", space))));
				assertTrue(filter.shouldMapClass(new StubClassInfo("_".replace("_", space))));
			}
		}

		@Test
		@SuppressWarnings("UnnecessaryUnicodeEscape")
		void testIncludeNonAsciiNameFilter() {
			IncludeNonAsciiNameFilter filter = new IncludeNonAsciiNameFilter(null);

			// Base case, a normal Java identifier should not be mapped.
		    assertFalse(filter.shouldMapClass(new StubClassInfo("com/example/Example")));

			// A class with a non-ASCII character in the name should be mapped.
			assertTrue(filter.shouldMapClass(new StubClassInfo("com/example/Exämple")));

			// Big block of Unicode characters, should be mapped.
			assertTrue(filter.shouldMapClass(new StubClassInfo("\u062a\u0634\u064a\u0632\u0020\u0628\u0631\u062c\u0631")));

			// Technically in the ASCII set, but still non-ASCII in the sense of Java identifiers, should be mapped.
			assertTrue(filter.shouldMapClass(new StubClassInfo("\0")));
		}

		@Test
		void testIncludeNonJavaIdentifierNameFilter() {
			IncludeNonJavaIdentifierNameFilter filter = new IncludeNonJavaIdentifierNameFilter(null);

			// Base case, a normal Java identifier should not be mapped.
			assertFalse(filter.shouldMapClass(new StubClassInfo("com/example/Example")));

			// Slight change, adding a number to the front is invalid.
			// A number at the end is fine, but not at the start.
			assertTrue(filter.shouldMapClass(new StubClassInfo("com/example/1Example")));
			assertFalse(filter.shouldMapClass(new StubClassInfo("com/example/Example1")));

			// Zero-width package separator is not a valid Java identifier character, should be mapped.
			assertFalse(filter.shouldMapClass(new StubClassInfo("com//Example")));

			// A package part that starts with a number is not valid, should be mapped.
			assertTrue(filter.shouldMapClass(new StubClassInfo("com/1A/Example")));
			assertFalse(filter.shouldMapClass(new StubClassInfo("com/A1/Example")));

			// Edge case: 'package-info' and 'module-info' are not valid Java identifiers, but they are exempt
			// from this filter since they have special meaning in Java.
			assertFalse(filter.shouldMapClass(new StubClassInfo("package-info")));
			assertFalse(filter.shouldMapClass(new StubClassInfo("module-info")));
		}

		@Test
		void testIncludeLongNameFilter() {
			// Anything > 100 characters
			IncludeLongNameFilter filter = new IncludeLongNameFilter(null, 100, true, true, true, true);

			// 10 character name -> no
			// 100 character name -> no
			// 101 character name -> yes
			// 110 character name -> yes
			String name10 = "0123456789";
			assertFalse(filter.shouldMapClass(new StubClassInfo(name10)));
			assertFalse(filter.shouldMapClass(new StubClassInfo(name10.repeat(10))));
			assertTrue(filter.shouldMapClass(new StubClassInfo(name10.repeat(10) + "0")));
			assertTrue(filter.shouldMapClass(new StubClassInfo(name10.repeat(11))));
		}
	}
}
