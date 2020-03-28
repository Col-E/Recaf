package me.coley.recaf;

import me.coley.recaf.control.Controller;
import me.coley.recaf.control.headless.HeadlessController;
import me.coley.recaf.decompile.cfr.CfrDecompiler;
import me.coley.recaf.decompile.fernflower.FernFlowerDecompiler;
import me.coley.recaf.decompile.procyon.ProcyonDecompiler;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.lang.reflect.Field;

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

	private static Controller setupController(JavaResource resource) throws IOException {
		Controller controller = new HeadlessController(null, null);
		controller.setWorkspace(new Workspace(resource));
		controller.config().initialize();
		return controller;
	}

	private static void removeController() {
		try {
			// In "Recaf.java" we prevent setting the controller twice...
			// I swear I'm taking crazy pills, because the surefire config should be isolating tests...
			// That means each test should get a separate JVM. But clearly something is wrong.
			Field f = Recaf.class.getDeclaredField("currentController");
			f.setAccessible(true);
			f.set(null, null);
		} catch(Exception ex) {
			fail("Failed to reset");
		}
	}

	// TODO: Test for options working by decompiling a synthetic member with differing options
}
