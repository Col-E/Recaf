package me.coley.recaf.bytecode.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import me.coley.recaf.Input;
import me.coley.recaf.bytecode.OpcodeUtil;

public class Search {
	public static List<Result> search(Parameter param) {
		List<Result> results = new ArrayList<>();
		Map<String, ClassNode> nodes = Input.get().getClasses();
		for (Entry<String, ClassNode> entry : nodes.entrySet()) {
			// check if entry should be skipped
			if (skip(param.getSkipList(), entry.getKey())) {
				continue;
			}
			// begin search
			ClassNode cn = entry.getValue();
			if (param.getType().equals(SearchType.DECLARATION)) {
				// check against class name.
				if (param.singleArg()) {
					if (param.validType(cn.name)) {
						results.add(Result.type(cn));
					}
					continue;
				}
				// search for matching field members
				for (FieldNode fn : cn.fields) {
					if (param.validMember(entry.getKey(), fn.name, fn.desc)) {
						results.add(Result.field(cn, fn));
					}
				}
				// search for matching method members
				for (MethodNode mn : cn.methods) {
					if (param.validMember(entry.getKey(), mn.name, mn.desc)) {
						results.add(Result.method(cn, mn));
					}
				}
				// skip to next entry. Lets the following code be less indented.
				continue;
			}
			for (MethodNode mn : cn.methods) {
				// Setup OPCODE_PATTERN with dummy values. Init if parameter
				// search type matches.
				List<String> expected = null;
				int max = mn.instructions.size();
				if (param.getType().equals(SearchType.OPCODE_PATTERN)) {
					expected = param.getArg(0);
					max = expected.size();
				}
				List<String> ops = new RollingList<>(max);
				// Iterate opcodes and check for matches of the parameter's
				// search type.
				for (AbstractInsnNode ain : mn.instructions.toArray()) {
					switch (param.getType()) {
					case OPCODE_PATTERN:
						ops.add(OpcodeUtil.opcodeToName(ain.getOpcode()));
						// check rolling-list content equal to expected. If
						// match found intended pattern.
						if (ops.equals(expected)) {
							int index = mn.instructions.indexOf(ain);
							int offset = expected.size() - 1;
							results.add(Result.opcode(cn, mn, mn.instructions.get(index - offset)));
						}
						break;
					case REFERENCE:
						switch (ain.getType()) {
						case AbstractInsnNode.FIELD_INSN:
							// check against member definition
							FieldInsnNode fin = (FieldInsnNode) ain;
							if (param.validMember(fin.owner, fin.name, fin.desc)) {
								results.add(Result.opcode(cn, mn, ain));
							}
							break;
						case AbstractInsnNode.METHOD_INSN:
							MethodInsnNode min = (MethodInsnNode) ain;
							// check against member definition
							if (param.validMember(min.owner, min.name, min.desc)) {
								results.add(Result.opcode(cn, mn, ain));
							}
							break;
						case AbstractInsnNode.TYPE_INSN:
							TypeInsnNode tin = (TypeInsnNode) ain;
							// check against type (in code, new Type) inits
							if (param.singleArg() && param.validType(tin.desc)) {
								results.add(Result.opcode(cn, mn, ain));
							}
							break;
						case AbstractInsnNode.LDC_INSN:
							LdcInsnNode ldc = (LdcInsnNode) ain;
							// check against types stored in ldc's.
							if (ldc.cst instanceof Type) {
								// TODO: Does this include the ability for LDC
								// to hold org.objectweb.asm.Handle?
								Type type = (Type) ldc.cst;
								if (type.getSort() == Type.OBJECT && param.validType(type.getClassName())) {
									results.add(Result.opcode(cn, mn, ain));
								}
							}
						default:
						}
						break;
					case STRING:
						if (ain.getType() == AbstractInsnNode.LDC_INSN) {
							// check ldc opcode's value is of type string.
							// check if string matched parameter arg.
							LdcInsnNode ldc = (LdcInsnNode) ain;
							if (ldc.cst instanceof String && param.check(0, ldc.cst.toString())) {
								results.add(Result.opcode(cn, mn, ldc));
							}
						}
						break;
					case VALUE:
						Number search = param.getArg(0);
						switch (ain.getType()) {
						case AbstractInsnNode.LDC_INSN:
							LdcInsnNode ldc = (LdcInsnNode) ain;
							if (ldc.cst instanceof Number) {
								// TODO: Can this work for ints AND floats?
								// check against ldc value
								Number value = (Number) ldc.cst;
								if (search.equals(value)) {
									results.add(Result.opcode(cn, mn, ldc));
								}
							}
							break;
						case AbstractInsnNode.INT_INSN:
							// check against int value
							IntInsnNode iin = (IntInsnNode) ain;
							if (search.equals(iin.operand)) {
								results.add(Result.opcode(cn, mn, iin));
							}
							break;
						case AbstractInsnNode.IINC_INSN:
							// check against increment value
							IincInsnNode iincn = (IincInsnNode) ain;
							if (search.equals(iincn.incr)) {
								results.add(Result.opcode(cn, mn, iincn));
							}
							break;
						}
						break;
					default:
						break;
					}
				}
			}

		}
		return results;
	}

	/**
	 * @param skip
	 * @param key
	 * @return Should key be skipped if skip list contains prefix of key.
	 */
	private static boolean skip(List<String> skip, String key) {
		for (String value : skip)
			if (key.startsWith(value)) return true;
		return false;
	}

	/**
	 * List with maximum size. Oldest entries are purged when new entries are
	 * added that exceed the max size of the list.
	 * 
	 * @author Matt
	 *
	 * @param <E>
	 */
	@SuppressWarnings("serial")
	private static class RollingList<E> extends ArrayList<E> {
		private final int max;

		public RollingList(int max) {
			this.max = (max - 1);
		}

		@Override
		public boolean add(E k) {
			boolean r = super.add(k);
			if (size() > max) {
				removeRange(0, size() - max - 1);
			}
			return r;
		}
	}
}
