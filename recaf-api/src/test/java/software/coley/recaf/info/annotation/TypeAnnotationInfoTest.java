package software.coley.recaf.info.annotation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.TypeReference;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.ClassWithAnnotation;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TypeAnnotationInfo}
 */
class TypeAnnotationInfoTest {
	static JvmClassInfo classWithAnnotation;
	static TypeAnnotationInfo typeAnnoOnTypeArgument;

	@BeforeAll
	static void setup() throws IOException {
		classWithAnnotation = TestClassUtils.fromRuntimeClass(ClassWithAnnotation.class);
		// Has only one type-anno on <T> arg.
		typeAnnoOnTypeArgument = classWithAnnotation.getTypeAnnotations().get(0);
	}

	@Test
	void getTypeRef() {
		assertEquals(TypeReference.CLASS_TYPE_PARAMETER, typeAnnoOnTypeArgument.getTypeRef());
	}

	@Test
	void getTypePath() {
		assertNull(typeAnnoOnTypeArgument.getTypePath());
	}
}