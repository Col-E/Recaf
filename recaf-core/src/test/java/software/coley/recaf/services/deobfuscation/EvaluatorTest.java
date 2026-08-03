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
import software.coley.recaf.util.analysis.value.ThrowableValue;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link Evaluator}.
 */
public class EvaluatorTest extends TransformerTestBase {
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

	@Test
	void testStackTrace() {
		String compiled = compile("""
				static String prior() {
				     StackTraceElement ste = new RuntimeException().getStackTrace()[1];
				     return ste.getClassName() + ":" + ste.getMethodName();
				}
				static String foo() {
				     return prior();
				}
				""");
		ReValue retVal = evaluate(compiled, "foo", "()Ljava/lang/String;", null, List.of());
		if (retVal instanceof StringValue str)
			assertEquals(CLASS_NAME + ":" + "foo", str.getText().orElse(null));
		else
			fail("Evaluation failure, unexpected return value: " + retVal);
	}

	@Test
	void testExplicitThrowCaughtBySubtype() {
		String compiled = compile("""
				static String caught() {
				    try { throw new IllegalArgumentException("bad"); }
				    catch (RuntimeException ex) { return "caught"; }
				}
				""");
		ReValue retVal = evaluate(compiled, "caught", "()Ljava/lang/String;", null, List.of());
		if (retVal instanceof StringValue str)
			assertEquals("caught", str.getText().orElse(null));
		else
			fail("Evaluation failure, unexpected return value: " + retVal);
	}

	@Test
	void testNestedThrowPropagatesToCallerHandler() {
		String compiled = compile("""
				static void inner() { throw new IllegalStateException(); }
				static String outer() {
				    try { inner(); return "bad"; }
				    catch (Exception ex) { return "caught"; }
				}
				""");
		if (evaluate(compiled, "outer", "()Ljava/lang/String;", null, List.of()) instanceof StringValue str)
			assertEquals("caught", str.getText().orElse(null));
		else
			fail("Evaluation failure, unexpected return value");
	}

	@Test
	void testUncaughtThrowProducesThrowableResult() {
		String compiled = compile("""
				static String fail() { throw new IllegalStateException(); }
				""");
		EvaluationResult result = evaluateResult(compiled, "fail", "()Ljava/lang/String;", null, List.of());
		if (result instanceof EvaluationThrowsResult(ReValue exception)
				&& exception instanceof ThrowableValue throwable) {
			assertEquals("java/lang/IllegalStateException", throwable.type().getInternalName());

			StackTraceElement ste = throwable.getStackTrace().getFirst();
			assertEquals(CLASS_NAME, ste.getClassName());
			assertEquals("fail", ste.getMethodName());
		} else
			fail("Expected thrown result, got: " + result);
	}

	@Test
	void testWorkspaceThrowableSubtypeAndArbitraryConstructor() {
		// Define a custom exception class in the workspace.
		compileFull("CustomException", """
				public class CustomException extends Exception {
				    public CustomException(int code) { super(); }
				}
				""");

		// Using it should still retain throwable handling.
		String compiled = compile("""
				static String caught() {
				    try { throw new CustomException(7); }
				    catch (Exception ex) { return "custom"; }
				}
				""");
		EvaluationResult result = evaluateResult(compiled, "caught", "()Ljava/lang/String;", null, List.of(), get("CustomException"));
		if (result instanceof EvaluationYieldResult(ReValue value) && value instanceof StringValue str)
			assertEquals("custom", str.getText().orElse(null));
		else
			fail("Evaluation failed: " + result);
	}

	@Test
	void testKnownImplicitFaultsAreCaught() {
		// Non-explicit exceptions caused by things like division by zero, null dereference,
		// and array access out of bounds should be caught by the evaluator.
		String compiled = compile("""
				static String arithmetic() {
				    try { int zero = 0; return String.valueOf(1 / zero); }
				    catch (ArithmeticException ex) { return "arith"; }
				}
				static String nullReceiver() {
				    try { String value = null; return value.length() + ""; }
				    catch (NullPointerException ex) { return "null"; }
				}
				static String array() {
				    try { int[] values = new int[1]; return String.valueOf(values[2]); }
				    catch (ArrayIndexOutOfBoundsException ex) { return "array"; }
				}
				static String negativeArray() {
				    try { int size = -1; int[] values = new int[size]; return "bad"; }
				    catch (NegativeArraySizeException ex) { return "negative"; }
				}
				""");
		assertEquals("arith", ((StringValue) evaluate(compiled, "arithmetic", "()Ljava/lang/String;", null, List.of())).getText().orElse(null));
		assertEquals("null", ((StringValue) evaluate(compiled, "nullReceiver", "()Ljava/lang/String;", null, List.of())).getText().orElse(null));
		assertEquals("array", ((StringValue) evaluate(compiled, "array", "()Ljava/lang/String;", null, List.of())).getText().orElse(null));
		assertEquals("negative", ((StringValue) evaluate(compiled, "negativeArray", "()Ljava/lang/String;", null, List.of())).getText().orElse(null));
	}

	@Nonnull
	private ReValue evaluate(@Nonnull String src, @Nonnull String name, @Nonnull String desc,
	                         @Nullable ReValue classInstance, @Nonnull List<ReValue> parameters) {
		EvaluationResult result = evaluateResult(src, name, desc, classInstance, parameters);
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

	@Nonnull
	private EvaluationResult evaluateResult(@Nonnull String src, @Nonnull String name, @Nonnull String desc,
	                                        @Nullable ReValue classInstance, @Nonnull List<ReValue> parameters,
	                                        @Nonnull JvmClassInfo... additionalClasses) {
		JvmClassInfo assembled = assemble(src, src.contains(".class"));
		JvmClassInfo[] classes = new JvmClassInfo[additionalClasses.length + 1];
		classes[0] = assembled;
		System.arraycopy(additionalClasses, 0, classes, 1, additionalClasses.length);
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(classes));
		JvmTransformerContext ctx = new JvmTransformerContext(workspace, workspace.getPrimaryResource(), Collections.emptyList());
		ReInterpreter interpreter = ctx.newInterpreter(new InheritanceGraph(workspace));
		return new Evaluator(workspace, interpreter, new FieldCacheManager(), 1000, false)
				.evaluate(CLASS_NAME, name, desc, classInstance, parameters);
	}
}
