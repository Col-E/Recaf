package me.coley.recaf.assemble.analysis;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.IllegalAstException;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.CodeEntry;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.TryCatch;
import me.coley.recaf.assemble.ast.meta.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Analyzer {
	private final String selfType;
	private final Unit unit;
	private final Code code;

	public Analyzer(String selfType, Unit unit) {
		this.selfType = selfType;
		this.unit = unit;
		code = Objects.requireNonNull(unit.getCode(), "AST Unit must define a code region!");
	}

	public List<Frame> analyze() throws AstException {
		List<CodeEntry> codeEntries = code.getEntries();
		List<Frame> frames = newFrames(codeEntries.size());
		// For each try-catch block, ensure the contained range can flow into the handler block.
		for (TryCatch tryCatch : code.getTryCatches()) {
			Label startLabel = code.getLabel(tryCatch.getStartLabel());
			Label endLabel = code.getLabel(tryCatch.getEndLabel());
			Label handlerLabel = code.getLabel(tryCatch.getHandlerLabel());
			if (startLabel == null)
				throw new IllegalAstException(tryCatch, "No associated start label");
			if (endLabel == null)
				throw new IllegalAstException(tryCatch, "No associated end label");
			if (handlerLabel == null)
				throw new IllegalAstException(tryCatch, "No associated handler label");
			int startIndex = codeEntries.indexOf(startLabel);
			int endIndex = codeEntries.indexOf(endLabel);
			int handlerIndex = codeEntries.indexOf(handlerLabel);
			Frame handlerFrame = frames.get(handlerIndex);
			for (int i = startIndex; i < endIndex; ++i) {
				Frame origin = frames.get(i);
				origin.addHandlerEdge(handlerFrame);
			}
		}
		// Initialize with method definition parameters
		frames.get(0).initialize(selfType, (MethodDefinition) unit.getDefinition());
		// Visit each instruction
		int pc = 0;

		// TODO: Is there a way to simplify how ASM does it?
		//  - numInstructionsToProcess[numInstructionsToProcess]
		//  - inInstructionsToProcess[insnIndex] = true/false

		// See: https://www.eclemma.org/jacoco/trunk/doc/flow.html
		//      https://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.35.7827&rep=rep1&type=pdf

		// TODO: When the PC hits an expression, jump into the converted AST
		//  - ExpressionToAstTransformer

		return frames;
	}

	private static List<Frame> newFrames(int size) {
		List<Frame> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
			list.add(new Frame());
		return list;
	}
}
