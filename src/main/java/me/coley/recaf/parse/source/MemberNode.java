package me.coley.recaf.parse.source;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.Type;

/**
 * Wrapper for an ASM member node with a reference to the declaring class. <br>
 * Can be a method or field.
 * 
 * @author Matt
 */
public class MemberNode {
	private final ClassNode owner;
	private FieldNode field;
	private MethodNode method;

	public MemberNode(ClassNode owner, FieldNode field) {
		this.owner = owner;
		this.field = field;
	}

	public MemberNode(ClassNode owner, MethodNode method) {
		this.owner = owner;
		this.method = method;
	}

	public ClassNode getOwner() {
		return owner;
	}

	public FieldNode getField() {
		return field;
	}

	public MethodNode getMethod() {
		return method;
	}

	public String getName() {
		return isField() ? field.name : method.name;
	}

	public String getDesc() {
		return isField() ? field.desc : method.desc;
	}

	public boolean isField() {
		return field != null;
	}

	public boolean isMethod() {
		return method != null;
	}

	/**
	 * @return Internal name of the type.
	 */
	public String getInternalType() {
		if (isField()) {
			return Type.getType(getDesc()).getClassName().replace(".", "/");
		} else {
			return Type.getType(getDesc()).getReturnType().getClassName().replace(".", "/");
		}
	}

	/**
	 * @return Member access flags.
	 */
	public int getAccess() {
		return isField() ? field.access : method.access;
	}
}
