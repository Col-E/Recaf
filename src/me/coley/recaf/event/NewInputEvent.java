package me.coley.recaf.event;

import java.io.File;
import java.io.IOException;

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
		Logging.info(String.format("Loaded input from: '%s'", input.input.getName()));
	}

	public NewInputEvent(File file) throws IOException {
		this(new Input(file));
	}

	public Input get() {
		return input;
	}
}
