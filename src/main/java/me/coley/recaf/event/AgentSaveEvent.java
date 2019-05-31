package me.coley.recaf.event;

import java.lang.instrument.Instrumentation;
import java.util.Map;

import me.coley.event.Event;

/**
 * Event for intercepting bytecode to be applied to instrumentation for
 * redefinition.
 * 
 * @author Matt
 */
public class AgentSaveEvent extends Event {
	private final Instrumentation instrumentation;
	private final Map<String, byte[]> contents;

	public AgentSaveEvent(Instrumentation instrumentation, Map<String, byte[]> contents) {
		this.instrumentation = instrumentation;
		this.contents = contents;
	}

	/**
	 * @return Instrumentation instance that will do the redefinition.
	 */
	public Instrumentation getInstrumentation() {
		return instrumentation;
	}

	/**
	 * @return Files to redefine.
	 */
	public Map<String, byte[]> getContents() {
		return contents;
	}

}
