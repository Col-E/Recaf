package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.BaseArg;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.*;
import me.coley.recaf.assemble.ast.insn.*;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.assemble.ast.meta.Signature;
import me.coley.recaf.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Disassembles a method or field into our AST format.
 *
 * @author Matt Coley
 */
public class BytecodeToAstTransformer {
	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int MAX_NAME_LEN = 35;
	private static final int UNDEFINED = -1;
	private static final int PARAM = -2;
	private final Map<Integer, TreeMap<Integer, Integer>> variableSorts = new HashMap<>();
	private final Map<Key, String> variableNames = new HashMap<>();
	private final MethodNode method;
	private final FieldNode field;
	private String labelPrefix = "";
	private Unit unit;

	/**
	 * @param field
	 * 		Field to disassemble.
	 */
	public BytecodeToAstTransformer(FieldNode field) {
		this(field, null);
	}

	/**
	 * @param method
	 * 		Method to disassemble.
	 */
	public BytecodeToAstTransformer(MethodNode method) {
		this(null, method);
	}

	private BytecodeToAstTransformer(FieldNode field, MethodNode method) {
		this.method = method;
		this.field = field;
	}

	/**
	 * Creates the {@link #getUnit() unit}.
	 */
	public void visit() {
		if (field != null) {
			visitField();
		} else {
			visitMethod();
		}
	}

	/**
	 * @param labelPrefix
	 * 		Prefix for generated label names.
	 */
	public void setLabelPrefix(String labelPrefix) {
		this.labelPrefix = labelPrefix;
	}

	/**
	 * Creates the {@link #getUnit() unit} as a field.
	 */
	private void visitField() {
		// Setup modifiers
		Modifiers modifiers = new Modifiers();
		for (AccessFlag flag : AccessFlag.getApplicableFlags(AccessFlag.Type.FIELD, field.access)) {
			modifiers.add(Modifier.byName(flag.getName()));
		}
		// Setup other attributes
		Code code = new Code();
		if (field.signature != null && !field.signature.equals(field.desc))
			code.setSignature(new Signature(field.signature));
		if (field.value != null) {
			Object v = field.value;
			if (v instanceof String)
				code.setConstVal(new ConstVal((String) v));
			else if (v instanceof Integer)
				code.setConstVal(new ConstVal((int) v));
			else if (v instanceof Float)
				code.setConstVal(new ConstVal((float) v));
			else if (v instanceof Double)
				code.setConstVal(new ConstVal((double) v));
			else if (v instanceof Long)
				code.setConstVal(new ConstVal((long) v));
		}
		AnnotationHelper.visitAnnos(code, true, field.visibleAnnotations);
		AnnotationHelper.visitAnnos(code, false, field.invisibleAnnotations);
		// Done
		FieldDefinition definition = new FieldDefinition(modifiers, field.name, field.desc);
		unit = new Unit(definition, code);
	}

	/**
	 * Creates the {@link #getUnit() unit} as a method.
	 */
	private void visitMethod() {
		LabelNode fallbackInitialLabel = null;
		Map<LabelNode, String> labelNames = new LinkedHashMap<>();
		// Collect labels
		for (AbstractInsnNode insn : method.instructions) {
			LabelNode label;
			if (insn.getType() == AbstractInsnNode.LABEL) {
				label = (LabelNode) insn;
			} else if (insn.getType() == AbstractInsnNode.VAR_INSN && labelNames.isEmpty()) {
				label = new LabelNode();
				fallbackInitialLabel = label;
			} else {
				continue;
			}
			String name = labelPrefix + StringUtil.generateName(ALPHABET, labelNames.size());
			labelNames.put(label, name);
		}
		// If there are no labels, we must add some.
		if (labelNames.isEmpty()) {
			fallbackInitialLabel = new LabelNode();
			labelNames.put(fallbackInitialLabel, labelPrefix + StringUtil.generateName(ALPHABET, 0));
		}
		// Setup modifiers
		Modifiers modifiers = new Modifiers();
		for (AccessFlag flag : AccessFlag.getApplicableFlags(AccessFlag.Type.METHOD, method.access)) {
			modifiers.add(Modifier.byName(flag.getName()));
		}
		// Setup parameters and return type
		if (!Types.isValidDesc(method.desc)) {
			throw new IllegalStateException("Invalid method descriptor: " + method.desc);
		}
		Type methodType = Type.getMethodType(method.desc);
		MethodParameters params = new MethodParameters();
		int argVarIndex = AccessFlag.isStatic(method.access) ? 0 : 1;
		for (Type argType : methodType.getArgumentTypes()) {
			String name = getVariableName(PARAM, argType, argVarIndex);
			params.add(new MethodParameter(argType.getDescriptor(), name));
			argVarIndex += argType.getSize();
		}
		String retType = methodType.getReturnType().getDescriptor();
		// Setup other attributes
		Code code = new Code();
		if (method.signature != null && !method.signature.equals(method.desc))
			code.setSignature(new Signature(method.signature));
		if (method.exceptions != null) {
			for (String ex : method.exceptions)
				code.addThrownException(new ThrownException(ex));
		}
		if (method.tryCatchBlocks != null) {
			for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) {
				String start = labelNames.get(tryCatch.start);
				String end = labelNames.get(tryCatch.end);
				String handler = labelNames.get(tryCatch.handler);
				String type = tryCatch.type;
				code.addTryCatch(new TryCatch(start, end, handler, type));
			}
		}
		AnnotationHelper.visitAnnos(code, true, method.visibleAnnotations);
		AnnotationHelper.visitAnnos(code, false, method.invisibleAnnotations);
		if (method.instructions != null) {
			// Add fallback if needed so variables have a starting range label.
			if (fallbackInitialLabel != null)
				code.addLabel(new Label(labelNames.get(fallbackInitialLabel)));
			// First pass to populate what type (prims vs obj) variables are at different offsets in the method.
			// This information is used to sanity check our variable name selection choice.
			for (int pos = 0; pos < method.instructions.size(); pos++) {
				AbstractInsnNode insn = method.instructions.get(pos);
				if (insn.getType() == AbstractInsnNode.VAR_INSN) {
					VarInsnNode varInsn = (VarInsnNode) insn;
					Type varType = Types.fromVarOpcode(varInsn.getOpcode());
					int varSort = Types.getNormalizedSort(varType.getSort());
					getVariablePositionalSorts(varInsn.var).put(pos, varSort);
				}
			}
			// Second pass to do everything else.
			AbstractInsnNode lastInsn = null;
			for (int pos = 0; pos < method.instructions.size(); pos++) {
				AbstractInsnNode insn = method.instructions.get(pos);
				lastInsn = insn;
				String op = OpcodeUtil.opcodeToName(insn.getOpcode());
				switch (insn.getType()) {
					case AbstractInsnNode.INSN:
						code.addInstruction(new Instruction(op));
						break;
					case AbstractInsnNode.INT_INSN:
						IntInsnNode intInsn = (IntInsnNode) insn;
						if (insn.getOpcode() == Opcodes.NEWARRAY) {
							code.addInstruction(new NewArrayInstruction(op, NewArrayInstruction.fromInt(intInsn.operand)));
							break;
						}
						code.addInstruction(new IntInstruction(op, intInsn.operand));
						break;
					case AbstractInsnNode.VAR_INSN:
						pos = method.instructions.indexOf(insn);
						VarInsnNode varInsn = (VarInsnNode) insn;
						String varName = getVariableName(pos, Types.fromVarOpcode(insn.getOpcode()), varInsn.var);
						code.addInstruction(new VarInstruction(op, varName));
						break;
					case AbstractInsnNode.TYPE_INSN:
						TypeInsnNode typeInsn = (TypeInsnNode) insn;
						code.addInstruction(new TypeInstruction(op, typeInsn.desc));
						break;
					case AbstractInsnNode.FIELD_INSN:
						FieldInsnNode fieldInsn = (FieldInsnNode) insn;
						code.addInstruction(new FieldInstruction(op, fieldInsn.owner, fieldInsn.name, fieldInsn.desc));
						break;
					case AbstractInsnNode.METHOD_INSN:
						MethodInsnNode methodInsn = (MethodInsnNode) insn;
						code.addInstruction(new MethodInstruction(op, methodInsn.owner, methodInsn.name, methodInsn.desc));
						break;
					case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
						InvokeDynamicInsnNode indyInsn = (InvokeDynamicInsnNode) insn;
						HandleInfo bsmHandle = new HandleInfo(
								OpcodeUtil.tagToName(indyInsn.bsm.getTag()),
								indyInsn.bsm.getOwner(),
								indyInsn.bsm.getName(),
								indyInsn.bsm.getDesc()
						);
						List<IndyInstruction.BsmArg> args = new ArrayList<>();
						for (Object v : indyInsn.bsmArgs) {
							args.add(BaseArg.of(IndyInstruction.BsmArg::new, v));
						}
						code.addInstruction(new IndyInstruction(op, indyInsn.name, indyInsn.desc, bsmHandle, args));
						break;
					case AbstractInsnNode.JUMP_INSN:
						JumpInsnNode jumpInsn = (JumpInsnNode) insn;
						String jumpTarget = labelNames.get(jumpInsn.label);
						if (jumpTarget == null)
							throw new IllegalStateException("Unmapped label instance to name!");
						code.addInstruction(new JumpInstruction(op, jumpTarget));
						break;
					case AbstractInsnNode.LABEL:
						LabelNode labelInsn = (LabelNode) insn;
						String label = labelNames.get(labelInsn);
						if (label == null)
							throw new IllegalStateException("Unmapped label instance to name!");
						code.addLabel(new Label(label));
						break;
					case AbstractInsnNode.LDC_INSN:
						LdcInsnNode ldcInsn = (LdcInsnNode) insn;
						code.addInstruction(LdcInstruction.of(ldcInsn.cst));
						break;
					case AbstractInsnNode.IINC_INSN:
						pos = method.instructions.indexOf(insn);
						IincInsnNode iincInsn = (IincInsnNode) insn;
						String iincName = getVariableName(pos, Types.fromVarOpcode(insn.getOpcode()), iincInsn.var);
						code.addInstruction(new IincInstruction(op, iincName, iincInsn.incr));
						break;
					case AbstractInsnNode.TABLESWITCH_INSN:
						TableSwitchInsnNode tableInsn = (TableSwitchInsnNode) insn;
						List<String> labels = new ArrayList<>();
						for (LabelNode tableLabel : tableInsn.labels) {
							String name = labelNames.get(tableLabel);
							if (name == null) {
								throw new IllegalStateException("Unmapped label instance to name!");
							}
							labels.add(name);
						}
						String tableDflt = labelNames.get(tableInsn.dflt);
						if (tableDflt == null) {
							throw new IllegalStateException("Unmapped label instance to name!");
						}
						code.addInstruction(new TableSwitchInstruction(op, tableInsn.min, tableInsn.max, labels, tableDflt));
						break;
					case AbstractInsnNode.LOOKUPSWITCH_INSN:
						LookupSwitchInsnNode lookupInsn = (LookupSwitchInsnNode) insn;
						List<LookupSwitchInstruction.Entry> entries = new ArrayList<>();
						for (int i = 0; i < lookupInsn.keys.size(); i++) {
							int key = lookupInsn.keys.get(i);
							String name = labelNames.get(lookupInsn.labels.get(i));
							if (name == null) {
								throw new IllegalStateException("Unmapped label instance to name!");
							}
							entries.add(new LookupSwitchInstruction.Entry(key, name));
						}
						String lookupDflt = labelNames.get(lookupInsn.dflt);
						if (lookupDflt == null) {
							throw new IllegalStateException("Unmapped label instance to name!");
						}
						code.addInstruction(new LookupSwitchInstruction(op, entries, lookupDflt));
						break;
					case AbstractInsnNode.MULTIANEWARRAY_INSN:
						MultiANewArrayInsnNode multiInsn = (MultiANewArrayInsnNode) insn;
						code.addInstruction(new MultiArrayInstruction(op, multiInsn.desc, multiInsn.dims));
						break;
					case AbstractInsnNode.LINE:
						// Edge case, technically has no "opcode"
						op = "LINE";
						LineNumberNode lineInsn = (LineNumberNode) insn;
						String lineTarget = labelNames.get(lineInsn.start);
						if (lineTarget == null)
							throw new IllegalStateException("Unmapped label instance to name!");
						code.addInstruction(new LineInstruction(op, lineTarget, lineInsn.line));
						break;
				}
			}
			// Add ending label
			if (lastInsn != null && lastInsn.getType() != AbstractInsnNode.LABEL) {
				LabelNode end = new LabelNode();
				labelNames.put(end, labelPrefix + StringUtil.generateName(ALPHABET, labelNames.size()));
				code.addLabel(new Label(labelNames.get(end)));
			}
		}
		// Done
		MethodDefinition definition = new MethodDefinition(modifiers, method.name, params, retType);
		unit = new Unit(definition, code);
	}

	/**
	 * @return Generated unit.
	 */
	public Unit getUnit() {
		return unit;
	}

	/**
	 * Assumes method is asserted to be not-null by this point.
	 *
	 * @param pos
	 * 		Position of variable usage. Negative for parameter values which have full scope.
	 * @param type
	 * 		Variable type.
	 * @param index
	 * 		Variable index.
	 *
	 * @return Name of variable, generated if no existing name is found.
	 */
	private String getVariableName(int pos, Type type, int index) {
		// Always use "this" to refer to self.
		if (index == 0 && !AccessFlag.isStatic(method.access)) {
			return "this";
		}
		// We will call this in cases where we have both extra type insight, and lessened insight.
		// So we will normalize the type so that the only options are "object" or "int-primitive".
		int normalizedSort = Types.getNormalizedSort(type.getSort());
		// Check for cached name first.
		// There may be a better fitting variable, so we will still check other options.
		Key key = new Key(index, normalizedSort);
		String name = variableNames.get(key);
		// Check for existing variable name
		if (method.localVariables != null) {
			for (LocalVariableNode local : method.localVariables) {
				if (isMatching(local, pos, type, index)) {
					name = local.name;
					break;
				}
			}
		}
		if (!isOkName(name)) {
			// No good existing name, we need to make one.
			// Check and see if using the type name makes sense.
			// First we want to manipulate the type we're looking at a bit.
			// We can't assume we will always know if an object is an array or not, so strip array info.
			if (type.getSort() == Type.ARRAY)
				type = type.getElementType();
			// Next, because descriptor types can be more specific about the type of it, we must generalize here.
			// Boolean? Char? Short? All are treated as Integers.
			if (type.getSort() < Type.INT)
				type = Type.INT_TYPE;
			// Next for the type lets get the last part of the name.
			// - "java/lang/String" -> "String"
			// - "Example$Inner"    -> "Inner"
			String typeName = StringUtil.shortenPath(type.getInternalName());
			int innerSplit = typeName.indexOf('$');
			if (innerSplit > 0)
				typeName = typeName.substring(innerSplit + 1);
			// Remove any trailing garbage characters (Array types ending with ";" for instance)
			typeName = typeName.replace(";", "");
			// And camel-case it
			name = StringUtil.lowercaseFirstChar(typeName) + index;
		}
		// If the type name is ok, we're ok
		if (!isOkName(name)) {
			// Fallback, all other options of naming exhausted.
			// Usually occurs when there is missing debug info, or compiler-generated locals.
			name = "v" + index;
		}
		// Update cache
		variableNames.put(key, name);
		return name;
	}

	/**
	 * @param local
	 * 		The local variable to check for matching the given information.
	 * @param position
	 * 		Position in the method code to check.
	 * @param type
	 * 		Expected type of usage at the position.
	 * @param variableIndex
	 * 		Variable index used at position.
	 *
	 * @return {@code true} when the information matches against the given local variable.
	 * This means we can use it for naming <i>(Unless the name is {@link #isOkName(String) illegal})</i>.
	 */
	private boolean isMatching(LocalVariableNode local, int position, Type type, int variableIndex) {
		// Wrong index
		if (local.index != variableIndex)
			return false;
		String desc = local.desc;
		if (!Types.isValidDesc(desc))
			return false;
		int localSort = Types.getNormalizedSort(Type.getType(desc).getSort());
		int targetSort = Types.getNormalizedSort(type.getSort());
		boolean isLocalPrimitive = localSort <= Type.DOUBLE;
		boolean isTargetPrimitive = targetSort <= Type.DOUBLE;
		// Comparing primitive to non-primitive
		if (isLocalPrimitive != isTargetPrimitive)
			return false;
		// Position must be in label range, unless its a parameter
		if (position != PARAM) {
			// I don't know why, but 'javac' will emit code like this:
			//  10: astore_1
			//  11: aload_1  <--- Variable starts here
			// Because of this we need to offset the start range by 1
			int start = method.instructions.indexOf(local.start);
			if (position < start - 1)
				return false;
			int end = method.instructions.indexOf(local.end);
			if (position > end)
				return false;
			// Sanity check the sort of the variable at the position makes sense with this case.
			if (!isSameSortOrUndefined(variableIndex, position, localSort))
				return false;
		}
		return true;
	}

	/**
	 * @param name
	 * 		Name to check.
	 *
	 * @return {@code true} if the name is allowed.
	 * Otherwise, we will have to disregard it and make a new one.
	 */
	private static boolean isOkName(String name) {
		if (name == null)
			return false;
		// Too long
		if (name.length() > MAX_NAME_LEN)
			return false;
		// Misc disallowed chars check
		if (name.contains("-"))
			return false;
		// If a generic variable type of 'object' is used it makes for a kinda poor name.
		if (name.startsWith("object"))
			return false;
		// If there's text that needs to be escaped in here, its not a good variable name.
		if (!name.equals(EscapeUtil.escape(name)))
			return false;
		// We don't want numbers
		if (StringUtil.isDecimal(name))
			return false;
		return true;
	}

	/**
	 * @param variableIndex
	 * 		Variable index to operate on.
	 * @param position
	 * 		Position to check in the method code.
	 * @param sort
	 * 		Expected sort to check against.
	 *
	 * @return {@code true} if the type at the position matches the last recorded type usage or {@link #UNDEFINED}.
	 */
	private boolean isSameSortOrUndefined(int variableIndex, int position, int sort) {
		Map.Entry<Integer, Integer> entry = getVariablePositionalSorts(variableIndex).floorEntry(position);
		int entrySort = entry.getValue();
		return entrySort == UNDEFINED || entrySort == sort;
	}

	/**
	 * Because of variable re-usage we may have different types occupying the same slot.
	 * To make our lives easier we will check for this and then differentiate between usages.
	 *
	 * @param variableIndex
	 * 		Variable index to get map for.
	 *
	 * @return Position range map to the current expected variable type sorts.
	 */
	private TreeMap<Integer, Integer> getVariablePositionalSorts(int variableIndex) {
		return variableSorts.computeIfAbsent(variableIndex, v -> {
			// New map with an undefined type as the floor value
			TreeMap<Integer, Integer> map = new TreeMap<>();
			map.put(-1, UNDEFINED);
			return map;
		});
	}

	/**
	 * Optional pre-population of variable name data.
	 *
	 * @param variables
	 * 		Data to populate.
	 */
	public void prepopulateVariableNames(Variables variables) {
		for (VariableInfo varInfo : variables) {
			int index = varInfo.getIndex();
			int sort = Types.getNormalizedSort(varInfo.getLastUsedType().getSort());
			variableNames.put(new Key(index, sort), varInfo.getName());
		}
	}

	/**
	 * Used to do a double-int lookup key.
	 */
	private static class Key {
		private final int index;
		private final int sort;

		public Key(int index, int sort) {
			this.index = index;
			this.sort = sort;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Key key = (Key) o;
			return index == key.index && sort == key.sort;
		}

		@Override
		public int hashCode() {
			return sort * 1000 + index;
		}

		@Override
		public String toString() {
			return "Key{" +
					"index=" + index +
					", sort=" + sort +
					'}';
		}
	}
}
