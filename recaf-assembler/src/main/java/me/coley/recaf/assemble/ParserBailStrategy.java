package me.coley.recaf.assemble;

import org.antlr.v4.runtime.*;

/**
 * Modified bail strategy to use {@link ParserException}.
 *
 * @author Matt Coley
 */
public class ParserBailStrategy extends BailErrorStrategy {
	@Override
	public void recover(Parser recognizer, RecognitionException e) {
		throw new ParserException((ParserRuleContext) e.getCtx(), e.getMessage());
	}

	@Override
	public Token recoverInline(Parser recognizer) throws RecognitionException {
		throw new ParserException(recognizer.getContext(), "Encountered unmatched token sequence");
	}
}