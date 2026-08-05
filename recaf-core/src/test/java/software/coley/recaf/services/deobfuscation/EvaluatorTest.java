package software.coley.recaf.services.deobfuscation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.eval.EvaluationFailureResult;
import software.coley.recaf.util.analysis.eval.EvaluationResult;
import software.coley.recaf.util.analysis.eval.EvaluationThrowsResult;
import software.coley.recaf.util.analysis.eval.EvaluationYieldResult;
import software.coley.recaf.util.analysis.eval.Evaluator;
import software.coley.recaf.util.analysis.eval.FieldCacheManager;
import software.coley.recaf.util.analysis.lookup.InvokeVirtualLookup;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.util.analysis.value.ThrowableValue;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

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
	void testFrameInitializationForLocalThis() {
		String compiled = compile("""
				String instanceStr() { return "instance"; }
				Example receiver() { return this; }
				String run() { return receiver().instanceStr(); }
				""");
		ObjectValue instance = ObjectValue.VAL_OBJECT;
		List<ReValue> arguments = List.of(IntValue.of(4), LongValue.of(5), IntValue.of(6));

		// Validate a method that returns 'this' is the same instance we pass to the evaluator as the class instance.
		ReValue receiver = evaluate(compiled, "receiver", "()L" + CLASS_NAME + ";", instance, List.of());
		assertSame(instance, receiver);

		// Validate that calling an instance method without a class instance fails.
		EvaluationResult failure = evaluateResult(compiled, "receiver", "()L" + CLASS_NAME + ";", null, List.of());
		if (failure instanceof EvaluationFailureResult result)
			assertEquals("Instance method requires a class instance", result.reason());
		else
			fail("Expected instance evaluation failure, got: " + failure);

		// Validate that the instance method can be called and returns the expected value.
		ReValue runResult = evaluate(compiled, "run", "()Ljava/lang/String;", instance, List.of());
		if (runResult instanceof StringValue str)
			assertEquals("instance", str.getText().orElse(null));
		else
			fail("Evaluation failure, unexpected return value: " + runResult);
	}

	@Test
	void testFrameInitializationForWideParams() {
		String compiled = compile("""
				static int staticWide(int first, long wide, int last) { return first + last; }
				int instanceWide(int first, long wide, int last) { return first + last; }
				""");
		ObjectValue instance = ObjectValue.VAL_OBJECT;
		List<ReValue> arguments = List.of(IntValue.of(4), LongValue.of(5), IntValue.of(6));

		// Validate that the wide arguments are properly handled and the correct result is returned.
		assertEquals(10, ((IntValue) evaluate(compiled, "staticWide", "(IJI)I", null, arguments)).value().orElseThrow());
		assertEquals(10, ((IntValue) evaluate(compiled, "instanceWide", "(IJI)I", instance, arguments)).value().orElseThrow());
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
				static String helloWorld() {
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

	@Test
	void testUnknownBranchBails() {
		String compiled = compile("""
				static int unary(int input) {
				    if (input == 0) return 1;
				    return 2;
				}
				
				static int binary(int left, int right) {
				    if (left == right) return 1;
				    return 2;
				}
				
				static int nullCheck(String input) {
				    if (input == null) return 1;
				    return 2;
				}
				
				static int referenceCheck(Object left, Object right) {
				    if (left == right) return 1;
				    return 2;
				}
				
				static int switchCheck(int input) {
				    switch (input) {
				        case 1: return 1;
				        case 2: return 2;
				        default: return 3;
				    }
				}
				""");

		// We currently don't support evaluating branches with unknown values,
		// so we should get an evaluation failure for each of these.
		//
		// At some later point we may be able to split into multiple
		// branches and see if we collapse into a shared result.
		assertUnknownBranchFailure(evaluateResult(compiled, "unary", "(I)I", null, List.of(IntValue.UNKNOWN)));
		assertUnknownBranchFailure(evaluateResult(compiled, "binary", "(II)I", null,
				List.of(IntValue.UNKNOWN, IntValue.of(1))));
		assertUnknownBranchFailure(evaluateResult(compiled, "nullCheck", "(Ljava/lang/String;)I", null,
				List.of(StringValue.VAL_STRING_MAYBE_NULL)));
		assertUnknownBranchFailure(evaluateResult(compiled, "referenceCheck", "(Ljava/lang/Object;Ljava/lang/Object;)I", null,
				List.of(ObjectValue.object(Type.getObjectType("unknown/Left"), Nullness.UNKNOWN),
						ObjectValue.object(Type.getObjectType("unknown/Right"), Nullness.UNKNOWN))));
		assertUnknownBranchFailure(evaluateResult(compiled, "switchCheck", "(I)I", null, List.of(IntValue.UNKNOWN)));
	}

	@Test
	void testWorkspaceObjectState() {
		// Create a workspace class that has a field and methods to read and write it.
		compileFull("Holder", """
				public class Holder {
				    private int value;
				    public Holder() {}
				    public Holder(int value) { this.value = value; }
				    public int read() { return value; }
				    public int add(int amount) { value += amount; return value; }
				    public int addThenRead(int amount) { add(amount); return read(); }
				}
				""");

		// Validate that we can create an instance of the workspace class, call methods on it, and read the field value.
		String compiled = compile("""
				static int run() { return new Holder(7).addThenRead(5); }
				static int zero() { return new Holder().read(); }
				""");
		assertEquals(12, ((IntValue) evaluate(compiled, "run", "()I", null, List.of(), get("Holder"))).value().orElseThrow());
		assertEquals(0, ((IntValue) evaluate(compiled, "zero", "()I", null, List.of(), get("Holder"))).value().orElseThrow());
	}

	@Test
	void testWorkspaceObjectAliasing() {
		// Create a workspace class that has a field and methods to read and write it.
		compileFull("Holder", """
				public class Holder {
				    private int value;
				    public Holder(int value) { this.value = value; }
				    public int read() { return value; }
				    public void add(int amount) { value += amount; }
				}
				""");

		// Validate that we can create an instance of the workspace class, alias it,
		// and see that changes to one reference are reflected in the other.
		// The unique instances of 'Holder' should not affect each other.
		String compiled = compile("""
				static int run() {
				    Holder first = new Holder(1);
				
				    Holder alias = first;
				    alias.add(4);
				
				    Holder other = new Holder(1);
				    other.add(2);
				
				    return first.read() * 10 + other.read();
				}
				""");

		assertEquals(53, ((IntValue) evaluate(compiled, "run", "()I", null, List.of(), get("Holder"))).value().orElseThrow());
	}

	@Test
	void testWorkspaceVirtualAndInterfaceDispatch() {
		// Create workspace classes in a hierarchy.
		compileFull("Base", """
				class Base {
				    public int value() { return 1; }
				}
				interface Op {
				    int value();
				}
				class Child extends Base implements Op {
				    @Override public int value() { return 2; }
				    int parentValue() { return super.value(); }
				}
				""");

		// Validate invokevirtual and invokeinterface dispatch works as expected, and that super calls work as expected.
		String compiled = compile("""
				static int run() {
				    Base base = new Child();
				    Op op = new Child();
				    return base.value() * 10 + op.value();
				}
				static int superValue() { return new Child().parentValue(); }
				""");

		assertEquals(22, ((IntValue) evaluate(compiled, "run", "()I", null, List.of(), get("Base"), get("Child"), get("Op"))).value().orElseThrow());
		assertEquals(1, ((IntValue) evaluate(compiled, "superValue", "()I", null, List.of(), get("Base"), get("Child"), get("Op"))).value().orElseThrow());
	}

	@Test
	void testWorkspaceInstanceOfKnownTypes() {
		// Compile simple Base + Child hierarchy.
		compileBaseChildHierarchy();

		// Validate that instanceof works as expected for known types,
		// and that null is not an instance of any type.
		String compiled = compile("""
				static int run() {
				    return (new Child() instanceof Base ? 1 : 0)
				            + (null instanceof Child ? 10 : 0);
				}
				""");

		assertEquals(1, ((IntValue) evaluate(compiled, "run", "()I", null, List.of(),
				get("Base"), get("Child"))).value().orElseThrow());
	}

	@Test
	void testWorkspaceSuccessfulCastPreservesIdentity() {
		// Compile simple Base + Child hierarchy.
		compileBaseChildHierarchy();

		// Validate that casting an object to a known type preserves identity.
		String compiled = compile("""
				static int run() {
				    Object object = new Child();
				    Child cast = (Child) object;
				    return object == cast ? 1 : 0;
				}
				""");

		assertEquals(1, ((IntValue) evaluate(compiled, "run", "()I", null, List.of(),
				get("Base"), get("Child"))).value().orElseThrow());
	}

	@Test
	void testWorkspaceInvalidCastIsCaught() {
		// Compile simple Base + Child hierarchy.
		compileBaseChildHierarchy();

		// Validate that casting an object to an incompatible type throws a ClassCastException.
		String compiled = compile("""
				static int run() {
				    try {
				        return ((Child) new Base()).value();
				    } catch (ClassCastException ex) {
				        return 1;
				    }
				}
				""");

		assertEquals(1, ((IntValue) evaluate(compiled, "run", "()I", null, List.of(),
				get("Base"), get("Child"))).value().orElseThrow());
	}

	@Test
	void testWorkspaceDistinctAllocationsHaveDistinctIdentity() {
		// Compile simple Base + Child hierarchy.
		compileBaseChildHierarchy();

		// Validate two distinct allocations of the same type are not equal.
		String compiled = compile("""
				static int run() { return new Child() == new Child() ? 1 : 0; }
				""");

		assertEquals(0, ((IntValue) evaluate(compiled, "run", "()I", null, List.of(),
				get("Base"), get("Child"))).value().orElseThrow());
	}

	private void compileBaseChildHierarchy() {
		compileFull("Base", """
				class Base {
				    int value() { return 1; }
				}
				class Child extends Base {
				    @Override int value() { return 2; }
				}
				""");
	}

	private void assertUnknownBranchFailure(@Nonnull EvaluationResult result) {
		if (result instanceof EvaluationFailureResult failure)
			assertEquals("Encountered unknown value while evaluating branch", failure.reason());
		else
			fail("Expected unknown-branch evaluation failure, got: " + result);
	}

	@Nonnull
	private ReValue evaluate(@Nonnull String src, @Nonnull String name, @Nonnull String desc,
	                         @Nullable ObjectValue classInstance, @Nonnull List<ReValue> parameters,
	                         @Nonnull JvmClassInfo... additionalClasses) {
		EvaluationResult result = evaluateResult(src, name, desc, classInstance, parameters, additionalClasses);
		switch (result) {
			case EvaluationYieldResult(ReValue value) -> {
				return value;
			}
			case EvaluationFailureResult failure -> fail("Evaluation failed for " + name, failure.cause());
			case EvaluationThrowsResult(ReValue exception) ->
					fail("Evaluation yielded a thrown exception: " + exception);
			default -> {}
		}

		// Won't reach here due to calls to 'fail()' above, but the compiler doesn't know that.
		throw new IllegalStateException();
	}

	@Nonnull
	private EvaluationResult evaluateResult(@Nonnull String src, @Nonnull String name, @Nonnull String desc,
	                                        @Nullable ObjectValue classInstance, @Nonnull List<ReValue> parameters,
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

	@Nonnull
	private ReValue evaluateWithVirtualLookup(@Nonnull String src, @Nonnull String name, @Nonnull String desc,
	                                          @Nonnull InvokeVirtualLookup lookup) {
		JvmClassInfo assembled = assemble(src, src.contains(".class"));
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(assembled));
		JvmTransformerContext ctx = new JvmTransformerContext(workspace, workspace.getPrimaryResource(), Collections.emptyList());
		ReInterpreter interpreter = ctx.newInterpreter(new InheritanceGraph(workspace));
		interpreter.setInvokeVirtualLookup(lookup);
		EvaluationResult result = new Evaluator(workspace, interpreter, new FieldCacheManager(), 1000, false)
				.evaluate(CLASS_NAME, name, desc, null, List.of());
		if (result instanceof EvaluationYieldResult yielded)
			return yielded.value();
		fail("Lookup evaluation failed: " + result);
		throw new IllegalStateException();
	}

}
