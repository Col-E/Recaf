package me.coley.recaf.event.impl;

import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.event.Event;

/**
 * Created when: User loads recaf as java-agent.
 * 
 * @author Matt
 */
public class EAgentOpen extends Event {
	private final Map<String, ClassNode> classes;

	public EAgentOpen(Map<String, ClassNode> classes) {
		this.classes = classes;
	}

	/**
	 * @return Map of class names to their node representations.
	 */
	public Map<String, ClassNode> getClasses() {
		return classes;
	}

}
