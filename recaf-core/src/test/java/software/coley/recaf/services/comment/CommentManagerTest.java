package software.coley.recaf.services.comment;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingApplierService;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.ClassWithFieldsAndMethods;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CommentManager}
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentManagerTest extends TestBase {
	static CommentManager commentManager;
	static CommentManagerConfig commentManagerConfig;
	static DecompilerManager decompilerManager;
	static MappingApplierService mappingApplierService;
	static JvmClassInfo classToDecompile;
	static Workspace workspace;

	@BeforeAll
	static void setup() throws IOException {
		// Setup workspace
		classToDecompile = TestClassUtils.fromRuntimeClass(ClassWithFieldsAndMethods.class);
		workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(classToDecompile));
		recaf.get(WorkspaceManager.class).setCurrent(workspace);

		// Grab services
		commentManager = recaf.get(CommentManager.class);
		commentManagerConfig = recaf.get(CommentManagerConfig.class);
		decompilerManager = recaf.get(DecompilerManager.class);
		mappingApplierService = recaf.get(MappingApplierService.class);
	}

	@Test
	@Order(1)
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

	@Test
	@Order(2)
	void testCommentsGetMigratedAfterRemapping() {
		ClassPathNode preMappingPath = workspace.findJvmClass(classToDecompile.getName());
		assertNotNull(preMappingPath);

		// Generate some mappings for the documented class (applied in the first test)
		String mappedClassName = "Foo";
		IntermediateMappings mappings = new IntermediateMappings();
		mappings.addClass(classToDecompile.getName(), mappedClassName);
		mappings.addField(classToDecompile.getName(), "I", "CONST_INT", "BAR");
		mappings.addMethod(classToDecompile.getName(), "()V", "methodWithLocalVariables", "fizz");

		// Apply the mappings
		MappingResults results = mappingApplierService.inCurrentWorkspace().applyToPrimaryResource(mappings);
		ClassPathNode postMappingPath = results.getPostMappingPath(classToDecompile.getName());
		assertNotNull(postMappingPath, "Post-mapping path does not exist in mapping results");
		results.apply();

		// Validate the old mappings are migrated.
		WorkspaceComments workspaceComments = commentManager.getOrCreateWorkspaceComments(workspace);
		assertNull(workspaceComments.getClassComments(preMappingPath), "Old comment container still exists");
		ClassComments newClassComments = workspaceComments.getClassComments(postMappingPath);
		assertNotNull(newClassComments, "New comment container does not exist");
		assertNotNull(newClassComments.getClassComment(), "Missing class comment");
		assertNotNull(newClassComments.getFieldComment("BAR", "I"), "Missing field comment");
		assertNotNull(newClassComments.getMethodComment("fizz", "()V"), "Missing method comment");
	}
}