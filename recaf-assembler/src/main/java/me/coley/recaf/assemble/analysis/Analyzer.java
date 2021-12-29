package me.coley.recaf.assemble.analysis;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.IllegalAstException;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.FlowControl;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.TryCatch;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.ast.meta.Label;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Objects;

/**
 * A simpler stack analysis tool for methods defined by {@link Unit}.
 *
 * @author Matt Coley
 */
public class Analyzer {
	private final String selfType;
	private final Unit unit;
	private final Code code;

	/**
	 * @param selfType
	 * 		Internal name of class defining the method.
	 * @param unit
	 * 		The unit containing the method code.
	 */
	public Analyzer(String selfType, Unit unit) {
		this.selfType = selfType;
		this.unit = unit;
		code = Objects.requireNonNull(unit.getCode(), "AST Unit must define a code region!");
	}

	/**
	 * @return Wrapper of analysis information.
	 *
	 * @throws AstException
	 * 		When the AST cannot resolve all required references <i>(missing label, etc)</i>.
	 */
	public Analysis analyze() throws AstException {
		List<AbstractInstruction> instructions = code.getChildrenOfType(AbstractInstruction.class);
		Analysis analysis = new Analysis(instructions.size());
		fillBlocks(analysis, instructions);
		fillFrames(analysis, instructions);
		return analysis;
	}

	private void fillFrames(Analysis analysis, List<AbstractInstruction> instructions) throws AstException {
		// Initialize with method definition parameters
		Frame entryFrame = analysis.frame(0);
		entryFrame.initialize(selfType, (MethodDefinition) unit.getDefinition());
		// Visit the handler block of all try-catches
		for (TryCatch tryCatch : code.getTryCatches()) {
			Label handlerLabel = code.getLabel(tryCatch.getHandlerLabel());
			if (handlerLabel == null)
				throw new IllegalAstException(tryCatch, "No associated handler label");
			int handlerIndex = instructions.indexOf(handlerLabel);
			branch(analysis, instructions, Integer.MIN_VALUE, handlerIndex);
		}
		// Visit each instruction
		branch(analysis, instructions, -1, 0);
	}

	private void branch(Analysis analysis, List<AbstractInstruction> instructions, int ctxPc, int initialPc) throws AstException {
		int maxPc = instructions.size();
		int pc = initialPc;
		while (pc < maxPc) {
			AbstractInstruction instruction = instructions.get(pc);
			if (execute(analysis, instructions, ctxPc, pc, instruction)) {
				ctxPc = pc;
				pc++;
			} else {
				break;
			}
		}
	}

	private boolean execute(Analysis analysis, List<AbstractInstruction> instructions,
							int ctxPc, int pc, AbstractInstruction instruction) throws AstException {
		Frame frame = analysis.frame(pc);
		// Merge from prior frame
		if (ctxPc >= 0) {
			Frame priorFrame = analysis.frame(ctxPc);
			boolean modified = frame.merge(priorFrame, this);
			if (frame.isVisited() && !modified) {
				// TODO: Need to merge with all following frames (including through flow)
			}
		}
		// Do not continue if the frame has already been visited
		if (frame.isVisited())
			return false;
		// Mark as visited, so we don't revisit the frame unintentionally elsewhere
		frame.markVisited();
		// Handle execution/flow control
		boolean continueExec = true;
		if (instruction instanceof FlowControl) {
			FlowControl flow = (FlowControl) instruction;
			for (Label label : flow.getTargets(code.getLabels())) {
				int labelPc = instructions.indexOf(label);
				branch(analysis, instructions, pc, labelPc);
			}
			// Visit next PC if flow is not forced
			// Examples of forced flow:
			//  - GOTO
			//  - TABLE/LOOKUP-SWITCH
			continueExec = !flow.isForced();
		} else {
			// X-RETURN and ATHROW end execution of the current block.
			int op = instruction.getOpcodeVal();
			if (op >= Opcodes.IRETURN && op <= Opcodes.RETURN)
				continueExec = false;
			else if (op == Opcodes.ATHROW)
				continueExec = false;
		}
		// Handle stack
		if (instruction instanceof Expression) {
			// TODO: Interpret expression.
			//  - Since expressions are self contained, the stack before and after should be the same.
			//  - Local variables may change though...

			// TODO: When the PC hits an expression, jump into the converted AST
			//  - ExpressionToAstTransformer
		} else {
			// TODO: Push and pop values accordingly
		}
		// Only continue if needed
		return continueExec;
	}

	private void fillBlocks(Analysis analysis, List<AbstractInstruction> instructions) throws IllegalAstException {
		// Create the first block
		Frame entryFrame = analysis.frame(0);
		Block entryBlock = new Block();
		entryBlock.add(instructions.get(0), entryFrame);
		analysis.addBlock(0, entryBlock);
		// Create new blocks from try-catch handlers
		for (TryCatch tryCatch : code.getTryCatches()) {
			Label handlerLabel = code.getLabel(tryCatch.getHandlerLabel());
			if (handlerLabel == null)
				throw new IllegalAstException(tryCatch, "No associated handler label");
			int handlerIndex = instructions.indexOf(handlerLabel);
			if (!analysis.isBlockStart(handlerIndex)) {
				Frame handlerFrame = analysis.frame(handlerIndex);
				Block handlerBlock = new Block();
				handlerBlock.add(handlerLabel, handlerFrame);
				analysis.addBlock(handlerIndex, handlerBlock);
			}
		}
		// Create new blocks from instructions
		for (int insnIndex = 0; insnIndex < instructions.size(); insnIndex++) {
			AbstractInstruction instruction = instructions.get(insnIndex);
			// The branch-taken/branch-not-taken elements begin new blocks
			//  - conditionals/switches/etc
			if (instruction instanceof FlowControl) {
				FlowControl flow = (FlowControl) instruction;
				List<Label> targets = flow.getTargets(code.getLabels());
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
				int nextIndex = insnIndex + 1;
				AbstractInstruction next = instructions.get(nextIndex);
				if (!analysis.isBlockStart(nextIndex)) {
					Frame nextFrame = analysis.frame(nextIndex);
					Block targetBlock = new Block();
					targetBlock.add(next, nextFrame);
					analysis.addBlock(nextIndex, targetBlock);
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
				throw new IllegalAstException(tryCatch, "No associated start label");
			if (endLabel == null)
				throw new IllegalAstException(tryCatch, "No associated end label");
			int startIndex = instructions.indexOf(startLabel);
			int endIndex = instructions.indexOf(endLabel);
			int handlerIndex = instructions.indexOf(handlerLabel);
			Block handlerBlock = analysis.block(handlerIndex);
			for (int i = startIndex; i < endIndex; i++) {
				Block block = analysis.blockFloot(i);
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
	}
}
