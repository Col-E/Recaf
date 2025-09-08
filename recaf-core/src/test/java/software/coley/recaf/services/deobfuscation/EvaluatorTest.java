package software.coley.recaf.services.deobfuscation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.util.analysis.ReEvaluationException;
import software.coley.recaf.util.analysis.ReEvaluator;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class EvaluatorTest extends BaseDeobfuscationTest {
	@Test
	void testSimpleCharArrayToString() throws ReEvaluationException {
		String src = """
				.method static decrypt (I)Ljava/lang/String; {
				    parameters: { length },
				    code: {
				    A:
				        iload length
				        newarray char
				        astore chars
				    B:
				        iconst_0
				        istore i
				    C:
				        iload i
				        iload length
				        if_icmpge F
				    D:
				        line 7
				        aload chars
				        iload i
				        bipush 97 // 'a'
				        iload i
				        iadd
				        i2c
				        castore
				    E:
				        iinc i 1
				        goto C
				    F:
				        aload chars
				        invokestatic java/lang/String.valueOf ([C)Ljava/lang/String;
				        areturn
				    G:
				    }
				}
				""";
		ReValue retVal = evaluate(src, "decrypt", "(I)Ljava/lang/String;", null,
				List.of(IntValue.of(26)));
		if (retVal instanceof StringValue str)
			assertEquals("abcdefghijklmnopqrstuvwxyz", str.getText().orElse(null));
		else
			fail("Evaluation failure");
	}

	@Test
	void testXorString() throws ReEvaluationException {
		String src = """
				.method static decrypt (Ljava/lang/String;I)Ljava/lang/String; {
				    parameters: { input, xor },
				    code: {
				    A:
				        aload input
				        invokevirtual java/lang/String.length ()I
				        istore length
				    B:
				        iload length
				        newarray char
				        astore chars
				    C:
				        iconst_0
				        istore i
				    D:
				        iload i
				        iload length
				        if_icmpge G
				    E:
				        aload chars
				        iload i
				        aload input
				        iload i
				        invokevirtual java/lang/String.charAt (I)C
				        iload xor
				        ixor
				        i2c
				        castore
				    F:
				        iinc i 1
				        goto D
				    G:
				        aload chars
				        invokestatic java/lang/String.valueOf ([C)Ljava/lang/String;
				        areturn
				    H:
				    }
				}
				""";
		ReValue retVal = evaluate(src, "decrypt", "(Ljava/lang/String;I)Ljava/lang/String;", null,
				List.of(ObjectValue.string("㘯㘂㘋㘋㘈㙇㘐㘈㘕㘋㘃"), IntValue.of(0b11011001100111)));
		if (retVal instanceof StringValue str)
			assertEquals("Hello world", str.getText().orElse(null));
		else
			fail("Evaluation failure");
	}

	@Nonnull
	private ReValue evaluate(@Nonnull String src, @Nonnull String name, @Nonnull String desc,
	                         @Nullable ReValue classInstance, @Nonnull List<ReValue> parameters) throws ReEvaluationException {
		JvmClassInfo assembled = assemble(src, false);
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(assembled));
		JvmTransformerContext ctx = new JvmTransformerContext(workspace, workspace.getPrimaryResource(), Collections.emptyList());
		ReInterpreter interpreter = ctx.newInterpreter(new InheritanceGraph(workspace));
		return new ReEvaluator(workspace, interpreter, 1000).evaluate(CLASS_NAME, name, desc, classInstance, parameters);
	}
}
