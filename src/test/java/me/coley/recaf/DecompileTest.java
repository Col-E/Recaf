package me.coley.recaf;

import me.coley.recaf.decompile.fernflower.FernFlowerDecompiler;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class DecompileTest extends Base {
	private Workspace workspace;

	@BeforeEach
	public void setup() {
		try {
			JavaResource resource = new JarResource(getClasspathFile("inherit.jar"));
			resource.getClasses();
			resource.getResources();
			workspace = new Workspace(resource);
		} catch(IOException ex) {
			fail(ex);
		}
	}


	@Nested
	public class FernFlower {
		@Test
		public void testFernFlower() {
			FernFlowerDecompiler decompiler = new FernFlowerDecompiler();
			for (String name : workspace.getPrimaryClassNames()) {
				String decomp = decompiler.decompile(workspace, name);
				assertNotNull(decomp);
			}
		}
	}
}
