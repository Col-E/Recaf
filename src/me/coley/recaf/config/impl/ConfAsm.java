package me.coley.recaf.config.impl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

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
	/**
	 * Version of ASM to use in tools. Not typically an issue unless you are
	 * using the agent functionality on a process that has a different version
	 * of ASM. For instance attaching to an ASM4 program and using ASM5 will
	 * have ASM throw an exception.
	 */
	public int version = Opcodes.ASM6;

	public ConfAsm() {
		super("rcasm");
	}
}