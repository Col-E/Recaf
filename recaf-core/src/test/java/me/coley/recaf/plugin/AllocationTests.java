package me.coley.recaf.plugin;

import me.coley.recaf.cdi.RecafContainer;
import me.coley.recaf.plugin.beans.Basic;
import me.coley.recaf.plugin.beans.CdiAppScope;
import me.coley.recaf.plugin.beans.CdiWorkspaceScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AllocationTests {
	private static final RecafPluginClassAllocator allocator = new RecafPluginClassAllocator();

	@BeforeAll
	public static void setup() {
		// Need to initialize with test classes in CDI scope
		//	RecafContainer.initialize(
		//			CdiAppScope.class,
		//			CdiWorkspaceScope.class
		//	);

		RecafContainer.initialize();
	}

	@Test
	public void testNormalClassAllocation() {
		assertDoesNotThrow(() -> {
			allocator.instance(Basic.class);
		});
	}

	@Test
	public void testCdiClassAllocation() {
		assertDoesNotThrow(() -> {
			allocator.instance(CdiAppScope.class);
		});
		assertDoesNotThrow(() -> {
			allocator.instance(CdiWorkspaceScope.class);
		});
	}

	// TODO: Create a test that validates only WorkspaceScoped components can inject other WorkspaceScoped components
	//       ApplicationScoped should not be able to do so (this is to force the create/destroy lifecycle of workspace scoped beans)
}
