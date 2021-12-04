package me.coley.recaf.assemble.transformer;

import javassist.*;
import javassist.bytecode.MethodInfo;
import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.ast.*;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.ThrownException;
import me.coley.recaf.assemble.ast.arch.TryCatch;
import me.coley.recaf.assemble.ast.insn.*;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.assemble.compiler.ClassSupplier;
import me.coley.recaf.assemble.compiler.JavassistASMTranslator;
import me.coley.recaf.assemble.compiler.JavassistCompilationResult;
import me.coley.recaf.assemble.compiler.JavassistCompiler;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
	private final ClassSupplier classSupplier;
	private final String selfType;
	private final Unit unit;
	// For quick reference
	private final MethodDefinition definition;
	private final Code code;
	// Method building
	private InsnList instructions;

	/**
	 * @param selfType
	 * 		The internal type of the class defining the method.
	 * @param unit
	 * 		The unit to pull data from.
	 */
	public AstToMethodTransformer(String selfType, Unit unit) {
		this(null, selfType, unit);
	}

	/**
	 * @param classSupplier
	 * 		Class information supplier. Required to support expressions.
	 * @param selfType
	 * 		The internal type of the class defining the method.
	 * @param unit
	 * 		The unit to pull data from.
	 */
	public AstToMethodTransformer(ClassSupplier classSupplier, String selfType, Unit unit) {
		this.classSupplier = classSupplier;
		this.unit = Objects.requireNonNull(unit);
		this.selfType = selfType;
		this.definition = (MethodDefinition) unit.getDefinition();
		this.code = unit.getCode();
	}

	/**
	 * Visits the AST and collects information necessary for building the method.
	 *
	 * @throws MethodCompileException
	 * 		When the variable type usage is inconsistent/illegal,
	 * 		or when a variable index is already reserved by a wide variable of the prior slot.
	 */
	public void visit() throws MethodCompileException {
		reset();
		createLabels();
		createVariables();
		instructions = createInstructions();
	}

	/**
	 * @return Generated method.
	 *
	 * @throws MethodCompileException
	 * 		When the try-catch labels could not be mapped to instances,
	 * 		or when code hasn't been visited. Please call {@link #visit()} first.
	 */
	public MethodNode get() throws MethodCompileException {
		if (instructions == null) {
			throw new MethodCompileException(unit, "The instructions have not been successfully generated!" +
					"Cannot build method instance.");
		}
		int access = definition.getModifiers().value();
		String name = definition.getName();
		String descriptor = definition.getDesc();
		String signature = code.getSignature() != null ? code.getSignature().getSignature() : null;
		int stack = 0xFF;
		for (TryCatch tryCatch : code.getTryCatches()) {
			LabelNode start = labelMap.get(tryCatch.getStartLabel());
			LabelNode end = labelMap.get(tryCatch.getEndLabel());
			LabelNode handler = labelMap.get(tryCatch.getHandlerLabel());
			if (start == null)
				throw new MethodCompileException(tryCatch,
						"No identifier mapping to label instance for '" + tryCatch.getStartLabel() + "'");
			if (end == null)
				throw new MethodCompileException(tryCatch,
						"No identifier mapping to label instance for '" + tryCatch.getEndLabel() + "'");
			if (handler == null)
				throw new MethodCompileException(tryCatch,
						"No identifier mapping to label instance for '" + tryCatch.getHandlerLabel() + "'");
			tryBlocks.add(new TryCatchBlockNode(start, end, handler, tryCatch.getExceptionType()));
		}
		MethodNode method = new MethodNode(access, name, descriptor, signature, null);
		method.instructions = instructions;
		method.exceptions.addAll(code.getThrownExceptions().stream()
				.map(ThrownException::getExceptionType)
				.collect(Collectors.toList()));
		method.tryCatchBlocks.addAll(tryBlocks);
		method.visitMaxs(stack, variables.getCurrentUsedCap());
		AnnotationHelper.visitAnnos(method, code.getAnnotations());
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
	 * {@code -1} if the instruction does not belong to the {@link #get() generated method}.
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
		for (String labelName : unit.getCode().getLabels().keySet()) {
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
		variables.visitDefinition(selfType, definition);
		variables.visitParams(definition);
		variables.visitCode(code);
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
		for (CodeEntry entry : code.getEntries()) {
			if (entry instanceof Label) {
				String labelName = ((Label) entry).getName();
				LabelNode labelInstance = labelMap.get(labelName);
				if (labelInstance == null)
					throw new MethodCompileException(entry,
							"No identifier mapping to label instance for '" + labelName + "'");
				addCode(list, entry, labelInstance);
			} else if (entry instanceof AbstractInstruction) {
				AbstractInstruction instruction = (AbstractInstruction) entry;
				int op = instruction.getOpcodeVal();
				switch (instruction.getInsnType()) {
					case FIELD:
						FieldInstruction field = (FieldInstruction) instruction;
						addCode(list, entry, new FieldInsnNode(op, field.getOwner(), field.getName(), field.getDesc()));
						break;
					case METHOD:
						MethodInstruction method = (MethodInstruction) instruction;
						addCode(list, entry, new MethodInsnNode(op, method.getOwner(), method.getName(), method.getDesc()));
						break;
					case INDY: {
						IndyInstruction indy = (IndyInstruction) instruction;
						HandleInfo hInfo = indy.getBsmHandle();
						boolean itf = hInfo.getTagVal() == Opcodes.H_INVOKEINTERFACE;
						Handle handle = new Handle(hInfo.getTagVal(), hInfo.getOwner(), hInfo.getName(), hInfo.getDesc(), itf);
						Object[] args = new Object[indy.getBsmArguments().size()];
						for (int i = 0; i < args.length; i++) {
							args[i] = indy.getBsmArguments().get(i).getValue();
						}
						addCode(list, entry, new InvokeDynamicInsnNode(indy.getName(), indy.getDesc(), handle, args));
						break;
					}
					case VAR: {
						VarInstruction variable = (VarInstruction) instruction;
						String id = variable.getVariableIdentifier();
						int index = variables.getIndex(id);
						if (index < 0)
							throw new MethodCompileException(variable,
									"No identifier mapping to variable slot for '" + id + "'");
						addCode(list, entry, new VarInsnNode(op, index));
						break;
					}
					case IINC: {
						IincInstruction iinc = (IincInstruction) instruction;
						String id = iinc.getVariableIdentifier();
						int index = variables.getIndex(id);
						if (index < 0)
							throw new MethodCompileException(iinc,
									"No identifier mapping to variable slot for '" + id + "'");
						addCode(list, entry, new IincInsnNode(index, iinc.getIncrement()));
						break;
					}
					case INT:
						IntInstruction integer = (IntInstruction) instruction;
						addCode(list, entry, new IntInsnNode(op, integer.getValue()));
						break;
					case LDC:
						LdcInstruction ldc = (LdcInstruction) instruction;
						addCode(list, entry, new LdcInsnNode(ldc.getValue()));
						break;
					case TYPE:
						TypeInstruction type = (TypeInstruction) instruction;
						addCode(list, entry, new TypeInsnNode(op, type.getType()));
						break;
					case MULTIARRAY:
						MultiArrayInstruction marray = (MultiArrayInstruction) instruction;
						addCode(list, entry, new MultiANewArrayInsnNode(marray.getDesc(), marray.getDimensions()));
						break;
					case NEWARRAY:
						NewArrayInstruction narray = (NewArrayInstruction) instruction;
						addCode(list, entry, new IntInsnNode(Opcodes.NEWARRAY, narray.getArrayTypeInt()));
						break;
					case INSN:
						addCode(list, entry, new InsnNode(op));
						break;
					case JUMP: {
						JumpInstruction jump = (JumpInstruction) instruction;
						LabelNode target = labelMap.get(jump.getLabel());
						if (target == null)
							throw new MethodCompileException(instruction,
									"No identifier mapping to label instance for '" + jump.getLabel() + "'");
						addCode(list, entry, new JumpInsnNode(op, target));
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
						addCode(list, entry, new LookupSwitchInsnNode(dflt, keys, labels));
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
						}
						String dfltName = table.getDefaultIdentifier();
						LabelNode dflt = labelMap.get(dfltName);
						if (dflt == null)
							throw new MethodCompileException(instruction,
									"No identifier mapping to label instance for '" + dfltName + "'");
						addCode(list, entry, new TableSwitchInsnNode(table.getMin(), table.getMax(), dflt, labels));
						break;
					}
					case LINE: {
						LineInstruction line = (LineInstruction) instruction;
						LabelNode target = labelMap.get(line.getLabel());
						if (target == null)
							throw new MethodCompileException(instruction,
									"No identifier mapping to label instance for '" + line.getLabel() + "'");
						addCode(list, entry, new LineNumberNode(line.getLineNo(), target));
						break;
					}
				}
			} else if (entry instanceof Expression) {
				Expression expression = (Expression) entry;
				if (classSupplier == null)
					throw new MethodCompileException(expression,
							"Expression not supported, translator not given class supplier!");
				try {
					CtClass declaring;
					byte[] selfClassBytes = classSupplier.getClass(selfType);
					if (selfClassBytes != null) {
						// Fetched from supplier
						InputStream stream = new ByteArrayInputStream(selfClassBytes);
						declaring = ClassPool.getDefault().makeClass(stream, false);
					} else {
						// Fallback, make a new class
						declaring = ClassPool.getDefault().makeClass(selfType);
					}
					if (declaring.isFrozen())
						declaring.defrost();
					CtBehavior containerMethod;
					String name = definition.getName();
					String descriptor = definition.getDesc();
					try {
						if (name.equals("<init>")) {
							containerMethod = declaring.getConstructor(descriptor);
						} else {
							containerMethod = declaring.getMethod(name, descriptor);
						}
					} catch (NotFoundException nfe) {
						// Seriously, fuck Javassist for not having a simple "hasX" and instead just throwing
						// unchecked exceptions instead. This is beyond stupid.
						MethodInfo minfo = new MethodInfo(declaring.getClassFile().getConstPool(), name, descriptor);
						containerMethod = CtMethod.make(minfo, declaring);
						declaring.addMethod((CtMethod) containerMethod);
					}
					// Compile with Javassist
					JavassistCompilationResult result =
							JavassistCompiler.compileExpression(declaring, containerMethod,
									classSupplier, expression, variables);
					// Translate to ASM
					JavassistASMTranslator translator = new JavassistASMTranslator();
					translator.visit(declaring, result.getBytecode().toCodeAttribute());
					addCode(list, entry, translator.getInstructions());
					tryBlocks.addAll(translator.getTryBlocks());
				} catch (Exception ex) {
					throw new MethodCompileException(expression, ex, "Failed to compile expression");
				}
			}
		}
		return list;
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
