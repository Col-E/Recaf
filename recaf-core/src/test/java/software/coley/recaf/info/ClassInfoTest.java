package software.coley.recaf.info;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ClassInfo}
 */
class ClassInfoTest {
	static JvmClassInfo arrayList;
	static JvmClassInfo accessibleFields;
	static JvmClassInfo multipleInterfacesClass;
	static JvmClassInfo annotationImpl;
	static JvmClassInfo classWithInner;
	static JvmClassInfo classWithInner$Inner;
	static JvmClassInfo classWithAnonymousInner;
	static JvmClassInfo classWithAnonymousInner$Inner;
	static JvmClassInfo classWithEmbeddedInners;
	static JvmClassInfo classWithEmbeddedInners$A;
	static JvmClassInfo classWithEmbeddedInners$B;
	static JvmClassInfo classWithEmbeddedInners$C;
	static JvmClassInfo classWithEmbeddedInners$D;
	static JvmClassInfo classWithEmbeddedInners$E;

	@BeforeAll
	static void setup() throws IOException {
		arrayList = TestClassUtils.fromRuntimeClass(ArrayList.class);
		accessibleFields = TestClassUtils.fromRuntimeClass(AccessibleFields.class);
		multipleInterfacesClass = TestClassUtils.fromRuntimeClass(MultipleInterfacesClass.class);
		annotationImpl = TestClassUtils.fromRuntimeClass(AnnotationImpl.class);
		classWithInner = TestClassUtils.fromRuntimeClass(ClassWithInner.class);
		classWithInner$Inner = TestClassUtils.fromRuntimeClass(ClassWithInner.TheInner.class);
		classWithAnonymousInner = TestClassUtils.fromRuntimeClass(ClassWithAnonymousInner.class);
		classWithAnonymousInner$Inner
				= new JvmClassInfoBuilder(new ClassReader(classWithAnonymousInner.getName() + "$1")).build();
		classWithEmbeddedInners = TestClassUtils.fromRuntimeClass(ClassWithEmbeddedInners.class);
		classWithEmbeddedInners$A = TestClassUtils.fromRuntimeClass(ClassWithEmbeddedInners.A.class);
		classWithEmbeddedInners$B = TestClassUtils.fromRuntimeClass(ClassWithEmbeddedInners.A.B.class);
		classWithEmbeddedInners$C = TestClassUtils.fromRuntimeClass(ClassWithEmbeddedInners.A.B.C.class);
		classWithEmbeddedInners$D = TestClassUtils.fromRuntimeClass(ClassWithEmbeddedInners.A.B.C.D.class);
		classWithEmbeddedInners$E = TestClassUtils.fromRuntimeClass(ClassWithEmbeddedInners.A.B.C.D.E.class);
	}

	@Test
	void getSourceFileName() {
		// Root class
		assertEquals("AccessibleFields.java", accessibleFields.getSourceFileName(),
				"Source file name missing, or incorrect");
		assertEquals("ClassWithInner.java", classWithInner.getSourceFileName(),
				"Source file name missing, or incorrect");

		// Inner classes should have their outer class's source name
		assertEquals(classWithInner.getSourceFileName(), classWithInner$Inner.getSourceFileName(),
				"Inner source file should match outer class's");
	}

	@Test
	void getInterfaces() {
		assertEquals(0, accessibleFields.getInterfaces().size(), "Class should have 0 interfaces");

		// Implicit annotation
		assertEquals(1, annotationImpl.getInterfaces().size(), "Annotation should implement annotation type");
		assertEquals("java/lang/annotation/Annotation", annotationImpl.getInterfaces().get(0),
				"Expected annotation interface");

		// This one has 2 interfaces
		List<String> interfaces = multipleInterfacesClass.getInterfaces();
		assertEquals(2, interfaces.size(), "Class should have 2 interfaces");
		assertEquals("java/lang/AutoCloseable", interfaces.get(0));
		assertEquals("java/util/Comparator", interfaces.get(1));
	}

	@Test
	void getSuperName() {
		assertEquals("java/lang/Object", accessibleFields.getSuperName(), "Class should extend Object");
		assertEquals("java/lang/Object", multipleInterfacesClass.getSuperName(), "Class should extend Object");

		// Edge case, while ours does, the JVM technically allows you to not define one for annotations.
		assertEquals("java/lang/Object", annotationImpl.getSuperName(), "Annotation should extend Object");
	}

	@Test
	void getPackageName() {
		String packageName = AccessibleFields.class.getPackageName().replace('.', '/');
		assertEquals(packageName, accessibleFields.getPackageName());

		// Should work for inner classes too
		assertEquals(packageName, classWithEmbeddedInners$E.getPackageName());
	}

	@Test
	void parentTypesStream() {
		// For basic class
		Set<String> fieldsParents = accessibleFields.parentTypesStream().collect(Collectors.toSet());
		assertEquals(Set.of("java/lang/Object"), fieldsParents);

		// Multiple interface class has object, plus the two interfaces it implements
		// (of which do not have any further parents)
		Set<String> interfaceParents = multipleInterfacesClass.parentTypesStream().collect(Collectors.toSet());
		assertEquals(Set.of("java/lang/Object", "java/lang/AutoCloseable", "java/util/Comparator"), interfaceParents);
	}

	@Test
	void getSignature() {
		assertNull(accessibleFields.getSignature(), "Should have no signature, no generics defined");

		// Multiple interfaces implements Comparable<String>
		assertEquals("Ljava/lang/Object;Ljava/lang/AutoCloseable;Ljava/util/Comparator<Ljava/lang/String;>;",
				multipleInterfacesClass.getSignature(),
				"Usage of generic interfaces should yield a signature");

		// ArrayList obviously has <T> pattern, but as with the above,
		// you get a lot of additional information from signatures.
		assertEquals("<E:Ljava/lang/Object;>Ljava/util/AbstractList<TE;>;Ljava/util/List<TE;>;" +
						"Ljava/util/RandomAccess;Ljava/lang/Cloneable;Ljava/io/Serializable;",
				arrayList.getSignature(),
				"ArrayList should have generic for <T> pattern");
	}

	@Test
	void getOuterClassName() {
		assertNull(accessibleFields.getOuterClassName(), "Should have no outer class");

		// Test inner-outer relation (normal)
		assertNull(classWithInner.getOuterClassName(), "Should have no outer class");
		assertEquals(classWithInner.getName(), classWithInner$Inner.getOuterClassName(),
				"Inner has wrong outer class name");

		// Test inner-outer relation (anonymous)
		assertNull(classWithAnonymousInner.getOuterClassName(), "Should have no outer class");
		assertEquals(classWithAnonymousInner.getName(), classWithAnonymousInner$Inner.getOuterClassName(),
				"Inner has wrong outer class name");

		// Test chain
		assertEquals(classWithEmbeddedInners.getName(), classWithEmbeddedInners$A.getOuterClassName(), "Expected Root -> A");
		assertEquals(classWithEmbeddedInners$A.getName(), classWithEmbeddedInners$B.getOuterClassName(), "Expected A -> B");
		assertEquals(classWithEmbeddedInners$B.getName(), classWithEmbeddedInners$C.getOuterClassName(), "Expected B -> C");
		assertEquals(classWithEmbeddedInners$C.getName(), classWithEmbeddedInners$D.getOuterClassName(), "Expected C -> D");
		assertEquals(classWithEmbeddedInners$D.getName(), classWithEmbeddedInners$E.getOuterClassName(), "Expected D -> E");
	}

	@Test
	void getOuterMethodName() {
		// Classes without outer methods
		assertNull(accessibleFields.getOuterMethodName(), "Should have no outer method");
		assertNull(classWithInner.getOuterMethodName(), "Should have no outer method");
		assertNull(classWithAnonymousInner.getOuterMethodName(), "Should have no outer method");
		assertNull(classWithEmbeddedInners$A.getOuterMethodName(), "Should have no outer method");
		assertNull(classWithEmbeddedInners$E.getOuterMethodName(), "Should have no outer method");

		// Classes with outer class, not method
		assertNull(classWithInner$Inner.getOuterMethodName(), "Should have no outer method");

		// Classes with outer method
		assertEquals("foo", classWithAnonymousInner$Inner.getOuterMethodName(),
				"Should have an outer method of 'foo'");
	}

	@Test
	void getOuterMethodDescriptor() {
		// Classes without outer methods
		assertNull(accessibleFields.getOuterMethodDescriptor(), "Should have no outer method");
		assertNull(classWithInner.getOuterMethodDescriptor(), "Should have no outer method");
		assertNull(classWithAnonymousInner.getOuterMethodDescriptor(), "Should have no outer method");
		assertNull(classWithEmbeddedInners$A.getOuterMethodName(), "Should have no outer method");
		assertNull(classWithEmbeddedInners$E.getOuterMethodName(), "Should have no outer method");

		// Classes with outer class, not method
		assertNull(classWithInner$Inner.getOuterMethodDescriptor(), "Should have no outer method");

		// Classes with outer method
		assertEquals("()V", classWithAnonymousInner$Inner.getOuterMethodDescriptor(),
				"Should have an outer method of 'void()'");
	}

	@Test
	void getOuterClassBreadcrumbs() {
		// No outer
		assertEquals(Collections.emptyList(), accessibleFields.getOuterClassBreadcrumbs(),
				"Should have no outer class, so no breadcrumbs");

		// Class outer
		assertEquals(Collections.singletonList(classWithInner.getName()), classWithInner$Inner.getOuterClassBreadcrumbs(),
				"Should have an outer class");

		// Class outer, inner is anonymous
		assertEquals(Collections.singletonList(classWithAnonymousInner.getName()), classWithAnonymousInner$Inner.getOuterClassBreadcrumbs(),
				"Should have an outer class");

		// Chained
		assertEquals(Collections.singletonList(classWithEmbeddedInners.getName()), classWithEmbeddedInners$A.getOuterClassBreadcrumbs(),
				"Expected chain [Root]");
		assertEquals(Arrays.asList(
						classWithEmbeddedInners.getName(),
						classWithEmbeddedInners$A.getName()
				), classWithEmbeddedInners$B.getOuterClassBreadcrumbs(),
				"Expected chain [Root, A]");
		assertEquals(Arrays.asList(
						classWithEmbeddedInners.getName(),
						classWithEmbeddedInners$A.getName(),
						classWithEmbeddedInners$B.getName()
				), classWithEmbeddedInners$C.getOuterClassBreadcrumbs(),
				"Expected chain [Root, A,B]");
		assertEquals(Arrays.asList(
						classWithEmbeddedInners.getName(),
						classWithEmbeddedInners$A.getName(),
						classWithEmbeddedInners$B.getName(),
						classWithEmbeddedInners$C.getName()
				), classWithEmbeddedInners$D.getOuterClassBreadcrumbs(),
				"Expected chain [Root, A,B,C]");
		assertEquals(Arrays.asList(
						classWithEmbeddedInners.getName(),
						classWithEmbeddedInners$A.getName(),
						classWithEmbeddedInners$B.getName(),
						classWithEmbeddedInners$C.getName(),
						classWithEmbeddedInners$D.getName()
				), classWithEmbeddedInners$E.getOuterClassBreadcrumbs(),
				"Expected chain [Root, A,B,C,D]");
	}

	@Test
	void getInnerClasses() {
		assertEquals(0, accessibleFields.getInnerClasses().size(), "Expected no inners");

		// Normal inner
		assertEquals(1, classWithInner.getInnerClasses().size(), "Expected 1 inner");
		assertEquals(1, classWithInner$Inner.getInnerClasses().size(), "Expected 1 inner, of self");

		// Anonymous inner
		assertEquals(1, classWithAnonymousInner.getInnerClasses().size(), "Expected 1 inner");
		assertEquals(1, classWithAnonymousInner$Inner.getInnerClasses().size(), "Expected 1 inner, of self");
	}

	@Test
	void isAnonymousInner() {
		assertFalse(classWithAnonymousInner.isAnonymousInnerClass());
		assertTrue(classWithAnonymousInner$Inner.isAnonymousInnerClass());
	}

	@Test
	void getFields() {
		// Normal case
		assertEquals(5, accessibleFields.getFields().size());

		// Non-static inner should have a reference to outer as synthetic field
		// Not anymore! :V
		// assertEquals(1, classWithInner$Inner.getFields().size());

		// Despite how the format looks, they're not fields
		assertEquals(0, annotationImpl.getFields().size());
	}

	@Test
	void getMethods() {
		// Single method 'foo()' plus the default constructor
		assertEquals(2, classWithAnonymousInner.getMethods().size());

		// No constructor, just the two annotation property methods.
		assertEquals(2, annotationImpl.getMethods().size());
	}

	@Test
	void fieldStream() {
		// Mirror results of prior tests
		assertEquals(5, accessibleFields.fieldStream().count());
		// See comment in getFields.
		// assertEquals(1, classWithInner$Inner.fieldStream().count());
		assertEquals(0, annotationImpl.fieldStream().count());
	}

	@Test
	void methodStream() {
		// Mirror results of prior tests
		assertEquals(2, classWithAnonymousInner.methodStream().count());
		assertEquals(2, annotationImpl.methodStream().count());
	}

	@Test
	void testClass() {
		assertTrue(accessibleFields.testClass(ClassInfo::isJvmClass));
		assertFalse(accessibleFields.testClass(ClassInfo::isAndroidClass));
	}

	@Test
	void mapClass() {
		assertDoesNotThrow(() -> {
			InnerClassInfo inner = classWithInner.mapClass(c -> c.getInnerClasses().get(0));
			assertNotNull(inner);
		});
	}

	@Test
	void asFile() {
		assertThrows(Exception.class, () -> accessibleFields.asFile());
	}

	@Test
	void isClass() {
		assertTrue(accessibleFields.isClass());
	}

	@Test
	void isFile() {
		assertFalse(accessibleFields.isFile());
	}

	@Test
	void isJvmClass() {
		assertTrue(accessibleFields.isJvmClass());
	}

	@Test
	void isAndroidClass() {
		assertFalse(accessibleFields.isAndroidClass());
	}
}