package me.coley.recaf.assemble.analysis;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.IllegalAstException;
import me.coley.recaf.assemble.analysis.insn.*;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.FlowControl;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.TryCatch;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.assemble.transformer.ExpressionToAstTransformer;
import me.coley.recaf.assemble.util.InheritanceChecker;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.DebuggingLogger;
import me.coley.recaf.util.logging.Logging;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * Stack analysis for {@link Analyzer}.
 *
 * @author Matt Coley
 */
public class CodeExecutor {
	private static final DebuggingLogger logger = Logging.get(CodeExecutor.class);
	private static final Map<Integer, InstructionExecutor> INSN_EXECUTORS = new HashMap<>();
	// Input
	private List<AbstractInstruction> instructions = Collections.emptyList();
	private Map<String, Label> labelMap = Collections.emptyMap();
	private Map<Label, String> catchHandlerTypes = Collections.emptyMap();
	private List<TryCatch> tryCatches = Collections.emptyList();
	// Tools
	private InheritanceChecker inheritanceChecker;
	private ExpressionToAstTransformer expressionToAstTransformer;
	// Output
	private Analysis analysis;

	/**
	 * Configure the executor with data pulled from the given code AST.
	 *
	 * @param code
	 * 		Code AST to pull data from.
	 *
	 * @throws AnalysisException
	 * 		Propagated from {@link #updateCatchHandlerTypesMap()}.
	 */
	public void configure(Code code) throws AnalysisException {
		setInstructions(code.getInstructions());
		setTryCatches(code.getTryCatches());
		setLabelMap(code.getLabels());
		updateCatchHandlerTypesMap();
	}

	/**
	 * Requires {@link #setLabelMap(Map)} and {@link #setTryCatches(List)}.
	 * Maps the {@link Label} instances to the catch handler exception types pushed at that location.
	 *
	 * @throws AnalysisException
	 * 		When a label used in a {@link TryCatch} could not be found in the label map.
	 */
	public void updateCatchHandlerTypesMap() throws AnalysisException {
		Map<Label, String> map = new HashMap<>();
		for (TryCatch tryCatch : tryCatches) {
			Label handlerLabel = labelMap.get(tryCatch.getHandlerLabel());
			if (handlerLabel == null)
				throw new AnalysisException(tryCatch, "No associated handler label");
			String type = tryCatch.getExceptionType();
			if (type == null)
				type = "java/lang/Throwable";
			map.merge(handlerLabel, type, (a, b) -> inheritanceChecker.getCommonType(a, b));
		}
		setCatchHandlerTypes(map);
	}

	/**
	 * Execute the instructions.
	 * <br>
	 * Results are stored in the given {@link Analysis} instance ({@link #setAnalysis(Analysis)}).
	 * It is assumed that the analysis instance is fresh and not being re-used.
	 *
	 * @throws AnalysisException
	 * 		When executed code contains
	 * @see #getAnalysis() Upon completion.
	 */
	public void execute() throws AnalysisException {
		// Visit starting instruction
		branch(-1, 0);
		// Visit the handler block of all try-catches.
		// But only visit each block once (with the common exception type) rather than once per handled type.
		Set<Label> visitedHandlerLabels = new HashSet<>();
		for (TryCatch tryCatch : tryCatches) {
			Label handlerLabel = labelMap.get(tryCatch.getHandlerLabel());
			if (visitedHandlerLabels.contains(handlerLabel))
				continue;
			visitedHandlerLabels.add(handlerLabel);
			int handlerIndex = instructions.indexOf(handlerLabel);
			branch(Integer.MIN_VALUE, handlerIndex);
		}
	}

	/**
	 * @param instructions
	 * 		Instructions to 'execute'
	 */
	public void setInstructions(List<AbstractInstruction> instructions) {
		this.instructions = instructions;
	}

	/**
	 * @param labelMap
	 * 		Mapping of label names to label instances.
	 */
	public void setLabelMap(Map<String, Label> labelMap) {
		this.labelMap = labelMap;
	}

	/**
	 * @param catchHandlerTypes
	 * 		Mapping of label instances to the types of caught exception types pushed,
	 * 		as declared by {@link TryCatch} elements.
	 */
	public void setCatchHandlerTypes(Map<Label, String> catchHandlerTypes) {
		this.catchHandlerTypes = catchHandlerTypes;
	}

	/**
	 * @param tryCatches
	 * 		The try-catch ranges for the {@link #setInstructions(List) instructions}.
	 */
	public void setTryCatches(List<TryCatch> tryCatches) {
		this.tryCatches = tryCatches;
	}

	/**
	 * @param inheritanceChecker
	 * 		Inheritance checker to use for computing common types.
	 */
	public void setInheritanceChecker(InheritanceChecker inheritanceChecker) {
		this.inheritanceChecker = inheritanceChecker;
	}

	/**
	 * @param expressionToAstTransformer
	 * 		Transformer for mapping {@link Expression} elements to blocks of {@link AbstractInstruction instructions}.
	 */
	public void setExpressionToAstTransformer(ExpressionToAstTransformer expressionToAstTransformer) {
		this.expressionToAstTransformer = expressionToAstTransformer;
	}

	/**
	 * @return Analysis instance holding results of the stack-frame computation.
	 */
	public Analysis getAnalysis() {
		return analysis;
	}

	/**
	 * @param analysis
	 * 		Analysis instance to store results into.
	 */
	public void setAnalysis(Analysis analysis) {
		this.analysis = analysis;
	}

	/**
	 * @param source
	 * 		Originating instruction offset.
	 * @param target
	 * 		Target offset to branch to.
	 *
	 * @throws AnalysisException
	 * 		Propagated from {@link #execute(int, int)}.
	 */
	public void branch(int source, int target) throws AnalysisException {
		int initialCtxPc = source;
		int maxPc = instructions.size();
		int pc = target;
		logger.debugging(l -> l.info("Branch from {} --> {}", initialCtxPc, target));
		while (pc < maxPc) {
			if (execute(source, pc)) {
				source = pc;
				pc++;
			} else {
				break;
			}
		}
	}

	/**
	 * @param contextOffset
	 * 		Prior executed instruction index.
	 * @param currentOffset
	 * 		Instruction index to execute.
	 *
	 * @return {@code true} for continued execution.
	 * {@code false} for when execution ends <i>(Such as encountering a {@code RETURN} instruction)</i>
	 *
	 * @throws AnalysisException
	 * 		When an {@link InstructionExecutor} fails to handle a given instruction,
	 * 		when {@link FlowControl} targets cannot be found in the current instructions list,
	 * 		or when the {@link ExpressionToAstTransformer} is not set.
	 */
	public boolean execute(int contextOffset, int currentOffset) throws AnalysisException {
		AbstractInstruction instruction = instructions.get(currentOffset);
		Frame frame = analysis.frame(currentOffset);
		Frame oldFrameState = frame.copy();
		// Mark as visited
		boolean wasVisited = frame.markVisited();
		if (contextOffset >= 0) {
			// Need to populate frame from prior state if we've not already done so
			Frame priorFrame = analysis.frame(contextOffset);
			frame.copy(priorFrame);
		}
		logger.debugging(l -> l.info("Executing {} : {}", currentOffset, instruction.print(PrintContext.DEFAULT_CTX)));
		logger.debugging(l -> l.info(" - Stack PRE: {}", frame.getStack().isEmpty() ? "." :
				StringUtil.substringRelative(frame.getStack().toString(), 1, 1)));
		// Collect flow control paths, track if the path is forced.
		// If it is forced we won't be going to the next instruction.
		boolean continueNextExec = true;
		List<Label> flowDestinations = new ArrayList<>();
		if (instruction instanceof FlowControl) {
			FlowControl flow = (FlowControl) instruction;
			try {
				for (Label label : flow.getTargets(labelMap)) {
					if (!flowDestinations.contains(label))
						flowDestinations.add(label);
				}
			} catch (IllegalAstException ex) {
				throw new AnalysisException(flow, ex, "Failed to lookup flow targets");
			}
			// Visit next PC if flow is not forced
			// Examples of forced flow:
			//  - GOTO
			//  - TABLE/LOOKUP-SWITCH
			continueNextExec = !flow.isForced();
		} else {
			// X-RETURN and ATHROW end execution of the current block.
			int op = instruction.getOpcodeVal();
			if (op >= Opcodes.IRETURN && op <= Opcodes.RETURN)
				continueNextExec = false;
			else if (op == Opcodes.ATHROW)
				continueNextExec = false;
		}
		// Handle stack
		if (instruction instanceof Expression) {
			// Ensure the analyzer supports expression unrolling
			if (expressionToAstTransformer == null)
				throw new AnalysisException(instruction, "Expression transformer not supplied!");
			// When the PC hits an expression, jump into the converted AST
			try {
				// Convert to independent code block
				Code exprCode = expressionToAstTransformer.transform((Expression) instruction);
				// Create analysis results for the code block.
				// Override the first frame so that its initializer copies state from the current frame.
				List<AbstractInstruction> exprInstructions = exprCode.getChildrenOfType(AbstractInstruction.class);
				Analysis exprResults = new Analysis(exprInstructions.size());
				exprResults.getFrames().set(0, new Frame() {
					@Override
					public void initialize(String selfTypeName, MethodDefinition definition) {
						super.initialize(selfTypeName, definition);
						getLocals().putAll(frame.getLocals());
					}
				});
				// Analyze the expression code. Use a copied executor using the converted expression AST as its source.
				CodeExecutor executor = new CodeExecutor();
				executor.setAnalysis(exprResults);
				executor.configure(exprCode);
				executor.setInheritanceChecker(inheritanceChecker);
				executor.execute();
				// Merge the last frame's results into the current frame
				List<Frame> exprFrames = exprResults.getFrames();
				Frame exprLastFrame = exprFrames.get(exprFrames.size() - 1);
				frame.merge(exprLastFrame, inheritanceChecker);
			} catch (Exception ex) {
				throw new AnalysisException(instruction, ex);
			}
		} else if (instruction instanceof Label) {
			// Try-catch handler blocks push throwable types onto the stack
			Label label = (Label) instruction;
			String type = catchHandlerTypes.get(label);
			if (type != null) {
				List<Value> stack = frame.getStack();
				// We will enforce exception type here,
				// because there are some obfuscators
				// that make jumps into handler blocks
				while (!stack.isEmpty())
					frame.pop();
				frame.push(new Value.ObjectValue(Type.getObjectType(type)));
			}
		} else {
			int op = instruction.getOpcodeVal();
			if (op >= 0) {
				InstructionExecutor insnExecutor = INSN_EXECUTORS.get(op);
				if (insnExecutor == null)
					throw new AnalysisException(instruction, "No instruction executor registered for instruction type: "
							+ instruction.getOpcode());
				insnExecutor.handle(frame, instruction);
			}
		}
		logger.debugging(l -> l.info(" - Stack POST: {}", frame.getStack().isEmpty() ? "." :
				StringUtil.substringRelative(frame.getStack().toString(), 1, 1)));
		// If we had already visited the frame the following frames may already be done.
		// We only need to recompute them if the old state and new state have matching local/stack states.
		boolean mergeWasDiff = false;
		if (wasVisited) {
			try {
				mergeWasDiff = frame.merge(oldFrameState, inheritanceChecker);
			} catch (FrameMergeException ex) {
				throw new AnalysisException(instruction, ex);
			}
		}
		// Now jump to the potential destinations
		if (!wasVisited || mergeWasDiff) {
			logger.debugging(l -> l.info(" - {}", !wasVisited ? "NOT VISITED" : "STATE DIFFERENCE"));
			for (Label flowDestination : flowDestinations) {
				int labelPc = instructions.indexOf(flowDestination);
				branch(currentOffset, labelPc);
			}
		} else {
			logger.debugging(l -> l.info(" - VISITED & NO STATE DIFF"));
			continueNextExec = false;
		}
		// Only continue to next adjacent instruction if needed
		return continueNextExec;
	}

	static {
		// Instructions are in order of opcode value (see Opcodes from ASM)
		NopExecutor nopExecutor = new NopExecutor();
		INSN_EXECUTORS.put(-1, nopExecutor); // catch-all
		INSN_EXECUTORS.put(NOP, nopExecutor);
		INSN_EXECUTORS.put(ACONST_NULL, new ConstPushExecutor(new Value.NullValue()));
		INSN_EXECUTORS.put(ICONST_M1, new ConstPushExecutor(new Value.NumericValue(INT_TYPE, -1)));
		INSN_EXECUTORS.put(ICONST_0, new ConstPushExecutor(new Value.NumericValue(INT_TYPE, 0)));
		INSN_EXECUTORS.put(ICONST_1, new ConstPushExecutor(new Value.NumericValue(INT_TYPE, 1)));
		INSN_EXECUTORS.put(ICONST_2, new ConstPushExecutor(new Value.NumericValue(INT_TYPE, 2)));
		INSN_EXECUTORS.put(ICONST_3, new ConstPushExecutor(new Value.NumericValue(INT_TYPE, 3)));
		INSN_EXECUTORS.put(ICONST_4, new ConstPushExecutor(new Value.NumericValue(INT_TYPE, 4)));
		INSN_EXECUTORS.put(ICONST_5, new ConstPushExecutor(new Value.NumericValue(INT_TYPE, 5)));
		INSN_EXECUTORS.put(LCONST_0, new ConstPushExecutor(new Value.NumericValue(LONG_TYPE, 0L)));
		INSN_EXECUTORS.put(LCONST_1, new ConstPushExecutor(new Value.NumericValue(LONG_TYPE, 1L)));
		INSN_EXECUTORS.put(FCONST_0, new ConstPushExecutor(new Value.NumericValue(FLOAT_TYPE, 0F)));
		INSN_EXECUTORS.put(FCONST_1, new ConstPushExecutor(new Value.NumericValue(FLOAT_TYPE, 1F)));
		INSN_EXECUTORS.put(FCONST_2, new ConstPushExecutor(new Value.NumericValue(FLOAT_TYPE, 2F)));
		INSN_EXECUTORS.put(DCONST_0, new ConstPushExecutor(new Value.NumericValue(DOUBLE_TYPE, 0D)));
		INSN_EXECUTORS.put(DCONST_1, new ConstPushExecutor(new Value.NumericValue(DOUBLE_TYPE, 1D)));
		IntPushExecutor intPushExecutor = new IntPushExecutor();
		INSN_EXECUTORS.put(BIPUSH, intPushExecutor);
		INSN_EXECUTORS.put(SIPUSH, intPushExecutor);
		INSN_EXECUTORS.put(LDC, new LdcExecutor());
		VarLoadExecutor varLoadExecutor = new VarLoadExecutor();
		INSN_EXECUTORS.put(ILOAD, varLoadExecutor);
		INSN_EXECUTORS.put(LLOAD, varLoadExecutor);
		INSN_EXECUTORS.put(FLOAD, varLoadExecutor);
		INSN_EXECUTORS.put(DLOAD, varLoadExecutor);
		INSN_EXECUTORS.put(ALOAD, varLoadExecutor);
		ArrayLoadExecutor arrayLoadExecutor = new ArrayLoadExecutor();
		INSN_EXECUTORS.put(IALOAD, arrayLoadExecutor);
		INSN_EXECUTORS.put(LALOAD, arrayLoadExecutor);
		INSN_EXECUTORS.put(FALOAD, arrayLoadExecutor);
		INSN_EXECUTORS.put(DALOAD, arrayLoadExecutor);
		INSN_EXECUTORS.put(AALOAD, arrayLoadExecutor);
		INSN_EXECUTORS.put(BALOAD, arrayLoadExecutor);
		INSN_EXECUTORS.put(CALOAD, arrayLoadExecutor);
		INSN_EXECUTORS.put(SALOAD, arrayLoadExecutor);
		VarStoreExecutor varStoreExecutor = new VarStoreExecutor();
		INSN_EXECUTORS.put(ISTORE, varStoreExecutor);
		INSN_EXECUTORS.put(LSTORE, varStoreExecutor);
		INSN_EXECUTORS.put(FSTORE, varStoreExecutor);
		INSN_EXECUTORS.put(DSTORE, varStoreExecutor);
		INSN_EXECUTORS.put(ASTORE, varStoreExecutor);
		ArrayStoreExecutor arrayStoreExecutor = new ArrayStoreExecutor();
		INSN_EXECUTORS.put(IASTORE, arrayStoreExecutor);
		INSN_EXECUTORS.put(LASTORE, arrayStoreExecutor);
		INSN_EXECUTORS.put(FASTORE, arrayStoreExecutor);
		INSN_EXECUTORS.put(DASTORE, arrayStoreExecutor);
		INSN_EXECUTORS.put(AASTORE, arrayStoreExecutor);
		INSN_EXECUTORS.put(BASTORE, arrayStoreExecutor);
		INSN_EXECUTORS.put(CASTORE, arrayStoreExecutor);
		INSN_EXECUTORS.put(SASTORE, arrayStoreExecutor);
		INSN_EXECUTORS.put(POP, new PopExecutor());
		INSN_EXECUTORS.put(POP2, new Pop2Executor());
		INSN_EXECUTORS.put(DUP, new DupExecutor());
		INSN_EXECUTORS.put(DUP_X1, new DupX1Executor());
		INSN_EXECUTORS.put(DUP_X2, new DupX2Executor());
		INSN_EXECUTORS.put(DUP2, new Dup2Executor());
		INSN_EXECUTORS.put(DUP2_X1, new Dup2X1Executor());
		INSN_EXECUTORS.put(DUP2_X2, new Dup2X2Executor());
		INSN_EXECUTORS.put(SWAP, new SwapExecutor());
		MathExecutor mathExecutor = new MathExecutor();
		INSN_EXECUTORS.put(IADD, mathExecutor);
		INSN_EXECUTORS.put(LADD, mathExecutor);
		INSN_EXECUTORS.put(FADD, mathExecutor);
		INSN_EXECUTORS.put(DADD, mathExecutor);
		INSN_EXECUTORS.put(ISUB, mathExecutor);
		INSN_EXECUTORS.put(LSUB, mathExecutor);
		INSN_EXECUTORS.put(FSUB, mathExecutor);
		INSN_EXECUTORS.put(DSUB, mathExecutor);
		INSN_EXECUTORS.put(IMUL, mathExecutor);
		INSN_EXECUTORS.put(LMUL, mathExecutor);
		INSN_EXECUTORS.put(FMUL, mathExecutor);
		INSN_EXECUTORS.put(DMUL, mathExecutor);
		INSN_EXECUTORS.put(IDIV, mathExecutor);
		INSN_EXECUTORS.put(LDIV, mathExecutor);
		INSN_EXECUTORS.put(FDIV, mathExecutor);
		INSN_EXECUTORS.put(DDIV, mathExecutor);
		INSN_EXECUTORS.put(IREM, mathExecutor);
		INSN_EXECUTORS.put(LREM, mathExecutor);
		INSN_EXECUTORS.put(FREM, mathExecutor);
		INSN_EXECUTORS.put(DREM, mathExecutor);
		INSN_EXECUTORS.put(INEG, mathExecutor);
		INSN_EXECUTORS.put(LNEG, mathExecutor);
		INSN_EXECUTORS.put(FNEG, mathExecutor);
		INSN_EXECUTORS.put(DNEG, mathExecutor);
		INSN_EXECUTORS.put(ISHL, mathExecutor);
		INSN_EXECUTORS.put(LSHL, mathExecutor);
		INSN_EXECUTORS.put(ISHR, mathExecutor);
		INSN_EXECUTORS.put(LSHR, mathExecutor);
		INSN_EXECUTORS.put(IUSHR, mathExecutor);
		INSN_EXECUTORS.put(LUSHR, mathExecutor);
		INSN_EXECUTORS.put(IAND, mathExecutor);
		INSN_EXECUTORS.put(LAND, mathExecutor);
		INSN_EXECUTORS.put(IOR, mathExecutor);
		INSN_EXECUTORS.put(LOR, mathExecutor);
		INSN_EXECUTORS.put(IXOR, mathExecutor);
		INSN_EXECUTORS.put(LXOR, mathExecutor);
		INSN_EXECUTORS.put(IINC, new IincExecutor());
		ConversionExecutor conversionExecutor = new ConversionExecutor();
		INSN_EXECUTORS.put(I2L, conversionExecutor);
		INSN_EXECUTORS.put(I2F, conversionExecutor);
		INSN_EXECUTORS.put(I2D, conversionExecutor);
		INSN_EXECUTORS.put(L2I, conversionExecutor);
		INSN_EXECUTORS.put(L2F, conversionExecutor);
		INSN_EXECUTORS.put(L2D, conversionExecutor);
		INSN_EXECUTORS.put(F2I, conversionExecutor);
		INSN_EXECUTORS.put(F2L, conversionExecutor);
		INSN_EXECUTORS.put(F2D, conversionExecutor);
		INSN_EXECUTORS.put(D2I, conversionExecutor);
		INSN_EXECUTORS.put(D2L, conversionExecutor);
		INSN_EXECUTORS.put(D2F, conversionExecutor);
		INSN_EXECUTORS.put(I2B, conversionExecutor);
		INSN_EXECUTORS.put(I2C, conversionExecutor);
		INSN_EXECUTORS.put(I2S, conversionExecutor);
		INSN_EXECUTORS.put(LCMP, mathExecutor);
		INSN_EXECUTORS.put(FCMPL, mathExecutor);
		INSN_EXECUTORS.put(FCMPG, mathExecutor);
		INSN_EXECUTORS.put(DCMPL, mathExecutor);
		INSN_EXECUTORS.put(DCMPG, mathExecutor);
		JumpExecutor jumpExecutor = new JumpExecutor();
		INSN_EXECUTORS.put(IFEQ, jumpExecutor);
		INSN_EXECUTORS.put(IFNE, jumpExecutor);
		INSN_EXECUTORS.put(IFLT, jumpExecutor);
		INSN_EXECUTORS.put(IFGE, jumpExecutor);
		INSN_EXECUTORS.put(IFGT, jumpExecutor);
		INSN_EXECUTORS.put(IFLE, jumpExecutor);
		INSN_EXECUTORS.put(IF_ICMPEQ, jumpExecutor);
		INSN_EXECUTORS.put(IF_ICMPNE, jumpExecutor);
		INSN_EXECUTORS.put(IF_ICMPLT, jumpExecutor);
		INSN_EXECUTORS.put(IF_ICMPGE, jumpExecutor);
		INSN_EXECUTORS.put(IF_ICMPGT, jumpExecutor);
		INSN_EXECUTORS.put(IF_ICMPLE, jumpExecutor);
		INSN_EXECUTORS.put(IF_ACMPEQ, jumpExecutor);
		INSN_EXECUTORS.put(IF_ACMPNE, jumpExecutor);
		INSN_EXECUTORS.put(GOTO, nopExecutor);
		UnsupportedExecutor unsupportedExecutor = new UnsupportedExecutor();
		INSN_EXECUTORS.put(JSR, unsupportedExecutor);
		INSN_EXECUTORS.put(RET, unsupportedExecutor);
		SwitchExecutor switchExecutor = new SwitchExecutor();
		INSN_EXECUTORS.put(TABLESWITCH, switchExecutor);
		INSN_EXECUTORS.put(LOOKUPSWITCH, switchExecutor);
		ReturnExecutor returnExecutor = new ReturnExecutor();
		INSN_EXECUTORS.put(IRETURN, returnExecutor);
		INSN_EXECUTORS.put(LRETURN, returnExecutor);
		INSN_EXECUTORS.put(FRETURN, returnExecutor);
		INSN_EXECUTORS.put(DRETURN, returnExecutor);
		INSN_EXECUTORS.put(ARETURN, returnExecutor);
		INSN_EXECUTORS.put(RETURN, returnExecutor);
		FieldExecutor fieldExecutor = new FieldExecutor();
		INSN_EXECUTORS.put(GETSTATIC, fieldExecutor);
		INSN_EXECUTORS.put(PUTSTATIC, fieldExecutor);
		INSN_EXECUTORS.put(GETFIELD, fieldExecutor);
		INSN_EXECUTORS.put(PUTFIELD, fieldExecutor);
		InvokeExecutor invokeExecutor = new InvokeExecutor();
		INSN_EXECUTORS.put(INVOKEVIRTUAL, invokeExecutor);
		INSN_EXECUTORS.put(INVOKESPECIAL, invokeExecutor);
		INSN_EXECUTORS.put(INVOKESTATIC, invokeExecutor);
		INSN_EXECUTORS.put(INVOKEINTERFACE, invokeExecutor);
		INSN_EXECUTORS.put(INVOKEDYNAMIC, new InvokeDynamicExecutor());
		INSN_EXECUTORS.put(NEW, new NewExecutor());
		INSN_EXECUTORS.put(NEWARRAY, new NewArrayExecutor());
		INSN_EXECUTORS.put(ANEWARRAY, new NewObjectArrayExecutor());
		INSN_EXECUTORS.put(ARRAYLENGTH, new ArrayLengthExecutor());
		INSN_EXECUTORS.put(ATHROW, new ThrowExecutor());
		INSN_EXECUTORS.put(CHECKCAST, new CheckcastExecutor());
		INSN_EXECUTORS.put(INSTANCEOF, new InstanceofExecutor());
		MonitorExecutor monitorExecutor = new MonitorExecutor();
		INSN_EXECUTORS.put(MONITORENTER, monitorExecutor);
		INSN_EXECUTORS.put(MONITOREXIT, monitorExecutor);
		INSN_EXECUTORS.put(MULTIANEWARRAY, new MultiNewArrayExecutor());
		INSN_EXECUTORS.put(IFNULL, jumpExecutor);
		INSN_EXECUTORS.put(IFNONNULL, jumpExecutor);
	}
}
