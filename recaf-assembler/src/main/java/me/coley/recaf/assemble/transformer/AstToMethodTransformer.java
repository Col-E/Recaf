package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.analysis.Analysis;
import me.coley.recaf.assemble.analysis.Analyzer;
import me.coley.recaf.assemble.ast.*;
import me.coley.recaf.assemble.ast.arch.*;
import me.coley.recaf.assemble.ast.insn.*;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.assemble.util.ClassSupplier;
import me.coley.recaf.assemble.util.InheritanceChecker;
import me.coley.recaf.assemble.util.ReflectiveInheritanceChecker;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.EscapeUtil;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Visits our bytecode AST {@link Unit} and transforms it into a normal method.
 *
 * @author Matt Coley
 */
public class AstToMethodTransformer {
	private final Map<String, LabelNode> labelMap = new HashMap<>();
	private final List<TryCatchBlockNode> tryBlocks = new ArrayList<>();
	private final Map<AbstractInsnNode, Element> insnToAstMap = new HashMap<>();
	private final Variables variables = new Variables();
	// Per-unit values, update when unit is changed
	private ExpressionToAsmTransformer exprToAsm;
	private ExpressionToAstTransformer exprToAst;
	private ClassSupplier classSupplier;
	private String selfType;
	private MethodDefinition definition;
	private Code code;
	// Configurable
	private InheritanceChecker inheritanceChecker = ReflectiveInheritanceChecker.getInstance();
	private boolean doLimitVarRange = true;
	private boolean useAnalysis;
	// Method building and other outputs
	private InsnList instructions;
	private Analysis analysis;

	/**
	 * @param selfType
	 * 		The internal type of the class defining the method.
	 */
	public AstToMethodTransformer(String selfType) {
		this(null, selfType);
	}

	/**
	 * @param classSupplier
	 * 		Class information supplier. Required to support expressions.
	 * @param selfType
	 * 		The internal type of the class defining the method.
	 */
	public AstToMethodTransformer(ClassSupplier classSupplier, String selfType) {
		this.classSupplier = classSupplier;
		this.selfType = selfType;
	}

	/**
	 * Visits the AST and collects information necessary for building the method.
	 *
	 * @throws MethodCompileException
	 * 		When the variable type usage is inconsistent/illegal,
	 * 		or when a variable index is already reserved by a wide variable of the prior slot.
	 */
	public void visit() throws MethodCompileException {
		// Validation
		if (definition == null)
			throw new IllegalArgumentException("No definition provided!");
		// Clear any old values if the transformer instance is being re-used
		reset();
		// Generate new label instances to map to label names.
		createLabels();
		// Generate variable data
		createVariables();
		// Lastly generate the instructions
		instructions = createInstructions();
	}

	/**
	 * @return Generated method.
	 *
	 * @throws MethodCompileException
	 * 		When the try-catch labels could not be mapped to instances,
	 * 		or when code hasn't been visited. Please call {@link #visit()} first.
	 */
	public MethodNode buildMethod() throws MethodCompileException {
		if (instructions == null) {
			throw new MethodCompileException(definition, "The instructions have not been successfully generated!" +
					"Cannot build method instance.");
		}
		int access = definition.getModifiers().value() | (definition.isDeprecated() ? Opcodes.ACC_DEPRECATED : 0);;
		String name = definition.getName();
		String descriptor = definition.getDesc();
		String signature = definition.getSignature() != null ? definition.getSignature().getSignature() : null;
		int stack = 0xFF;
		for (TryCatch tryCatch : code.getTryCatches()) {
			LabelNode start = labelMap.get(tryCatch.getStartLabel());
			LabelNode end = labelMap.get(tryCatch.getEndLabel());
			LabelNode handler = labelMap.get(tryCatch.getHandlerLabel());
			if (start == null)
				throw new MethodCompileException(tryCatch,
						"No identifier mapping to start label instance for '" + tryCatch.getStartLabel() + "'");
			if (end == null)
				throw new MethodCompileException(tryCatch,
						"No identifier mapping to end label instance for '" + tryCatch.getEndLabel() + "'");
			if (handler == null)
				throw new MethodCompileException(tryCatch,
						"No identifier mapping to handler label instance for '" + tryCatch.getHandlerLabel() + "'");
			tryBlocks.add(new TryCatchBlockNode(start, end, handler, tryCatch.getExceptionType()));
		}
		List<LocalVariableNode> variableList = new ArrayList<>();
		if (!AccessFlag.isAbstract(definition.getModifiers().value()))
			for (VariableInfo varInfo : variables.inSortedOrder()) {
				String varName = varInfo.getName();
				String varDesc = varInfo.getCommonType(inheritanceChecker).getDescriptor();
				int index = varInfo.getIndex();
				// TODO: Consider scoped re-usage of the same name.
				//  - Can't really do that until analysis logic is implemented
				//  - Disassembler makes each scope its own variable, so unless the user intervenes this shouldn't be a
				//    major concern.
				Element fs = varInfo.getFirstSource();
				Element ls = varInfo.getLastSource();
				LabelNode start;
				LabelNode end;
				if (doLimitVarRange) {
					if (fs instanceof CodeEntry) {
						Label label = code.getPrevLabel((CodeEntry) fs);
						if (label == null)
							throw new MethodCompileException(fs,
									"Cannot resolve usage of '" + varName + "' to start label!");
						start = labelMap.get(label.getName());
					} else if (fs instanceof MethodParameter || fs instanceof MethodDefinition) {
						Label label = code.getFirstLabel();
						if (label == null)
							throw new MethodCompileException(fs,
									"Cannot resolve usage of '" + varName + "' to start label!");
						start = labelMap.get(label.getName());
					} else {
						throw new MethodCompileException(fs,
								"Cannot resolve usage of '" + varName + "' to start label!");
					}
					if (ls instanceof CodeEntry) {
						Label label = code.getNextLabel((CodeEntry) ls);
						if (label == null)
							throw new MethodCompileException(fs,
									"Cannot resolve usage of '" + varName + "' to end label!");
						end = labelMap.get(label.getName());
					} else if (ls instanceof MethodParameter || fs instanceof MethodDefinition) {
						Label label = code.getLastLabel();
						if (label == null)
							throw new MethodCompileException(fs,
									"Cannot resolve usage of '" + varName + "' to end label!");
						end = labelMap.get(label.getName());
					} else {
						throw new MethodCompileException(ls,
								"Cannot resolve usage of '" + varName + "' to end label!");
					}
				} else {
					start = labelMap.get(code.getFirstLabel().getName());
					end = labelMap.get(code.getLastLabel().getName());
				}
				LocalVariableNode lvn = new LocalVariableNode(varName, varDesc, null, start, end, index);
				variableList.add(lvn);
			}
		MethodNode method = new MethodNode(access, name, descriptor, signature, null);
		List<AnnotationNode> visibleAnnotations = new ArrayList<>();
		List<AnnotationNode> invisibleAnnotations = new ArrayList<>();
		for (Annotation annotation : definition.getAnnotations()) {
			AnnotationNode node = new AnnotationNode("L" + annotation.getType() + ";");
			node.values = new ArrayList<>();
			annotation.getArgs().forEach((argName, argVal) -> {
				node.values.add(argName);
				node.values.add(AnnotationHelper.map(argVal));
			});
			if (annotation.isVisible()) {
				visibleAnnotations.add(node);
			} else {
				invisibleAnnotations.add(node);
			}
		}
		if (visibleAnnotations.size() > 0)
			method.visibleAnnotations = visibleAnnotations;
		else
			method.visibleAnnotations = null;
		if (invisibleAnnotations.size() > 0)
			method.invisibleAnnotations = invisibleAnnotations;
		else
			method.invisibleAnnotations = null;
		method.instructions = instructions;
		method.localVariables = variableList;
		method.exceptions.addAll(definition.getThrownExceptions().stream()
				.map(ThrownException::getExceptionType)
				.collect(Collectors.toList()));
		method.tryCatchBlocks.addAll(tryBlocks);
		method.visitMaxs(stack, variables.getCurrentUsedCap());
		return method;
	}

	/**
	 * @param insn
	 * 		Instruction to look up.
	 *
	 * @return AST element that was used to generate the instruction.
	 */
	public Element getAstFromInsn(AbstractInsnNode insn) {
		return insnToAstMap.get(insn);
	}

	/**
	 * @param insn
	 * 		Instruction to look up.
	 *
	 * @return Line number the AST that generated the instruction was defined on.
	 * {@code -1} if the instruction does not belong to the {@link #buildMethod() generated method}.
	 */
	public int getLineFromInsn(AbstractInsnNode insn) {
		Element element = getAstFromInsn(insn);
		if (element == null)
			return -1;
		return element.getLine();
	}

	/**
	 * Clear data.
	 */
	private void reset() {
		tryBlocks.clear();
		labelMap.clear();
		insnToAstMap.clear();
		variables.clear();
	}

	/**
	 * Populate the label name to instance map.
	 */
	private void createLabels() {
		for (String labelName : definition.getCode().getLabels().keySet()) {
			labelMap.put(labelName, new LabelNode());
		}
	}

	/**
	 * Populate the variables and lookup information.
	 *
	 * @throws MethodCompileException
	 * 		When the variable type usage is inconsistent/illegal,
	 * 		or when a variable index is already reserved by a wide variable of the prior slot.
	 */
	private void createVariables() throws MethodCompileException {
		variables.visitImplicitThis(selfType, definition);
		variables.visitParams(definition);
		if (!AccessFlag.isAbstract(definition.getModifiers().value())) {
			variables.visitCodeFirstPass(code);
			// Analyze the code so variable generation can yield more accurate types
			// We do so at this point so that the 'variables' has already populated some
			// basic level of variable types for the analyzer to work with.
			analysis = null; // reset first
			if (doUseAnalysis()) {
				Analyzer analyzer = new Analyzer(selfType, definition);
				analyzer.setInheritanceChecker(inheritanceChecker);
				analyzer.setExpressionToAstTransformer(exprToAst);
				try {
					analysis = analyzer.analyze();
				} catch (AstException ex) {
					throw new MethodCompileException(ex.getSource(), ex, ex.getMessage());
				}
				variables.visitCodeSecondPass(code, analysis);
			}
		}
	}

	/**
	 * Generate actual instructions.
	 *
	 * @return ASM instruction list.
	 *
	 * @throws MethodCompileException
	 * 		When a variable name-to-index mapping or label name-to-instance lookup fails.
	 */
	private InsnList createInstructions() throws MethodCompileException {
		InsnList list = new InsnList();
		for (AbstractInstruction instruction : code.getInstructions()) {
			int op = instruction.getOpcodeVal();
			switch (instruction.getInsnType()) {
				case LABEL:
					String labelName = ((Label) instruction).getName();
					LabelNode labelInstance = labelMap.get(labelName);
					if (labelInstance == null)
						throw new MethodCompileException(instruction,
								"No identifier mapping to label instance for '" + labelName + "'");
					addCode(list, instruction, labelInstance);
					break;
				case FIELD:
					FieldInstruction field = (FieldInstruction) instruction;
					addCode(list, instruction, new FieldInsnNode(op, field.getOwner(), field.getName(), field.getDesc()));
					break;
				case METHOD:
					MethodInstruction method = (MethodInstruction) instruction;
					addCode(list, instruction, new MethodInsnNode(op, method.getOwner(), method.getName(), method.getDesc(), method.isItf()));
					break;
				case INDY: {
					IndyInstruction indy = (IndyInstruction) instruction;
					HandleInfo hInfo = indy.getBsmHandle();
					boolean itf = hInfo.getTagVal() == Opcodes.H_INVOKEINTERFACE;
					Handle handle = new Handle(hInfo.getTagVal(), hInfo.getOwner(), hInfo.getName(), hInfo.getDesc(), itf);
					Object[] args = new Object[indy.getBsmArguments().size()];
					for (int i = 0; i < args.length; i++) {
						Object arg = indy.getBsmArguments().get(i).getValue();
						if (arg instanceof HandleInfo) {
							arg = ((HandleInfo) arg).toHandle();
						}
						args[i] = arg;
					}
					addCode(list, instruction, new InvokeDynamicInsnNode(indy.getName(), indy.getDesc(), handle, args));
					break;
				}
				case VAR: {
					VarInstruction variable = (VarInstruction) instruction;
					String id = variable.getVariableIdentifier();
					int index = variables.getIndex(id);
					if (index < 0)
						throw new MethodCompileException(variable,
								"No identifier mapping to variable slot for '" + id + "'");
					addCode(list, instruction, new VarInsnNode(op, index));
					break;
				}
				case IINC: {
					IincInstruction iinc = (IincInstruction) instruction;
					String id = iinc.getVariableIdentifier();
					int index = variables.getIndex(id);
					if (index < 0)
						throw new MethodCompileException(iinc,
								"No identifier mapping to variable slot for '" + id + "'");
					addCode(list, instruction, new IincInsnNode(index, iinc.getIncrement()));
					break;
				}
				case INT:
					IntInstruction integer = (IntInstruction) instruction;
					addCode(list, instruction, new IntInsnNode(op, integer.getValue()));
					break;
				case LDC:
					LdcInstruction ldc = (LdcInstruction) instruction;
					Object cst = ldc.getValue();
					// TODO: We need to handle escapes better, but this works for \\u0000 escapes for now
					if (cst instanceof String)
						cst = StringEscapeUtils.unescapeJava((String) cst);
					addCode(list, instruction, new LdcInsnNode(cst));
					break;
				case TYPE:
					TypeInstruction type = (TypeInstruction) instruction;
					addCode(list, instruction, new TypeInsnNode(op, type.getType()));
					break;
				case MULTIARRAY:
					MultiArrayInstruction marray = (MultiArrayInstruction) instruction;
					addCode(list, instruction, new MultiANewArrayInsnNode(marray.getDesc(), marray.getDimensions()));
					break;
				case NEWARRAY:
					NewArrayInstruction narray = (NewArrayInstruction) instruction;
					addCode(list, instruction, new IntInsnNode(Opcodes.NEWARRAY, narray.getArrayTypeInt()));
					break;
				case INSN:
					addCode(list, instruction, new InsnNode(op));
					break;
				case JUMP: {
					JumpInstruction jump = (JumpInstruction) instruction;
					LabelNode target = labelMap.get(jump.getLabel());
					if (target == null)
						throw new MethodCompileException(instruction,
								"No identifier mapping to label instance for '" + jump.getLabel() + "'");
					addCode(list, instruction, new JumpInsnNode(op, target));
					break;
				}
				case LOOKUP: {
					LookupSwitchInstruction lookup = (LookupSwitchInstruction) instruction;
					int[] keys = new int[lookup.getEntries().size()];
					LabelNode[] labels = new LabelNode[lookup.getEntries().size()];
					int i = 0;
					for (LookupSwitchInstruction.Entry e : lookup.getEntries()) {
						int key = e.getKey();
						LabelNode value = labelMap.get(e.getName());
						if (value == null)
							throw new MethodCompileException(instruction,
									"No identifier mapping to label instance for '" + e.getName() + "'");
						keys[i] = key;
						labels[i] = value;
						i++;
					}
					String dfltName = lookup.getDefaultIdentifier();
					LabelNode dflt = labelMap.get(dfltName);
					if (dflt == null)
						throw new MethodCompileException(instruction,
								"No identifier mapping to label instance for '" + dfltName + "'");
					addCode(list, instruction, new LookupSwitchInsnNode(dflt, keys, labels));
					break;
				}
				case TABLE: {
					TableSwitchInstruction table = (TableSwitchInstruction) instruction;
					LabelNode[] labels = new LabelNode[table.getLabels().size()];
					for (int i = 0; i < labels.length; i++) {
						String name = table.getLabels().get(i);
						LabelNode value = labelMap.get(name);
						if (value == null)
							throw new MethodCompileException(instruction,
									"No identifier mapping to label instance for '" + name + "'");
						labels[i] = value;
					}
					String dfltName = table.getDefaultIdentifier();
					LabelNode dflt = labelMap.get(dfltName);
					if (dflt == null)
						throw new MethodCompileException(instruction,
								"No identifier mapping to label instance for '" + dfltName + "'");
					addCode(list, instruction, new TableSwitchInsnNode(table.getMin(), table.getMax(), dflt, labels));
					break;
				}
				case LINE: {
					LineInstruction line = (LineInstruction) instruction;
					LabelNode target = labelMap.get(line.getLabel());
					if (target == null)
						throw new MethodCompileException(instruction,
								"No identifier mapping to label instance for '" + line.getLabel() + "'");
					addCode(list, instruction, new LineNumberNode(line.getLineNo(), target));
					break;
				}
				case EXPRESSION: {
					Expression expression = (Expression) instruction;
					if (classSupplier == null)
						throw new MethodCompileException(expression,
								"Expression not supported, translator not given class supplier!");
					try {
						ExpressionToAsmTransformer.TransformResult result = exprToAsm.transform(expression);
						addCode(list, instruction, result.getInstructions());
						tryBlocks.addAll(result.getTryBlocks());
					} catch (Exception ex) {
						throw new MethodCompileException(expression, ex, "Failed to compile expression");
					}
				}
			}
		}
		return list;
	}

	/**
	 * @return Lookup for class inheritance. Used to compute common usage types of variables.
	 */
	public InheritanceChecker getInheritanceChecker() {
		return inheritanceChecker;
	}

	/**
	 * @param inheritanceChecker
	 * 		Lookup for class inheritance.
	 */
	public void setInheritanceChecker(InheritanceChecker inheritanceChecker) {
		this.inheritanceChecker = inheritanceChecker;
	}

	/**
	 * @return {@code true} when variables should limit their scope based on first and last seen references.
	 * {@code false} when variables are emitted without scope, occupying the entire method.
	 */
	public boolean doLimitVarRange() {
		return doLimitVarRange;
	}

	/**
	 * @param doLimitVarRange
	 * 		Limiting value.
	 *
	 * @see #doLimitVarRange() Detailed explaination of value.
	 */
	public void setDoLimitVarRange(boolean doLimitVarRange) {
		this.doLimitVarRange = doLimitVarRange;
	}

	/**
	 * {@code true} will generate a {@link #getAnalysis() analysis instance} allowing more detailed output.
	 *
	 * @return Flag to analyze AST for improved results.
	 */
	public boolean doUseAnalysis() {
		return useAnalysis;
	}

	/**
	 * @param useAnalysis
	 * 		Flag to analyze AST for improved results.
	 */
	public void setUseAnalysis(boolean useAnalysis) {
		this.useAnalysis = useAnalysis;
	}

	/**
	 * @param definition
	 * 		Definition to transform.
	 */
	public void setDefinition(MethodDefinition definition) {
		// Only trigger changes if the incoming unit is different
		if (!Objects.equals(getDefinition(), definition)) {
			this.definition = definition;
			// Convenience
			this.code = this.definition.getCode();
			// Initialize transformers
			this.exprToAsm = new ExpressionToAsmTransformer(classSupplier, definition, variables, selfType);
			this.exprToAst = new ExpressionToAstTransformer(definition, variables, exprToAsm);
		}
	}

	/**
	 * @return Current definition.
	 */
	public MethodDefinition getDefinition() {
		return definition;
	}

	/**
	 * Generated via {@link #visit()} when {@link #doUseAnalysis()} is set to {@code true}.
	 *
	 * @return Analysis results containing stack and local variable information.
	 */
	public Analysis getAnalysis() {
		return analysis;
	}

	/**
	 * Generated via {@link #visit()}.
	 * Results are enchanced when {@link #doUseAnalysis()} is set to {@code true}.
	 *
	 * @return Generated variables.
	 */
	public Variables getVariables() {
		return variables;
	}

	/**
	 * Adds the given list of instructions to the instruction list,
	 * and associated the instruction back to it's AST element.
	 *
	 * @param list
	 * 		List to add instruction into.
	 * @param element
	 * 		Element representing the instruction.
	 * @param insns
	 * 		Generated instructions to add.
	 */
	private void addCode(InsnList list, Element element, InsnList insns) {
		for (AbstractInsnNode insn : insns)
			addCode(list, element, insn);
	}

	/**
	 * Adds the given instruction to the instruction list,
	 * and associated the instruction back to it's AST element.
	 *
	 * @param list
	 * 		List to add instruction into.
	 * @param element
	 * 		Element representing the instruction.
	 * @param insn
	 * 		Generated instruction to add.
	 */
	private void addCode(InsnList list, Element element, AbstractInsnNode insn) {
		list.add(insn);
		insnToAstMap.put(insn, element);
	}
}
