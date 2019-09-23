package me.coley.recaf;

import com.google.common.primitives.Bytes;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Value;
import com.sun.jdi.request.*;
import me.coley.recaf.debug.*;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for using {@link VMWrap} implementations.
 *
 * @author Matt
 */
public class DebugTest extends Base {
	private DebuggerResource resource;
	private OutWrapper out;
	private VMWrap vm;

	@BeforeEach
	public void setup() {
		try {
			// The debugger resource is simply backed by another resource since the JDI doesn't
			// allow direct class lookups like instrumentation does.
			File file = getClasspathFile("calc.jar");
			JavaResource backing = new JarResource(file);
			// Specify main, classpath, suspend-on-launch
			vm = VMWrap.launching("Start", "-cp \"" + file.getAbsolutePath() + "\"", true);
			resource = vm.toResource(backing);
			resource.getVm().setup(out = new OutWrapper());
		} catch(Exception ex) {
			fail(ex);
		}
	}

	@Test
	public void testSuspendActuallySuspends() {
		// Prompt calculator to close
		queue(this::close);
		// Since the process is suspended it should be alive after 1 second THEN process the
		// input line above and terminate.
		sleep(1000);
		assertTrue(vm.getProcess().isAlive());
		execute();
	}

	@Test
	public void testMethodEntryEvent() {
		// Create a request to log method entries in the AddAndSub class
		boolean[] visited = { false };
		MethodEntryRequest request = vm.methodEntry(e -> visited[0] = true);
		request.addClassFilter("calc.AddAndSub");
		request.enable();
		// Send commands that should load the AddAndSub class
		queue(() -> {
			sendInput("3+3\n");
			close();
		});
		execute();
		// Assertions
		assertTrue(visited[0]);
		out.assertContains("COMPUTED: 6.0");
	}

	@Test
	public void testClassRedefinition() {
		// Intercept the preparation of the "AddAndSub" class, force it to multiply instead of add
		vm.prepare("calc.AddAndSub", e -> {
			// Replace the "DADD" instruction with "DMUL"
			byte[] code = resource.getClasses().get("calc/AddAndSub");
			// 0x63 = DADD
			// 0x39 = DSTORE
			int daddIndex = Bytes.indexOf(code, new byte[]{ 0x63, 0x39 });
			// 0x6b = DMUL
			code[daddIndex] = 0x6b;
			// Redefine the class
			try {
				assertTrue(vm.redefine("calc.AddAndSub", code));
			} catch(JdiRedefineException ex) {
				fail(ex);
			}
		}).enable();
		// Send commands that normally would yield "6.0" but now will be "9.0"
		queue(() -> {
			sendInput("3+3\n");
			close();
		});
		execute();
		// Assertions: 3+3 = 9
		out.assertContains("COMPUTED: 9.0");
	}

	@Test
	public void testInvokeStatic() {
		// Invoke a static method on the JVM.
		// - Must wait until the class is loaded, so we execute this on the prepare class event
		double[] value = { -1 };
		ClassPrepareRequest request = vm.prepare("calc.Calculator", e -> {
			try {
				Value v = vm.invokeStatic("calc.Calculator", "evaluate", "(Ljava/lang/String;)D", "9+1");
				value[0] = ((DoubleValue) v).value();
			} catch(JdiInvokeException ex) {
				fail(ex);
			}
		});
		// This line is VERY important when calling "vm.invokeStatic" in this context
		// - Without it, the target VM will hang forever
		request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		request.enable();
		// Run / finish calculator. This should cause the Calculator class to load.
		queue(() -> {
			sendInput("0\n");
			close();
		});
		execute();
		// Assertions
		assertEquals(10.0, value[0]);
	}

	// ==================================================================================== //

	/**
	 * Start the debugged process.
	 */
	private void execute() {
		vm.start();
	}

	/**
	 * @param action
	 * 		Action to run.
	 */
	private void queue(Runnable action) {
		new Thread(action).start();
	}

	/**
	 * Prompt the calculator to end.
	 */
	private void close() {
		sendInput("\n");
	}

	/**
	 * @param text
	 * 		Text to send to the debug process's std-in.
	 */
	private void sendInput(String text) {
		try {
			OutputStream out = vm.getProcess().getOutputStream();
			out.write(text.getBytes(StandardCharsets.UTF_8));
			out.flush();
		} catch(IOException ex) {
			fail(ex);
		}
	}

	/**
	 * Standard sleeping.
	 *
	 * @param l
	 * 		Time to sleep.
	 */
	private static void sleep(long l) {
		try {Thread.sleep(l);} catch(Exception ex) {}
	}

	/**
	 * PrintStream testing wrapper.
	 */
	private static class OutWrapper extends PrintStream {
		private List<String> lines = new ArrayList<>();

		OutWrapper() {
			super(System.out);
		}

		@Override
		public void println(String s) {
			super.println(s);
			lines.add(s);
		}

		void assertContains(String expected) {
			if (lines.isEmpty())
				throw new IllegalStateException("No output has been recorded! " +
						"Perhaps the process ended before its output buffer was copied?");
			assertTrue(lines.stream()
					.flatMap(line -> Arrays.stream(StringUtil.splitNewline(line)))
					.anyMatch(line -> line.contains(expected)));
		}
	}
}
