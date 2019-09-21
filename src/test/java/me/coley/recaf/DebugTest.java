package me.coley.recaf;

import com.sun.jdi.request.MethodEntryRequest;
import me.coley.recaf.debug.VMWrap;
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
		queue(() -> {
			// Send empty line, will prompt calculator to close
			sendInput("\n");
		});
		// Since the process is suspended it should be alive after 1 second THEN process the
		// input line above and terminate.
		sleep(1000);
		assertTrue(vm.getProcess().isAlive());
		execute();
	}

	@Test
	public void testDebugCalculatorCanDoSimpleAdditionAndThenExitsWhenExpected() {
		queue(() -> {
			// Send a basic math equation
			sendInput("1+1\n");
			// An empty line should terminate the program
			sendInput("\n");
		});
		execute();
		// Assertions
		out.assertContains("COMPUTED: 2.0");
	}

	@Test
	public void testMethodEntryEvent() {
		// Create a request to log method entries in the AddAndSub class
		boolean[] visited = { false };
		MethodEntryRequest request = vm.methodEntry(e -> {
			visited[0] = true;
		});
		request.addClassFilter("calc.AddAndSub");
		request.enable();
		// Send commands that should load the AddAndSub class
		queue(() -> {
			sendInput("1+1\n");
			sendInput("\n");
		});
		execute();
		// Assertions
		assertTrue(visited[0]);
	}

	// TODO: Create tests for things like breakpoint
	//  - Requires more VMWrap implementation

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
	 * Wait for the debugged process to end.
	 *
	 * @param timeout
	 * 		Time until giving up/failing.
	 */
	private void waitEnd(long timeout) {
		long start = System.currentTimeMillis();
		while(vm.getProcess().isAlive()) {
			sleep(100);
			if(System.currentTimeMillis() - start > timeout)
				fail("Exceeded timeout waiting for debug process to terminate.");
		}
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
			sleep(100);
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
