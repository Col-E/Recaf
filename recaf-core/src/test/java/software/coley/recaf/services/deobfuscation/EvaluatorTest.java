package software.coley.recaf.services.deobfuscation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.printer.JvmPrinterUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.util.ClassMethodPair;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.ReFrame;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.eval.EvaluationFailureResult;
import software.coley.recaf.util.analysis.eval.EvaluationListener;
import software.coley.recaf.util.analysis.eval.EvaluationResult;
import software.coley.recaf.util.analysis.eval.EvaluationThrowsResult;
import software.coley.recaf.util.analysis.eval.EvaluationYieldResult;
import software.coley.recaf.util.analysis.eval.Evaluator;
import software.coley.recaf.util.analysis.eval.FieldCacheManager;
import software.coley.recaf.util.analysis.eval.InstancedObjectValue;
import software.coley.recaf.util.analysis.lookup.InvokeVirtualLookup;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.util.analysis.value.ThrowableValue;
import software.coley.recaf.workspace.model.Workspace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
		assertStringValue("abcdefghijklmnopqrstuvwxyz", retVal);
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
		assertStringValue("Hello world", retVal);
	}

	@Test
	void testSystemArraycopy() {
		String compiled = compile("""
				static String copy() {
				    int[] source = {1, 2, 3, 4};
				    int[] destination = {9, 9, 9, 9};
				    System.arraycopy(source, 1, destination, 0, 3);
				    return destination[0] + ":" + destination[1] + ":" + destination[2] + ":" + destination[3];
				}
				static String overlap() {
				    int[] values = {1, 2, 3, 4, 5};
				    System.arraycopy(values, 0, values, 1, 4);
				    return values[0] + ":" + values[1] + ":" + values[2] + ":" + values[3] + ":" + values[4];
				}
				static int nullSource() {
				    int[] source = null;
				    int[] destination = new int[1];
				    System.arraycopy(source, 0, destination, 0, 1);
				    return 0;
				}
				static int outOfBounds() {
				    int[] source = {1};
				    int[] destination = new int[1];
				    System.arraycopy(source, 1, destination, 0, 1);
				    return 0;
				}
				static int primitiveMismatch() {
				    int[] source = {1};
				    long[] destination = {0};
				    System.arraycopy(source, 0, destination, 0, 1);
				    return 0;
				}
				static int referenceMismatch() {
				    Object[] source = {Integer.valueOf(1)};
				    String[] destination = new String[1];
				    System.arraycopy(source, 0, destination, 0, 1);
				    return 0;
				}
				static int caught() {
				    try {
				        int[] source = {1};
				        int[] destination = new int[1];
				        System.arraycopy(source, 1, destination, 0, 1);
				    } catch (ArrayIndexOutOfBoundsException ex) {
				        return 7;
				    }
				    return 0;
				}
				""");

		// Validate ordinary and overlapping copies through the evaluator only.
		assertStringValue("2:3:4:9", evaluate(compiled, "copy", "()Ljava/lang/String;", null, List.of()));
		assertStringValue("1:1:2:3:4", evaluate(compiled, "overlap", "()Ljava/lang/String;", null, List.of()));

		// Validate deterministic JVM faults are routed through the evaluator's exception model.
		assertEvaluationThrows(compiled, "nullSource", "()I", "java/lang/NullPointerException");
		assertEvaluationThrows(compiled, "outOfBounds", "()I", "java/lang/ArrayIndexOutOfBoundsException");
		assertEvaluationThrows(compiled, "primitiveMismatch", "()I", "java/lang/ArrayStoreException");
		assertEvaluationThrows(compiled, "referenceMismatch", "()I", "java/lang/ArrayStoreException");
		assertIntValue(7, evaluate(compiled, "caught", "()I", null, List.of()));
	}

	@Test
	void testStringBuilder() {
		String compiled = compile("""
				static String make() { return new StringBuilder().append('T').append("est").toString(); }
				static String makeTwo() { return make().repeat(2); }
				""");
		ReValue retVal = evaluate(compiled, "makeTwo", "()Ljava/lang/String;", null, List.of());
		assertStringValue("TestTest", retVal);
	}

	@Test
	void testStringMakeConcatWithConstants() {
		// Basic concat uses 'makeConcatWithConstants'
		String compiled = compile("""
				static String concat(String left, String right) { return left + " " + right; }
				""");
		assertStringValue("Hello World", evaluate(compiled, "concat",
				"(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", null,
				List.of(ObjectValue.string("Hello"), ObjectValue.string("World"))));
	}

	@Test
	void testStringConcatWithoutConstants() {
		// Cannot figure out a java source form for 'makeConcat' so we have to assemble it manually.
		String assembly = """
				.super java/lang/Object
				.class public super Example {
					.method static concat (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; {
					    parameters: { left, right },
					    code: {
					    A:
					        aload left
					        aload right
					        invokedynamic makeConcat (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String; { invokestatic, java/lang/invoke/StringConcatFactory.makeConcat, (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; } {}
					        areturn
					    B:
					    }
					}
				}
				""";
		assertStringValue("HelloWorld", evaluate(assembly, "concat",
				"(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", null,
				List.of(ObjectValue.string("Hello"), ObjectValue.string("World"))));
	}

	@Test
	void testEvaluationCallback() {
		// Compile a simple method that calls a helper method and multiplies the result.
		compile("""
				static int helper(int value) { return value * 3; }
				static int run() { return helper(2) * 4; }
				""");
		Evaluator evaluator = createEvaluator();

		// Sets for tracking which methods and classes were observed during evaluation (ordered, de-duplicating)
		Set<String> observedMethods = new LinkedHashSet<>();
		Set<String> observedClasses = new LinkedHashSet<>();
		Map<String, Integer> postImulValues = new HashMap<>();

		// Add a listener to the evaluator that tracks which methods and classes are observed, and captures the result of the IMUL instruction.
		EvaluationListener listener = (classNode, methodNode, instruction, frame) -> {
			// Class/method should be non-null given we are evaluating a method, not a block.
			assertNotNull(classNode);
			assertNotNull(methodNode);
			observedClasses.add(classNode.name);
			observedMethods.add(methodNode.name);

			// Capture the result of the IMUL instruction for later validation.
			if (instruction.getOpcode() == Opcodes.IMUL) {
				IntValue value = assertInstanceOf(IntValue.class, frame.getStackTop().orElseThrow());
				postImulValues.put(methodNode.name, value.value().orElseThrow());
			}
		};
		evaluator.addListener(listener);

		// Eval the method and validate the result, observed classes/methods, and the post-IMUL values.
		EvaluationResult result = evaluator.evaluate(CLASS_NAME, "run", "()I", null, List.of());
		EvaluationYieldResult yielded = assertInstanceOf(EvaluationYieldResult.class, result);
		assertIntValue(24, yielded.value());
		assertEquals(Set.of(CLASS_NAME), observedClasses);
		assertEquals(Set.of("helper", "run"), observedMethods);
		assertEquals(6, postImulValues.get("helper"));
		assertEquals(24, postImulValues.get("run"));
	}

	@Test
	void testEvaluationCallbackEnterExit() {
		// Compile a method that re-enters the same method until the base case returns.
		compile("""
				static int countdown(int value) {
				    if (value == 0) return 1;
				    return countdown(value - 1) + 1;
				}
				""");
		Evaluator evaluator = createEvaluator();
		List<List<String>> entries = new ArrayList<>();
		List<List<String>> returns = new ArrayList<>();
		EvaluationListener listener = new EvaluationListener() {
			@Override
			public void onInstruction(@Nullable ClassNode classNode,
			                          @Nullable MethodNode methodNode,
			                          @Nonnull AbstractInsnNode instruction,
			                          @Nonnull ReFrame frame) {
				// no-op
			}

			@Override
			public void onMethodEnter(@Nonnull ClassNode classNode, @Nonnull MethodNode methodNode,
			                          @Nonnull ReFrame frame, @Nonnull List<ClassMethodPair> stack) {
				entries.add(stackMethodNames(stack));
			}

			@Override
			public void onMethodReturn(@Nonnull ClassNode classNode, @Nonnull MethodNode methodNode,
			                           @Nonnull ReFrame frame, @Nonnull ReValue value,
			                           @Nonnull List<ClassMethodPair> stack) {
				returns.add(stackMethodNames(stack));
			}
		};
		evaluator.addListener(listener);

		// Evaluate enough recursive calls to prove repeated method invocations can be observed through the listener.
		EvaluationResult result = evaluator.evaluate(CLASS_NAME, "countdown", "(I)I", null, List.of(IntValue.of(3)));
		EvaluationYieldResult yielded = assertInstanceOf(EvaluationYieldResult.class, result);
		assertIntValue(4, yielded.value());
		assertEquals(List.of(
				List.of("countdown"),
				List.of("countdown", "countdown"),
				List.of("countdown", "countdown", "countdown"),
				List.of("countdown", "countdown", "countdown", "countdown")), entries);
		assertEquals(List.of(
				List.of("countdown", "countdown", "countdown", "countdown"),
				List.of("countdown", "countdown", "countdown"),
				List.of("countdown", "countdown"),
				List.of("countdown")), returns);
	}

	@Test
	void testEvaluationCallbackThrowing() {
		// Compile a method that re-enters the same method until the base case throws an exception.
		compile("""
				static int countdown(int value) {
				    if (value == 0) throw new RuntimeException("The end");
				    return countdown(value - 1) + 1;
				}
				""");
		Evaluator evaluator = createEvaluator();
		List<String> thrown = new ArrayList<>();
		EvaluationListener listener = new EvaluationListener() {
			private final Map<MethodNode, AbstractInsnNode> lastInstruction = new IdentityHashMap<>();

			@Override
			public void onInstruction(@Nullable ClassNode classNode,
			                          @Nullable MethodNode methodNode,
			                          @Nonnull AbstractInsnNode instruction,
			                          @Nonnull ReFrame frame) {
				lastInstruction.put(methodNode, instruction);
			}

			@Override
			public void onMethodThrow(@Nonnull ClassNode classNode,
			                          @Nonnull MethodNode methodNode,
			                          @Nonnull ReFrame frame,
			                          @Nonnull ReValue exception,
			                          @Nonnull List<ClassMethodPair> stack) {
				AbstractInsnNode throwingInsn = lastInstruction.get(methodNode).getNext();
				thrown.add(String.join(":", stackMethodNames(stack)) + ":" + JvmPrinterUtil.toString(throwingInsn) + ":" + exception);
			}
		};
		evaluator.addListener(listener);

		// Evaluate the method, and validate we observe throwing through the listener.
		// - The exception propagates up the call stack, so we should see the throw event for each method invocation.
		EvaluationResult result = evaluator.evaluate(CLASS_NAME, "countdown", "(I)I", null, List.of(IntValue.of(3)));
		if (result instanceof EvaluationThrowsResult(ReValue exception)) {
			assertEquals("java/lang/RuntimeException", exception.type().getInternalName());
			assertEquals(List.of(
					"countdown:countdown:countdown:countdown:athrow:java/lang/RuntimeException",
					"countdown:countdown:countdown:invokestatic Example.countdown (I)I:java/lang/RuntimeException",
					"countdown:countdown:invokestatic Example.countdown (I)I:java/lang/RuntimeException",
					"countdown:invokestatic Example.countdown (I)I:java/lang/RuntimeException"
			), thrown);
		} else {
			fail("Expected evaluation to throw RuntimeException, got: " + result);
		}
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
		assertStringValue("instance", runResult);
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
		assertIntValue(10, evaluate(compiled, "staticWide", "(IJI)I", null, arguments));
		assertIntValue(10, evaluate(compiled, "instanceWide", "(IJI)I", instance, arguments));
	}

	@Test
	void testRandom() {
		String compiled = compile("""
				static int notSoRandom() { return new Random(1234).nextInt(1000); }
				""", Random.class);
		ReValue retVal = evaluate(compiled, "notSoRandom", "()I", null, List.of());
		assertIntValue(new Random(1234).nextInt(1000), retVal);
	}

	@Test
	void testSupplierLambda() {
		String compiled = compile("""
				static String supply() { return ((Supplier<String>) () -> "Hello").get(); }
				""", Supplier.class);
		ReValue retVal = evaluate(compiled, "supply", "()Ljava/lang/String;", null, List.of());
		assertStringValue("Hello", retVal);
	}

	@Test
	void testFunctionLambda() {
		String compiled = compile("""
				static String apply() { return ((Function<String, String>) (s -> s + " World")).apply("Hello"); }
				""", Function.class);
		ReValue retVal = evaluate(compiled, "apply", "()Ljava/lang/String;", null, List.of());
		assertStringValue("Hello World", retVal);
	}

	@Test
	void testPredicateLambda() {
		String compiled = compile("""
				static boolean test() { return ((Predicate<String>) (s -> s.length() > 5)).test("Hello World"); }
				""", Predicate.class);
		ReValue retVal = evaluate(compiled, "test", "()Z", null, List.of());
		assertIntValue(1, retVal);
	}

	@Test
	void testSimulatedThreadJoinWithoutStart() {
		String compiled = compile("""
				static int childState() throws Exception {
				    int[] state = {0};
				    Thread child = new Thread(() -> state[0] = 1);
				    child.join(); // Join without start should not run the thread.
				    return state[0];
				}
				""");
		assertIntValue(0, evaluate(compiled, "childState", "()I", null, List.of()));
	}

	@Test
	void testSimulatedThreadSleep() {
		String compiled = compile("""
				static long sleptMillis() throws Exception {
				    long before = System.currentTimeMillis();
				    Thread.sleep(100);
				    return System.currentTimeMillis() - before;
				}
				""");
		assertLongValue(100, evaluate(compiled, "sleptMillis", "()J", null, List.of()));
	}

	@Test
	void testSimulatedThreadRefEquality() {
		String compiled = compile("""
				static int sameThread() { return Thread.currentThread() == Thread.currentThread() ? 1 : 0; }
				""");
		assertIntValue(1, evaluate(compiled, "sameThread", "()I", null, List.of()));
	}

	@Test
	void testSimulatedThreadIdentityAndMetadata() {
		String compiled = compile("""
				static int identity() {
				    Thread parent = Thread.currentThread();
				    int[] state = {0};
				    Thread child = new Thread(() -> {
				        state[0] = Thread.currentThread() != parent ? 1 : 0;
				        state[0] += Thread.currentThread().isAlive() ? 10 : 0;
				        state[0] += Thread.currentThread().getId() == 2 ? 100 : 0;
				        state[0] += Thread.currentThread().getName().equals("worker") ? 1000 : 0;
				    });
				    child.setName("worker");
				    child.start();
				    return state[0];
				}
				""");
		assertIntValue(1111, evaluate(compiled, "identity", "()I", null, List.of()));
	}

	@Test
	void testSimulatedThreadLifecycleOperations() {
		String compiled = compile("""
				static int lifecycle() {
				    Thread child = new Thread(() -> {});
				    int before = child.isAlive() ? 1 : 0;
				    child.interrupt();
				    int interrupted = child.isInterrupted() ? 1 : 0;
				    child.start();
				    int after = child.isAlive() ? 1 : 0;
				    return before + interrupted * 10 + after * 100;
				}
				""");
		assertIntValue(10, evaluate(compiled, "lifecycle", "()I", null, List.of()));
	}

	@Test
	void testSimulatedThreadDirectRun() {
		String compiled = compile("""
				static int directRun() {
				    int[] state = {0};
				    Thread child = new Thread(() -> state[0] = 1);
				    child.run();
				    return state[0] + (child.isAlive() ? 10 : 0);
				}
				""");
		assertIntValue(1, evaluate(compiled, "directRun", "()I", null, List.of()));
	}

	@Test
	void testSimulatedThreadSubclassRun() {
		compileFull("ChildThread", """
				class ChildThread extends Thread {
				    static int STATE;
				    @Override public void run() { STATE = 3; }
				}
				""");
		String compiled = compile("""
				static int run() {
				    ChildThread child = new ChildThread();
				    child.run();
				    return ChildThread.STATE;
				}
				""");
		assertIntValue(3, evaluate(compiled, "run", "()I", null, List.of(), get("ChildThread")));
	}

	@Test
	void testSimulatedNestedExecutionNotifiesListeners() {
		compile("""
				static int run() {
				    int[] state = {0};
				    new Thread(() -> state[0] = 1).start();
				    return state[0];
				}
				""");
		Evaluator evaluator = createEvaluator();
		Set<String> entered = new HashSet<>();
		evaluator.addListener(new EvaluationListener() {
			@Override
			public void onInstruction(@Nullable ClassNode classNode, @Nullable MethodNode methodNode,
			                          @Nonnull AbstractInsnNode instruction, @Nonnull ReFrame frame) {
				// Method-entry observations below are sufficient for this nested execution contract.
			}

			@Override
			public void onMethodEnter(@Nonnull ClassNode classNode, @Nonnull MethodNode methodNode,
			                          @Nonnull ReFrame frame, @Nonnull List<ClassMethodPair> stack) {
				entered.add(methodNode.name);
			}
		});
		EvaluationResult result = evaluator.evaluate(CLASS_NAME, "run", "()I", null, List.of());
		assertIntValue(1, assertInstanceOf(EvaluationYieldResult.class, result).value());
		assertTrue(entered.contains("run"));
		assertTrue(entered.stream().anyMatch(name -> name.startsWith("lambda$")));
	}

	@Test
	void testSimulatedThreadLifecycleAndCatchableRestart() {
		String compiled = compile("""
				static int lifecycle() {
				    Thread child = new Thread(() -> {});
				    child.start();
				    try {
				        child.start();
				        return 0;
				    } catch (IllegalThreadStateException ex) {
				        return 1;
				    }
				}
				""");
		assertIntValue(1, evaluate(compiled, "lifecycle", "()I", null, List.of()));
	}

	@Test
	void testSimulatedCompletableFutureCallbacks() {
		String compiled = compile("""
				static int apply() {
				    return CompletableFuture.completedFuture(1).thenApply(x -> x + 1).join();
				}
				static int asyncRun() {
				    int[] state = {0};
				    CompletableFuture.runAsync(() -> state[0] = 7).join();
				    return state[0];
				}
				static int acceptAndRun() {
				    int[] state = {0};
				    CompletableFuture.completedFuture(1).thenAccept(x -> state[0] = x).thenRun(() -> state[0]++);
				    return state[0];
				}
				static int exceptional() {
				    return CompletableFuture.<Integer>failedFuture(new IllegalStateException())
				            .exceptionally(ex -> 7).join();
				}
				static int handled() {
				    return CompletableFuture.<Integer>failedFuture(new IllegalStateException())
				            .handle((value, error) -> error == null ? 0 : 1).join();
				}
				static int deferred() {
				    CompletableFuture<Integer> source = new CompletableFuture<>();
				    CompletableFuture<Integer> next = source.thenApply(x -> x + 1);
				    source.complete(1);
				    return next.join();
				}
				static int composed() {
				    return CompletableFuture.<Integer>completedFuture(1)
				            .thenCompose((Integer x) -> CompletableFuture.<Integer>completedFuture(x + 1)).join();
				}
				""", CompletableFuture.class, Consumer.class, BiFunction.class, Function.class);
		assertIntValue(2, evaluate(compiled, "apply", "()I", null, List.of()));
		assertIntValue(7, evaluate(compiled, "asyncRun", "()I", null, List.of()));
		assertIntValue(2, evaluate(compiled, "acceptAndRun", "()I", null, List.of()));
		assertIntValue(7, evaluate(compiled, "exceptional", "()I", null, List.of()));
		assertIntValue(1, evaluate(compiled, "handled", "()I", null, List.of()));
		assertIntValue(2, evaluate(compiled, "deferred", "()I", null, List.of()));
		assertIntValue(2, evaluate(compiled, "composed", "()I", null, List.of()));
	}

	@Test
	void testSimulatedCompletableFutureCompletionAndIncompleteJoin() {
		String compiled = compile("""
				static int complete() {
				    CompletableFuture<Integer> future = new CompletableFuture<>();
				    return future.complete(4) ? future.join() : 0;
				}
				static int firstCompletion() {
				    CompletableFuture<Integer> future = new CompletableFuture<>();
				    return (future.complete(1) ? 1 : 0) + (future.complete(2) ? 10 : 0) + future.join();
				}
				static int exceptionalCompletion() {
				    CompletableFuture<Integer> future = new CompletableFuture<>();
				    return future.completeExceptionally(new IllegalArgumentException())
				            && future.isCompletedExceptionally() ? 1 : 0;
				}
				static int incomplete() { return new CompletableFuture<Integer>().join(); }
				""", CompletableFuture.class);
		assertIntValue(4, evaluate(compiled, "complete", "()I", null, List.of()));
		assertIntValue(2, evaluate(compiled, "firstCompletion", "()I", null, List.of()));
		assertIntValue(1, evaluate(compiled, "exceptionalCompletion", "()I", null, List.of()));
		EvaluationResult result = evaluateResult(compiled, "incomplete", "()I", null, List.of());
		EvaluationFailureResult failure = assertInstanceOf(EvaluationFailureResult.class, result);
		assertEquals("Simulated future cannot complete", failure.reason());
	}

	@Test
	void testBase64ScalarAndMime() {
		String compiled = compile("""
				static String basic() {
				    byte[] input = "Hello".getBytes();
				    String encoded = Base64.getEncoder().encodeToString(input);
				    return new String(Base64.getDecoder().decode(encoded));
				}
				static String scalar() {
				    byte[] input = "Hello".getBytes();
				    return new String(Base64.getDecoder().decode(Base64.getEncoder().encode(input)));
				}
				static String url() {
				    String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(">>>>".getBytes());
				    return encoded + ":" + new String(Base64.getUrlDecoder().decode(encoded));
				}
				static String mimeDefault() {
				    return Base64.getMimeEncoder().encodeToString("HelloWorld".getBytes());
				}
				static String mimeCustom() {
				    return Base64.getMimeEncoder(4, "!".getBytes()).encodeToString("HelloWorld".getBytes());
				}
				static String mimeDecode() {
				    String encoded = Base64.getMimeEncoder(4, "!".getBytes()).encodeToString("HelloWorld".getBytes());
				    return new String(Base64.getMimeDecoder().decode(encoded));
				}
				""", Base64.class);

		// Verify we can round-trip 'Hello'
		assertStringValue("Hello", evaluate(compiled, "basic", "()Ljava/lang/String;", null, List.of()));
		assertStringValue("Hello", evaluate(compiled, "scalar", "()Ljava/lang/String;", null, List.of()));

		// Verify URL-safe encoding and decoding of '>>>>' with no padding.
		assertStringValue("Pj4-Pg:>>>>", evaluate(compiled, "url", "()Ljava/lang/String;", null, List.of()));

		// Verify default MIME output, custom separators, and MIME decoding.
		assertStringValue("SGVsbG9Xb3JsZA==", evaluate(compiled, "mimeDefault", "()Ljava/lang/String;", null, List.of()));
		assertStringValue("SGVs!bG9X!b3Js!ZA==", evaluate(compiled, "mimeCustom", "()Ljava/lang/String;", null, List.of()));
		assertStringValue("HelloWorld", evaluate(compiled, "mimeDecode", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testBase64DestinationArrays() {
		// A number of Base64 methods have overloads that write to a destination array and return the number of bytes written.
		String compiled = compile("""
				static String destination() {
				    byte[] source = "Hello".getBytes();
				    byte[] encoded = new byte[8];
				    int encodedCount = Base64.getEncoder().encode(source, encoded);
				    byte[] decoded = new byte[5];
				    int decodedCount = Base64.getDecoder().decode(encoded, decoded);
				    return encodedCount + ":" + decodedCount + ":" + new String(decoded);
				}
				""", Base64.class);

		// Confirm both destination arrays are visible after host-side writes.
		assertStringValue("8:5:Hello", evaluate(compiled, "destination", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testBase64ByteBuffers() {
		String compiled = compile("""
				static String bufferEncode(ByteBuffer input) {
				    ByteBuffer encoded = Base64.getEncoder().encode(input);
				    return new String(encoded.array(), encoded.position(), encoded.remaining());
				}
				static String bufferDecode(ByteBuffer input) {
				    ByteBuffer decoded = Base64.getDecoder().decode(input);
				    return new String(decoded.array(), decoded.position(), decoded.remaining());
				}
				""", Base64.class, ByteBuffer.class);

		// Construct byte-buffers with known content and verify the Base64 encoder/decoder round-trip through the evaluator.
		InstancedObjectValue<ByteBuffer> input = new InstancedObjectValue<>(ByteBuffer.wrap("Hello".getBytes()));
		assertStringValue("SGVsbG8=", evaluate(compiled, "bufferEncode",
				"(Ljava/nio/ByteBuffer;)Ljava/lang/String;", null, List.of(input)));
		input = new InstancedObjectValue<>(ByteBuffer.wrap("SGVsbG8=".getBytes()));
		assertStringValue("Hello", evaluate(compiled, "bufferDecode",
				"(Ljava/nio/ByteBuffer;)Ljava/lang/String;", null, List.of(input)));
	}

	@Test
	void testBase64Streams() {
		// Encoding with byte-array streams
		String compiled = compile("""
				static String streamEncode(ByteArrayOutputStream output) throws Exception {
				    var encoded = Base64.getEncoder().wrap(output);
				    encoded.write('H');
				    encoded.write("el".getBytes(), 0, 2);
				    encoded.write("lo".getBytes());
				    encoded.flush();
				    encoded.close();
				    return new String(output.toByteArray());
				}
				static String streamEncodeCreated() throws Exception {
				    ByteArrayOutputStream output = new ByteArrayOutputStream(8);
				    var encoded = Base64.getEncoder().wrap(output);
				    encoded.write("Hello".getBytes());
				    encoded.close();
				    return new String(output.toByteArray());
				}
				static String streamDecode(ByteArrayInputStream input) throws Exception {
				    return new String(Base64.getDecoder().wrap(input).readAllBytes());
				}
				static String streamDecodeRead(ByteArrayInputStream input) throws Exception {
				    var decoded = Base64.getDecoder().wrap(input);
				    byte[] output = new byte[5];
				    int first = decoded.read(output, 0, 2);
				    int second = decoded.read(output, 2, 3);
				    return first + ":" + second + ":" + new String(output);
				}
				static String streamDecodeCreated() throws Exception {
				    ByteArrayInputStream input = new ByteArrayInputStream("SGVsbG8=".getBytes());
				    return new String(Base64.getDecoder().wrap(input).readAllBytes());
				}
				""", Base64.class, ByteArrayInputStream.class, ByteArrayOutputStream.class);

		// Verify writes through an externally supplied output stream and a stream created in the evaluator.
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		assertStringValue("SGVsbG8=", evaluate(compiled, "streamEncode",
				"(Ljava/io/ByteArrayOutputStream;)Ljava/lang/String;", null,
				List.of(new InstancedObjectValue<>(output))));
		assertStringValue("SGVsbG8=", evaluate(compiled, "streamEncodeCreated",
				"()Ljava/lang/String;", null, List.of()));

		// Verify complete reads and split reads through a wrapped input stream.
		ByteArrayInputStream input = new ByteArrayInputStream("SGVsbG8=".getBytes());
		assertStringValue("Hello", evaluate(compiled, "streamDecode",
				"(Ljava/io/ByteArrayInputStream;)Ljava/lang/String;", null,
				List.of(new InstancedObjectValue<>(input))));
		assertStringValue("2:3:Hello", evaluate(compiled, "streamDecodeRead",
				"(Ljava/io/ByteArrayInputStream;)Ljava/lang/String;", null,
				List.of(new InstancedObjectValue<>(new ByteArrayInputStream("SGVsbG8=".getBytes())))));
		assertStringValue("Hello", evaluate(compiled, "streamDecodeCreated",
				"()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testBase64InvalidInput() {
		// Bad base64 usage
		String compiled = compile("""
				static String invalid() {
				    return new String(Base64.getDecoder().decode("%%%"));
				}
				""", Base64.class);

		// We should get the expected illegal argument exception from the decoder, not a generic evaluation failure.
		EvaluationResult invalid = evaluateResult(compiled, "invalid", "()Ljava/lang/String;", null, List.of());
		EvaluationThrowsResult thrown = assertInstanceOf(EvaluationThrowsResult.class, invalid);
		ThrowableValue exception = assertInstanceOf(ThrowableValue.class, thrown.exception());
		assertEquals("java/lang/IllegalArgumentException", exception.type().getInternalName());
	}

	@Test
	void testByteBufferFactories() {
		// Compile each factory independently so capacity, range, and directness assertions stay local.
		String compiled = compile("""
				static ByteBuffer wrapped() { return ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }); }
				static ByteBuffer ranged() { return ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }, 1, 2); }
				static ByteBuffer allocated() { return ByteBuffer.allocate(4); }
				static ByteBuffer direct() { return ByteBuffer.allocateDirect(4); }
				""", ByteBuffer.class);

		// A plain wrap starts at zero and exposes the whole backing array.
		InstancedObjectValue<ByteBuffer> wrapped = assertInstanceOf(InstancedObjectValue.class,
				evaluate(compiled, "wrapped", "()Ljava/nio/ByteBuffer;", null, List.of()));
		assertEquals(4, wrapped.getRealInstance().capacity());
		assertEquals(0, wrapped.getRealInstance().position());
		assertEquals(4, wrapped.getRealInstance().limit());

		// A ranged wrap preserves the requested position and limit over the same capacity.
		InstancedObjectValue<ByteBuffer> ranged = assertInstanceOf(InstancedObjectValue.class,
				evaluate(compiled, "ranged", "()Ljava/nio/ByteBuffer;", null, List.of()));
		assertEquals(4, ranged.getRealInstance().capacity());
		assertEquals(1, ranged.getRealInstance().position());
		assertEquals(3, ranged.getRealInstance().limit());

		// Heap allocation and direct allocation must remain distinguishable host-backed results.
		InstancedObjectValue<ByteBuffer> allocated = assertInstanceOf(InstancedObjectValue.class,
				evaluate(compiled, "allocated", "()Ljava/nio/ByteBuffer;", null, List.of()));
		assertEquals(4, allocated.getRealInstance().capacity());
		assertFalse(allocated.getRealInstance().isDirect());
		InstancedObjectValue<ByteBuffer> direct = assertInstanceOf(InstancedObjectValue.class,
				evaluate(compiled, "direct", "()Ljava/nio/ByteBuffer;", null, List.of()));
		assertEquals(4, direct.getRealInstance().capacity());
		assertTrue(direct.getRealInstance().isDirect());
	}

	@Test
	void testByteBufferStateAndBacking() {
		// Compile state transitions and backing-array queries together because both inspect the same receiver contract.
		// TLDR: "Random bullshit, go!"
		String compiled = compile("""
				static String state(ByteBuffer buffer) {
				    buffer.position(2);
				    buffer.limit(6);
				    buffer.mark();
				    buffer.position(4);
				    buffer.reset();
				    int marked = buffer.position();
				    buffer.clear();
				    int cleared = buffer.position() + buffer.limit();
				    buffer.position(3);
				    buffer.flip();
				    int flippedPosition = buffer.position();
				    int flippedLimit = buffer.limit();
				    buffer.rewind();
				    int rewoundPosition = buffer.position();
				    buffer.position(1);
				    buffer.compact();
				    return buffer.capacity() + ":" + marked + ":" + cleared + ":" + flippedPosition + ":" +
				            flippedLimit + ":" + rewoundPosition + ":" + buffer.position() + ":" + buffer.limit() + ":" +
				            buffer.remaining() + ":" + buffer.hasRemaining();
				}
				static String backing(ByteBuffer buffer) {
				    return buffer.isDirect() + ":" + buffer.isReadOnly() + ":" + buffer.hasArray() + ":" +
				            buffer.arrayOffset() + ":" + buffer.array().length;
				}
				""", ByteBuffer.class);

		// Mark/reset, clear, flip, rewind, and compact must all update the host receiver in order.
		assertStringValue("8:2:8:0:3:0:2:8:6:true", evaluate(compiled, "state",
				"(Ljava/nio/ByteBuffer;)Ljava/lang/String;", null,
				List.of(new InstancedObjectValue<>(ByteBuffer.allocate(8)))));

		// Heap buffers expose their backing array while remaining non-direct and writable.
		assertStringValue("false:false:true:0:4", evaluate(compiled, "backing",
				"(Ljava/nio/ByteBuffer;)Ljava/lang/String;", null,
				List.of(new InstancedObjectValue<>(ByteBuffer.wrap(new byte[4])))));
	}

	@Test
	void testByteBufferPrimitiveAccess() {
		// Compile absolute writes and relative reads with fixed-width values at non-overlapping offsets.
		String compiled = compile("""
				static String primitives(ByteBuffer buffer, ByteOrder order) {
				    buffer.order(order);
				    buffer.put(0, (byte) 1);
				    buffer.putChar(1, 'A');
				    buffer.putShort(3, (short) 4660);
				    buffer.putInt(5, 16909060);
				    buffer.putLong(9, 72623859790382856L);
				    buffer.putFloat(17, 1.5f);
				    buffer.putDouble(21, 2.5d);
				    char relativeChar;
				    short relativeShort;
				    int relativeInt;
				    long relativeLong;
				    float relativeFloat;
				    double relativeDouble;
				    buffer.position(1);
				    relativeChar = buffer.getChar();
				    relativeShort = buffer.getShort();
				    relativeInt = buffer.getInt();
				    relativeLong = buffer.getLong();
				    relativeFloat = buffer.getFloat();
				    relativeDouble = buffer.getDouble();
				    return buffer.get(0) + ":" + buffer.getChar(1) + ":" + buffer.getShort(3) + ":" +
				            buffer.getInt(5) + ":" + buffer.getLong(9) + ":" + buffer.getFloat(17) + ":" +
				            buffer.getDouble(21) + ":" + relativeChar + ":" + relativeShort + ":" + relativeInt + ":" +
				            relativeLong + ":" + relativeFloat + ":" + relativeDouble;
				}
				""", ByteBuffer.class, ByteOrder.class);

		// Exact values prove both relative and absolute primitive access use the selected byte order.
		assertStringValue("1:A:4660:16909060:72623859790382856:1.5:2.5:A:4660:16909060:72623859790382856:1.5:2.5",
				evaluate(compiled, "primitives",
						"(Ljava/nio/ByteBuffer;Ljava/nio/ByteOrder;)Ljava/lang/String;", null,
						List.of(new InstancedObjectValue<>(ByteBuffer.allocate(32)),
								new InstancedObjectValue<>(ByteOrder.BIG_ENDIAN))));
	}

	@Test
	void testByteBufferFluentMutationsAndOrder() {
		// Compile fluent writes and order changes separately from the primitive value matrix.
		String compiled = compile("""
				static String fluent(ByteBuffer buffer) {
				    boolean same = buffer.put((byte) 7) == buffer;
				    return same + ":" + buffer.position();
				}
				static String order(ByteBuffer buffer, ByteOrder order) {
				    buffer.order(order);
				    return buffer.order().toString() + ":" + (buffer.put(0, (byte) 8) == buffer);
				}
				""", ByteBuffer.class, ByteOrder.class);

		// Fluent mutations must return the original evaluator-backed receiver and advance relative position.
		assertStringValue("true:1", evaluate(compiled, "fluent",
				"(Ljava/nio/ByteBuffer;)Ljava/lang/String;", null,
				List.of(new InstancedObjectValue<>(ByteBuffer.allocate(4)))));

		// order(ByteOrder) is also fluent, while order() exposes a host-backed ByteOrder result.
		assertStringValue("LITTLE_ENDIAN:true", evaluate(compiled, "order",
				"(Ljava/nio/ByteBuffer;Ljava/nio/ByteOrder;)Ljava/lang/String;", null,
				List.of(new InstancedObjectValue<>(ByteBuffer.allocate(4)),
						new InstancedObjectValue<>(ByteOrder.LITTLE_ENDIAN))));
	}

	@Test
	void testByteBufferBulkGets() {
		// Compile all four array-writing get() variants.
		String compiled = compile("""
				static String bulkGets(ByteBuffer input) {
				    byte[] relative = new byte[3];
				    input.position(0);
				    input.get(relative);
				    int relativePosition = input.position();
				    byte[] ranged = new byte[] { 99, 99, 99, 99 };
				    input.position(0);
				    input.get(ranged, 1, 2);
				    byte[] absolute = new byte[3];
				    input.get(1, absolute);
				    byte[] absoluteRanged = new byte[] { 99, 99, 99, 99 };
				    input.get(2, absoluteRanged, 1, 2);
				    return relative[0] + "," + relative[1] + "," + relative[2] + ":" +
				            ranged[0] + "," + ranged[1] + "," + ranged[2] + "," + ranged[3] + ":" +
				            absolute[0] + "," + absolute[1] + "," + absolute[2] + ":" +
				            absoluteRanged[0] + "," + absoluteRanged[1] + "," + absoluteRanged[2] + "," +
				            absoluteRanged[3] + ":" + relativePosition;
				}
				""", ByteBuffer.class);

		// Bulk get operations use ReFrame.replaceValue()
		// So we should see the host-backed array values updated and the relative position advanced as expected.
		assertStringValue("10,11,12:99,10,11,99:11,12,13:99,12,13,99:3",
				evaluate(compiled, "bulkGets",
						"(Ljava/nio/ByteBuffer;)Ljava/lang/String;", null,
						List.of(new InstancedObjectValue<>(ByteBuffer.wrap(new byte[]{10, 11, 12, 13})))));
	}

	@Test
	void testByteBufferBulkPuts() {
		String compiled = compile("""
				static String putArrays() {
					// Relative puts advance the source and target
				    ByteBuffer relative = ByteBuffer.allocate(6);
				    relative.put(new byte[] { 1, 2 });
				    relative.put(new byte[] { 3, 4, 5 }, 1, 2);
				
				    // Absolute puts leave both positions unchanged
				    ByteBuffer absolute = ByteBuffer.allocate(5);
				    absolute.put(1, new byte[] { 6, 7 });
				    absolute.put(2, new byte[] { 8, 9, 10 }, 1, 2);
				    return relative.position() + ":" + relative.get(0) + "," + relative.get(1) + "," +
				            relative.get(2) + "," + relative.get(3) + ":" + absolute.position() + ":" +
				            absolute.get(0) + "," + absolute.get(1) + "," + absolute.get(2) + "," +
				            absolute.get(3) + "," + absolute.get(4);
				}
				static String putBuffers() {
				    ByteBuffer target = ByteBuffer.allocate(4);
				    ByteBuffer source = ByteBuffer.wrap(new byte[] { 11, 12, 13 });
				    target.put(source);
				
				    ByteBuffer absoluteTarget = ByteBuffer.allocate(4);
				    ByteBuffer absoluteSource = ByteBuffer.wrap(new byte[] { 21, 22, 23 });
				
				    absoluteTarget.put(1, absoluteSource, 1, 2);
				
				    return target.position() + ":" + source.position() + ":" + target.get(0) + "," +
				            target.get(1) + "," + target.get(2) + ":" + absoluteTarget.position() + ":" +
				            absoluteSource.position() + ":" + absoluteTarget.get(0) + "," +
				            absoluteTarget.get(1) + "," + absoluteTarget.get(2) + "," + absoluteTarget.get(3);
				}
				""", ByteBuffer.class);

		// Array puts must preserve relative positions and write only the requested absolute ranges.
		assertStringValue("4:1,2,4,5:0:0,6,9,10,0",
				evaluate(compiled, "putArrays", "()Ljava/lang/String;", null, List.of()));

		// Relative buffer puts advance the source and target.
		// Absolute buffer puts leave both positions unchanged.
		assertStringValue("3:3:11,12,13:0:0:0,22,23,0",
				evaluate(compiled, "putBuffers", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testByteBufferDistinctResults() {
		String compiled = compile("""
				static ByteBuffer duplicate(ByteBuffer buffer) { return buffer.duplicate(); }
				static ByteBuffer slice(ByteBuffer buffer) { return buffer.slice(1, 2); }
				static ByteBuffer aligned(ByteBuffer buffer) { return buffer.alignedSlice(1); }
				static ByteBuffer readOnly(ByteBuffer buffer) { return buffer.asReadOnlyBuffer(); }
				static int readPosition(ByteBuffer buffer) { return buffer.position(); }
				""", ByteBuffer.class);

		// The duplicate() creates an independent position/limit state while sharing content with the original.
		InstancedObjectValue<ByteBuffer> input = new InstancedObjectValue<>(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}));
		InstancedObjectValue<ByteBuffer> duplicate = assertInstanceOf(InstancedObjectValue.class,
				evaluate(compiled, "duplicate", "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", null, List.of(input)));
		assertNotSame(input, duplicate);
		assertEquals(4, duplicate.getRealInstance().capacity());
		assertEquals(0, duplicate.getRealInstance().position());
		duplicate.getRealInstance().put(0, (byte) 9);
		assertEquals(9, input.getRealInstance().get(0));
		assertIntValue(0, evaluate(compiled, "readPosition",
				"(Ljava/nio/ByteBuffer;)I", null, List.of(duplicate)));

		// slice() is a distinct host-backed view with the requested content window.
		InstancedObjectValue<ByteBuffer> slice = assertInstanceOf(InstancedObjectValue.class,
				evaluate(compiled, "slice", "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", null, List.of(input)));
		assertEquals(2, slice.getRealInstance().capacity());
		assertEquals(2, slice.getRealInstance().get(0));

		// aligned() must also produce distinct host-backed results with the expected properties.
		InstancedObjectValue<ByteBuffer> aligned = assertInstanceOf(InstancedObjectValue.class,
				evaluate(compiled, "aligned", "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", null, List.of(input)));
		assertEquals(4, aligned.getRealInstance().capacity());

		// readOnly() must also produce distinct host-backed results with the expected properties.
		InstancedObjectValue<ByteBuffer> readOnly = assertInstanceOf(InstancedObjectValue.class,
				evaluate(compiled, "readOnly", "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", null, List.of(input)));
		assertTrue(readOnly.getRealInstance().isReadOnly());
	}

	@Test
	void testByteBufferExceptions() {
		// Bunch of example bad ByteBuffer usage that should produce exceptions.
		String compiled = compile("""
				static ByteBuffer negativeAllocation() { return ByteBuffer.allocate(-1); }
				static ByteBuffer invalidWrap() { return ByteBuffer.wrap(new byte[] { 1 }, 1, 2); }
				static byte invalidAbsolute(ByteBuffer buffer) { return buffer.get(4); }
				static byte[] directArray() { return ByteBuffer.allocateDirect(1).array(); }
				static ByteBuffer readOnlyWrite(ByteBuffer buffer) { return buffer.asReadOnlyBuffer().put(0, (byte) 1); }
				""", ByteBuffer.class);

		// Negative capacities are rejected by the allocation factory.
		assertByteBufferException(compiled, "negativeAllocation", "()Ljava/nio/ByteBuffer;",
				"java/lang/IllegalArgumentException");

		// Invalid wrap ranges and absolute indexes retain their modeled index exception.
		assertByteBufferException(compiled, "invalidWrap", "()Ljava/nio/ByteBuffer;",
				"java/lang/IndexOutOfBoundsException");
		assertByteBufferException(compiled, "invalidAbsolute", "(Ljava/nio/ByteBuffer;)B",
				"java/lang/IndexOutOfBoundsException", new InstancedObjectValue<>(ByteBuffer.allocate(4)));

		// Direct buffers have no accessible array, and read-only views reject writes.
		assertByteBufferException(compiled, "directArray", "()[B",
				"java/lang/UnsupportedOperationException");
		assertByteBufferException(compiled, "readOnlyWrite", "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;",
				"java/nio/ReadOnlyBufferException", new InstancedObjectValue<>(ByteBuffer.allocate(4)));
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
		assertStringValue("Hello World", retVal);
	}

	@Test
	void testListOf() {
		String compiled = compile("""
				static String run() {
				    List<String> values = List.of("a", "b");
				    return values.get(1);
				}
				""", List.class);
		assertStringValue("b", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testCopyOfArrayListOfListOf() {
		// Yeah that name's a mouth-full ain't it?
		String compiled = compile("""
				static String run() {
				    String[] source = {"a", "b"};
				    List<String> values = List.copyOf(new ArrayList<>(List.of(source)));
				    return values.get(1);
				}
				""", ArrayList.class, List.class);
		assertStringValue("b", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testSetOfAndCopyOfUniqueness() {
		String compiled = compile("""
				static String run() {
				    Set<String> values = Set.of("a", "b");
				    Set<String> copy = Set.copyOf(new ArrayList<>(List.of("a", "b", "a")));
				    return values.size() + ":" + values.contains("b") + ":" + copy.size();
				}
				""", Set.class, List.class, ArrayList.class);
		assertStringValue("2:true:2", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testSetOfDuplicateFailure() {
		String compiled = compile("""
				static Set<String> run() {
				    return Set.of("a", "a");
				}
				""", Set.class);
		assertEvaluationThrows(compiled, "run", "()Ljava/util/Set;", "java/lang/IllegalArgumentException");
	}

	@Test
	void testMapFactoriesAndCopyOf() {
		String compiled = compile("""
				static String run() {
				    Map<String, String> values = Map.of("a", "one", "b", "two");
				    Map.Entry<String, String> entry = Map.entry("c", "three");
				    Map<String, String> entries = Map.ofEntries(entry);
				    Map<String, String> copy = Map.copyOf(values);
				    return values.get("b") + ":" + entries.get("c") + ":" + copy.get("a");
				}
				""", Map.class);
		assertStringValue("two:three:one", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testCollectionsBasicFactories() {
		String compiled = compile("""
				static String run() {
				    return Collections.emptyList().size() + ":"
				            + Collections.singleton("a").contains("a") + ":"
				            + Collections.singletonList("b").get(0) + ":"
				            + Collections.singletonMap("c", "d").get("c") + ":"
				            + Collections.nCopies(2, "e").get(1);
				}
				""", Collections.class);
		assertStringValue("0:true:b:d:e", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testUnmodifiableListReads() {
		String compiled = compile("""
				static String run() {
				    List<String> values = Collections.unmodifiableList(new ArrayList<>(List.of("a", "b")));
				    return values.size() + ":" + values.get(1);
				}
				""", Collections.class, ArrayList.class, List.class);
		assertStringValue("2:b", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testUnmodifiableListMutationFailure() {
		String compiled = compile("""
				static boolean run() {
				    List<String> values = Collections.unmodifiableList(new ArrayList<>(List.of("a")));
				    return values.add("b");
				}
				""", Collections.class, ArrayList.class, List.class);
		assertEvaluationThrows(compiled, "run", "()Z", "java/lang/UnsupportedOperationException");
	}

	@Test
	void testUnmodifiableSetAndMapReads() {
		String compiled = compile("""
				static String run() {
				    Set<String> set = Collections.unmodifiableSet(Set.of("a"));
				    Map<String, String> map = Collections.unmodifiableMap(Map.of("b", "c"));
				    return set.contains("a") + ":" + map.get("b");
				}
				""", Collections.class, Set.class, Map.class);
		assertStringValue("true:c", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testUnmodifiableSetAndMapMutationFailures() {
		String compiled = compile("""
				static boolean runSet() {
				    Collections.unmodifiableSet(Set.of("a")).clear();
				    return false;
				}
				static boolean runMap() {
				    Collections.unmodifiableMap(Map.of("a", "b")).clear();
				    return false;
				}
				""", Collections.class, Set.class, Map.class);
		assertEvaluationThrows(compiled, "runSet", "()Z", "java/lang/UnsupportedOperationException");
		assertEvaluationThrows(compiled, "runMap", "()Z", "java/lang/UnsupportedOperationException");
	}

	@Test
	void testSynchronizedListOperations() {
		String compiled = compile("""
				static String run() {
				    List<String> values = Collections.synchronizedList(new ArrayList<>());
				    values.add("a");
				    values.add(0, "b");
				    return values.get(1) + ":" + values.size();
				}
				""", Collections.class, ArrayList.class, List.class);
		assertStringValue("a:2", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testSynchronizedSetOperations() {
		String compiled = compile("""
				static boolean run() {
				    Set<String> values = Collections.synchronizedSet(new HashSet<>());
				    values.add("a");
				    return values.contains("a");
				}
				""", Collections.class, HashSet.class, Set.class);
		assertIntValue(1, evaluate(compiled, "run", "()Z", null, List.of()));
	}

	@Test
	void testSynchronizedMapOperations() {
		String compiled = compile("""
				static String run() {
				    Map<String, String> values = Collections.synchronizedMap(new HashMap<>());
				    values.put("a", "b");
				    return values.get("a");
				}
				""", Collections.class, HashMap.class, Map.class);
		assertStringValue("b", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testArrayListOperations() {
		String compiled = compile("""
				static String run() {
				    ArrayList<String> values = new ArrayList<>(4);
				    values.add("a");
				    values.add(0, "b");
				    values.set(1, "c");
				    values.addAll(new ArrayList<>(List.of("d")));
				    values.remove("b");
				    Object[] all = values.toArray();
				    Object[] typed = values.toArray(new Object[0]);
				    return values.get(0) + ":" + values.size() + ":" + values.containsAll(List.of("c", "d"))
				            + ":" + all.length + ":" + typed.length;
				}
				""", ArrayList.class, List.class);
		assertStringValue("c:2:true:2:2", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testLinkedListOperations() {
		String compiled = compile("""
				static String run() {
				    LinkedList<String> values = new LinkedList<>(List.of("a", "b"));
				    values.add(0, "z");
				    values.add("c");
				    values.remove(1);
				    return values.get(0) + ":" + values.get(2) + ":" + values.size();
				}
				""", LinkedList.class, List.class);
		assertStringValue("z:c:3", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testHashSetOperations() {
		String compiled = compile("""
				static String run() {
				    HashSet<String> values = new HashSet<>(4, 0.75f);
				    values.addAll(new ArrayList<>(List.of("a", "b", "a")));
				    HashSet<String> copy = new HashSet<>(values);
				    return copy.size() + ":" + copy.contains("b");
				}
				""", HashSet.class, ArrayList.class, List.class);
		assertStringValue("2:true", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testLinkedHashSetOperations() {
		String compiled = compile("""
				static String run() {
				    LinkedHashSet<String> values = new LinkedHashSet<>(4, 0.75f);
				    values.add("a");
				    values.add("b");
				    LinkedHashSet<String> copy = new LinkedHashSet<>(values);
				    return copy.iterator().next();
				}
				""", LinkedHashSet.class);
		assertStringValue("a", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testTreeSetSortedOperations() {
		String compiled = compile("""
				static String run() {
				    TreeSet<String> values = new TreeSet<>(List.of("b", "a"));
				    return values.first() + ":" + values.last() + ":" + values.ceiling("a");
				}
				""", TreeSet.class, List.class);
		assertStringValue("a:b:a", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testHashMapOperations() {
		String compiled = compile("""
				static String run() {
				    HashMap<String, String> values = new HashMap<>(4, 0.75f);
				    values.put("a", "one");
				    values.putIfAbsent("b", "two");
				    HashMap<String, String> copy = new HashMap<>(values);
				    return copy.getOrDefault("a", "missing") + ":" + copy.size();
				}
				""", HashMap.class);
		assertStringValue("one:2", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testLinkedHashMapOperations() {
		String compiled = compile("""
				static String run() {
				    LinkedHashMap<String, String> values = new LinkedHashMap<>(4, 0.75f, true);
				    values.put("a", "one");
				    values.put("b", "two");
				    values.get("a");
				    LinkedHashMap<String, String> copy = new LinkedHashMap<>(values);
				    return copy.keySet().iterator().next();
				}
				""", LinkedHashMap.class);
		assertStringValue("b", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testTreeMapSortedOperations() {
		String compiled = compile("""
				static String run() {
				    TreeMap<String, String> values = new TreeMap<>(Map.of("b", "two", "a", "one"));
				    return values.firstKey() + ":" + values.lastKey() + ":" + values.ceilingKey("a");
				}
				""", TreeMap.class, Map.class);
		assertStringValue("a:b:a", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testCollectionConstructorFailures() {
		String compiled = compile("""
				static ArrayList<String> badArrayList() { return new ArrayList<>(-1); }
				static HashSet<String> badHashSet() { return new HashSet<>(-1); }
				static HashMap<String, String> badHashMap() { return new HashMap<>(-1); }
				static TreeSet<String> badTreeSet() {
				    ArrayList<String> values = new ArrayList<>();
				    values.add(null);
				    return new TreeSet<>(values);
				}
				""", ArrayList.class, HashSet.class, HashMap.class, TreeSet.class);
		assertEvaluationThrows(compiled, "badArrayList", "()Ljava/util/ArrayList;", "java/lang/IllegalArgumentException");
		assertEvaluationThrows(compiled, "badHashSet", "()Ljava/util/HashSet;", "java/lang/IllegalArgumentException");
		assertEvaluationThrows(compiled, "badHashMap", "()Ljava/util/HashMap;", "java/lang/IllegalArgumentException");
		assertEvaluationThrows(compiled, "badTreeSet", "()Ljava/util/TreeSet;", "java/lang/NullPointerException");
	}

	@Test
	void testIteratorNext() {
		String compiled = compile("""
				static String run() {
				    Iterator<String> iterator = List.of("a").iterator();
				    return iterator.hasNext() ? iterator.next() : "missing";
				}
				""", Iterator.class, List.class);
		assertStringValue("a", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
	}

	@Test
	void testMapEntryView() {
		String compiled = compile("""
				static String run() {
				    Map<String, String> values = new HashMap<>();
				    values.put("a", "one");
				    Map.Entry<String, String> entry = values.entrySet().iterator().next();
				    String old = entry.setValue("two");
				    return entry.getKey() + ":" + old + ":" + entry.getValue() + ":" + values.get("a");
				}
				""", HashMap.class, Map.class, Set.class, Iterator.class);
		assertStringValue("a:one:two:two", evaluate(compiled, "run", "()Ljava/lang/String;", null, List.of()));
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
		assertStringValue(CLASS_NAME + ":" + "foo", retVal);
	}

	@Test
	void testStackTraceWithCallerSeed() {
		compile("""
				static String prior() {
				     StackTraceElement ste = new RuntimeException().getStackTrace()[1];
				     return ste.getClassName() + ":" + ste.getMethodName();
				}
				""");

		// Read the example class from the workspace.
		ClassNode classNode = new ClassNode();
		get(CLASS_NAME).getClassReader().accept(classNode, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

		// Create an evaluator with a synthetic method that calls prior() to seed the stack trace.
		MethodNode foo = new MethodNode(Opcodes.ACC_STATIC, "injectedMethod", "()Ljava/lang/String;", null, null);
		Evaluator evaluator = createEvaluator(List.of(new ClassMethodPair(classNode, foo)));

		// When we evaluate prior() in the context of the synthetic method, it should report the synthetic method as the caller.
		EvaluationResult result = evaluator.evaluate(CLASS_NAME, "prior", "()Ljava/lang/String;", null, List.of());
		EvaluationYieldResult yielded = assertInstanceOf(EvaluationYieldResult.class, result);
		assertStringValue(CLASS_NAME + ":injectedMethod", yielded.value());
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
		assertStringValue("caught", retVal);
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
		assertStringValue("caught", evaluate(compiled, "outer", "()Ljava/lang/String;", null, List.of()));
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
		assertStringValue("custom", assertInstanceOf(EvaluationYieldResult.class, result).value());
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
		assertStringValue("arith", evaluate(compiled, "arithmetic", "()Ljava/lang/String;", null, List.of()));
		assertStringValue("null", evaluate(compiled, "nullReceiver", "()Ljava/lang/String;", null, List.of()));
		assertStringValue("array", evaluate(compiled, "array", "()Ljava/lang/String;", null, List.of()));
		assertStringValue("negative", evaluate(compiled, "negativeArray", "()Ljava/lang/String;", null, List.of()));
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
		assertIntValue(12, evaluate(compiled, "run", "()I", null, List.of(), get("Holder")));
		assertIntValue(0, evaluate(compiled, "zero", "()I", null, List.of(), get("Holder")));
	}

	@Test
	void testWorkspaceClassInitializerOptIn() {
		// Compile class with static int field initialized to 1.
		compileStaticState();

		// Validate that the workspace class initializer runs before reading the static field value.
		// We should be able to get the '1' value from a GETSTATIC instruction.
		String compiled = compile("""
				static int run() { return StaticState.VALUE; }
				""");
		EvaluationResult result = evaluateResult(compiled, "run", "()I", null, List.of(), true, get("StaticState"));
		EvaluationYieldResult yielded = assertInstanceOf(EvaluationYieldResult.class, result);
		assertIntValue(1, yielded.value());
	}

	@Test
	void testWorkspaceClassInitializerOptOut() {
		// Compile class with static int field initialized to 1.
		compileStaticState();

		// Same test as above, but without initialization enabled.
		// The static field value should be unknown.
		String compiled = compile("""
				static int run() { return StaticState.VALUE; }
				""");

		EvaluationResult result = evaluateResult(compiled, "run", "()I", null, List.of(), false, get("StaticState"));
		EvaluationYieldResult yielded = assertInstanceOf(EvaluationYieldResult.class, result);
		assertSame(IntValue.UNKNOWN, yielded.value());
	}

	@Test
	void testWorkspaceClassInitializerRunsOnce() {
		// Compile class with static int field initialized to 1.
		compileStaticState();

		// Validate that methods that rely on that static field initialization work and
		// JVM initialization bounds (method calls, field access, etc.) don't cause the initializer to run multiple times.
		String compiled = compile("""
				static int run() { return StaticState.read() * 10 + StaticState.VALUE; }
				""");

		EvaluationResult result = evaluateResult(compiled, "run", "()I", null, List.of(), true, get("StaticState"));
		EvaluationYieldResult yielded = assertInstanceOf(EvaluationYieldResult.class, result);
		assertIntValue(11, yielded.value());
	}

	@Test
	void testWorkspaceClassInitializerFailurePropagates() {
		// Compile class that fails during static initialization.
		// The failure should propagate to the evaluator.
		compileFull("FailingState", """
				public class FailingState {
				    public static int VALUE;
				    static { VALUE = fail(); }
				    private static int fail() { throw new IllegalStateException(); }
				}
				""");

		// Validate that when we try to read the static field, the initializer runs and throws an exception.
		String compiled = compile("""
				static int run() { return FailingState.VALUE; }
				""");
		EvaluationResult result = evaluateResult(compiled, "run", "()I", null, List.of(), true, get("FailingState"));
		EvaluationThrowsResult thrown = assertInstanceOf(EvaluationThrowsResult.class, result);
		ThrowableValue throwable = assertInstanceOf(ThrowableValue.class, thrown.exception());
		assertEquals("java/lang/IllegalStateException", throwable.type().getInternalName());
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

		assertIntValue(53, evaluate(compiled, "run", "()I", null, List.of(), get("Holder")));
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

		assertIntValue(22, evaluate(compiled, "run", "()I", null, List.of(), get("Base"), get("Child"), get("Op")));
		assertIntValue(1, evaluate(compiled, "superValue", "()I", null, List.of(), get("Base"), get("Child"), get("Op")));
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

		assertIntValue(1, evaluate(compiled, "run", "()I", null, List.of(),
				get("Base"), get("Child")));
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

		assertIntValue(1, evaluate(compiled, "run", "()I", null, List.of(),
				get("Base"), get("Child")));
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

		assertIntValue(1, evaluate(compiled, "run", "()I", null, List.of(),
				get("Base"), get("Child")));
	}

	@Test
	void testWorkspaceDistinctAllocationsHaveDistinctIdentity() {
		// Compile simple Base + Child hierarchy.
		compileBaseChildHierarchy();

		// Validate two distinct allocations of the same type are not equal.
		String compiled = compile("""
				static int run() { return new Child() == new Child() ? 1 : 0; }
				""");

		assertIntValue(0, evaluate(compiled, "run", "()I", null, List.of(),
				get("Base"), get("Child")));
	}

	private void compileStaticState() {
		compileFull("StaticState", """
				public class StaticState {
				    public static int VALUE;
				    static { VALUE++; }
				    public static int read() { return VALUE; }
				}
				""");
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

	private void assertByteBufferException(@Nonnull String compiled,
	                                       @Nonnull String name,
	                                       @Nonnull String descriptor,
	                                       @Nonnull String expectedType,
	                                       ReValue... parameters) {
		EvaluationResult result = evaluateResult(compiled, name, descriptor, null, List.of(parameters));
		EvaluationThrowsResult thrown = assertInstanceOf(EvaluationThrowsResult.class, result);
		ThrowableValue exception = assertInstanceOf(ThrowableValue.class, thrown.exception());
		assertEquals(expectedType, exception.type().getInternalName());
	}

	private void assertEvaluationThrows(@Nonnull String compiled,
	                                    @Nonnull String name,
	                                    @Nonnull String descriptor,
	                                    @Nonnull String expectedType) {
		EvaluationResult result = evaluateResult(compiled, name, descriptor, null, List.of());
		if (result instanceof EvaluationFailureResult failure)
			fail("Evaluation failed: " + failure.reason(), failure.cause());
		EvaluationThrowsResult thrown = assertInstanceOf(EvaluationThrowsResult.class, result);
		ThrowableValue exception = assertInstanceOf(ThrowableValue.class, thrown.exception());
		assertEquals(expectedType, exception.type().getInternalName());
	}

	private static void assertIntValue(int value, @Nullable ReValue result) {
		if (result instanceof IntValue intVal)
			assertEquals(value, intVal.value().orElseThrow());
		else
			fail("Evaluation failure, unexpected return value: " + result);
	}

	private static void assertLongValue(long value, @Nullable ReValue result) {
		if (result instanceof LongValue longVal)
			assertEquals(value, longVal.value().orElseThrow());
		else
			fail("Evaluation failure, unexpected return value: " + result);
	}

	private static void assertStringValue(@Nullable String value, @Nullable ReValue result) {
		if (result instanceof StringValue strVal)
			assertEquals(value, strVal.getText().orElseThrow());
		else
			fail("Evaluation failure, unexpected return value: " + result);
	}

	private static void assertUnknownBranchFailure(@Nonnull EvaluationResult result) {
		if (result instanceof EvaluationFailureResult failure)
			assertEquals("Encountered unknown value while evaluating branch", failure.reason());
		else
			fail("Expected unknown-branch evaluation failure, got: " + result);
	}

	@Nonnull
	private static List<String> stackMethodNames(@Nonnull List<ClassMethodPair> stack) {
		List<String> names = new ArrayList<>(stack.size());
		for (ClassMethodPair pair : stack)
			names.add(pair.methodNode().name);
		return names;
	}

	@Nonnull
	private Evaluator createEvaluator() {
		return createEvaluator(List.of());
	}

	@Nonnull
	private Evaluator createEvaluator(@Nonnull List<ClassMethodPair> callStackSeed) {
		JvmClassInfo assembled = get(CLASS_NAME);
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(assembled));
		JvmTransformerContext ctx = new JvmTransformerContext(workspace, workspace.getPrimaryResource(), Collections.emptyList());
		ReInterpreter interpreter = ctx.newInterpreter(new InheritanceGraph(workspace));
		Evaluator evaluator = new Evaluator(workspace, interpreter, new FieldCacheManager(), 1000, false, false);
		evaluator.setCallStackSeed(callStackSeed);
		return evaluator;
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
		return evaluateResult(src, name, desc, classInstance, parameters, false, additionalClasses);
	}

	@Nonnull
	private EvaluationResult evaluateResult(@Nonnull String src, @Nonnull String name, @Nonnull String desc,
	                                        @Nullable ObjectValue classInstance, @Nonnull List<ReValue> parameters,
	                                        boolean evaluateClassInitializers,
	                                        @Nonnull JvmClassInfo... additionalClasses) {
		JvmClassInfo assembled = assemble(src, src.contains(".class"));
		JvmClassInfo[] classes = new JvmClassInfo[additionalClasses.length + 1];
		classes[0] = assembled;
		System.arraycopy(additionalClasses, 0, classes, 1, additionalClasses.length);
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(classes));
		JvmTransformerContext ctx = new JvmTransformerContext(workspace, workspace.getPrimaryResource(), Collections.emptyList());
		ReInterpreter interpreter = ctx.newInterpreter(new InheritanceGraph(workspace));
		return new Evaluator(workspace, interpreter, new FieldCacheManager(), 1000, false, evaluateClassInitializers)
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
		EvaluationResult result = new Evaluator(workspace, interpreter, new FieldCacheManager(), 1000, false, false)
				.evaluate(CLASS_NAME, name, desc, null, List.of());
		if (result instanceof EvaluationYieldResult yielded)
			return yielded.value();
		fail("Lookup evaluation failed: " + result);
		throw new IllegalStateException();
	}

}
