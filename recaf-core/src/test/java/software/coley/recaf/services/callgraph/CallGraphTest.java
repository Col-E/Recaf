package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.StringConsumer;
import software.coley.recaf.test.dummy.StringConsumerUser;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for {@link CallGraph}
 */
class CallGraphTest {
	private static final byte[] fooBytes;
	private static final byte[] fooCallerBytes;

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
		assertEquals(0, callVertex.getCalls().size());
		assertEquals(1, callGraph.getUnresolvedDeclarations().get("Foo").size(), "Expected to have unresolved call to Foo.bar()");

		// Add the missing Foo class to the workspace
		workspace.getPrimaryResource().getJvmClassBundle().put(new JvmClassInfoBuilder(fooBytes).build());

		// The call to Foo.bar() should be resolved now
		assertEquals(1, callVertex.getCalls().size());
		assertEquals(0, callGraph.getUnresolvedDeclarations().size(), "Expected to have resolved unresolved call to Foo.bar()");

		// TODO: Make a similar test case, but in reverse.
		//  We probably want to prune the call-graph model when things get removed.
	}

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
		ClassWriter cv = new ClassWriter(0);
		cv.visit(V1_8, ACC_PUBLIC, "Foo", null, "java/lang/Object", null);
		MethodVisitor mv = cv.visitMethod(0, "bar", "()V", null, null);
		mv.visitCode();
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		cv.visitEnd();
		fooBytes = cv.toByteArray();

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
	}
}