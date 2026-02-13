package software.coley.recaf.services.deobfuscation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.eval.EvaluationFailureResult;
import software.coley.recaf.util.analysis.eval.EvaluationResult;
import software.coley.recaf.util.analysis.eval.EvaluationThrowsResult;
import software.coley.recaf.util.analysis.eval.EvaluationYieldResult;
import software.coley.recaf.util.analysis.eval.Evaluator;
import software.coley.recaf.util.analysis.eval.FieldCacheManager;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class EvaluatorTest extends BaseDeobfuscationTest {
	@Test
	void testSimpleCharArrayToString() {
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
	void testXorString() {
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
			fail("Evaluation failure, unexpected return value: " + retVal);
	}

	@Test
	void testStringBuilder() {
		String compiled = compile("""
				static String make() { return new StringBuilder().append('T').append("est").toString(); }
				static String makeTwo() { return make().repeat(2); }
				""");
		ReValue retVal = evaluate(compiled, "makeTwo", "()Ljava/lang/String;", null, List.of());
		if (retVal instanceof StringValue str)
			assertEquals("TestTest", str.getText().orElse(null));
		else
			fail("Evaluation failure, unexpected return value: " + retVal);
	}

	@Test
	void testRandom() {
		String compiled = compile("""
				static int notSoRandom() { return new Random(1234).nextInt(1000); }
				""", Random.class);
		ReValue retVal = evaluate(compiled, "notSoRandom", "()I", null, List.of());
		if (retVal instanceof IntValue str)
			assertEquals(new Random(1234).nextInt(1000), str.value().orElseThrow());
		else
			fail("Evaluation failure, unexpected return value: " + retVal);
	}

	@Test
	void testArrayList() {
		String compiled = compile("""
				String helloWorld() {
				    List<String> strings = new ArrayList<>();
				    strings.add("World");
				    strings.add(0, "Hello");
				    CharSequence[] arr = new CharSequence[strings.size()];
				    for (int i = 0; i < strings.size(); i++) arr[i] = strings.get(i);
				    return String.join(" ", arr);
				}
				""", List.class, ArrayList.class);
		ReValue retVal = evaluate(compiled, "helloWorld", "()Ljava/lang/String;", null, List.of());
		if (retVal instanceof StringValue str)
			assertEquals("Hello World", str.getText().orElseThrow());
		else
			fail("Evaluation failure, unexpected return value: " + retVal);
	}

	@Nonnull
	private ReValue evaluate(@Nonnull String src, @Nonnull String name, @Nonnull String desc,
	                         @Nullable ReValue classInstance, @Nonnull List<ReValue> parameters) {
		JvmClassInfo assembled = assemble(src, src.contains(".class"));
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(assembled));
		JvmTransformerContext ctx = new JvmTransformerContext(workspace, workspace.getPrimaryResource(), Collections.emptyList());
		ReInterpreter interpreter = ctx.newInterpreter(new InheritanceGraph(workspace));
		EvaluationResult result = new Evaluator(workspace, interpreter, new FieldCacheManager(), 1000, false)
				.evaluate(CLASS_NAME, name, desc, classInstance, parameters);
		switch (result) {
			case EvaluationYieldResult(ReValue value) -> {
				return value;
			}
			case EvaluationFailureResult fail -> fail("Evaluation failed", fail.cause());
			case EvaluationThrowsResult(ReValue exception) ->
					fail("Evaluation yielded a thrown exception: " + exception);
			default -> {}
		}

		// Won't reach here due to calls to 'fail()' above, but the compiler doesn't know that.
		throw new IllegalStateException();
	}
}
