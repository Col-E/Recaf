package me.coley.recaf;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class Options {
	/**
	 * Show confirmation prompt on doing potentially dangerous things.
	 */
	public boolean classConfirmDanger = false;
	/**
	 * Show extra jump information.
	 */
	public boolean opcodeShowJumpHelp = true;
	/**
	 * Simplify descriptor displays on the opcode list.
	 */
	public boolean opcodeSimplifyDescriptors = true;
	/**
	 * Flags for reading in classes.
	 */
	public int classFlagsInput = ClassReader.EXPAND_FRAMES;
	/**
	 * Flags for writing classes.
	 */
	public int classFlagsOutput = ClassWriter.COMPUTE_FRAMES;
}
