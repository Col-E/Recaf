package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.pipeline.AstValidationListener;
import me.coley.recaf.assemble.pipeline.BytecodeFailureListener;
import me.coley.recaf.assemble.pipeline.ParserFailureListener;
import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.Validator;
import me.coley.recaf.assemble.validation.ast.AstValidator;
import me.darknet.assembler.parser.AssemblerException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

public class FailureListener implements AstValidationListener, ParserFailureListener, BytecodeFailureListener {
	@Override
	public void onValidationFailure(Object object, BytecodeException ex) {
		fail("Validation failure", ex);
	}

	@Override
	public void onCompileFailure(Unit unit, MethodCompileException ex) {
		fail("Compile failure", ex);
	}

	@Override
	public void onParseFail(AssemblerException ex) {
		fail("Parse failure", ex);
	}

	@Override
	public void onParserTransformFail(AssemblerException ex) {
		fail("Parse transform failure", ex);
	}

	@Override
	public void onAstValidationError(AstException ex) {
		fail("Validation failure", ex);
	}

	@Override
	public void onAstValidationBegin(Unit unit) {
		// no-op
	}

	@Override
	public void onAstValidationComplete(Unit unit, Validator<?> validator) {
		boolean failed = false;
		for (ValidationMessage message : validator.getMessages()) {
			System.err.println(message);
			failed = true;
		}
		assertFalse(failed);
	}
}
