package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.util.OpcodeUtil;
import org.objectweb.asm.Opcodes;

/**
 * Handle tag AST.
 *
 * @author Matt
 */
public class TagAST extends AST {
	private final String name;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param name
	 * 		Tag display name.
	 */
	public TagAST(int line, int start, String name) {
		super(line, start);
		this.name = name;
	}

	/**
	 * @return Opcode display name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Opcode value.
	 */
	public int getTag() {
		return OpcodeUtil.nameToTag(getName());
	}

	/**
	 * @return {@code true} if the tag is for a field reference.
	 */
	public boolean isField() {
		int tag = getTag();
		return tag >= Opcodes.H_GETFIELD && tag <= Opcodes.H_PUTSTATIC;
	}

	/**
	 * @return {@code true} if the tag is for a method reference.
	 */
	public boolean isMethod() {
		return !isField();
	}

	@Override
	public String print() {
		return name;
	}
}
