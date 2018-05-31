package me.coley.recaf.event;

import javafx.application.Application.Parameters;
import me.coley.event.Event;

/**
 * Event for when the main stage is populated and shown.
 * 
 * @author Matt
 */
public class UiInitEvent extends Event {
	private final Parameters parameters;

	public UiInitEvent(Parameters parameters) {
		this.parameters = parameters;
	}

	/**
	 * @return  JavaFX wrapper of main arguments.
	 */
	public Parameters getLaunchParameters() {
		return parameters;
	}
}
