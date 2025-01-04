package software.coley.recaf;

import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.coley.recaf.services.assembler.AssemblerPipelineManager;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.mapping.aggregate.AggregateMappingManager;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.workspace.model.EmptyWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Bootstrap}
 */
class BootstrapTest extends TestBase {
	@BeforeEach
	void setup() {
		workspaceManager.closeCurrent();
	}

	@AfterAll
	static void cleanup() {
		workspaceManager.closeCurrent();
	}

	@Test
	void testGetApplicationScopedInstance() {
		assertNotNull(workspaceManager, "Failed to get instance of workspace manager, which should be application-scoped");
	}

	@Test
	void testGetWorkspaceInstance() throws IOException {
		// Create workspace with single class
		JvmClassBundle classes = TestClassUtils.fromClasses(HelloWorld.class);
		Workspace workspace = TestClassUtils.fromBundle(classes);

		// Should be null since nothing is active in the workspace manager.
		// Thus, the supplier method should yield 'null'.
		Instance<Workspace> currentWorkspaceInstance = recaf.instance(Workspace.class);
		assertSame(EmptyWorkspace.get(), currentWorkspaceInstance.get(),
				"No current workspace should be set, thus empty should be provided");

		// Assign a workspace.
		workspaceManager.setCurrent(workspace);

		// Should no longer be null.
		assertEquals(workspace, currentWorkspaceInstance.get(),
				"Workspace manager should expose current workspace as dependent scoped bean");
	}

	@Test
	void testGetWorkspaceScopedInstance() {
		// Get the manager when one workspace is open.
		workspaceManager.setCurrent(EmptyWorkspace.get());
		AssemblerPipelineManager manager1 = unwrapProxy(recaf.get(AssemblerPipelineManager.class));
		AssemblerPipelineManager manager2 = unwrapProxy(recaf.get(AssemblerPipelineManager.class));
		assertSame(manager1, manager2, "Workspace-scoped values not shared during same workspace scope!");

		// Assign a new workspace.
		// The manager should be different since the prior workspace is closed.
		workspaceManager.setCurrent(EmptyWorkspace.get());
		AssemblerPipelineManager manager3 = unwrapProxy(recaf.get(AssemblerPipelineManager.class));
		AssemblerPipelineManager manager4 = unwrapProxy(recaf.get(AssemblerPipelineManager.class));
		assertSame(manager3, manager4, "Workspace-scoped values not shared during same workspace scope!");
		assertNotSame(manager1, manager3, "Workspace-scoped values are shared during different workspace scopes!");
	}
}
