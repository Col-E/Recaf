package me.coley.recaf.config;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class AsmConfig extends Config {
	/**
	 * Flags for reading in classes.
	 */
	public int classFlagsInput = ClassReader.EXPAND_FRAMES;
	/**
	 * Flags for writing classes.
	 */
	public int classFlagsOutput = ClassWriter.COMPUTE_FRAMES;
	
	public AsmConfig() {
		super("rcasm");
	}
}
