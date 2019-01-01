package me.coley.recaf.bytecode.search;

import java.util.Arrays;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.Ordering;

import me.coley.recaf.ui.FormatFactory;

public class Result implements Comparable<Result> {
	private final ResultType type;
	private final ClassNode cn;
	private FieldNode fn;
	private MethodNode mn;
	private AbstractInsnNode ain;
	/**
	 * Text to display for dummy result entries.
	 */
	private String dummyText;
	/**
	 * ToString representation of the result.
	 */
	private String strRep;

	private Result(ResultType type, ClassNode cn) {
		this.type = type;
		this.cn = cn;
	}

	private Result(String text) {
		this(ResultType.EMPTY, null);
		this.dummyText = text;
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

	@Override
	public String toString() {
		if (strRep == null) {
			StringBuilder sb = new StringBuilder();
			if (getCn() != null) {
				sb.append(getCn().name);
			}
			if (getFn() != null) {
				sb.append("." + getFn().name + " " + getFn().desc);
			} else if (getMn() != null) {
				sb.append("." + getMn().name + getMn().desc);
				if (getAin() != null) {
					sb.append(" " + FormatFactory.opcode(getAin(), getMn()).getText());
				}
			}
			strRep = sb.toString();
		}
		return strRep;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * @return Text to display for dummy result entries.
	 */
	public String getText() {
		return dummyText;
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
		return Ordering.natural().compare(Arrays.toString(getParts()), Arrays.toString(res.getParts()));
	}

	public String[] getParts() {
		switch (type) {
		case EMPTY:
			return dummyText.split("/");
		case FIELD:
			return (cn.name + "/" + fn.name).split("/");
		case METHOD:
			// Split with dummy tail value.
			// Then set the tail to the method's (name + desc)
			// This differentiates between same name methods, but with differing
			// descriptors.
			String[] method = (cn.name + "/METHOD").split("/");
			method[method.length - 1] = mn.name + mn.desc;
			return method;
		case OPCODE:
			// Same reasoning as above, but with index offset by 1.
			String[] opcode = (cn.name + "/METHOD/" + mn.instructions.indexOf(ain)).split("/");
			opcode[opcode.length - 2] = mn.name + mn.desc;
			return opcode;
		case TYPE:
			return cn.name.split("/");
		}
		return new String[] { "" };
	}

	public static Result empty(String text) {
		return new Result(text);
	}

	public static Result type(ClassNode cn) {
		return new Result(ResultType.TYPE, cn);
	}

	public static Result field(ClassNode cn, FieldNode fn) {
		return new Result(cn, fn);
	}

	public static Result method(ClassNode cn, MethodNode mn) {
		return new Result(cn, mn);
	}

	public static Result opcode(ClassNode cn, MethodNode mn, AbstractInsnNode ain) {
		return new Result(cn, mn, ain);
	}
}