package me.coley.recaf.debug;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import com.sun.tools.jdi.ProcessAttachingConnector;
import me.coley.recaf.workspace.DebuggerResource;
import me.coley.recaf.workspace.JavaResource;
import org.tinylog.Logger;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.function.Consumer;

/**
 * JDI {@link VirtualMachine} wrapper.
 *
 * @author Matt
 */
public class VMWrap {
	private static final long PRINT_THREAD_DELAY = 250L;
	private static final int CONNECTOR_TIMEOUT = 5000;
	//
	private final Map<Class<? extends Event>, Consumer<Event>> eventConsumers = new HashMap<>();
	//
	private final Multimap<String, Consumer<ClassPrepareEvent>> prepares = newMultiMap();
	private final Multimap<Location, Consumer<BreakpointEvent>> breakpoints = newMultiMap();
	private final Multimap<ThreadReference, Consumer<StepEvent>> steps = newMultiMap();
	private final Set<Consumer<MethodEntryEvent>> methodEntries = new HashSet<>();
	private final Set<Consumer<MethodExitEvent>> methodExits = new HashSet<>();
	//
	private final VirtualMachine vm;
	private boolean isLocal;

	/**
	 * @param vm
	 * 		Virtual machine to wrap.
	 */
	private VMWrap(VirtualMachine vm) {
		this.vm = vm;
		setupEventConsumers();
	}

	/**
	 * Assuming the current process is running as a debugee, connects to the current process.
	 *
	 * @return Wrapper for the current running context.
	 *
	 * @throws IOException
	 * 		Thrown if connecting to the current process failed.
	 */
	public static VMWrap current() throws IOException {
		// Get pid
		String name = ManagementFactory.getRuntimeMXBean().getName();
		String pid = name.substring(0, name.indexOf('@'));
		//
		VMWrap wrapper = process(pid);
		wrapper.isLocal = true;
		return wrapper;
	}

	/**
	 * Assuming the given process is running as a debugee, connects to the current process.
	 *
	 * @param pid Process id.
	 *
	 * @return Wrapper for the given running context.
	 *
	 * @throws IOException
	 * 		Thrown if connecting to the given process failed.
	 */
	public static VMWrap process(String pid) throws IOException {
		// Create connector to self
		ProcessAttachingConnector connector = new ProcessAttachingConnector();
		Map<String, ? extends Connector.Argument> args = connector.defaultArguments();
		args.get("pid").setValue(pid);
		args.get("timeout").setValue(String.valueOf(CONNECTOR_TIMEOUT));
		// Try to connect
		try {
			return new VMWrap(connector.attach(args));
		} catch(IllegalConnectorArgumentsException ex) {
			throw new IOException(ex);
		}
	}

	/**
	 * Starts a process with a debugger and connects to it.
	 *
	 * @param main
	 * 		Name of class containing the main method.
	 * @param options
	 * 		Launch arguments, should include the classpath.
	 *
	 * @return Wrapper for the newly created running context.
	 *
	 * @throws IOException
	 * 		Thrown if connecting to the given process failed.
	 */
	public static VMWrap launching(String main, String options) throws IOException {
		VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
		LaunchingConnector connector = vmm.defaultConnector();
		Map<String, ? extends Connector.Argument> args = connector.defaultArguments();
		args.get("options").setValue(options);
		args.get("main").setValue(main);
		try {
			return new VMWrap(connector.launch(args));
		} catch(VMStartException | IllegalConnectorArgumentsException ex) {
			throw new IOException(ex);
		}
	}

	/**
	 * @param backing
	 * 		Resource to defer to.
	 *
	 * @return Workspace resource with the current vm link.
	 */
	public DebuggerResource toResource(JavaResource backing) {
		return new DebuggerResource(this, backing);
	}

	/**
	 * Register an action for when classes are prepared.
	 *
	 * @param name
	 * 		Quantified name of the class. {@code null} to accept any class.
	 * @param action
	 * 		Action to run.
	 *
	 * @return The request.
	 */
	public ClassPrepareRequest prepare(String name, Consumer<ClassPrepareEvent> action) {
		ClassPrepareRequest request = vm.eventRequestManager().createClassPrepareRequest();
		if (name != null)
			request.addClassFilter(name);
		prepares.put(name, action);
		return request;
	}

	/**
	 * Register an action &amp; breakpoint for when a location is hit.
	 *
	 * @param location
	 * 		Location to add breakpoint to.
	 * @param action
	 * 		Action to run.
	 *
	 * @return The request.
	 */
	public BreakpointRequest breakpoint(Location location, Consumer<BreakpointEvent> action) {
		BreakpointRequest request = vm.eventRequestManager().createBreakpointRequest(location);
		breakpoints.put(location, action);
		return request;
	}

	/**
	 * Register a step on the given thread.
	 *
	 * @param thread
	 * 		The thread to intercept steps in.
	 * @param size
	 * 		Step size. <ul>
	 * 		<li>{@link StepRequest#STEP_LINE} for stepping to the next line.
	 * 		If no debug info is givem, this defaults to {@link StepRequest#STEP_MIN}</li>
	 * 		<li>{@link StepRequest#STEP_MIN} for <i>any</i> code index change.</li>
	 * 		</ul>
	 * @param depth
	 * 		Step depth.
	 * 		<ul>
	 * 		<li>{@link StepRequest#STEP_INTO} for stepping into new frames.</li>
	 * 		<li>{@link StepRequest#STEP_OVER} for stepping over new frames.</li>
	 * 		<li>{@link StepRequest#STEP_OUT} for stepping out of the current frame.</li>
	 * 		</ul>
	 * @param action
	 * 		Action to run.
	 *
	 * @return The request.
	 */
	public StepRequest step(ThreadReference thread, int size, int depth,
							Consumer<StepEvent> action) {
		StepRequest request = vm.eventRequestManager().createStepRequest(thread, size, depth);
		steps.put(thread, action);
		return request;
	}

	/**
	 * Register an action for entering methods.
	 *
	 * @param action
	 * 		Action to run.
	 *
	 * @return The request.
	 */
	public MethodEntryRequest methodEntry(Consumer<MethodEntryEvent> action) {
		MethodEntryRequest request = vm.eventRequestManager().createMethodEntryRequest();
		methodEntries.add(action);
		return request;
	}

	/**
	 * Register an action for exiting methods.
	 *
	 * @param action
	 * 		Action to run.
	 *
	 * @return The request.
	 */
	public MethodExitRequest methodExit(Consumer<MethodExitEvent> action) {
		MethodExitRequest request = vm.eventRequestManager().createMethodExitRequest();
		methodExits.add(action);
		return request;
	}

	/**
	 * Begin handling vm events.
	 *
	 * @param out
	 * 		Stream to send VM's output to. May be {@code null}.
	 * @param in
	 * 		Stream to use as input for VM's input. May be {@code null}.
	 */
	public void start(PrintStream out, InputStream in) {
		// Used to keep track of if we're still attached to the VM.
		boolean[] running = {true};
		try {
			// Start redirecting process output
			new Thread(() -> {
				try {
					InputStream pOutput = vm.process().getInputStream();
					OutputStream pInput = vm.process().getOutputStream();
					byte[] buffer = new byte[4096];
					while(running[0] && vm.process().isAlive()) {
						// Handle receiving output
						if(out != null) {
							int size = pOutput.available();
							if(size > 0) {
								int n = pOutput.read(buffer, 0, Math.min(size, buffer.length));
								out.println(new String(buffer, 0, n));
							}
						}
						// Handle feeding input
						if(in != null) {
							int size = in.available();
							if(size > 0) {
								int n = in.read(buffer, 0, Math.min(size, buffer.length));
								pInput.write(buffer, 0, n);
								pInput.flush();
							}
						}
						Thread.sleep(PRINT_THREAD_DELAY);
					}
				} catch(InterruptedException | IOException ex) {
					Logger.error(ex, "Exception occurred while processing VM IPC");
				}
			}).start();
			// Handle vm events
			eventLoop();
		} catch(VMDisconnectedException ex) {
			// Expected
			// - Stop print redirect thread
			running[0] = false;
		} catch(InterruptedException ex) {
			Logger.error(ex, "Failed processing VM event queue");
		}
	}

	private void eventLoop() throws VMDisconnectedException, InterruptedException {
		EventSet eventSet = null;
		while((eventSet = vm.eventQueue().remove()) != null) {
			for(Event event : eventSet) {
				Consumer<Event> consumer = eventConsumers.get(event.getClass());
				if(consumer != null)
					consumer.accept(event);
				vm.resume();
			}
		}
	}

	private void setupEventConsumers() {
		eventConsumers.put(ClassPrepareEvent.class, (event) -> {
			ClassPrepareEvent prepare = (ClassPrepareEvent) event;
			String key = prepare.referenceType().name();
			prepares.get(key).forEach(consumer -> consumer.accept(prepare));
		});
		eventConsumers.put(BreakpointEvent.class, (event) -> {
			BreakpointEvent breakpoint = (BreakpointEvent) event;
			Location key = breakpoint.location();
			breakpoints.get(key).forEach(consumer -> consumer.accept(breakpoint));
		});
		eventConsumers.put(StepEvent.class, (event) -> {
			StepEvent step = (StepEvent) event;
			ThreadReference key = step.thread();
			steps.get(key).forEach(consumer -> consumer.accept(step));
		});
		eventConsumers.put(MethodEntryEvent.class, (event) -> {
			MethodEntryEvent entry = (MethodEntryEvent) event;
			methodEntries.forEach(consumer -> consumer.accept(entry));
		});
		eventConsumers.put(MethodExitEvent.class, (event) -> {
			MethodExitEvent exit = (MethodExitEvent) event;
			methodExits.forEach(consumer -> consumer.accept(exit));
		});
		// TODO: map entries for:
		//  - MonitorWaitEvent
		//  - MonitorWaitedEvent
		//  - MonitorContendedEnterEvent
		//  - MonitorContendedEnteredEvent
		//  - AccessWatchpointEvent
		//  - ModificationWatchpointEvent
		//  - ExceptionEvent
		//  - ThreadStartEvent
		//  - ThreadDeathEvent
		//  - ClassUnloadEvent
		//  - VMStartEvent
		//  - VMDisconnectEvent
		//  - VMDeathEvent
	}

	private static <K, V> Multimap<K, V> newMultiMap() {
		return MultimapBuilder.hashKeys().arrayListValues().build();
	}
}
