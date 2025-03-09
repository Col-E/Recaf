package software.coley.recaf.services.deobfuscation;

import org.junit.jupiter.api.Test;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalAnnotationRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalSignatureRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalVarargsRemovingTransformer;

import java.util.List;

/**
 * Tests {@link IllegalVarargsRemovingTransformer} / {@link IllegalSignatureRemovingTransformer} / {@link IllegalAnnotationRemovingTransformer}.
 */
public class IllegalAttributeDeobfuscationTest extends BaseDeobfuscationTest {
	@Test
	void illegalSignatureRemoving() {
		// An int primitive is an invalid argument for a field signature (Valhalla when Brian?).
		// Most other invalid signatures aren't visible via decompilation so this suffices to show the transformer works.
		// Most of it delegates to code that is tested elsewhere.
		String asm = """
				.signature "Ljava/util/List<I>;"
				.field private static foo Ljava/lang/List;
				""";
		validateBeforeAfterDecompile(asm, List.of(IllegalSignatureRemovingTransformer.class), "List<int> foo", "List foo");
	}

	@Test
	void illegalVarargsRemoving() {
		// CFR actually checks for illegal varargs use and emits a nice warning for us.
		// So we'll just check if that goes away.
		String asm = """
				.method public static varargs example ([I[II)V {
				    code: {
				    A:
				        return
				    B:
				    }
				}
				""";
		validateBeforeAfterDecompile(asm, List.of(IllegalVarargsRemovingTransformer.class), "/* corrupt varargs signature?! */", null);
	}
}
