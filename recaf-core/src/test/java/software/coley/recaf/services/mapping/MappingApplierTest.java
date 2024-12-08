package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.AnnotationElement;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.info.properties.builtin.OriginalClassNameProperty;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.mapping.aggregate.AggregateMappingManager;
import software.coley.recaf.services.mapping.aggregate.AggregatedMappings;
import software.coley.recaf.services.mapping.gen.MappingGenerator;
import software.coley.recaf.services.mapping.gen.naming.NameGenerator;
import software.coley.recaf.services.mapping.gen.filter.NameGeneratorFilter;
import software.coley.recaf.services.mapping.gen.naming.AlphabetNameGenerator;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.*;
import software.coley.recaf.util.ClassDefiner;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MappingApplier} with some edge case classes.
 */
class MappingApplierTest extends TestBase {
	static NameGenerator nameGenerator;
	MappingGenerator mappingGenerator;
	Workspace workspace;
	WorkspaceResource resource;
	AggregateMappingManager aggregateMappingManager;
	InheritanceGraph inheritanceGraph;
	MappingApplierService mappingApplierService;

	@BeforeAll
	static void setupGenerator() {
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		nameGenerator = new AlphabetNameGenerator(alphabet, 3);
	}

	@BeforeEach
	void prepareWorkspace() throws IOException {
		// We want to reset the workspace before each test
		workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				AnonymousLambda.class,
				StringSupplier.class,
				//
				DummyEnum.class,
				DummyEnumPrinter.class,
				//
				AnnotationImpl.class,
				ClassWithAnnotation.class,
				//
				OverlapInterfaceA.class,
				OverlapInterfaceB.class,
				OverlapClassAB.class,
				OverlapCaller.class
		));
		resource = workspace.getPrimaryResource();
		workspaceManager.setCurrent(workspace);
		aggregateMappingManager = recaf.get(AggregateMappingManager.class);
		inheritanceGraph = recaf.get(InheritanceGraphService.class).getCurrentWorkspaceInheritanceGraph();
		mappingGenerator = recaf.get(MappingGenerator.class);
		mappingApplierService = recaf.get(MappingApplierService.class);
	}

	@Test
	void longName() {
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		AlphabetNameGenerator longNameGenerator = new AlphabetNameGenerator(alphabet, 2048);
		workspace.getPrimaryResource().getJvmClassBundle().forEach(cls -> {
			assertDoesNotThrow(() -> longNameGenerator.mapClass(cls));
		});
	}

	@Test
	void applyAnonymousLambda() {
		String stringSupplierName = StringSupplier.class.getName().replace('.', '/');
		String anonymousLambdaName = AnonymousLambda.class.getName().replace('.', '/');

		// Create mappings for all classes but the runner 'AnonymousLambda'
		Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, new NameGeneratorFilter(null, true) {
			@Override
			public boolean shouldMapClass(@Nonnull ClassInfo info) {
				return !info.getName().equals(anonymousLambdaName);
			}

			@Override
			public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
				return shouldMapClass(owner);
			}
		});

		// Preview the mapping operation
		MappingResults results = mappingApplierService.inCurrentWorkspace().applyToPrimaryResource(mappings);

		// The supplier class we define should be remapped.
		// The runner class (AnonymousLambda) itself should not be remapped, but should be updated to point to
		// the new StringSupplier class name.
		String mappedStringSupplierName = mappings.getMappedClassName(stringSupplierName);
		assertNotNull(mappedStringSupplierName, "StringSupplier should be remapped");
		assertNull(mappings.getMappedClassName(anonymousLambdaName), "AnonymousLambda should not be remapped");
		assertTrue(results.wasMapped(stringSupplierName), "StringSupplier should have updated");
		assertTrue(results.wasMapped(anonymousLambdaName), "AnonymousLambda should have updated");

		// Verify that the original name is stored as a property.
		ClassPathNode classPath = results.getPostMappingPath(stringSupplierName);
		assertNotNull(classPath, "Could not find mapped StringSupplier in workspace");
		JvmClassInfo mappedStringSupplier = classPath.getValue().asJvmClass();
		assertEquals(stringSupplierName, OriginalClassNameProperty.get(mappedStringSupplier),
				"Did not record original name after applying mappings");

		// Assert that the method is still runnable.
		String result = runMapped(AnonymousLambda.class, "run");
		assertTrue(result.contains("One: java.util.function.Supplier"),
				"JDK class reference should not be mapped");
		assertFalse(result.contains(stringSupplierName),
				"Class reference to '" + stringSupplierName + "' should have been remapped");

		// Assert aggregate updated too.
		// We will validate this is only done AFTER 'results.apply()' is run.
		// For future tests we will skip this since if it works here, it works there.
		AggregatedMappings aggregatedMappings = aggregateMappingManager.getAggregatedMappings();
		assertNull(aggregatedMappings.getMappedClassName(stringSupplierName),
				"StringSupplier should not yet be tracked in aggregate");
		results.apply();
		assertNotNull(aggregatedMappings.getMappedClassName(stringSupplierName),
				"StringSupplier should be tracked in aggregate");
	}

	@Test
	void applyDummyEnumPrinter() {
		String dummyEnumName = DummyEnum.class.getName().replace('.', '/');
		String dummyEnumPrinterName = DummyEnumPrinter.class.getName().replace('.', '/');

		// Create mappings for all classes but the runner 'DummyEnumPrinter'
		Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, new NameGeneratorFilter(null, true) {
			@Override
			public boolean shouldMapClass(@Nonnull ClassInfo info) {
				return !info.getName().equals(dummyEnumPrinterName);
			}

			@Override
			public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
				return shouldMapClass(owner);
			}
		});

		// Preview the mapping operation
		MappingResults results = mappingApplierService.inCurrentWorkspace().applyToPrimaryResource(mappings);

		// The enum class we define should be remapped.
		// The runner class (DummyEnumPrinter) itself should not be remapped, but should be updated to point to
		// the new DummyEnum class name.
		assertNotNull(mappings.getMappedClassName(dummyEnumName), "DummyEnum should be remapped");
		assertNull(mappings.getMappedClassName(dummyEnumPrinterName), "DummyEnumPrinter should not be remapped");
		assertNull(mappings.getMappedMethodName(dummyEnumName, "values", "()[L" + dummyEnumName + ";"),
				"DummyEnum#values() should not be remapped");
		assertNull(mappings.getMappedMethodName(dummyEnumName, "valueOf", "(Ljava/lang/String;)L" + dummyEnumName + ";"),
				"DummyEnum#valueOf(String) should not be remapped");
		assertTrue(results.wasMapped(dummyEnumName), "DummyEnum should have updated");
		assertTrue(results.wasMapped(dummyEnumPrinterName), "DummyEnumPrinter should have updated");

		// Assert aggregate updated too.
		results.apply();
		AggregatedMappings aggregatedMappings = aggregateMappingManager.getAggregatedMappings();
		assertNotNull(aggregatedMappings.getMappedClassName(dummyEnumName),
				"DummyEnum should be tracked in aggregate");

		// Assert that the methods are still runnable.
		runMapped(DummyEnumPrinter.class, "run1");
		runMapped(DummyEnumPrinter.class, "run2");
	}

	@Test
	void applyClassWithAnnotation() {
		String annotationName = AnnotationImpl.class.getName().replace('.', '/');
		String classWithAnnotationName = ClassWithAnnotation.class.getName().replace('.', '/');

		// Create mappings for all classes but the target 'ClassWithAnnotation'
		Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, new NameGeneratorFilter(null, true) {
			@Override
			public boolean shouldMapClass(@Nonnull ClassInfo info) {
				return !info.getName().equals(classWithAnnotationName);
			}
		});

		// Preview the mapping operation
		MappingResults results = mappingApplierService.inCurrentWorkspace().applyToPrimaryResource(mappings);

		// The annotation class we define should be remapped.
		// The user class (ClassWithAnnotation) itself should not be remapped,
		// but its annotation usage should be updated.
		String mappedAnnotationName = mappings.getMappedClassName(annotationName);
		assertNotNull(mappedAnnotationName, "AnnotationImpl should be remapped");
		assertNull(mappings.getMappedClassName(classWithAnnotationName), "ClassWithAnnotation should not be remapped");
		assertTrue(results.wasMapped(annotationName), "AnnotationImpl should have updated");
		assertTrue(results.wasMapped(classWithAnnotationName), "ClassWithAnnotation should have updated");

		// Assert aggregate updated too.
		results.apply();
		AggregatedMappings aggregatedMappings = aggregateMappingManager.getAggregatedMappings();
		assertNotNull(aggregatedMappings.getMappedClassName(annotationName),
				"AnnotationImpl should be tracked in aggregate");

		// Get the names of the annotation's mapped attribute methods
		String annoValueName = mappings.getMappedMethodName(annotationName, "value", "()Ljava/lang/String;");
		String annoPolicyName = mappings.getMappedMethodName(annotationName, "policy", "()Ljava/lang/annotation/Retention;");

		// Assert the user class has the correct new values
		ClassPathNode classPath = results.getPostMappingPath(classWithAnnotationName);
		assertNotNull(classPath, "Could not find: " + classWithAnnotationName);
		JvmClassInfo classWithAnnotation = classPath.getValue().asJvmClass();
		AnnotationInfo annotationInfo = classWithAnnotation.getAnnotations().get(0);
		assertEquals("L" + mappedAnnotationName + ";", annotationInfo.getDescriptor(),
				"AnnotationImpl not remapped in ClassWithAnnotation");
		AnnotationElement valueElement = annotationInfo.getElements().get(annoValueName);
		AnnotationElement policyElement = annotationInfo.getElements().get(annoPolicyName);
		assertNotNull(valueElement, "Missing mapped value element");
		assertNotNull(policyElement, "Missing mapped policy element");
	}

	@Test
	void applyOverlapping() {
		String overlapInterfaceAName = OverlapInterfaceA.class.getName().replace('.', '/');
		String overlapInterfaceBName = OverlapInterfaceB.class.getName().replace('.', '/');
		String overlapClassABName = OverlapClassAB.class.getName().replace('.', '/');
		String overlapCallerName = OverlapCaller.class.getName().replace('.', '/');

		// Create mappings for all classes but the runner 'OverlapCaller'
		Mappings mappings = mappingGenerator.generate(workspace, resource, inheritanceGraph, nameGenerator, new NameGeneratorFilter(null, true) {
			@Override
			public boolean shouldMapClass(@Nonnull ClassInfo info) {
				return !info.getName().equals(overlapCallerName);
			}

			@Override
			public boolean shouldMapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
				return shouldMapClass(owner);
			}
		});

		// Preview the mapping operation
		MappingResults results = mappingApplierService.inCurrentWorkspace().applyToPrimaryResource(mappings);

		assertNotNull(mappings.getMappedClassName(overlapInterfaceAName), "OverlapInterfaceA should be remapped");
		assertNotNull(mappings.getMappedClassName(overlapInterfaceBName), "OverlapInterfaceB should be remapped");
		assertNotNull(mappings.getMappedClassName(overlapClassABName), "OverlapClassAB should be remapped");
		assertNull(mappings.getMappedClassName(overlapCallerName), "OverlapCaller should not be remapped");
		assertTrue(results.wasMapped(overlapInterfaceAName), "OverlapInterfaceA should have updated");
		assertTrue(results.wasMapped(overlapInterfaceBName), "OverlapInterfaceB should have updated");
		assertTrue(results.wasMapped(overlapClassABName), "OverlapClassAB should have updated");
		assertTrue(results.wasMapped(overlapCallerName), "OverlapCaller should have updated");

		// Assert aggregate updated too.
		results.apply();
		AggregatedMappings aggregatedMappings = aggregateMappingManager.getAggregatedMappings();
		assertNotNull(aggregatedMappings.getMappedClassName(overlapInterfaceAName),
				"OverlapInterfaceA should be tracked in aggregate");
		assertNotNull(aggregatedMappings.getMappedClassName(overlapInterfaceBName),
				"OverlapInterfaceB should be tracked in aggregate");
		assertNotNull(aggregatedMappings.getMappedClassName(overlapClassABName),
				"OverlapClassAB should be tracked in aggregate");
		assertNull(aggregatedMappings.getMappedClassName(overlapCallerName),
				"OverlapCaller should not be tracked in aggregate");

		// Assert that the method is still runnable.
		runMapped(OverlapCaller.class, "run");
	}

	private String runMapped(Class<?> cls, String methodName) {
		String className = cls.getName();
		ClassDefiner definer = newDefinerFromWorkspace();
		try {
			Class<?> runner = definer.findClass(className);
			Method main = runner.getDeclaredMethod(methodName);
			try {
				return (String) main.invoke(null);
			} catch (ReflectiveOperationException ex) {
				fail("Failed to execute '" + methodName + "' method", ex);
			}
		} catch (ClassNotFoundException | NoSuchMethodException ex) {
			fail("Class '" + className + "' or '" + methodName + "' method missing", ex);
		}
		throw new IllegalStateException();
	}

	private ClassDefiner newDefinerFromWorkspace() {
		Map<String, byte[]> map = resource.getJvmClassBundle().values().stream()
				.collect(Collectors.toMap(e -> e.getName().replace('/', '.'), JvmClassInfo::getBytecode));
		return new ClassDefiner(map);
	}
}