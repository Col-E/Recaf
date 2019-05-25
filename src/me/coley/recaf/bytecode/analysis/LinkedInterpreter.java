package me.coley.recaf.bytecode.analysis;

import java.util.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/**
 * SourceInterpreter implementation that properly links SourceValue instructions
 * together. This allows all items that modify an item on the stack to be merged
 * into one item. With all modifying instructions in one place, a quick glance
 * can show the entire history of the value on the stack.
 * 
 * @author Matt
 */
public class LinkedInterpreter extends SourceInterpreter {
	public LinkedInterpreter() {
		super(ASM7);
	}

	@Override
	public SourceValue copyOperation(final AbstractInsnNode insn, final SourceValue value) {
		return new SourceValue(value.getSize(), of(value.insns, insn));
	}

	@Override
	public SourceValue unaryOperation(final AbstractInsnNode insn, final SourceValue value) {
		int size;
		switch (insn.getOpcode()) {
		case LNEG:
		case DNEG:
		case I2L:
		case I2D:
		case L2D:
		case F2L:
		case F2D:
		case D2L:
			size = 2;
			break;
		case GETFIELD:
			size = Type.getType(((FieldInsnNode) insn).desc).getSize();
			break;
		default:
			size = 1;
			break;
		}
		return new SourceValue(size, of(value.insns, insn));
	}

	@Override
	public SourceValue binaryOperation(final AbstractInsnNode insn, final SourceValue value1, final SourceValue value2) {
		int size;
		switch (insn.getOpcode()) {
		case LALOAD:
		case DALOAD:
		case LADD:
		case DADD:
		case LSUB:
		case DSUB:
		case LMUL:
		case DMUL:
		case LDIV:
		case DDIV:
		case LREM:
		case DREM:
		case LSHL:
		case LSHR:
		case LUSHR:
		case LAND:
		case LOR:
		case LXOR:
			size = 2;
			break;
		default:
			size = 1;
			break;
		}
		return new SourceValue(size, of(mix(value1.insns, value2.insns), insn));
	}

	@Override
	public SourceValue ternaryOperation(final AbstractInsnNode insn, final SourceValue value1, final SourceValue value2,
			final SourceValue value3) {
		return new SourceValue(1, of(mix(mix(value1.insns, value2.insns), value3.insns), insn));
	}

	@Override
	public SourceValue naryOperation(final AbstractInsnNode insn, final List<? extends SourceValue> values) {
		int size;
		int opcode = insn.getOpcode();
		if (opcode == MULTIANEWARRAY) {
			size = 1;
		} else if (opcode == INVOKEDYNAMIC) {
			size = Type.getReturnType(((InvokeDynamicInsnNode) insn).desc).getSize();
		} else {
			size = Type.getReturnType(((MethodInsnNode) insn).desc).getSize();
		}
		return new SourceValue(size, ofValue(values, insn));
	}

	@Override
	public SourceValue merge(final SourceValue value1, final SourceValue value2) {
		if (value1.insns.getClass().getSimpleName().equals("SmallSet") && value2.insns.getClass().getSimpleName().equals(
				"SmallSet")) {
			return super.merge(value1, value2);
		}
		if (value1.size != value2.size || !containsAll(value1.insns, value2.insns)) {
			Set<AbstractInsnNode> setUnion = new LinkedHashSet<>();
			setUnion.addAll(value1.insns);
			setUnion.addAll(value2.insns);
			return new SourceValue(Math.min(value1.size, value2.size), setUnion);
		}
		return value1;
	}

	private static <E> boolean containsAll(final Set<E> self, final Set<E> other) {
		if (self.size() < other.size()) {
			return false;
		}
		return self.containsAll(other);
	}

	private static Set<AbstractInsnNode> mix(Collection<AbstractInsnNode> a, Collection<AbstractInsnNode> b) {
		LinkedHashSet<AbstractInsnNode> set = new LinkedHashSet<>();
		set.addAll(a);
		set.addAll(b);
		return set;

	}

	private static Set<AbstractInsnNode> of(Collection<AbstractInsnNode> col, AbstractInsnNode insn) {
		LinkedHashSet<AbstractInsnNode> set = new LinkedHashSet<>(col);
		set.add(insn);
		return set;
	}

	private static Set<AbstractInsnNode> ofValue(Collection<? extends SourceValue> col, AbstractInsnNode insn) {
		LinkedHashSet<AbstractInsnNode> set = new LinkedHashSet<>();
		for (SourceValue value : col)
			set.addAll(value.insns);
		set.add(insn);
		return set;
	}
}
