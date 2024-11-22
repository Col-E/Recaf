package software.coley.recaf.services.decompile;


import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.coley.recaf.services.decompile.fallback.FallbackDecompiler;
import software.coley.recaf.services.decompile.fallback.print.ClassPrinter;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.AccessibleFields;
import software.coley.recaf.test.dummy.ClassWithAnnotation;
import software.coley.recaf.test.dummy.ClassWithExceptions;
import software.coley.recaf.test.dummy.ClassWithStaticInit;
import software.coley.recaf.test.dummy.DummyEmptyMap;
import software.coley.recaf.test.dummy.DummyEnum;
import software.coley.recaf.test.dummy.InvisAnnotationImpl;
import software.coley.recaf.util.ClasspathUtil;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FallbackDecompiler}
 */
class FallbackDecompilerTest {
	static TextFormatConfig textConfig = new TextFormatConfig();

	public static Stream<String> penis() {
		return ClasspathUtil.getSystemClassSet().stream();
	}

	@Test
	void fieldModifiers() {
		String decompile = decompile(AccessibleFields.class);
		assertTrue(decompile.contains("public static final int CONSTANT_FIELD = 16;"));
		assertTrue(decompile.contains("private final int privateFinalField = 8;"));
		assertTrue(decompile.contains("protected final int protectedField = 4;"));
		assertTrue(decompile.contains("public final int publicField = 2;"));
		assertTrue(decompile.contains("final int packageField = 1;"));
	}

	@Test
	void classAnnotation() {
		String decompile = decompile(ClassWithAnnotation.class);
		assertTrue(decompile.contains("@AnnotationImpl(value = \"Hello\", policy = @Retention(RetentionPolicy.CLASS))"));
	}

	@Test
	void annotationClass() {
		String decompile = decompile(InvisAnnotationImpl.class);
		assertTrue(decompile.contains("""
				@Retention(RetentionPolicy.CLASS)
				@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
				public @interface InvisAnnotationImpl
				"""));
	}

	@Test
	void throwsException() {
		String decompile = decompile(ClassWithExceptions.class);
		assertTrue(decompile.contains("static int readInt(Object input) throws NumberFormatException"));
	}

	@Test
	void clinit() {
		String decompile = decompile(ClassWithStaticInit.class);
		assertTrue(decompile.contains("\n    static {\n"));
	}

	@Test
	void enumFields() {
		String decompile = decompile(DummyEnum.class);
		assertTrue(decompile.contains(" ONE,"));
		assertTrue(decompile.contains(" TWO,"));
		assertTrue(decompile.contains(" THREE;"));
		assertTrue(decompile.contains("private static final /* synthetic */ DummyEnum[] $VALUES;"));
	}


	@Test
	@Disabled("Need to implement signature parsing in the fallback decompiler")
	void genericClassArgs() {
		String decompile = decompile(DummyEmptyMap.class);
		assertTrue(decompile.contains("class DummyEmptyMap<K, V> implements Map<K, V> {"));
	}

	@Nonnull
	private static String decompile(@Nonnull Class<?> cls) {
		return assertDoesNotThrow(() -> {
			BasicJvmClassBundle bundle = TestClassUtils.fromClasses(cls);
			return new ClassPrinter(textConfig, bundle.get(cls.getName().replace('.', '/'))).print();
		});
	}
}