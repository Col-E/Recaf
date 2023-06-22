package software.coley.recaf.info.annotation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.AnnotationImpl;
import software.coley.recaf.test.dummy.ClassWithAnnotation;
import software.coley.recaf.test.dummy.ClassWithInvisAnnotation;
import software.coley.recaf.test.dummy.InvisAnnotationImpl;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AnnotationInfo}
 */
class AnnotationInfoTest {
	static JvmClassInfo classWithAnnotation;
	static JvmClassInfo classWithInvisAnnotation;

	@BeforeAll
	static void setup() throws IOException {
		classWithAnnotation = TestClassUtils.fromRuntimeClass(ClassWithAnnotation.class);
		classWithInvisAnnotation = TestClassUtils.fromRuntimeClass(ClassWithInvisAnnotation.class);
	}

	@Test
	void isVisible() {
		assertTrue(classWithAnnotation.getAnnotations().get(0).isVisible());
		assertFalse(classWithInvisAnnotation.getAnnotations().get(0).isVisible());
	}

	@Test
	void getDescriptor() {
		assertEquals(Type.getDescriptor(AnnotationImpl.class),
				classWithAnnotation.getAnnotations().get(0).getDescriptor());
		assertEquals(Type.getDescriptor(InvisAnnotationImpl.class),
				classWithInvisAnnotation.getAnnotations().get(0).getDescriptor());
	}

	@Test
	void getElements() {
		// Empty
		assertEquals(Collections.emptyMap(),
				classWithInvisAnnotation.getAnnotations().get(0).getElements());

		// Some values
		Map<String, AnnotationElement> elements = classWithAnnotation.getAnnotations().get(0).getElements();
		assertEquals(2, elements.size());

		// Basic string
		AnnotationElement valueElement = elements.get("value");
		assertEquals("value", valueElement.getElementName());
		assertEquals("Hello", valueElement.getElementValue());

		// Another annotation
		AnnotationElement policyElement = elements.get("policy");
		assertEquals("policy", policyElement.getElementName());
		Object policyValue = policyElement.getElementValue();
		if (policyValue instanceof AnnotationInfo policyInfo) {
			assertEquals(1, policyInfo.getElements().size());
		} else {
			fail("Annotation element of another annotation should yield embedded 'AnnotationInfo'");
		}
	}
}