package me.coley.recaf.bytecode.search;

import me.coley.recaf.Input;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.util.RollingList;
import me.coley.recaf.util.Threads;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static me.coley.recaf.bytecode.search.SearchType.*;

public class Search {
	public static List<Result> search(Parameter... params) {
		Set<Result> results = Collections.newSetFromMap(new ConcurrentHashMap<>());
		Map<String, ClassNode> nodes = Input.get().getClasses();
		ExecutorService pool =  Threads.pool();
		for (Entry<String, ClassNode> entry : nodes.entrySet()) {
			// TODO: Optimize by reducing needless iteration
			// * Make wrapper for params[],
			// * Make proxy calls for references called on param
			// * Skip removes a param from the array
			// * The next Entry<String, ClassNode> resets, adds back param
			for (Parameter param : params) {
				// check if entry should be skipped
				if (skip(param.getSkipList(), entry.getKey())) {
					continue;
				}
				// begin search
				pool.submit(() -> {
					ClassNode cn = entry.getValue();
					searchClass(cn, param, results);
					for (MethodNode mn : cn.methods) {
						searchMethodInstructions(cn, mn, param, results);
					}
				});
			}
		}
		Threads.waitForCompletion(pool);
		return new ArrayList<>(results);
	}

	/**
	 * @param cn
	 * 		Class to search.
	 * @param param
	 * 		Search configuration.
	 * @param results
	 * 		Results set to add to.
	 *
	 * @return {@code true} if the current entry does not need to continue being scanned for
	 * results after this method returns.
	 */
	private static boolean searchClass(ClassNode cn, Parameter param, Set<Result> results) {
		if (param.isType(DECLARATION)) {
			// If user only searches for the name, only add the class,
			// not every member.
			if (param.singleArg() && param.validType(cn.name)) {
				results.add(Result.type(cn));
				return true;
			}
			// search for matching field members
			for (FieldNode fn : cn.fields) {
				if (param.validMember(cn.name, fn.name, fn.desc)) {
					results.add(Result.field(cn, fn));
				}
			}
			// search for matching method members
			for (MethodNode mn : cn.methods) {
				if (param.validMember(cn.name, mn.name, mn.desc)) {
					results.add(Result.method(cn, mn));
				}
			}
			// skip to next parameter. The following code is for a
			// different search-type.
			return true;
		} else if (param.isType(STRING) || param.isType(VALUE)) {
			searchAnnotations(cn, cn, cn.visibleAnnotations, param, results);
			searchAnnotations(cn, cn, cn.visibleTypeAnnotations, param, results);
			searchAnnotations(cn, cn, cn.invisibleAnnotations, param, results);
			searchAnnotations(cn, cn, cn.invisibleTypeAnnotations, param, results);
			for (FieldNode fn : cn.fields) {
				searchAnnotations(cn, fn, fn.visibleAnnotations, param, results);
				searchAnnotations(cn, fn, fn.visibleTypeAnnotations, param, results);
				searchAnnotations(cn, fn, fn.invisibleAnnotations, param, results);
				searchAnnotations(cn, fn, fn.invisibleTypeAnnotations, param, results);
			}
			for (MethodNode mn : cn.methods) {
				searchAnnotations(cn, mn, mn.visibleAnnotations, param, results);
				searchAnnotations(cn, mn, mn.visibleTypeAnnotations, param, results);
				searchAnnotations(cn, mn, mn.invisibleAnnotations, param, results);
				searchAnnotations(cn, mn, mn.invisibleTypeAnnotations, param, results);
			}
		}
		return false;
	}

	/**
	 * @param cn
	 * 		Class to search.
	 * @param host
	 * 		Object hosting the annotation.
	 * @param annos
	 * 		Annotations to search.
	 * @param param
	 * 		Search configuration, assumed to be STRING of VALUE.
	 * @param results
	 * 		Results set to add to.
	 */
	private static void searchAnnotations(ClassNode cn, Object host, List<? extends AnnotationNode> annos, Parameter param, Set<Result> results) {
		// Skip where possible
		if (annos == null || annos.isEmpty()) {
			return;
		}
		for (AnnotationNode anno : annos) {
			searchAnnotation(cn, host, anno, param, results);
		}
	}

	/**
	 * Stub for {@link #searchAnnotations(ClassNode, Object, List, Parameter, Set)}.
	 */
	private static void searchAnnotation(ClassNode cn, Object host, AnnotationNode anno, Parameter param, Set<Result> results) {
		int max = anno.values == null ? 0 : anno.values.size();
		if(max == 0) {
			return;
		}
		for(int i = 0; i < max; i += 2) {
			String name = (String) anno.values.get(i);
			Object value = anno.values.get(i + 1);
			if(param.isType(STRING) && value instanceof String) {
				String str = (String) value;
				if (param.check(0, str, false)) {
					results.add(Result.annotation(cn, host, anno, str));
				}
			} else if(param.isType(VALUE) && value instanceof Number) {
				Number search = param.getArg(0);
				Number num = (Number) value;
				if (search.equals(num)) {
					results.add(Result.annotation(cn, host, anno, search.toString()));
				}
			} else if(value instanceof List) {
				List<?> l = (List<?>) value;
				if(l.isEmpty()) {
					continue;
				}
				Object first = l.get(0);
				if(param.isType(STRING) && first instanceof String[]) {
					List<String[]> list = (List<String[]>) value;
					for(String[] array : list)
					for(String str : array) {
						if(param.check(0, str, false)) {
							results.add(Result.annotation(cn, host, anno, str));
						}
					}
				} else if(param.isType(STRING) && first instanceof String) {
					List<String> list = (List<String>) value;
					for(String str : list) {
						if(param.check(0, str, false)) {
							results.add(Result.annotation(cn, host, anno, str));
						}
					}
				} else if(param.isType(VALUE) && first instanceof Number) {
					Number search = param.getArg(0);
					List<Number> list = (List<Number>) value;
					for(Number num : list) {
						if (search.equals(num)) {
							results.add(Result.annotation(cn, host, anno, search.toString()));
						}
					}
				}
			} else if(param.isType(STRING) && value instanceof String[]) { //
				// enum
				String[] array = (String[]) value;
				for(String str : array) {
					if(param.check(0, str, false)) {
						results.add(Result.annotation(cn, host, anno, str));
					}
				}
			} else if(value instanceof AnnotationNode) {
				searchAnnotation(cn, host, (AnnotationNode) value, param, results);
			}
		}
	}


	/**
	 * @param cn
	 * 		Class to search.
	 * @param mn
	 * 		Method to search.
	 * @param param
	 * 		Search configuration.
	 * @param results
	 * 		Results set to add to.
	 */
	private static void searchMethodInstructions(ClassNode cn, MethodNode mn, Parameter param, Set<Result> results) {
		// Setup OPCODE_PATTERN with dummy values. Init if parameter
		// search type matches.
		List<String> expected = null;
		int max = mn.instructions.size();
		if (param.isType(OPCODE_PATTERN)) {
			expected = param.getArg(0);
			max = expected.size();
		}
		StringList ops = new StringList(max);
		// Iterate instructions and check for matches of the parameter's search type.
		for (AbstractInsnNode ain : mn.instructions.toArray()) {
			switch (param.getType()) {
				case OPCODE_PATTERN:
					ops.add(FormatFactory.insnNode(ain, mn).getText());
					// Check rolling-list content equal to expected.
					// If match found intended pattern.
					if (ops.match(param, expected)) {
						int index = mn.instructions.indexOf(ain);
						int offset = expected.size() - 1;
						results.add(Result.opcode(cn, mn, mn.instructions.get(index - offset)));
					}
					break;
				case REFERENCE:
					boolean ownerOnly = (param.getArg(1) == null && param.getArg(2) == null);
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
							if (ownerOnly && param.validType(tin.desc)) {
								results.add(Result.opcode(cn, mn, ain));
							}
							break;
						case AbstractInsnNode.LDC_INSN:
							LdcInsnNode ldc = (LdcInsnNode) ain;
							// check against types stored in ldc's.
							if (ldc.cst instanceof Type) {
								// TODO: Does this include the ability
								// for LDC to hold org.objectweb.asm.Handle?
								Type type = (Type) ldc.cst;
								if (ownerOnly && type.getSort() == Type.OBJECT && param.validType(type.getClassName())) {
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
						if (ldc.cst instanceof String && param.check(0, ldc.cst.toString(), false)) {
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

	@SuppressWarnings("serial")
	private static class StringList extends RollingList<String> {

		public StringList(int max) {
			super(max);
		}

		public boolean match(Parameter param, List<String> expected) {
			if (size() != expected.size()) {
				return false;
			}
			try {
				for (int i = 0; i < size(); i++) {
					String sOpcode = get(i);
					String sExpected = expected.get(i);
					// Remove artifact from FormatFactory
					sOpcode = sOpcode.substring(sOpcode.indexOf(":") + 2);
					if (!param.isCaseSensitive() && param.getStringMode() != StringMode.REGEX) {
						sOpcode = sOpcode.toLowerCase();
						sExpected = sExpected.toLowerCase();
					}
					switch (param.getStringMode()) {
					case CONTAINS:
						if (!sOpcode.contains(sExpected)) return false;
						break;
					case ENDS_WITH:
						if (!sOpcode.endsWith(sExpected)) return false;
						break;
					case EQUALITY:
						if (!sOpcode.equals(sExpected)) return false;
						break;
					case REGEX:
						if (!Pattern.compile(sExpected).matcher(sOpcode).find()) return false;
						break;
					case STARTS_WITH:
						if (!sOpcode.startsWith(sExpected)) return false;
						break;
					default:
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}
}
