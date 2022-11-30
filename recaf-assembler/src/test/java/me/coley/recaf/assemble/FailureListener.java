package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.pipeline.AstValidationListener;
import me.coley.recaf.assemble.pipeline.BytecodeFailureListener;
import me.coley.recaf.assemble.pipeline.BytecodeValidationListener;
import me.coley.recaf.assemble.pipeline.ParserFailureListener;
import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.Validator;
import me.darknet.assembler.exceptions.AssemblerException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

public class FailureListener implements AstValidationListener, ParserFailureListener,
		BytecodeFailureListener, BytecodeValidationListener {
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

	@Override
	public void onBytecodeValidationBegin(Object object) {
		// no-op
	}

	@Override
	public void onBytecodeValidationComplete(Object object, Validator<?> validator) {
		boolean failed = false;
		for (ValidationMessage message : validator.getMessages()) {
			System.err.println(message);
			failed = true;
		}
		assertFalse(failed);
	}
}
