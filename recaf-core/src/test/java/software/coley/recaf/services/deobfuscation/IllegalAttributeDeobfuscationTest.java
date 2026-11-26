package software.coley.recaf.services.deobfuscation;

import org.junit.jupiter.api.Test;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalAnnotationRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalSignatureRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalVarargsRemovingTransformer;
import software.coley.recaf.util.Types;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests {@link IllegalVarargsRemovingTransformer} / {@link IllegalSignatureRemovingTransformer} / {@link IllegalAnnotationRemovingTransformer}.
 */
public class IllegalAttributeDeobfuscationTest extends TransformerTestBase {
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
	void illegalSignatureRemovingExcessivelyNestedClassSignature() {
		// Obfuscators abuse the lack of limits in the signature grammar to emit class signatures that nest type arguments
		// thousands of levels deep, which is enough to crash decompilers. Even one level past our limit is not real code.
		String signature = "Ljava/util/List<".repeat(Types.MAX_SIGNATURE_NESTING + 1) + "Ljava/lang/String;" + ">;".repeat(Types.MAX_SIGNATURE_NESTING + 1);
		String asm = """
				.signature "%s"
				.super java/lang/Object
				.class public super Example {
				}
				""".formatted(signature);
		validateAfterAssembly(asm, List.of(IllegalSignatureRemovingTransformer.class),
				disassembly -> assertFalse(disassembly.contains(".signature"), "Class signature should be removed"));
	}

	@Test
	void illegalSignatureRemovingExcessivelyWideClassSignature() {
		// The same goes for class signatures that declare more type parameters than anything real would.
		StringBuilder signature = new StringBuilder("<");
		for (int i = 0; i <= Types.MAX_SIGNATURE_TYPE_PARAMETERS; i++)
			signature.append('T').append(i).append(":Ljava/lang/Object;");
		signature.append(">Ljava/lang/Object;");
		String asm = """
				.signature "%s"
				.super java/lang/Object
				.class public super Example {
				}
				""".formatted(signature);
		validateAfterAssembly(asm, List.of(IllegalSignatureRemovingTransformer.class),
				disassembly -> assertFalse(disassembly.contains(".signature"), "Class signature should be removed"));
	}

	@Test
	void illegalSignatureRemovingKeepsComplexButRealisticClassSignature() {
		// Generic signatures that are heavy, but still within what real code does, must be left alone.
		String asm = """
				.signature "<K:Ljava/lang/Object;V::Ljava/lang/Comparable<-TV;>;>Ljava/util/AbstractMap<TK;Ljava/util/List<Ljava/util/Map<TK;[TV;>;>;>;Ljava/util/Map<TK;Ljava/util/List<TV;>;>;"
				.super java/util/AbstractMap
				.implements java/util/Map
				.class public abstract super Example {
				}
				""";
		validateNoTransformation(asm, List.of(IllegalSignatureRemovingTransformer.class));
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
