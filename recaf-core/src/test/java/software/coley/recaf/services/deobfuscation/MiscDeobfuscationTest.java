package software.coley.recaf.services.deobfuscation;

import org.junit.jupiter.api.Test;
import software.coley.recaf.services.deobfuscation.transform.generic.EnumNameRestorationTransformer;
import software.coley.recaf.util.StringUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for various deobfuscation transformers that don't fit in other test classes.
 */
class MiscDeobfuscationTest extends BaseDeobfuscationTest {
	@Test
	void enumNameRestoration() {
		String asm = """
				.super java/lang/Enum
				.class public final enum Example {
				    .field public static final enum a LExample;
				    .field public static final enum b LExample;
				    .field public static final enum c LExample;
				    .field public static final enum d LExample;
				    .field private static final x [LExample;
									
				    .method static <clinit> ()V {
				        code: {
				        A:
				            new Example
				            dup
				            ldc "STATIC"
				            iconst_0
				            invokespecial Example.<init> (Ljava/lang/String;I)V
				            putstatic Example.a LExample;
				        B:
				            new Example
				            dup
				            ldc "WORLDGEN"
				            iconst_1
				            invokespecial Example.<init> (Ljava/lang/String;I)V
				            putstatic Example.b LExample;
				        C:
				            new Example
				            dup
				            ldc "DIMENSIONS"
				            iconst_2
				            invokespecial Example.<init> (Ljava/lang/String;I)V
				            putstatic Example.c LExample;
				        D:
				            new Example
				            dup
				            ldc "RELOADABLE"
				            iconst_3
				            invokespecial Example.<init> (Ljava/lang/String;I)V
				            putstatic Example.d LExample;
				        E:
				            invokestatic Example.$values ()[LExample;
				            putstatic Example.x [LExample;
				        F:
				            return
				        G:
				        }
				    }
				}
				""";
		validateMappingAfterAssembly(asm, List.of(EnumNameRestorationTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("enum STATIC LExample;", dis), "Missing enum const mapping");
			assertEquals(1, StringUtil.count("enum WORLDGEN LExample;", dis), "Missing enum const mapping");
			assertEquals(1, StringUtil.count("enum DIMENSIONS LExample;", dis), "Missing enum const mapping");
			assertEquals(1, StringUtil.count("enum RELOADABLE LExample;", dis), "Missing enum const mapping");
			assertEquals(1, StringUtil.count("final $values [LExample;", dis), "Missing enum $values array mapping");
		});
	}
}