package me.coley.recaf.parse.assembly;

import me.coley.recaf.parse.assembly.visitors.AssemblyVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.*;

/**
 * Assembly verifier.
 *
 * @author Matt
 */
public class AssemblyVerifier extends Analyzer<BasicValue> {
	private final AssemblyVisitor asm;

	/**
	 * @param asm
	 * 		Parent assembler with content to analyze.
	 */
	public AssemblyVerifier(AssemblyVisitor asm) {
		super(new BasicVerifier());
		this.asm = asm;
	}

	/**
	 * Run the generated method through a basic verifier.
	 *
	 * @throws VerifyException
	 * 		When the code failed to pass verification.
	 */
	public void verify() throws VerifyException {
		MethodNode method = asm.getMethod();
		try {
			// We "could" analyze the stack beforehand.... Nah
			method.maxStack = 0xFF;
			// Run analysis
			analyze("Assembled", method);
		} catch(AnalyzerException ex) {
			// Thrown on failure.
			int line = asm.getLine(ex.node);
			throw new VerifyException(ex,
					"Verification failed on line: " + line + "\n" + ex.getMessage());
		} catch(IndexOutOfBoundsException ex) {
			// Thrown when local variables are messed up.
			throw new VerifyException(ex, null);
		} catch(Exception ex) {
			// Unknown error
			throw new VerifyException(ex, "Unknown error");
		}
	}
}
