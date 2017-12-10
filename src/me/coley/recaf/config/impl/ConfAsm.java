package me.coley.recaf.config.impl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import me.coley.recaf.config.Config;

/**
 * Options for ASM reading and writing.
 * 
 * @author Matt
 */
public class ConfAsm extends Config {
	/**
	 * Flags for reading in classes.
	 */
	public int classFlagsInput = ClassReader.EXPAND_FRAMES;
	/**
	 * Flags for writing classes.
	 */
	public int classFlagsOutput = ClassWriter.COMPUTE_FRAMES;

	public ConfAsm() {
		super("rcasm");
	}
}