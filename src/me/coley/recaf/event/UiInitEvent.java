package me.coley.recaf.event;

import javafx.application.Application.Parameters;
import me.coley.event.Event;

public class UiInitEvent extends Event {
	private final Parameters parameters;

	public UiInitEvent(Parameters parameters) {
		this.parameters = parameters;
	}
	
	public Parameters getLaunchParameters() {
		return parameters;
	}
}
