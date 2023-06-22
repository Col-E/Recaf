package software.coley.recaf.info.annotation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.AnnotationImpl;
import software.coley.recaf.test.dummy.ClassWithAnnotation;
import software.coley.recaf.test.dummy.TypeAnnotationImpl;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Annotated}
 */
class AnnotatedTest {
	static JvmClassInfo classWithAnnotation;

	@BeforeAll
	static void setup() throws IOException {
		classWithAnnotation = TestClassUtils.fromRuntimeClass(ClassWithAnnotation.class);
	}

	@Test
	void getAnnotations() {
		List<AnnotationInfo> annotations = classWithAnnotation.getAnnotations();
		assertEquals(1, annotations.size(), "Should declare one standard annotation");
		assertEquals(Type.getDescriptor(AnnotationImpl.class), annotations.get(0).getDescriptor());
	}

	@Test
	void getTypeAnnotations() {
		List<TypeAnnotationInfo> typeAnnotations = classWithAnnotation.getTypeAnnotations();
		assertEquals(1, typeAnnotations.size(), "Should declare one type annotation");
		assertEquals(Type.getDescriptor(TypeAnnotationImpl.class), typeAnnotations.get(0).getDescriptor());
	}

	@Test
	void annotationStream() {
		// Mirror results of prior test
		assertEquals(1, classWithAnnotation.annotationStream().count());
	}

	@Test
	void typeAnnotationStream() {
		// Mirror results of prior test
		assertEquals(1, classWithAnnotation.typeAnnotationStream().count());
	}

	@Test
	void allAnnotationsStream() {
		// Class has one standard annotation, one type annotation
		assertEquals(2, classWithAnnotation.allAnnotationsStream().count());
	}
}