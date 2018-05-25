package me.coley.recaf.bytecode.search;

import java.util.Arrays;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.Ordering;

import me.coley.recaf.Logging;

public class Result implements Comparable<Result> {
	private final ResultType type;
	private final ClassNode cn;
	private FieldNode fn;
	private MethodNode mn;
	private AbstractInsnNode ain;
	/**
	 * Text to display for dummy result entries.
	 */
	private String text;

	private Result(ResultType type, ClassNode cn) {
		this.type = type;
		this.cn = cn;
	}

	private Result(String text) {
		this(ResultType.EMPTY, null);
		this.text = text;
	}

	private Result(ClassNode cn, FieldNode fn) {
		this(ResultType.FIELD, cn);
		this.fn = fn;
	}

	private Result(ClassNode cn, MethodNode mn) {
		this(ResultType.METHOD, cn);
		this.mn = mn;
	}

	private Result(ClassNode cn, MethodNode mn, AbstractInsnNode ain) {
		this(ResultType.OPCODE, cn);
		this.mn = mn;
		this.ain = ain;
	}

	public ResultType getType() {
		return type;
	}

	public ClassNode getCn() {
		return cn;
	}

	public FieldNode getFn() {
		return fn;
	}

	public MethodNode getMn() {
		return mn;
	}

	public AbstractInsnNode getAin() {
		return ain;
	}

	/**
	 * @return Text to display for dummy result entries.
	 */
	public String getText() {
		return text;
	}

	@Override
	public int compareTo(Result res) {
		// Ensure packages come before any other type.
		if (type == ResultType.EMPTY && res.type != ResultType.EMPTY) {
			return -1;
		} else if (type != ResultType.EMPTY && res.type == ResultType.EMPTY) {
			return 1;
		}
		// sort opcode by their index
		if (type == res.type && type == ResultType.OPCODE) {
			return Integer.compare(mn.instructions.indexOf(ain), mn.instructions.indexOf(res.ain));
		}
		// Sort alpha-numeric
		return Ordering.natural()
                .compare(
                		Arrays.toString(getParts()), 
                		Arrays.toString(res.getParts()));
	}

	public String[] getParts() {
		switch (type) {
		case EMPTY:
			return text.split("/");
		case FIELD:
			return (cn.name + "/" + fn.name).split("/");
		case METHOD:
			return (cn.name + "/" + mn.name).split("/");
		case OPCODE:
			return (cn.name + "/" + mn.name + "/" + mn.instructions.indexOf(ain)).split("/");
		case TYPE:
			return cn.name.split("/");
		}
		return new String[] { "" };
	}

	public static Result empty(String text) {
		return new Result(text);
	}

	public static Result type(ClassNode cn) {
		Logging.trace("Type: " + cn.name);
		return new Result(ResultType.TYPE, cn);
	}

	public static Result field(ClassNode cn, FieldNode fn) {
		Logging.trace("Field: " + cn.name + " . " + fn.name);
		return new Result(cn, fn);
	}

	public static Result method(ClassNode cn, MethodNode mn) {
		Logging.trace("Method: " + cn.name + " . " + mn.name);
		return new Result(cn, mn);
	}

	public static Result opcode(ClassNode cn, MethodNode mn, AbstractInsnNode ain) {
		Logging.trace("Opcode: " + cn.name + " . " + mn.name + " " + ain.getClass().getSimpleName());
		return new Result(cn, mn, ain);
	}
}