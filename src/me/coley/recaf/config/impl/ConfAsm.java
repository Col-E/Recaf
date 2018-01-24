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

	public void checkVersion() {
		// Compiler replaces field ref's with ldc constants of field values, so
		// references to fields isn't an issue. Just the value being used in the
		// ClassWriter is. Thus we ensure we don't use a future version in-case
		// recaf is attached to a process with an outdated ASM library.
		try {
			Opcodes.class.getDeclaredField("ASM6");
			// (<= 6) --> any
			return;
		} catch (NoSuchFieldException e1) {
			try {
				Opcodes.class.getDeclaredField("ASM5");
				// (<= 5) --> any
				if (version == Opcodes.ASM6) {
					version = Opcodes.ASM5;
				}
				return;
			} catch (NoSuchFieldException e2) {
				// Assume ASM4, if this fails at runtime, we're not supporting
				// anything further back.
				//
				// (== 4) --> any
				if (version == Opcodes.ASM6 || version == Opcodes.ASM5) {
					version = Opcodes.ASM4;
				}
			}
		} catch (Exception e) {}
	}
}