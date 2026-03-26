package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Invoke;
import me.darknet.dex.tree.type.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.StubMethodMember;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.TestDexUtils;
import software.coley.recaf.test.dummy.ClassWithLambda;
import software.coley.recaf.test.dummy.StringConsumer;
import software.coley.recaf.test.dummy.StringConsumerUser;
import software.coley.recaf.util.visitors.MemberFilteringVisitor;
import software.coley.recaf.util.visitors.MemberPredicate;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicAndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for {@link CallGraph}
 */
class CallGraphTest {
	private static final byte[] fooBytes;
	private static final byte[] fooCallerBytes;
	private static final byte[] interfaceBytes;
	private static final byte[] interfaceCallerBytes;
	private static final byte[] specialInterfaceBytes;
	private static final byte[] specialInterfaceImplBytes;
	private static final byte[] specialInterfaceCallerBytes;
	private static final byte[] privateSpecialBytes;

	@Test
	@Timeout(10)
	void testCalleeCallerRelation() throws IOException {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				StringConsumer.class,
				StringConsumerUser.class
		));

		ClassPathNode pathUser = workspace.findJvmClass(StringConsumerUser.class.getName().replace('.', '/'));
		ClassPathNode pathFunc = workspace.findJvmClass(StringConsumer.class.getName().replace('.', '/'));
		assertNotNull(pathUser, "Missing main class");
		assertNotNull(pathFunc, "Missing function class");
		JvmClassInfo mainClass = pathUser.getValue().asJvmClass();
		JvmClassInfo functionClass = pathFunc.getValue().asJvmClass();

		CallGraph callGraph = newCallGraph(workspace);

		ClassMethodsContainer containerMain = callGraph.getClassMethodsContainer(mainClass);
		ClassMethodsContainer containerFunction = callGraph.getClassMethodsContainer(functionClass);

		// Get outbound calls for main. Should just be to 'new StringConsumer()' and 'StringConsumer.accept(String)'
		MethodVertex mainVertex = containerMain.getVertex("main", "([Ljava/lang/String;)V");
		assertNotNull(mainVertex, "Missing method vertex for 'main'");
		assertEquals(2, mainVertex.getCalls().size());

		// Assert main calls 'accept'
		MethodVertex acceptVertex = containerFunction.getVertex("accept", "(Ljava/lang/String;)V");
		assertNotNull(acceptVertex, "Missing method vertex for 'accept'");
		assertTrue(acceptVertex.getCallers().contains(mainVertex));

		// Assert main calls 'new StringConsumer()'
		MethodVertex newVertex = containerFunction.getVertex("<init>", "()V");
		assertNotNull(newVertex, "Missing method vertex for '<init>'");
		assertTrue(newVertex.getCallers().contains(mainVertex));
	}

	@Test
	@Timeout(10)
	void testUnresolvedCall() {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				new JvmClassInfoBuilder(fooCallerBytes).build()
		));

		ClassPathNode pathUser = workspace.findJvmClass("FooCaller");
		assertNotNull(pathUser, "Missing FooCaller class");
		JvmClassInfo mainClass = pathUser.getValue().asJvmClass();

		CallGraph callGraph = newCallGraph(workspace);

		// Get outbound calls for call(Foo). Should just be to 'foo.bar()' which is unresolved
		ClassMethodsContainer fooCaller = callGraph.getClassMethodsContainer(mainClass);
		MethodVertex callVertex = fooCaller.getVertex("call", "(LFoo;)V");
		assertNotNull(callVertex, "Missing method vertex for 'call'");
		assertEquals(0, callVertex.getCalls().size(), "Expected to have no resolved calls from call(Foo)");
		assertEquals(1, callGraph.getUnresolvedDeclarations().get("Foo").size(), "Expected to have unresolved call to Foo.bar()");

		// Add the missing Foo class to the workspace
		workspace.getPrimaryResource().getJvmClassBundle().put(new JvmClassInfoBuilder(fooBytes).build());

		// The call to Foo.bar() should be resolved now
		assertEquals(1, callVertex.getCalls().size());
		assertEquals(0, callGraph.getUnresolvedDeclarations().size(), "Expected to have resolved unresolved call to Foo.bar()");

		// Updating the Foo class (with no real changes) should not cause the call to
		// become unresolved again, assuming the method's code is unchanged.
		workspace.getPrimaryResource().getJvmClassBundle().put(new JvmClassInfoBuilder(fooBytes).build());
		assertEquals(1, callVertex.getCalls().size());
		assertEquals(0, callGraph.getUnresolvedDeclarations().size(), "Expected to have resolved call to Foo.bar() after updating Foo class");

		// Updating the FooCaller class (with no real changes) should also not cause the
		// call to become unresolved again, assuming the method's code is unchanged.
		// - Need to get new container/vertex references since the class is updated and the old ones will be stale.
		// - Need to also update 'mainClass' to use the modified class reference since the old one will be stale.
		workspace.getPrimaryResource().getJvmClassBundle().put(mainClass = new JvmClassInfoBuilder(fooCallerBytes).build());
		fooCaller = callGraph.getClassMethodsContainer(mainClass);
		callVertex = fooCaller.getVertex("call", "(LFoo;)V");
		assertNotNull(callVertex, "Missing method vertex for 'call' after updating FooCaller class");
		assertEquals(1, callVertex.getCalls().size());
		assertEquals(0, callGraph.getUnresolvedDeclarations().size(), "Expected to have resolved call to Foo.bar() after updating FooCaller class");

		// If we change the Foo class to remove the bar() method, the call should become unresolved again
		// since the called method no longer exists in the class.
		ClassWriter cw = new ClassWriter(0);
		new ClassReader(fooBytes).accept(new MemberFilteringVisitor(cw, MemberPredicate.of(new StubMethodMember("", "", 0))), 0);
		workspace.getPrimaryResource().getJvmClassBundle().put(new JvmClassInfoBuilder(cw.toByteArray()).build());
		assertEquals(0, callVertex.getCalls().size());
		assertEquals(1, callGraph.getUnresolvedDeclarations().get("Foo").size(), "Expected to have unresolved call to Foo.bar() after removing bar() method from Foo class");

		// Put it back to resolve the call again (mainly to set up for the next part of the test)
		workspace.getPrimaryResource().getJvmClassBundle().put(new JvmClassInfoBuilder(fooBytes).build());
		assertEquals(1, callVertex.getCalls().size());
		assertEquals(0, callGraph.getUnresolvedDeclarations().size(), "Expected to have resolved call to Foo.bar() after updating Foo class");

		// Remove the Foo class again should cause the call to become unresolved again
		workspace.getPrimaryResource().getJvmClassBundle().remove("Foo");
		fooCaller = callGraph.getClassMethodsContainer(mainClass);
		callVertex = fooCaller.getVertex("call", "(LFoo;)V");
		assertNotNull(callVertex);
		assertEquals(0, callVertex.getCalls().size());
		assertEquals(1, callGraph.getUnresolvedDeclarations().get("Foo").size(), "Expected to have unresolved call to Foo.bar() after removing Foo class");
	}

	@Test
	@Timeout(10)
	void testLambdaMetafactoryLinksSyntheticTarget() throws IOException {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(ClassWithLambda.class));
		ClassPathNode lambdaClassPath = workspace.findJvmClass(ClassWithLambda.class.getName().replace('.', '/'));
		assertNotNull(lambdaClassPath, "Missing lambda test class");
		JvmClassInfo lambdaClass = lambdaClassPath.getValue().asJvmClass();

		// Assert that the lambda method links to its synthetic target method.
		// The 'runnable()' method should link to the lambda method generated by the lambda metafactory.
		CallGraph callGraph = newCallGraph(workspace);
		MethodVertex runnableVertex = callGraph.getClassMethodsContainer(lambdaClass).getVertex("runnable", "()V");
		assertNotNull(runnableVertex, "Missing runnable() vertex");
		assertTrue(runnableVertex.getCalls().stream()
						.map(MethodVertex::getResolvedMethod)
						.filter(Objects::nonNull)
						.anyMatch(method -> method.getDeclaringClass() == lambdaClass
								&& method.getName().startsWith("lambda$runnable$")),
				"Expected runnable() to link to its synthetic lambda target");
	}

	@Test
	@Timeout(10)
	void testInterfaceCallRelinksAfterRemoval() {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				new JvmClassInfoBuilder(interfaceBytes).build(),
				new JvmClassInfoBuilder(interfaceCallerBytes).build()
		));

		ClassPathNode interfacePath = workspace.findJvmClass("FooInterface");
		ClassPathNode callerPath = workspace.findJvmClass("FooInterfaceCaller");
		assertNotNull(interfacePath, "Missing interface test class");
		assertNotNull(callerPath, "Missing interface caller test class");
		JvmClassInfo interfaceClass = interfacePath.getValue().asJvmClass();
		JvmClassInfo callerClass = callerPath.getValue().asJvmClass();

		// Assert that the call to the interface method is initially resolved.
		CallGraph callGraph = newCallGraph(workspace);
		MethodVertex callVertex = callGraph.getClassMethodsContainer(callerClass).getVertex("call", "(LFooInterface;)V");
		MethodVertex barVertex = callGraph.getClassMethodsContainer(interfaceClass).getVertex("bar", "()V");
		assertNotNull(callVertex, "Missing call(FooInterface) vertex");
		assertNotNull(barVertex, "Missing interface bar() vertex");
		assertEquals(1, callVertex.getCalls().size(), "Expected interface call to resolve initially");
		assertTrue(callVertex.getCalls().contains(barVertex), "Expected caller to link to interface method");

		// Removing the interface class should cause the call to become unresolved since the method declaration is no longer present.
		workspace.getPrimaryResource().getJvmClassBundle().remove("FooInterface");
		assertEquals(0, callVertex.getCalls().size(), "Expected interface call to become unresolved after removal");
		assertEquals(1, callGraph.getUnresolvedDeclarations().get("FooInterface").size(),
				"Expected unresolved interface declaration after removal");

		// Putting the interface class back should cause the call to relink to the restored method declaration.
		workspace.getPrimaryResource().getJvmClassBundle().put(interfaceClass = new JvmClassInfoBuilder(interfaceBytes).build());
		MethodVertex relinkedBarVertex = callGraph.getClassMethodsContainer(interfaceClass).getVertex("bar", "()V");
		assertNotNull(relinkedBarVertex, "Missing interface bar() vertex after restore");
		assertEquals(1, callVertex.getCalls().size(), "Expected interface call to relink after restore");
		assertTrue(callVertex.getCalls().contains(relinkedBarVertex),
				"Expected caller to relink to restored interface method");
		assertEquals(0, callGraph.getUnresolvedDeclarations().size(),
				"Expected unresolved interface declaration to clear after restore");
	}

	@Test
	@Timeout(10)
	void testSpecialCallResolvesInheritedInterfaceDefaultMethod() {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				new JvmClassInfoBuilder(specialInterfaceBytes).build(),
				new JvmClassInfoBuilder(specialInterfaceImplBytes).build(),
				new JvmClassInfoBuilder(specialInterfaceCallerBytes).build()
		));

		// Get classes and graph.
		ClassPathNode interfacePath = workspace.findJvmClass("SpecialInterface");
		ClassPathNode callerPath = workspace.findJvmClass("SpecialInterfaceCaller");
		assertNotNull(interfacePath, "Missing default-method interface");
		assertNotNull(callerPath, "Missing special caller class");
		JvmClassInfo interfaceClass = interfacePath.getValue().asJvmClass();
		JvmClassInfo callerClass = callerPath.getValue().asJvmClass();
		CallGraph callGraph = newCallGraph(workspace);

		// Validate that the invokespecial call in the caller resolves to the default method declared in the implemented interface.
		MethodVertex callVertex = callGraph.getClassMethodsContainer(callerClass).getVertex("call", "()V");
		MethodVertex fooVertex = callGraph.getClassMethodsContainer(interfaceClass).getVertex("foo", "()V");
		assertNotNull(callVertex, "Missing call() vertex for special caller");
		assertNotNull(fooVertex, "Missing default foo() vertex");
		assertEquals(1, callVertex.getCalls().size(), "Expected invokespecial to resolve one default-method target");
		assertTrue(callVertex.getCalls().contains(fooVertex),
				"Expected invokespecial owner without declaration to resolve inherited default method");
	}

	@Test
	@Timeout(10)
	void testPrivateSpecialCallRelation() {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(
				new JvmClassInfoBuilder(privateSpecialBytes).build()
		));

		// Get class and graph.
		ClassPathNode privatePath = workspace.findJvmClass("PrivateSpecial");
		assertNotNull(privatePath, "Missing private special-call class");
		JvmClassInfo privateClass = privatePath.getValue().asJvmClass();
		CallGraph callGraph = newCallGraph(workspace);

		// Validate that the private invokespecial call resolves to the private method defined in the same class.
		// This ensures that invokespecial lookups correctly consider private methods versus the other lookup rules
		// for invokespecial which skip private methods in superclasses.
		MethodVertex callVertex = callGraph.getClassMethodsContainer(privateClass).getVertex("callHidden", "()V");
		MethodVertex hiddenVertex = callGraph.getClassMethodsContainer(privateClass).getVertex("hidden", "()V");
		assertNotNull(callVertex, "Missing callHidden() vertex");
		assertNotNull(hiddenVertex, "Missing hidden() vertex");
		assertEquals(1, callVertex.getCalls().size(), "Expected private invokespecial to resolve exactly one target");
		assertTrue(callVertex.getCalls().contains(hiddenVertex), "Expected private invokespecial to link to hidden()");
	}

	@Test
	@Timeout(10)
	void testAndroidToAndroidCallRelation() {
		// class AndroidCaller {
		//     public static void call() {
		//         AndroidTarget.callee();
		//     }
		// }
		// class AndroidTarget {
		//     public static void callee() {}
		// }
		AndroidClassInfo caller = newAndroidCaller("demo/AndroidCaller", "demo/AndroidTarget");
		AndroidClassInfo target = newAndroidCallee("demo/AndroidTarget");
		BasicAndroidClassBundle bundle = new BasicAndroidClassBundle();
		bundle.initialPut(caller);
		bundle.initialPut(target);

		// Get graph
		Workspace workspace = TestClassUtils.fromBundle(bundle);
		CallGraph callGraph = newCallGraph(workspace);

		// Validate call -> callee relation
		ClassMethodsContainer callerContainer = callGraph.getClassMethodsContainer(caller);
		ClassMethodsContainer targetContainer = callGraph.getClassMethodsContainer(target);
		MethodVertex callVertex = callerContainer.getVertex("call", "()V");
		MethodVertex calleeVertex = targetContainer.getVertex("callee", "()V");
		assertNotNull(callVertex, "Missing Android caller vertex");
		assertNotNull(calleeVertex, "Missing Android callee vertex");
		assertSame(caller, callerContainer.getClassInfo(), "Caller container should expose generic class info");
		assertSame(target, targetContainer.getClassInfo(), "Target container should expose generic class info");
		assertEquals(1, callVertex.getCalls().size(), "Expected Android caller to resolve one call");
		assertTrue(callVertex.getCalls().contains(calleeVertex), "Expected Android caller to link to Android callee");
		assertTrue(calleeVertex.getCallers().contains(callVertex), "Expected Android callee to know its caller");
	}

	@Test
	@Timeout(10)
	void testAndroidUnresolvedCallLifecycle() {
		// Same test as above but with dynamic class updates to validate that
		// calls are resolved and unresolved as expected.
		String callerName = "demo/AndroidLifecycleCaller";
		String calleeName = "demo/AndroidLifecycleTarget";
		AndroidClassInfo caller = newAndroidCaller(callerName, calleeName);
		BasicAndroidClassBundle bundle = new BasicAndroidClassBundle();
		bundle.initialPut(caller);

		// Get graph
		Workspace workspace = TestClassUtils.fromBundle(bundle);
		CallGraph callGraph = newCallGraph(workspace);

		// Validate initial unresolved state
		ClassMethodsContainer callerContainer = callGraph.getClassMethodsContainer(caller);
		MethodVertex callVertex = callerContainer.getVertex("call", "()V");
		assertNotNull(callVertex, "Missing Android caller vertex");
		assertEquals(0, callVertex.getCalls().size(), "Expected Android call to start unresolved");
		assertEquals(1, callGraph.getUnresolvedDeclarations().get(calleeName).size(),
				"Expected unresolved Android target declaration");

		// Add target class (callee) and validate graph updates lead to call resolution.
		AndroidClassInfo target = newAndroidCallee(calleeName);
		bundle.put(target);
		assertEquals(1, callVertex.getCalls().size(), "Expected Android call to resolve when target is added");
		assertEquals(0, callGraph.getUnresolvedDeclarations().size(),
				"Expected unresolved Android declaration to be cleared after add");

		// Redundant updates shouldn't break anything.
		bundle.put(target = newAndroidCallee(calleeName));
		assertEquals(1, callVertex.getCalls().size(), "Expected Android call to remain resolved after target update");
		assertEquals(0, callGraph.getUnresolvedDeclarations().size(),
				"Expected unresolved Android declaration to stay cleared after target update");

		// Redundant caller update shouldn't break anything either.
		bundle.put(caller = newAndroidCaller(callerName, calleeName));
		callerContainer = callGraph.getClassMethodsContainer(caller);
		callVertex = callerContainer.getVertex("call", "()V");
		assertNotNull(callVertex, "Missing Android caller vertex after caller update");
		assertEquals(1, callVertex.getCalls().size(), "Expected Android caller update to preserve resolved call");
		assertEquals(0, callGraph.getUnresolvedDeclarations().size(),
				"Expected unresolved Android declaration to stay cleared after caller update");

		// If we remove the method, the call should become unresolved again.
		bundle.put(TestDexUtils.newAndroidClass(calleeName));
		assertEquals(0, callVertex.getCalls().size(), "Expected Android call to become unresolved after method removal");
		assertEquals(1, callGraph.getUnresolvedDeclarations().get(calleeName).size(),
				"Expected unresolved Android declaration after method removal");

		// If we add the method back, the call should resolve again.
		bundle.put(target = newAndroidCallee(calleeName));
		assertEquals(1, callVertex.getCalls().size(), "Expected Android call to resolve after method restoration");
		assertEquals(0, callGraph.getUnresolvedDeclarations().size(),
				"Expected unresolved Android declaration to clear after method restoration");

		// If we remove the entire class, the call should become unresolved again.
		bundle.remove(calleeName);
		assertEquals(0, callVertex.getCalls().size(), "Expected Android call to become unresolved after class removal");
		assertEquals(1, callGraph.getUnresolvedDeclarations().get(calleeName).size(),
				"Expected unresolved Android declaration after class removal");

		// If we add the entire class back, the call should resolve again.
		bundle.put(target);
		assertEquals(1, callVertex.getCalls().size(), "Expected Android call to relink after class restoration");
		assertEquals(0, callGraph.getUnresolvedDeclarations().size(),
				"Expected unresolved Android declaration to clear after class restoration");
	}

	@Test
	@Timeout(10)
	void testAndroidDirectCallRelation() {
		AndroidClassInfo caller = newAndroidDirectCaller("demo/AndroidDirectCaller");
		BasicAndroidClassBundle bundle = new BasicAndroidClassBundle();
		bundle.initialPut(caller);

		// Get graph.
		Workspace workspace = TestClassUtils.fromBundle(bundle);
		CallGraph callGraph = newCallGraph(workspace);

		// Validate that the invoke-direct call in the caller resolves to the private method defined in the same class.
		ClassMethodsContainer callerContainer = callGraph.getClassMethodsContainer(caller);
		MethodVertex callVertex = callerContainer.getVertex("call", "()V");
		MethodVertex calleeVertex = callerContainer.getVertex("callee", "()V");
		assertNotNull(callVertex, "Missing Android direct caller vertex");
		assertNotNull(calleeVertex, "Missing Android direct callee vertex");
		assertEquals(1, callVertex.getCalls().size(), "Expected Android direct call to resolve one target");
		assertTrue(callVertex.getCalls().contains(calleeVertex), "Expected Android direct call to link to private callee");
		assertTrue(calleeVertex.getCallers().contains(callVertex), "Expected Android direct callee to know its caller");
	}

	@Test
	@Timeout(10)
	void testAndroidSuperCallRelation() {
		String parentName = "demo/AndroidSuperParent";
		AndroidClassInfo parent = newAndroidSuperParent(parentName);
		AndroidClassInfo caller = newAndroidSuperCaller("demo/AndroidSuperCaller", parentName);
		BasicAndroidClassBundle bundle = new BasicAndroidClassBundle();
		bundle.initialPut(parent);
		bundle.initialPut(caller);

		// Get graph.
		Workspace workspace = TestClassUtils.fromBundle(bundle);
		CallGraph callGraph = newCallGraph(workspace);

		// Validate that the invoke-super call in the caller resolves to the method defined in the parent class.
		MethodVertex callVertex = callGraph.getClassMethodsContainer(caller).getVertex("call", "()V");
		MethodVertex calleeVertex = callGraph.getClassMethodsContainer(parent).getVertex("callee", "()V");
		assertNotNull(callVertex, "Missing Android super caller vertex");
		assertNotNull(calleeVertex, "Missing Android super callee vertex");
		assertEquals(1, callVertex.getCalls().size(), "Expected Android invoke-super to resolve one target");
		assertTrue(callVertex.getCalls().contains(calleeVertex), "Expected Android invoke-super to link to parent callee");
		assertTrue(calleeVertex.getCallers().contains(callVertex), "Expected Android super callee to know its caller");
	}

	@Test
	@Timeout(10)
	void testAndroidToJvmCallRelation() {
		// class AndroidCaller {
		//     public static void call() {
		//         JvmTarget.callee();
		//     }
		// }
		// class JvmTarget {
		//     public static void callee() {}
		// }
		AndroidClassInfo caller = newAndroidCaller("mixed/AndroidCaller", "mixed/JvmTarget");
		JvmClassInfo target = newJvmCallee("mixed/JvmTarget");
		BasicAndroidClassBundle androidBundle = new BasicAndroidClassBundle();
		BasicJvmClassBundle jvmBundle = new BasicJvmClassBundle();
		androidBundle.initialPut(caller);
		jvmBundle.initialPut(target);

		// Get graph
		Workspace workspace = new BasicWorkspace(new WorkspaceResourceBuilder()
				.withJvmClassBundle(jvmBundle)
				.withAndroidClassBundles(Map.of("classes.dex", androidBundle))
				.build(), List.of(), false);
		CallGraph callGraph = newCallGraph(workspace);

		// Calls between Android and JVM classes should resolve just like normal calls.
		MethodVertex callVertex = callGraph.getClassMethodsContainer(caller).getVertex("call", "()V");
		MethodVertex calleeVertex = callGraph.getClassMethodsContainer(target).getVertex("callee", "()V");
		assertNotNull(callVertex, "Missing mixed Android caller vertex");
		assertNotNull(calleeVertex, "Missing mixed JVM callee vertex");
		assertEquals(1, callVertex.getCalls().size(), "Expected Android caller to resolve mixed-format call");
		assertTrue(callVertex.getCalls().contains(calleeVertex), "Expected Android caller to resolve JVM callee");
		assertTrue(calleeVertex.getCallers().contains(callVertex), "Expected JVM callee to track Android caller");
	}

	/**
	 * @param callerType
	 * 		Internal name of the caller class.
	 * @param calleeType
	 * 		Internal name of the callee class.
	 *
	 * @return Android class with a single static method "call" that calls the static "callee" method of the specified callee class.
	 */
	@Nonnull
	private static AndroidClassInfo newAndroidCaller(@Nonnull String callerType, @Nonnull String calleeType) {
		return TestDexUtils.newAndroidClass(callerType, TestDexUtils.newDexMethod("call", "()V", ACC_PUBLIC | ACC_STATIC, code -> code
				.arguments(0, 0)
				.registers(0)
				.invoke(Invoke.STATIC, Types.instanceTypeFromInternalName(calleeType), "callee", Types.methodTypeFromDescriptor("()V"))
				.return_void()
		));
	}

	/**
	 * @param callerType
	 * 		Internal name of the caller class.
	 *
	 * @return Android class with a single public method "call" that calls a private method "callee" in the same class using invoke-direct.
	 */
	@Nonnull
	private static AndroidClassInfo newAndroidDirectCaller(@Nonnull String callerType) {
		return TestDexUtils.newAndroidClass(callerType,
				TestDexUtils.newDexMethod("call", "()V", ACC_PUBLIC, code -> code
						.arguments(1, 1)
						.registers(1)
						.invoke(Invoke.DIRECT, Types.instanceTypeFromInternalName(callerType), "callee",
								Types.methodTypeFromDescriptor("()V"), 0)
						.return_void()
				),
				TestDexUtils.newDexMethod("callee", "()V", ACC_PRIVATE, code -> code
						.arguments(1, 0)
						.registers(1)
						.return_void()
				)
		);
	}

	/**
	 * @param parentType
	 * 		Internal name of the parent class.
	 *
	 * @return Android class with a single public method "callee" that will be called by a child class using invoke-super.
	 */
	@Nonnull
	private static AndroidClassInfo newAndroidSuperParent(@Nonnull String parentType) {
		return TestDexUtils.newAndroidClass(parentType, TestDexUtils.newDexMethod("callee", "()V", ACC_PUBLIC, code -> code
				.arguments(1, 0)
				.registers(1)
				.return_void()
		));
	}

	/**
	 * @param callerType
	 * 		Internal name of the caller class.
	 * @param parentType
	 * 		Internal name of the parent class.
	 *
	 * @return Android class that extends the specified parent and has a single public method "call"
	 * that calls the parent's "callee" method using invoke-super.
	 */
	@Nonnull
	private static AndroidClassInfo newAndroidSuperCaller(@Nonnull String callerType, @Nonnull String parentType) {
		return TestDexUtils.newAndroidClass(callerType,
				definition -> definition.setSuperClass(Types.instanceTypeFromInternalName(parentType)),
				TestDexUtils.newDexMethod("call", "()V", ACC_PUBLIC, code -> code
						.arguments(1, 1)
						.registers(1)
						.invoke(Invoke.SUPER, Types.instanceTypeFromInternalName(parentType), "callee",
								Types.methodTypeFromDescriptor("()V"), 0)
						.return_void()
				)
		);
	}

	/**
	 * @param calleeType
	 * 		Internal name of the callee class.
	 *
	 * @return Android class with a single static method "callee" that will be called.
	 *
	 * @see #newJvmCallee(String)
	 */
	@Nonnull
	private static AndroidClassInfo newAndroidCallee(@Nonnull String calleeType) {
		return TestDexUtils.newAndroidClass(calleeType, TestDexUtils.newDexMethod("callee", "()V", ACC_PUBLIC | ACC_STATIC, code -> code
				.arguments(0, 0)
				.registers(0)
				.return_void()
		));
	}

	/**
	 * @param className
	 * 		Internal name of the class to create.
	 *
	 * @return Jvm class with the specified name and a single static method "callee" that will be called.
	 *
	 * @see #newAndroidCallee(String)
	 */
	@Nonnull
	private static JvmClassInfo newJvmCallee(@Nonnull String className) {
		ClassWriter writer = new ClassWriter(0);
		writer.visit(V1_8, ACC_PUBLIC, className, null, "java/lang/Object", null);
		MethodVisitor method = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "callee", "()V", null, null);
		method.visitCode();
		method.visitInsn(RETURN);
		method.visitMaxs(0, 0);
		method.visitEnd();
		writer.visitEnd();
		return new JvmClassInfoBuilder(writer.toByteArray()).build();
	}

	/**
	 * @param workspace
	 * 		Target workspace.
	 *
	 * @return Call graph for the workspace.
	 */
	@Nonnull
	static CallGraph newCallGraph(@Nonnull Workspace workspace) {
		CallGraph callGraph = new CallGraph(workspace);
		callGraph.initialize();

		// Need to wait until async population of graph contents is done.
		ObservableBoolean ready = callGraph.isReady();
		assertDoesNotThrow(() -> {
			while (!ready.getValue()) {
				Thread.sleep(100);
			}
		});

		return callGraph;
	}

	static {
		// class Foo {
		//     public void bar() {}
		// }
		ClassWriter cv = new ClassWriter(0);
		cv.visit(V1_8, ACC_PUBLIC, "Foo", null, "java/lang/Object", null);
		MethodVisitor mv = cv.visitMethod(0, "bar", "()V", null, null);
		mv.visitCode();
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		cv.visitEnd();
		fooBytes = cv.toByteArray();

		// class FooCaller {
		//     public static void call(Foo foo) {
		//         foo.bar();
		//     }
		// }
		cv = new ClassWriter(0);
		cv.visit(V1_8, ACC_PUBLIC | ACC_STATIC, "FooCaller", null, "java/lang/Object", null);
		mv = cv.visitMethod(0, "call", "(LFoo;)V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, "Foo", "bar", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 2);
		mv.visitEnd();
		cv.visitEnd();
		fooCallerBytes = cv.toByteArray();

		// interface FooInterface { void bar(); }
		cv = new ClassWriter(0);
		cv.visit(V1_8, ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT, "FooInterface", null, "java/lang/Object", null);
		mv = cv.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "bar", "()V", null, null);
		mv.visitEnd();
		cv.visitEnd();
		interfaceBytes = cv.toByteArray();

		// class FooInterfaceCaller {
		//     public static void call(FooInterface foo) {
		//         foo.bar();
		//     }
		// }
		cv = new ClassWriter(0);
		cv.visit(V1_8, ACC_PUBLIC, "FooInterfaceCaller", null, "java/lang/Object", null);
		mv = cv.visitMethod(ACC_PUBLIC | ACC_STATIC, "call", "(LFooInterface;)V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEINTERFACE, "FooInterface", "bar", "()V", true);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		cv.visitEnd();
		interfaceCallerBytes = cv.toByteArray();

		// interface SpecialInterface { default void foo() {} }
		cv = new ClassWriter(0);
		cv.visit(V1_8, ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT, "SpecialInterface", null, "java/lang/Object", null);
		mv = cv.visitMethod(ACC_PUBLIC, "foo", "()V", null, null);
		mv.visitCode();
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 1);
		mv.visitEnd();
		cv.visitEnd();
		specialInterfaceBytes = cv.toByteArray();

		// class SpecialInterfaceImpl implements SpecialInterface {}
		cv = new ClassWriter(0);
		cv.visit(V1_8, ACC_PUBLIC | ACC_ABSTRACT, "SpecialInterfaceImpl", null, "java/lang/Object",
				new String[]{"SpecialInterface"});
		mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		cv.visitEnd();
		specialInterfaceImplBytes = cv.toByteArray();

		// class SpecialInterfaceCaller extends SpecialInterfaceImpl {
		//     public void call() {
		//         <invokespecial>SpecialInterfaceImpl.foo()
		//     }
		// }
		cv = new ClassWriter(0);
		cv.visit(V1_8, ACC_PUBLIC, "SpecialInterfaceCaller", null, "SpecialInterfaceImpl", null);
		mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "SpecialInterfaceImpl", "<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		mv = cv.visitMethod(ACC_PUBLIC, "call", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "SpecialInterfaceImpl", "foo", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		cv.visitEnd();
		specialInterfaceCallerBytes = cv.toByteArray();

		// class PrivateSpecial {
		//     private void hidden() {}
		//     public void callHidden() {
		//         <invokespecial>PrivateSpecial.hidden()
		//     }
		// }
		cv = new ClassWriter(0);
		cv.visit(V1_8, ACC_PUBLIC, "PrivateSpecial", null, "java/lang/Object", null);
		mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		mv = cv.visitMethod(ACC_PRIVATE, "hidden", "()V", null, null);
		mv.visitCode();
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 1);
		mv.visitEnd();
		mv = cv.visitMethod(ACC_PUBLIC, "callHidden", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "PrivateSpecial", "hidden", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		cv.visitEnd();
		privateSpecialBytes = cv.toByteArray();
	}
}