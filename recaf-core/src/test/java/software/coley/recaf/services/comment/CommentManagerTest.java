package software.coley.recaf.services.comment;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.ClassWithFieldsAndMethods;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CommentManager}
 */
class CommentManagerTest extends TestBase {
	static CommentManager commentManager;
	static CommentManagerConfig commentManagerConfig;
	static DecompilerManager decompilerManager;
	static JvmClassInfo classToDecompile;
	static Workspace workspace;

	@BeforeAll
	static void setup() throws IOException {
		commentManager = recaf.get(CommentManager.class);
		commentManagerConfig = recaf.get(CommentManagerConfig.class);
		decompilerManager = recaf.get(DecompilerManager.class);
		classToDecompile = TestClassUtils.fromRuntimeClass(ClassWithFieldsAndMethods.class);
		workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(classToDecompile));
	}

	@Test
	void testCommentsInsertedIntoDecompilation() {
		ClassPathNode path = workspace.findClass(classToDecompile.getName());
		assertNotNull(path, "Failed to find class in workspace");

		WorkspaceComments workspaceComments = commentManager.getOrCreateWorkspaceComments(workspace);
		ClassComments classComments = workspaceComments.getOrCreateClassComments(path);
		classComments.setClassComment("Class comment with too many words to be put on a single line, which should " +
				"trigger the automatic word wrapping so that the full contents of this message are legible " +
				"without having to scroll to the right, which is annoying");
		classComments.setFieldComment("CONST_INT", "I", "Field comment\nThis is a constant value.");
		classComments.setMethodComment("methodWithLocalVariables", "()V", "Method comment");

		// Validate that decompiling the class inserts the comments
		try {
			commentManagerConfig.getWordWrappingLimit().setValue(100);
			DecompileResult result = decompilerManager.decompile(workspace, classToDecompile).get();
			String text = result.getText();
			assertNotNull(text, "Decompile failed");
			assertTrue(text.contains("""
					/**
					 * Class comment with too many words to be put on a single line, which should trigger the automatic
					 * word wrapping so that the full contents of this message are legible without having to scroll to the
					 * right, which is annoying
					 */
					"""), "Expected class comment to exist and be line wrapped (100)");
			assertTrue(text.contains("/** Method comment */"), "Expected single line method comment");
			assertTrue(text.contains("""
					    /**
					     * Field comment
					     * This is a constant value.
					     */
					"""), "Expected multi-line indented field comment");
		} catch (Exception ex) {
			fail(ex);
		}
	}
}