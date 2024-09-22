package software.coley.recaf.services.workspace.patch;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.error.Error;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.StubClassInfo;
import software.coley.recaf.info.StubFileInfo;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.workspace.patch.model.WorkspacePatch;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.util.visitors.MethodNoopingVisitor;
import software.coley.recaf.util.visitors.MethodPredicate;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

import java.util.List;
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
		assertTrue(patchApplier.apply(patch, failOnErrors()));

		// Validate the patch was applied
		JvmClassInfo patchedClassInfo = classes.get(classKey);
		assertNotSame(initialClass, patchedClassInfo, "Class bundle post-patch yielded initial class state");
	}

	@Test
	void testFile_textDiff() throws PatchGenerationException {
		TextFileInfo textFile = new StubFileInfo("foo.txt").withText("""
				one
				two
				three
				""");
		TextFileInfo textFileAlt = new StubFileInfo(textFile.getName()).withText("""
				one
				five
				three
				""");
		FileBundle fileInfos = TestClassUtils.fromFiles(textFile);
		Workspace workspace = TestClassUtils.fromBundle(fileInfos);

		// Modify the file
		fileInfos.put(textFileAlt);

		// Build the patch.
		WorkspacePatch patch = patchProvider.createPatch(workspace);

		// Assert serialization/deserialization doesn't result in breakage.
		String serialized = patchProvider.serializePatch(patch);
		WorkspacePatch deserializePatch = patchProvider.deserializePatch(workspace, serialized);
		assertEquals(patch, deserializePatch);

		// Undo the change.
		String fileKey = textFile.getName();
		fileInfos.decrementHistory(fileKey);
		TextFileInfo revertedTextFile = fileInfos.get(fileKey).asTextFile();
		assertEquals(textFile.getText(), revertedTextFile.getText(), "Revert failed");

		// Apply the patch
		assertTrue(patchApplier.apply(patch, failOnErrors()));

		// Validate the patch was applied
		TextFileInfo patchedTextFile = fileInfos.get(fileKey).asTextFile();
		assertNotEquals(textFile, patchedTextFile, "File bundle post-patch yielded initial file state");
		assertEquals(textFileAlt, patchedTextFile, "File bundle post-patch yielded unexpected state");
	}

	@Test
	void testRemove_file() throws Throwable {
		StubFileInfo bar = new StubFileInfo("bar");
		FileBundle fileBundle = TestClassUtils.fromFiles(new StubFileInfo("foo"), bar, new StubFileInfo("fizz"));
		Workspace workspace = TestClassUtils.fromBundle(fileBundle);

		// Remove 'bar'
		fileBundle.remove(bar.getName());

		// Build the patch.
		WorkspacePatch patch = patchProvider.createPatch(workspace);

		// Assert the patch has the removal
		assertEquals(1, patch.removals().size(), "Expected 1 file removal");

		// Assert serialization/deserialization doesn't result in breakage.
		String serialized = patchProvider.serializePatch(patch);
		WorkspacePatch deserializePatch = patchProvider.deserializePatch(workspace, serialized);
		assertEquals(patch, deserializePatch);

		// Undo the change.
		fileBundle.put(bar);

		// Apply the patch
		assertTrue(patchApplier.apply(patch, failOnErrors()));

		// Validate the patch was applied
		assertNull(fileBundle.get(bar.getName()), "File bundle post-patch did not remove 'bar'");
	}

	@Test
	void testRemove_jvmClass() throws Throwable {
		JvmClassInfo bar = new StubClassInfo("bar").asJvmClass();
		JvmClassBundle classBundle = TestClassUtils.fromClasses(new StubClassInfo("foo").asJvmClass(), bar, new StubClassInfo("fizz").asJvmClass());
		Workspace workspace = TestClassUtils.fromBundle(classBundle);

		// Remove 'bar'
		classBundle.remove(bar.getName());

		// Build the patch.
		WorkspacePatch patch = patchProvider.createPatch(workspace);

		// Assert the patch has the removal
		assertEquals(1, patch.removals().size(), "Expected 1 class removal");

		// Assert serialization/deserialization doesn't result in breakage.
		String serialized = patchProvider.serializePatch(patch);
		WorkspacePatch deserializePatch = patchProvider.deserializePatch(workspace, serialized);
		assertEquals(patch, deserializePatch);

		// Undo the change.
		classBundle.put(bar);

		// Apply the patch
		assertTrue(patchApplier.apply(patch, failOnErrors()));

		// Validate the patch was applied
		assertNull(classBundle.get(bar.getName()), "Class bundle post-patch did not remove 'bar'");
	}

	@Nonnull
	private PatchFeedback failOnErrors() {
		return new PatchFeedback() {
			@Override
			public void onAssemblerErrorsObserved(@Nonnull List<Error> errors) {
				fail("Errors encountered applying patch: " + errors.stream().map(Error::getMessage).collect(Collectors.joining(", ")));
			}

			@Override
			public void onIncompletePathObserved(@Nonnull PathNode<?> path) {
				fail("Incomplete path: " + path);
			}
		};
	}
}