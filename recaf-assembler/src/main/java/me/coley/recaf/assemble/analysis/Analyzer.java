package me.coley.recaf.assemble.analysis;

import me.coley.recaf.assemble.AnalysisException;
import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.IllegalAstException;
import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.FlowControl;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.TryCatch;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.assemble.transformer.ExpressionToAstTransformer;
import me.coley.recaf.assemble.util.InheritanceChecker;
import me.coley.recaf.assemble.util.ReflectiveInheritanceChecker;
import me.coley.recaf.util.logging.DebuggingLogger;
import me.coley.recaf.util.logging.Logging;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * A simpler stack analysis tool for methods defined by {@link Unit}.
 *
 * @author Matt Coley
 */
public class Analyzer {
	private static final DebuggingLogger logger = Logging.get(Analyzer.class);
	private final String selfType;
	private final MethodDefinition method;
	private final Code code;
	private ExpressionToAstTransformer expressionToAstTransformer;
	private InheritanceChecker inheritanceChecker = ReflectiveInheritanceChecker.getInstance();

	/**
	 * @param selfType
	 * 		Internal name of class defining the method.
	 * @param method
	 * 		The method definition.
	 */
	public Analyzer(String selfType, MethodDefinition method) {
		this(selfType, method, method.getCode());
	}

	/**
	 * @param selfType
	 * 		Internal name of class defining the method.
	 * @param method
	 * 		The method definition.
	 * @param code
	 * 		The code to analyze.
	 * 		Typically {@link MethodDefinition#getCode()} but can be different for things like {@link Expression} parsing.
	 */
	public Analyzer(String selfType, MethodDefinition method, Code code) {
		this.selfType = selfType;
		this.method = method;
		this.code = code;
	}

	/**
	 * @param expressionToAstTransformer
	 * 		Transformer to convert expressions into AST.
	 * 		This allows expressions to be properly analyzed.
	 */
	public void setExpressionToAstTransformer(ExpressionToAstTransformer expressionToAstTransformer) {
		this.expressionToAstTransformer = expressionToAstTransformer;
	}

	/**
	 * @return Lookup for child-parent relations between classes.
	 */
	public InheritanceChecker getInheritanceChecker() {
		return inheritanceChecker;
	}

	/**
	 * @param inheritanceChecker
	 * 		Lookup for child-parent relations between classes.
	 */
	public void setInheritanceChecker(InheritanceChecker inheritanceChecker) {
		this.inheritanceChecker = inheritanceChecker;
	}

	/**
	 * @return Wrapper of analysis information.
	 *
	 * @throws AstException
	 * 		When the AST cannot resolve all required references <i>(missing label, etc)</i>.
	 */
	public Analysis analyze() throws AstException {
		return analyze(true, true);
	}

	/**
	 * @param block
	 *        {@code true} to generate {@link Analysis#getBlocks()}.
	 * @param frames
	 *        {@code true} to generate {@link Analysis#getFrames()}.
	 *
	 * @return Wrapper of analysis information.
	 *
	 * @throws AstException
	 * 		When the AST cannot resolve all required references <i>(missing label, etc)</i>.
	 */
	public Analysis analyze(boolean block, boolean frames) throws AstException {
		List<AbstractInstruction> instructions = code.getInstructions();
		Analysis analysis = new Analysis(instructions.size());
		try {
			if (!instructions.isEmpty()) {
				if (block)
					fillBlocks(analysis, instructions);
				if (frames)
					fillFrames(analysis);
			}
		} catch (AstException e) {
			throw e;
		} catch (Exception t) {
			logger.error("Uncaught exception during analysis", t);
			throw new MethodCompileException(code, t, "Uncaught exception during analysis!");
		}
		return analysis;
	}

	private void fillFrames(Analysis analysis) throws AnalysisException {
		// Initialize with method definition parameters
		Frame entryFrame = analysis.frame(0);
		entryFrame.initialize(selfType, method);
		// Simulate execution
		CodeExecutor executor = new CodeExecutor();
		executor.setAnalysis(analysis);
		executor.setInheritanceChecker(inheritanceChecker);
		executor.setExpressionToAstTransformer(expressionToAstTransformer);
		executor.configure(code);
		executor.execute();
	}

	private void fillBlocks(Analysis analysis, List<AbstractInstruction> instructions) throws AnalysisException {
		// Create the first block
		Frame entryFrame = analysis.frame(0);
		Block entryBlock = new Block();
		entryBlock.add(instructions.get(0), entryFrame);
		analysis.addBlock(0, entryBlock);
		// Create new blocks from try-catch handlers
		for (TryCatch tryCatch : code.getTryCatches()) {
			Label handlerLabel = code.getLabel(tryCatch.getHandlerLabel());
			if (handlerLabel == null)
				throw new AnalysisException(tryCatch, "No associated handler label");
			int handlerIndex = instructions.indexOf(handlerLabel);
			if (!analysis.isBlockStart(handlerIndex)) {
				Frame handlerFrame = analysis.frame(handlerIndex);
				Block handlerBlock = new Block();
				handlerBlock.add(handlerLabel, handlerFrame);
				analysis.addBlock(handlerIndex, handlerBlock);
			}
		}
		// Create new blocks from instructions
		int maxInsnIndex = instructions.size() - 1;
		for (int insnIndex = 0; insnIndex <= maxInsnIndex; insnIndex++) {
			AbstractInstruction instruction = instructions.get(insnIndex);
			// The branch-taken/branch-not-taken elements begin new blocks
			//  - conditionals/switches/etc
			if (instruction instanceof FlowControl) {
				FlowControl flow = (FlowControl) instruction;
				List<Label> targets;
				try {
					targets = flow.getTargets(code.getLabels());
				} catch (IllegalAstException ex) {
					throw new AnalysisException(flow, ex,"Failed to lookup flow targets");
				}
				// Branch taken
				for (Label target : targets) {
					int targetIndex = instructions.indexOf(target);
					Frame targetFrame = analysis.frame(targetIndex);
					if (!analysis.isBlockStart(targetIndex)) {
						Block targetBlock = new Block();
						targetBlock.add(target, targetFrame);
						analysis.addBlock(targetIndex, targetBlock);
					}
				}
				// Branch not taken
				if (!flow.isForced() && insnIndex < maxInsnIndex) {
					int nextIndex = insnIndex + 1;
					AbstractInstruction next = instructions.get(nextIndex);
					if (!analysis.isBlockStart(nextIndex)) {
						Frame nextFrame = analysis.frame(nextIndex);
						Block targetBlock = new Block();
						targetBlock.add(next, nextFrame);
						analysis.addBlock(nextIndex, targetBlock);
					}
				}
			} else {
				// Instructions after return statements are the last sources of new blocks
				int op = instruction.getOpcodeVal();
				if (op >= Opcodes.IRETURN && op <= Opcodes.RETURN) {
					int nextIndex = insnIndex + 1;
					if (nextIndex >= instructions.size())
						continue;
					AbstractInstruction next = instructions.get(nextIndex);
					if (!analysis.isBlockStart(nextIndex)) {
						Frame nextFrame = analysis.frame(nextIndex);
						Block targetBlock = new Block();
						targetBlock.add(next, nextFrame);
						analysis.addBlock(nextIndex, targetBlock);
					}
				}
			}
		}
		// Ensure all blocks inside a 'try' range flow into the 'catch' handler.
		for (TryCatch tryCatch : code.getTryCatches()) {
			Label startLabel = code.getLabel(tryCatch.getStartLabel());
			Label endLabel = code.getLabel(tryCatch.getEndLabel());
			Label handlerLabel = code.getLabel(tryCatch.getHandlerLabel());
			if (startLabel == null)
				throw new AnalysisException(tryCatch, "No associated start label");
			if (endLabel == null)
				throw new AnalysisException(tryCatch, "No associated end label");
			int startIndex = instructions.indexOf(startLabel);
			int endIndex = instructions.indexOf(endLabel);
			int handlerIndex = instructions.indexOf(handlerLabel);
			Block handlerBlock = analysis.block(handlerIndex);
			for (int i = startIndex; i < endIndex; i++) {
				Block block = analysis.blockFloor(i);
				block.addHandlerEdge(handlerBlock);
			}
		}
		// Fill in block's instructions so consecutive instructions belong to the same block
		Block block = analysis.block(0);
		for (int insnIndex = 1; insnIndex < instructions.size(); insnIndex++) {
			if (analysis.isBlockStart(insnIndex)) {
				block = analysis.block(insnIndex);
			} else {
				AbstractInstruction instruction = instructions.get(insnIndex);
				Frame frame = analysis.frame(insnIndex);
				block.add(instruction, frame);
			}
		}
		// Populate edges between blocks
		for (int insnIndex = 0; insnIndex <= maxInsnIndex; insnIndex++) {
			AbstractInstruction instruction = instructions.get(insnIndex);
			Block blockCurrent = analysis.blockFloor(insnIndex);
			if (instruction instanceof FlowControl) {
				FlowControl flow = (FlowControl) instruction;
				List<Label> targets;
				try {
					targets = flow.getTargets(code.getLabels());
				} catch (IllegalAstException ex) {
					throw new AnalysisException(flow, ex, "Failed to lookup flow targets");
				}
				// Branch taken
				for (Label target : targets) {
					int targetIndex = instructions.indexOf(target);
					Block blockTarget = analysis.blockFloor(targetIndex);
					blockCurrent.addJumpEdge(blockTarget);
				}
				// Branch not taken
				if (!flow.isForced() && insnIndex < maxInsnIndex) {
					int nextIndex = insnIndex + 1;
					Block blockTarget = analysis.blockFloor(nextIndex);
					blockCurrent.addJumpEdge(blockTarget);
				}
			} else {
				// Any non-return instruction should flow to the next block.
				int op = instruction.getOpcodeVal();
				if ((op < Opcodes.IRETURN || op > Opcodes.RETURN)) {
					Block blockTarget = analysis.blockFloor(insnIndex + 1);
					if (blockCurrent != blockTarget)
						blockCurrent.addJumpEdge(blockTarget);
				}

			}
		}
	}
}
