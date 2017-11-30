package me.coley.recaf.event.impl;

import java.io.File;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

public class EFileOpen extends EFile {
	private final Map<String, ClassNode> classes;
	private final Map<String, byte[]> resources;

	public EFileOpen(File file, Map<String, ClassNode> classes, Map<String, byte[]> resources) {
		super(file);
		this.classes = classes;
		this.resources = resources;
	}

	/**
	 * @return Map of class names to their node representations.
	 */
	public Map<String, ClassNode> getClasses() {
		return classes;
	}

	/**
	 * @return Map of jar file entry names to their bytes.
	 */
	public Map<String, byte[]> getResources() {
		return resources;
	}

}
