package software.coley.recaf.services.workspace.patch;

import me.darknet.assembler.error.Error;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.workspace.patch.model.WorkspacePatch;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.util.visitors.MethodNoopingVisitor;
import software.coley.recaf.util.visitors.MethodPredicate;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PatchProvider} and {@link PatchApplier}
 */
class PatchingTest extends TestBase {
	private static final PatchProvider patchProvider;
	private static final PatchApplier patchApplier;

	static {
		patchProvider = recaf.get(PatchProvider.class);
		patchApplier = recaf.get(PatchApplier.class);
	}

	@Test
	void testClass_methodNoop() throws Throwable {
		JvmClassInfo initialClass = TestClassUtils.fromRuntimeClass(HelloWorld.class);
		BasicJvmClassBundle classes = TestClassUtils.fromClasses(initialClass);
		Workspace workspace = TestClassUtils.fromBundle(classes);

		// Modify the class. We'll just no-op a method to make things simple.
		ClassWriter writer = new ClassWriter(0);
		MethodNoopingVisitor visitor = new MethodNoopingVisitor(writer, MethodPredicate.of(initialClass.getMethods().getLast()));
		initialClass.getClassReader().accept(visitor, 0);
		JvmClassInfo modifiedClass = initialClass.toJvmClassBuilder().adaptFrom(writer.toByteArray()).build();
		classes.put(modifiedClass);

		// Build the patch.
		WorkspacePatch patch = patchProvider.createPatch(workspace);

		// Assert serialization/deserialization doesn't result in breakage.
		String serialized = patchProvider.serializePatch(patch);
		WorkspacePatch deserializePatch = patchProvider.deserializePatch(workspace, serialized);
		assertEquals(patch, deserializePatch);

		// Undo the change.
		String classKey = initialClass.getName();
		classes.decrementHistory(classKey);
		assertArrayEquals(initialClass.getBytecode(), classes.get(classKey).getBytecode(), "Revert failed");

		// Apply the patch
		patchApplier.apply(patch, errors -> fail("Errors encountered applying patch: " + errors.stream().map(Error::getMessage).collect(Collectors.joining(", "))));

		// Validate the patch was applied
		JvmClassInfo patchedClassInfo = classes.get(initialClass.getName());
		assertNotEquals(initialClass, patchedClassInfo, "Class bundle post-patch yielded initial class state");
	}
}