package me.coley.recaf.parse.bytecode;

import me.coley.analysis.SimInterpreter;
import me.coley.analysis.value.AbstractValue;
import me.coley.recaf.parse.bytecode.exception.VerifierException;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.*;

/**
 * Basic method analyzer.
 *
 * @author Matt
 */
class MethodVerifier extends MethodAnalyzer {
	private final String currentType;
	private final MethodAssembler assembler;

	/**
	 * Create the verifier.
	 *
	 * @param assembler
	 * 		Assembler responsible assembling the method.
	 * @param currentType
	 * 		Declaring type of method to be analyzed.
	 */
	MethodVerifier(MethodAssembler assembler, String currentType) {
		super(new SimInterpreter());
		this.currentType = currentType;
		this.assembler = assembler;
		this.setSkipDeadCodeBlocks(false);
	}

	Frame<AbstractValue>[] verify(MethodNode method) throws VerifierException {
		try {
			return analyze(currentType, method);
		} catch(AnalyzerException ex) {
			// Thrown on verify failure.
			int line = assembler.getLine(ex.node);
			if (line == -1) {
				// This is an ugly hack, but sometimes ASM does not include "ex.node" even when the
				// damn insn index is known, meaning it TOTALLY can provide it....
				String errMessage = ex.getMessage();
				if (errMessage != null) {
					int msgIndex = errMessage.indexOf("Error at instruction ");
					if (msgIndex > 0) {
						msgIndex += "Error at instruction ".length();
						String numStr = errMessage.substring(msgIndex, errMessage.indexOf(':', msgIndex));
						if (numStr.matches("\\d+")) {
							int insnIndex = Integer.parseInt(numStr);
							line = assembler.getLine(method.instructions.get(insnIndex));
						}
					}
				}
			}
			throw new VerifierException(ex,
					"Verification failed on line: " + line + "\n" + ex.getMessage(), line);
		} catch(Exception ex) {
			// IndexOutOfBoundsException: When local variables are messed up
			// Exception: ?
			throw new VerifierException(ex, "Verifier crashed: (" + ex.getClass().getSimpleName() + ") "
					+ ex.getMessage(), -1);
		}
	}
}
