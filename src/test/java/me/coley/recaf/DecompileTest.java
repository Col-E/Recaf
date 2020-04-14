package me.coley.recaf;

import me.coley.recaf.control.Controller;
import me.coley.recaf.decompile.cfr.CfrDecompiler;
import me.coley.recaf.decompile.fernflower.FernFlowerDecompiler;
import me.coley.recaf.decompile.procyon.ProcyonDecompiler;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static me.coley.recaf.util.TestUtils.*;

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
				controller = setupController(resource);
			} catch(IOException ex) {
				fail(ex);
			}
		}

		@AfterEach
		public void shutdown() {
			removeController();
		}

		@Test
		public void testFernFlower() {
			FernFlowerDecompiler decompiler = new FernFlowerDecompiler(controller);
			for (String name : controller.getWorkspace().getPrimaryClassNames()) {
				String decomp = decompiler.decompile(name);
				assertNotNull(decomp);
			}
		}

		@Test
		public void testCfr() {
			CfrDecompiler decompiler = new CfrDecompiler(controller);
			for (String name : controller.getWorkspace().getPrimaryClassNames()) {
				String decomp = decompiler.decompile(name);
				assertNotNull(decomp);
			}
		}

		@Test
		public void testProcyon() {
			ProcyonDecompiler decompiler = new ProcyonDecompiler(controller);
			for (String name : controller.getWorkspace().getPrimaryClassNames()) {
				String decomp = decompiler.decompile(name);
				assertNotNull(decomp);
			}
		}
	}

	@Nested
	public class FernFlower {
		@AfterEach
		public void shutdown() {
			removeController();
		}

		@Test
		public void testNamedInner() {
			try {
				JavaResource resource = new JarResource(getClasspathFile("InnerTest.jar"));
				resource.getClasses();
				resource.getFiles();
				controller = setupController(resource);
				FernFlowerDecompiler decompiler = new FernFlowerDecompiler(controller);
				String decomp = decompiler.decompile("Host$InnerMember");
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
				controller = setupController(resource);
				FernFlowerDecompiler decompiler = new FernFlowerDecompiler(controller);
				String decomp = decompiler.decompile("Host$1");
				assertNotNull(decomp);
				assertFalse(decomp.trim().isEmpty());
			} catch(IOException ex) {
				fail(ex);
			}
		}
	}

	// TODO: Test for options working by decompiling a synthetic member with differing options
}
