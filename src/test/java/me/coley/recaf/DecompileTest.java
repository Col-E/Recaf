package me.coley.recaf;

import me.coley.recaf.control.Controller;
import me.coley.recaf.control.headless.HeadlessController;
import me.coley.recaf.decompile.cfr.CfrDecompiler;
import me.coley.recaf.decompile.fernflower.FernFlowerDecompiler;
import me.coley.recaf.decompile.procyon.ProcyonDecompiler;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Decompiler implementation tests.
 *
 * @author Matt
 */
public class DecompileTest extends Base {
	private Controller controller;

	@Nested
	public class Basic {
		@BeforeEach
		public void setup() {
			try {
				JavaResource resource = new JarResource(getClasspathFile("inherit.jar"));
				resource.getClasses();
				resource.getFiles();
				controller = DecompileTest.setup(resource);
			} catch(IOException ex) {
				fail(ex);
			}
		}

		@Test
		public void testFernFlower() {
			FernFlowerDecompiler decompiler = new FernFlowerDecompiler();
			for (String name : controller.getWorkspace().getPrimaryClassNames()) {
				String decomp = decompiler.decompile(controller, name);
				assertNotNull(decomp);
			}
		}

		@Test
		public void testCfr() {
			CfrDecompiler decompiler = new CfrDecompiler();
			for (String name : controller.getWorkspace().getPrimaryClassNames()) {
				String decomp = decompiler.decompile(controller, name);
				assertNotNull(decomp);
			}
		}

		@Test
		public void testProcyon() {
			ProcyonDecompiler decompiler = new ProcyonDecompiler();
			for (String name : controller.getWorkspace().getPrimaryClassNames()) {
				String decomp = decompiler.decompile(controller, name);
				assertNotNull(decomp);
			}
		}
	}

	@Nested
	public class FernFlower {
		@Test
		public void testNamedInner() {
			try {
				JavaResource resource = new JarResource(getClasspathFile("InnerTest.jar"));
				resource.getClasses();
				resource.getFiles();
				controller = DecompileTest.setup(resource);
				FernFlowerDecompiler decompiler = new FernFlowerDecompiler();
				String decomp = decompiler.decompile(controller, "Host$InnerMember");
				assertNotNull(decomp);
				assertFalse(decomp.trim().isEmpty());
			} catch(IOException ex) {
				fail(ex);
			}
		}

		@Test
		public void testAnonymousInner() {
			try {
				JavaResource resource = new JarResource(getClasspathFile("InnerTest.jar"));
				resource.getClasses();
				resource.getFiles();
				controller = DecompileTest.setup(resource);
				FernFlowerDecompiler decompiler = new FernFlowerDecompiler();
				String decomp = decompiler.decompile(controller, "Host$1");
				assertNotNull(decomp);
				assertFalse(decomp.trim().isEmpty());
			} catch(IOException ex) {
				fail(ex);
			}
		}
	}

	private static Controller setup(JavaResource resource) {
		Controller controller = new HeadlessController(null, null);
		controller.setWorkspace(new Workspace(resource));
		return controller;
	}

	// TODO: Test for options working by decompiling a synthetic member with differing options
}
