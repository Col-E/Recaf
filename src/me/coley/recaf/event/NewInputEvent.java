package me.coley.recaf.event;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

import me.coley.event.Event;
import me.coley.recaf.Input;
import me.coley.recaf.Logging;

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
}
