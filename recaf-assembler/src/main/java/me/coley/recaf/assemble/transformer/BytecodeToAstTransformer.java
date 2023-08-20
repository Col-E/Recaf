package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.BaseArg;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.*;
import me.coley.recaf.assemble.ast.arch.module.*;
import me.coley.recaf.assemble.ast.arch.module.Module;
import me.coley.recaf.assemble.ast.arch.record.Record;
import me.coley.recaf.assemble.ast.arch.record.RecordComponent;
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
	private final Map<Integer, MethodParameter> parameterMap = new HashMap<>();
	private final ClassNode classNode;
	private final FieldNode fieldNode;
	private final MethodNode methodNode;
	private String labelPrefix = "";
	private Unit unit;

	/**
	 * @param fieldNode
	 * 		Field to disassemble.
	 */
	public BytecodeToAstTransformer(FieldNode fieldNode) {
		this(null, fieldNode, null);
	}

	/**
	 * @param methodNode
	 * 		Method to disassemble.
	 */
	public BytecodeToAstTransformer(MethodNode methodNode) {
		this(null, null, methodNode);
	}

	/**
	 * @param classNode
	 * 		Class to disassemble.
	 */
	public BytecodeToAstTransformer(ClassNode classNode) {
		this(classNode, null, null);
	}

	private BytecodeToAstTransformer(ClassNode classNode, FieldNode fieldNode, MethodNode methodNode) {
		this.classNode = classNode;
		this.fieldNode = fieldNode;
		this.methodNode = methodNode;
	}

	/**
	 * Creates the {@link #getUnit() unit}.
	 */
	public void visit() {
		if (methodNode != null) {
			visitMethod();
		} else if (fieldNode != null) {
			visitField();
		} else if (classNode != null) {
			visitClass();
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
		for (AccessFlag flag : AccessFlag.getApplicableFlags(AccessFlag.Type.FIELD, fieldNode.access)) {
			modifiers.add(Modifier.byName(flag.getName()));
		}
		// Setup other attributes
		FieldDefinition definition = new FieldDefinition(modifiers, fieldNode.name, fieldNode.desc);
		// check if ACC_DEPRECATED is set
		if ((fieldNode.access & Opcodes.ACC_DEPRECATED) != 0)
			definition.setDeprecated(true);
		if (fieldNode.signature != null && !fieldNode.signature.equals(fieldNode.desc))
			definition.setSignature(new Signature(fieldNode.signature));
		if (fieldNode.value != null) {
			Object v = fieldNode.value;
			if (v instanceof String)
				definition.setConstVal(new ConstVal((String) v));
			else if (v instanceof Integer)
				definition.setConstVal(new ConstVal((int) v));
			else if (v instanceof Float)
				definition.setConstVal(new ConstVal((float) v));
			else if (v instanceof Double)
				definition.setConstVal(new ConstVal((double) v));
			else if (v instanceof Long)
				definition.setConstVal(new ConstVal((long) v));
		}
		AnnotationHelper.visitAnnos(definition, true, fieldNode.visibleAnnotations);
		AnnotationHelper.visitAnnos(definition, false, fieldNode.invisibleAnnotations);
		// Done
		unit = new Unit(definition);
	}

	/**
	 * Creates the {@link #getUnit() unit} as a method.
	 */
	private void visitMethod() {
		LabelNode fallbackInitialLabel = null;
		Map<LabelNode, String> labelNames = new LinkedHashMap<>();
		// Collect labels
		for (AbstractInsnNode insn : methodNode.instructions) {
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
		for (AccessFlag flag : AccessFlag.getApplicableFlags(AccessFlag.Type.METHOD, methodNode.access)) {
			modifiers.add(Modifier.byName(flag.getName()));
		}
		// Setup parameters and return type
		if (!Types.isValidDesc(methodNode.desc)) {
			throw new IllegalStateException("Invalid method descriptor: " + methodNode.desc);
		}
		Type methodType = Type.getMethodType(methodNode.desc);
		MethodParameters params = new MethodParameters();
		int argVarIndex = AccessFlag.isStatic(methodNode.access) ? 0 : 1;
		for (Type argType : methodType.getArgumentTypes()) {
			String name = getVariableName(PARAM, argType, argVarIndex);
			MethodParameter param = new MethodParameter(argType.getDescriptor(), name);
			params.add(param);
			parameterMap.put(argVarIndex, param);
			argVarIndex += argType.getSize();
		}
		String retType = methodType.getReturnType().getDescriptor();
		Code code = new Code();
		MethodDefinition definition = new MethodDefinition(modifiers, methodNode.name, params, retType, code);
		// check if ACC_DEPRECATED is set
		if ((methodNode.access & Opcodes.ACC_DEPRECATED) != 0)
			definition.setDeprecated(true);
		// Setup other attributes
		if (methodNode.signature != null && !methodNode.signature.equals(methodNode.desc))
			definition.setSignature(new Signature(methodNode.signature));
		if (methodNode.exceptions != null) {
			for (String ex : methodNode.exceptions)
				definition.addThrownException(new ThrownException(ex));
		}
		if (methodNode.tryCatchBlocks != null) {
			for (TryCatchBlockNode tryCatch : methodNode.tryCatchBlocks) {
				String start = labelNames.get(tryCatch.start);
				String end = labelNames.get(tryCatch.end);
				String handler = labelNames.get(tryCatch.handler);
				String type = tryCatch.type;
				code.addTryCatch(new TryCatch(start, end, handler, type));
			}
		}
		AnnotationHelper.visitAnnos(definition, true, methodNode.visibleAnnotations);
		AnnotationHelper.visitAnnos(definition, false, methodNode.invisibleAnnotations);
		if (methodNode.instructions != null) {
			// Add fallback if needed so variables have a starting range label.
			if (fallbackInitialLabel != null)
				code.addLabel(new Label(labelNames.get(fallbackInitialLabel)));
			// First pass to populate what type (prims vs obj) variables are at different offsets in the method.
			// This information is used to sanity check our variable name selection choice.
			for (int pos = 0; pos < methodNode.instructions.size(); pos++) {
				AbstractInsnNode insn = methodNode.instructions.get(pos);
				if (insn.getType() == AbstractInsnNode.VAR_INSN) {
					VarInsnNode varInsn = (VarInsnNode) insn;
					Type varType = Types.fromVarOpcode(varInsn.getOpcode());
					int varSort = Types.getNormalizedSort(varType.getSort());
					getVariablePositionalSorts(varInsn.var).put(pos, varSort);
				}
			}
			// Second pass to do everything else.
			AbstractInsnNode lastInsn = null;
			for (int pos = 0; pos < methodNode.instructions.size(); pos++) {
				AbstractInsnNode insn = methodNode.instructions.get(pos);
				lastInsn = insn;
				int op = insn.getOpcode();
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
						pos = methodNode.instructions.indexOf(insn);
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
						code.addInstruction(new MethodInstruction(op, methodInsn.owner, methodInsn.name, methodInsn.desc,
								methodInsn.itf));
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
						pos = methodNode.instructions.indexOf(insn);
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
						op = -1;
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
		unit = new Unit(definition);
	}

	/**
	 * Creates the {@link #getUnit() unit} as a class.
	 */
	private void visitClass() {
		Modifiers modifiers = new Modifiers();
		for (AccessFlag flag : AccessFlag.getApplicableFlags(AccessFlag.Type.CLASS, classNode.access)) {
			modifiers.add(Modifier.byName(flag.getName()));
		}
		// Setup other attributes
		ClassDefinition definition = new ClassDefinition(modifiers, classNode.name, classNode.superName, classNode.interfaces);
		if (classNode.signature != null)
			definition.setSignature(new Signature(classNode.signature));
		// check if ACC_DEPRECATED is set
		if ((classNode.access & Opcodes.ACC_DEPRECATED) != 0)
			definition.setDeprecated(true);
		AnnotationHelper.visitAnnos(definition, true, classNode.visibleAnnotations);
		AnnotationHelper.visitAnnos(definition, false, classNode.invisibleAnnotations);
		// Create AST for all methods & fields
		for (MethodNode methodNode : classNode.methods) {
			BytecodeToAstTransformer transformer = new BytecodeToAstTransformer(methodNode);
			transformer.visit();
			definition.addMethod(transformer.getUnit().getDefinitionAsMethod());
		}
		for (FieldNode fieldNode : classNode.fields) {
			BytecodeToAstTransformer transformer = new BytecodeToAstTransformer(fieldNode);
			transformer.visit();
			definition.addField(transformer.getUnit().getDefinitionAsField());
		}
		definition.setVersion(classNode.version);
		definition.setSourceFile(classNode.sourceFile);
		if(classNode.permittedSubclasses != null) {
			for (String permittedSubclass : classNode.permittedSubclasses) {
				definition.addPermittedSubclass(permittedSubclass);
			}
		}
		if(classNode.innerClasses != null) {
			for (InnerClassNode innerClass : classNode.innerClasses) {
				Modifiers mods = new Modifiers();
				for (AccessFlag flag : AccessFlag.getApplicableFlags(AccessFlag.Type.INNER_CLASS, innerClass.access)) {
					mods.add(Modifier.byName(flag.getName()));
				}
				definition.addInnerClass(new InnerClass(mods, innerClass.name, innerClass.outerName, innerClass.innerName));
			}
		}
		if(classNode.module != null) {
			ModuleNode moduleNode = classNode.module;
			Modifiers mods = new Modifiers();
			for (AccessFlag flag : AccessFlag.getApplicableFlags(AccessFlag.Type.MODULE, moduleNode.access)) {
				mods.add(Modifier.byName(flag.getName()));
			}
			Module module = new Module(moduleNode.name, mods);
			if(moduleNode.version != null) {
				module.setVersion(moduleNode.version);
			}
			if(moduleNode.mainClass != null) {
				module.setMainClass(moduleNode.mainClass);
			}
			if(moduleNode.packages != null) {
				for (String pkg : moduleNode.packages) {
					module.addPackage(pkg);
				}
			}
			if(moduleNode.requires != null) {
				for (ModuleRequireNode requires : moduleNode.requires) {
					Modifiers reqMods = new Modifiers();
					for (AccessFlag flag : AccessFlag.getApplicableFlags(AccessFlag.Type.MODULE, requires.access)) {
						reqMods.add(Modifier.byName(flag.getName()));
					}
					ModuleRequire moduleRequire = new ModuleRequire(requires.module, reqMods);
					if(requires.version != null) {
						moduleRequire.setVersion(requires.version);
					}
					module.addRequire(moduleRequire);
				}
			}
			if(moduleNode.exports != null) {
				for (ModuleExportNode exports : moduleNode.exports) {
					Modifiers expMods = new Modifiers();
					for (AccessFlag flag : AccessFlag.getApplicableFlags(AccessFlag.Type.MODULE, exports.access)) {
						expMods.add(Modifier.byName(flag.getName()));
					}
					ModuleExport moduleExport = new ModuleExport(exports.packaze, expMods);
					if(exports.modules != null) {
						for (String target : exports.modules) {
							moduleExport.addPackage(target);
						}
					}
					module.addExport(moduleExport);
				}
			}
			if(moduleNode.opens != null) {
				for (ModuleOpenNode opens : moduleNode.opens) {
					Modifiers opnMods = new Modifiers();
					for (AccessFlag flag : AccessFlag.getApplicableFlags(AccessFlag.Type.MODULE, opens.access)) {
						opnMods.add(Modifier.byName(flag.getName()));
					}
					ModuleOpen moduleOpen = new ModuleOpen(opens.packaze, opnMods);
					if(opens.modules != null) {
						for (String target : opens.modules) {
							moduleOpen.addPackage(target);
						}
					}
					module.addOpen(moduleOpen);
				}
			}
			if(moduleNode.uses != null) {
				for (String service : moduleNode.uses) {
					module.addUse(service);
				}
			}
			if(moduleNode.provides != null) {
				for (ModuleProvideNode provides : moduleNode.provides) {
					ModuleProvide moduleProvide = new ModuleProvide(provides.service);
					if(provides.providers != null) {
						for (String provider : provides.providers) {
							moduleProvide.addPackage(provider);
						}
					}
					module.addProvide(moduleProvide);
				}
			}
			definition.setModule(module);
		}
		if(classNode.nestHostClass != null) {
			definition.setNestHost(classNode.nestHostClass);
		}
		if(classNode.nestMembers != null) {
			for (String nestMember : classNode.nestMembers) {
				definition.addNestMember(nestMember);
			}
		}
		if(classNode.recordComponents != null) {
			Record record = new Record();
			for (RecordComponentNode recordComponent : classNode.recordComponents) {
				RecordComponent component = new RecordComponent(recordComponent.name, recordComponent.descriptor);
				if(recordComponent.signature != null) {
					component.setSignature(new Signature(recordComponent.signature));
				}
				AnnotationHelper.visitAnnos(component, true, recordComponent.visibleAnnotations);
				AnnotationHelper.visitAnnos(component, false, recordComponent.invisibleAnnotations);
				record.addComponent(component);
			}
			definition.setRecord(record);
		}
		// Done
		unit = new Unit(definition);
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
		if (index == 0 && !AccessFlag.isStatic(methodNode.access)) {
			return "this";
		}
		// We will call this in cases where we have both extra type insight, and lessened insight.
		// So we will normalize the type so that the only options are "object" or "int-primitive".
		int normalizedSort = Types.getNormalizedSort(type.getSort());
		// Check for parameter index first
		if (parameterMap.containsKey(index)) {
			MethodParameter parameter = parameterMap.get(index);
			return parameter.getName();
		}
		// Check for cached name next.
		// There may be a better fitting variable, so we will still check other options.
		Key key = new Key(index, normalizedSort);
		String name = variableNames.get(key);
		// Check for existing variable name
		if (name == null && methodNode.localVariables != null) {
			for (LocalVariableNode local : methodNode.localVariables) {
				if (isMatching(local, pos, type, index)) {
					// Check if variable name is in use by another variable of a different sort
					if (variableNames.containsValue(local.name) && !variableNames.containsKey(key)) {
						continue;
					}
					// No collision, can use this name.
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
			int start = methodNode.instructions.indexOf(local.start);
			if (position < start - 1)
				return false;
			int end = methodNode.instructions.indexOf(local.end);
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
