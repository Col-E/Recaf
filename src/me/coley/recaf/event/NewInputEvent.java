package me.coley.recaf.event;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

import me.coley.event.Bus;
import me.coley.event.Event;
import me.coley.recaf.Input;
import me.coley.recaf.Logging;
import me.coley.recaf.util.Threads;

/**
 * Event for when a new input is loaded.
 * 
 * @author Matt
 */
public class NewInputEvent extends Event {
	private final Input input;

	public NewInputEvent(Input input) {
		this.input = input;
		if (input.input != null) {
			Logging.info("Loaded input from: " + input.input.getName());
		} else {
			Logging.info("Loaded input from instrumentation");
		}
	}

	public NewInputEvent(File file) throws IOException {
		this(new Input(file));
	}

	public NewInputEvent(Instrumentation instrumentation) throws IOException {
		this(new Input(instrumentation));
	}

	public Input get() {
		return input;
	}

	/**
	 * Multi-threaded invoke for event.
	 * 
	 * @param file
	 *            File to load.
	 */
	public static void call(File file) {
		Threads.runLaterFx(0, () -> {
			try {
				Bus.post(new NewInputEvent(file));
			} catch (Exception e) {
				Logging.error(e);
			}
		});
	}

	/**
	 * Multi-threaded invoke for event.
	 * 
	 * @param inst
	 *            Instrumented runtime.
	 */
	public static void call(Instrumentation inst) {
		Threads.runLaterFx(0, () -> {
			try {
				Bus.post(new NewInputEvent(inst));
			} catch (Exception e) {
				Logging.error(e);
			}
		});
	}
}
